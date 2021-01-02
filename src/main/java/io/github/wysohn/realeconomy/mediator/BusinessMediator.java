package io.github.wysohn.realeconomy.mediator;

import io.github.wysohn.rapidframework3.bukkit.data.BukkitPlayer;
import io.github.wysohn.rapidframework3.core.inject.annotations.PluginDirectory;
import io.github.wysohn.rapidframework3.core.inject.factory.IStorageFactory;
import io.github.wysohn.rapidframework3.core.main.Mediator;
import io.github.wysohn.rapidframework3.data.SimpleChunkLocation;
import io.github.wysohn.rapidframework3.interfaces.store.IKeyValueStorage;
import io.github.wysohn.realeconomy.interfaces.business.IBusiness;
import io.github.wysohn.realeconomy.interfaces.business.IBusinessProvider;
import io.github.wysohn.realeconomy.interfaces.business.IVisitStateProvider;
import io.github.wysohn.realeconomy.manager.business.tiers.TierAdapter;
import io.github.wysohn.realeconomy.manager.business.tiers.TierRegistry;
import io.github.wysohn.realeconomy.manager.business.types.AbstractBusiness;
import io.github.wysohn.realeconomy.manager.claim.ChunkClaimManager;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.File;
import java.util.*;
import java.util.function.Consumer;

@Singleton
public class BusinessMediator extends Mediator {
    public static final String KEY_CHUNK = "key_chunk";

    private static final Map<String, IBusinessProvider> BUSINESSES_PROVIDERS = new HashMap<>();
    private static final Set<IVisitStateProvider> VISIT_STATE_PROVIDERS = new HashSet<>();
    private static IKeyValueStorage tierConfigs;
    private static boolean enabled = false;

    /**
     * Register the IBusinessProvider. Note that all the methods should be thread-safe, so that
     * it can be used in the update thread along with the server thread.
     * <p>
     * Must register before the plugin enables.
     *
     * @param provider provider
     * @throws RuntimeException same provider is registered again or plugin is already enabled.
     */
    public static void registerBusinessProvider(IBusinessProvider provider) {
        if (enabled)
            throw new RuntimeException("Must register before enabled.");

        if (BUSINESSES_PROVIDERS.containsKey(provider.getTierName()))
            throw new RuntimeException("Duplicated provider of tier " + provider.getTierName());

        BUSINESSES_PROVIDERS.put(provider.getTierName(), provider);
    }

    /**
     * Register the IVisitorStateProvider. Note that all the methods should be thread-safe, so that
     * it can be used in the update thread along with the server thread.
     * <p>
     * Must register before the plugin enables.
     *
     * @param provider provider
     * @throws RuntimeException same provider is registered again or plugin is already enabled.
     */
    public static void registerVisitorStateProvider(IVisitStateProvider provider) {
        if (enabled)
            throw new RuntimeException("Must register before enabled.");

        if (VISIT_STATE_PROVIDERS.contains(provider))
            throw new RuntimeException("Duplicated provider " + provider);

        VISIT_STATE_PROVIDERS.add(provider);
    }

    public static IKeyValueStorage getTierConfigs() {
        return tierConfigs;
    }

    //-----------------------------------------------------------------------------------------

    private final ChunkClaimManager chunkClaimManager;

    private final long INTERVAL = 1000L;
    private Timer updateTimer;

    @Inject
    public BusinessMediator(@PluginDirectory File pluginDir,
                            IStorageFactory storageFactory,
                            ChunkClaimManager chunkClaimManager) {
        this.chunkClaimManager = chunkClaimManager;

        tierConfigs = storageFactory.create(pluginDir, "tiers.yml");
    }

    @Override
    public void enable() throws Exception {
        tierConfigs.getKeys(false).forEach(name ->
                TierRegistry.register(new TierAdapter(name, tierConfigs)));

        enabled = true;

        forEach(business -> {
            if (business.hasData(KEY_CHUNK)) {
                SimpleChunkLocation scloc = SimpleChunkLocation.valueOf(business.getData(KEY_CHUNK));
                chunkClaimManager.updateMapping(scloc, business);
            }
        });
    }

    @Override
    public void load() throws Exception {
        if (updateTimer != null)
            updateTimer.cancel();

        BUSINESSES_PROVIDERS.forEach((tier, provider) -> provider.keys().forEach(key ->
                Optional.ofNullable(provider.getBusiness(key))
                        .ifPresent(IBusiness::init)));

        updateTimer = new Timer();
        updateTimer.scheduleAtFixedRate(new UpdateTask(), INTERVAL, INTERVAL);
    }

    @Override
    public void disable() throws Exception {
        if (updateTimer != null)
            updateTimer.cancel();

        BUSINESSES_PROVIDERS.forEach((tier, provider) -> provider.keys().forEach(key ->
                Optional.ofNullable(provider.getBusiness(key))
                        .ifPresent(IBusiness::stop)));

        enabled = false;
    }

    public IBusiness getBusiness(UUID uuid) {
        for (Map.Entry<String, IBusinessProvider> entry : BUSINESSES_PROVIDERS.entrySet()) {
            IBusinessProvider provider = entry.getValue();
            IBusiness business = provider.getBusiness(uuid);
            if (business != null)
                return business;
        }
        return null;
    }

    public Set<UUID> keys() {
        Set<UUID> keys = new HashSet<>();
        BUSINESSES_PROVIDERS.forEach((tier, provider) -> keys.addAll(provider.keys()));
        return keys;
    }

    /**
     * Create a new business. Note that (unless the provider is missing) this will create
     * new instances with random UUID everytime, so return value will be never null. So,
     * to prevent un-trackable business to be created, the caller must keep the UUID of business
     * all the time. If something went wrong, use {@link #deleteBusiness(IBusiness)} to
     * delete it, so the business is not 'ghosted.'
     *
     * @param tier      name of business tier set in the tier config
     * @param ownerUuid business owner uuid
     * @param subType   sub-type of the tier as specified in tier config
     * @return business instance; null if no IBusinessProvider registered with given tier name
     */
    public AbstractBusiness openNewBusiness(String tier, UUID ownerUuid, String subType) {
        if (!BUSINESSES_PROVIDERS.containsKey(tier))
            return null;

        return BUSINESSES_PROVIDERS.get(tier).openNewBusiness(ownerUuid, subType);
    }

    /**
     * @param business
     * @return
     */
    public boolean deleteBusiness(IBusiness business) {
        if (!BUSINESSES_PROVIDERS.containsKey(business.currentTier().name()))
            throw new RuntimeException("business has tier " + business.currentTier().name() + " but its provider" +
                    " is not found. Perhaps it was registered by another plugin, yet the plugin is not loaded?");

        boolean deleteBusiness = BUSINESSES_PROVIDERS.get(business.currentTier().name()).deleteBusiness(business);
        if (deleteBusiness) {
            Optional.of(business)
                    .map(chunkClaimManager::getChunkOfBusiness)
                    .ifPresent(chunk -> {
                        chunkClaimManager.removeMapping(chunk, business);
                        chunkClaimManager.delete(chunk);
                    });
        }
        return deleteBusiness;
    }

    public Set<IBusiness> getUsingBusiness(UUID memberUuid) {
        Set<IBusiness> visiting = new HashSet<>();
        for (IVisitStateProvider visitStateProvider : VISIT_STATE_PROVIDERS) {
            Optional.of(visitStateProvider)
                    .map(provider -> provider.getUsingBusiness(memberUuid))
                    .ifPresent(visiting::addAll);
        }
        return visiting;
    }

    public boolean isMember(IBusiness business, UUID memberUuid) {
        for (IVisitStateProvider visitStateProvider : VISIT_STATE_PROVIDERS) {
            if (visitStateProvider.isMember(business, memberUuid))
                return true;
        }
        return false;
    }

    public void forEach(Consumer<IBusiness> businessConsumer) {
        BUSINESSES_PROVIDERS.forEach((tier, provider) -> provider.keys().forEach(key ->
                businessConsumer.accept(provider.getBusiness(key))));
    }

    public void forEach(String tierName, Consumer<IBusiness> businessConsumer) {
        Optional.ofNullable(BUSINESSES_PROVIDERS.get(tierName))
                .ifPresent(provider -> provider.keys().forEach(key ->
                        businessConsumer.accept(provider.getBusiness(key))));
    }

    /**
     * This is to provide stand-alone support without any region claiming plugins.
     * It will simply use claim as where a business can start, and members are simply
     * stored in the {@link io.github.wysohn.realeconomy.manager.claim.ChunkClaim} as Set
     * of UUIDs.
     *
     * @param tierName name of tier as specified in the tiers config
     * @param subType  sub-type of the tier
     * @param player   player who is going to own the claim where he or she is standing
     * @return the result
     */
    public Result openNewBusinessInChunk(String tierName, String subType, BukkitPlayer player) {
        UUID playerUuid = player.getUuid();
        SimpleChunkLocation chunk = player.getScloc();

        AbstractBusiness business = openNewBusiness(tierName, playerUuid, subType);
        if (business == null)
            return Result.NO_PROVIDER;

        try {
            if (chunkClaimManager.getBusinessFromChunk(chunk) != null)
                return Result.DUP_CHUNK;

            business.putData(KEY_CHUNK, chunk.toString());
            chunkClaimManager.updateMapping(chunk, business);
        } catch (Exception ex) {
            // if something is wrong, delete the business to avoid creating the ghosted businesses.
            deleteBusiness(business);
            ex.printStackTrace();
        }

        return Result.OK;
    }

    private class UpdateTask extends TimerTask {
        @Override
        public void run() {
            BUSINESSES_PROVIDERS.forEach((tier, provider) ->
                    provider.keys().forEach(key -> Optional.ofNullable(provider.getBusiness(key))
                            .ifPresent(IBusiness::init)));
        }
    }

    public enum Result {
        NO_PROVIDER, DUP_CHUNK, OK
    }
}
