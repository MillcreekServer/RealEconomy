package io.github.wysohn.realeconomy.mediator;

import io.github.wysohn.rapidframework3.bukkit.data.BukkitPlayer;
import io.github.wysohn.rapidframework3.core.inject.annotations.PluginDirectory;
import io.github.wysohn.rapidframework3.core.inject.factory.IStorageFactory;
import io.github.wysohn.rapidframework3.core.main.Mediator;
import io.github.wysohn.rapidframework3.data.SimpleLocation;
import io.github.wysohn.rapidframework3.interfaces.store.IKeyValueStorage;
import io.github.wysohn.realeconomy.interfaces.business.IBusiness;
import io.github.wysohn.realeconomy.interfaces.business.IBusinessContextHandler;
import io.github.wysohn.realeconomy.interfaces.business.IBusinessProvider;
import io.github.wysohn.realeconomy.manager.business.tiers.TierAdapter;
import io.github.wysohn.realeconomy.manager.business.tiers.TierRegistry;
import io.github.wysohn.realeconomy.manager.business.types.AbstractBusiness;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.File;
import java.util.*;
import java.util.function.Consumer;

@Singleton
public class BusinessMediator extends Mediator {
    public static final String KEY_LOC = "key_chunk";

    private static final Map<String, IBusinessProvider> BUSINESSES_PROVIDERS = new HashMap<>();
    private static final List<IBusinessContextHandler> BUSINESS_CONTEXT_HANDLERS = new ArrayList<>();
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
     * Register the IBusinessContextHandler. Note that all the methods should be thread-safe, so that
     * it can be used in the update thread along with the server thread.
     * <p>
     * Must register before the plugin enables.
     *
     * @param claimHandler claimHandler
     * @throws RuntimeException same claimHandler is registered again or plugin is already enabled.
     */
    public static void registerClaimHandler(IBusinessContextHandler claimHandler) {
        if (enabled)
            throw new RuntimeException("Must register before enabled.");

        if (BUSINESS_CONTEXT_HANDLERS.contains(claimHandler))
            throw new RuntimeException("Duplicated claimHandler " + claimHandler);

        BUSINESS_CONTEXT_HANDLERS.add(claimHandler);
    }

    public static IKeyValueStorage getTierConfigs() {
        return tierConfigs;
    }

    //-----------------------------------------------------------------------------------------

    private final long OFFER_WAITING_SECS = 180L;
    private final long INTERVAL = 1000L;
    private Timer updateTimer;

    @Inject
    public BusinessMediator(@PluginDirectory File pluginDir,
                            IStorageFactory storageFactory) {
        tierConfigs = storageFactory.create(pluginDir, "tiers.yml");
    }

    @Override
    public void preload() throws Exception {
        tierConfigs.getKeys(false).forEach(name ->
                TierRegistry.register(new TierAdapter(name, tierConfigs)));
    }

    @Override
    public void enable() throws Exception {
        enabled = true;
        BUSINESS_CONTEXT_HANDLERS.sort(Comparator.comparingInt(IBusinessContextHandler::priority));

        forEach(business -> {
            if (business.hasData(KEY_LOC)) {
                SimpleLocation location = SimpleLocation.valueOf(business.getData(KEY_LOC));
                updateMapping(location, business);
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
     * <p>
     * This only creates the business itself, so to make a business that is linked with the specific
     * location, use {@link #openNewBusinessLocation(String, String, BukkitPlayer)} instead.
     * <p>
     * Note that this does not invoke {@link AbstractBusiness#init()}, which loads business information
     * from the config.
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
            BUSINESS_CONTEXT_HANDLERS.forEach(provider -> {
                SimpleLocation location = provider.getLocationOfBusiness(business);
                if (location != null)
                    provider.removeMapping(location, business);
            });
        }
        return deleteBusiness;
    }

    public Set<UUID> getUsingBusiness(UUID memberUuid) {
        Set<UUID> visiting = new HashSet<>();
        for (IBusinessContextHandler claimHandler : BUSINESS_CONTEXT_HANDLERS) {
            Optional.of(claimHandler)
                    .map(provider -> provider.getUsingBusiness(memberUuid))
                    .ifPresent(visiting::addAll);
        }
        return visiting;
    }

    public boolean addMember(IBusiness business, UUID memberUuid) {
        for (IBusinessContextHandler claimHandler : BUSINESS_CONTEXT_HANDLERS) {
            if (claimHandler.addMember(business, memberUuid))
                return true;
        }
        return false;
    }

    public boolean removeMember(IBusiness business, UUID memberUuid) {
        for (IBusinessContextHandler claimHandler : BUSINESS_CONTEXT_HANDLERS) {
            if (claimHandler.removeMember(business, memberUuid))
                return true;
        }
        return false;
    }

    public boolean isMember(IBusiness business, UUID memberUuid) {
        if (Objects.equals(business.getOwnerUuid(), memberUuid))
            return true;

        for (IBusinessContextHandler claimHandler : BUSINESS_CONTEXT_HANDLERS) {
            if (claimHandler.isMember(business, memberUuid))
                return true;
        }
        return false;
    }

    public boolean isInBusiness(IBusiness business, UUID memberUuid) {
        for (IBusinessContextHandler claimHandler : BUSINESS_CONTEXT_HANDLERS) {
            if (claimHandler.isInBusiness(business, memberUuid))
                return true;
        }
        return false;
    }

    public UUID queryBusiness(SimpleLocation location) {
        for (IBusinessContextHandler claimHandler : BUSINESS_CONTEXT_HANDLERS) {
            UUID businessUuid = claimHandler.queryBusiness(location);
            if (businessUuid != null)
                return businessUuid;
        }

        return null;
    }

    public SimpleLocation getLocationOfBusiness(IBusiness business) {
        for (IBusinessContextHandler claimHandler : BUSINESS_CONTEXT_HANDLERS) {
            SimpleLocation location = claimHandler.getLocationOfBusiness(business);
            if (location != null)
                return location;
        }

        return null;
    }

    public boolean updateMapping(SimpleLocation location, IBusiness business) {
        if (BUSINESS_CONTEXT_HANDLERS.size() < 1)
            throw new RuntimeException("No claim handlers found.");

        for (IBusinessContextHandler claimHandler : BUSINESS_CONTEXT_HANDLERS) {
            if (claimHandler.updateMapping(location, business))
                return true;
        }

        return false;
    }

    public boolean removeMapping(SimpleLocation location, IBusiness business) {
        if (BUSINESS_CONTEXT_HANDLERS.size() < 1)
            throw new RuntimeException("No claim handlers found.");

        for (IBusinessContextHandler claimHandler : BUSINESS_CONTEXT_HANDLERS) {
            if (claimHandler.removeMapping(location, business))
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
     * Claim the current location and open a new business. The location here is arbitrary since
     * we don't know what the location will be backed by. For example, the default implementation
     * provided by RealEconomy will consider a 'chunk' as one unit of claim, so once the player
     * claim the location, the entire chunk will belong to the player and the business opened.
     * <p>
     * On the other hand, if plugins like WorldGuard is supported in the future, the unit of claim
     * is not necessarily a 'chunk,' so the meaning of claim will be different.
     *
     * @param tierName name of tier as specified in the tiers config
     * @param subType  sub-type of the tier
     * @param player   player who is going to own the claim where he or she is standing
     * @return the result
     * @throws RuntimeException throws when no claim handler exist.
     */
    public Result openNewBusinessLocation(String tierName, String subType, BukkitPlayer player) {
        UUID playerUuid = player.getUuid();
        SimpleLocation location = player.getSloc();

        AbstractBusiness business = openNewBusiness(tierName, playerUuid, subType);
        if (business == null)
            return Result.NO_PROVIDER;

        try {
            business.init();

            if (queryBusiness(location) != null)
                return Result.DUP_LOCATION;

            business.putData(KEY_LOC, location.toString());
            updateMapping(location, business);
        } catch (Exception ex) {
            // if something is wrong, delete the business to avoid creating the ghosted businesses.
            deleteBusiness(business);
            ex.printStackTrace();
            return Result.UNKNOWN;
        }

        return Result.OK;
    }

    private class UpdateTask extends TimerTask {
        @Override
        public void run() {
            BUSINESSES_PROVIDERS.forEach((tier, provider) ->
                    provider.keys().forEach(key -> Optional.ofNullable(provider.getBusiness(key))
                            .ifPresent(IBusiness::update)));
        }
    }

    public interface InviteResultHandle {
        void handle(InviteResult handle);
    }

    public enum InviteResult {
        ACCEPT, TIMEOUT
    }

    public enum Result {
        NO_PROVIDER, DUP_LOCATION, UNKNOWN, OK
    }
}
