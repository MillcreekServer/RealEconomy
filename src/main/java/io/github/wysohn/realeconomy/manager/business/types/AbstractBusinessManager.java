package io.github.wysohn.realeconomy.manager.business.types;

import com.google.inject.Injector;
import io.github.wysohn.rapidframework3.core.caching.AbstractManagerElementCaching;
import io.github.wysohn.rapidframework3.core.main.ManagerConfig;
import io.github.wysohn.rapidframework3.interfaces.plugin.IShutdownHandle;
import io.github.wysohn.rapidframework3.interfaces.serialize.ISerializer;
import io.github.wysohn.rapidframework3.interfaces.serialize.ITypeAsserter;
import io.github.wysohn.rapidframework3.interfaces.store.IKeyValueStorage;
import io.github.wysohn.rapidframework3.utils.Validation;
import io.github.wysohn.realeconomy.interfaces.business.IBusiness;
import io.github.wysohn.realeconomy.interfaces.business.IBusinessContextHandler;
import io.github.wysohn.realeconomy.interfaces.business.IBusinessProvider;
import io.github.wysohn.realeconomy.interfaces.business.tiers.ITier;
import io.github.wysohn.realeconomy.manager.asset.signature.AssetSignature;
import io.github.wysohn.realeconomy.manager.business.tiers.TierAdapter;
import io.github.wysohn.realeconomy.manager.business.tiers.TierRegistry;
import io.github.wysohn.realeconomy.manager.listing.AssetListingManager;
import io.github.wysohn.realeconomy.mediator.BusinessMediator;

import java.io.File;
import java.lang.ref.Reference;
import java.util.*;
import java.util.function.Consumer;
import java.util.logging.Logger;

public abstract class AbstractBusinessManager<V extends AbstractBusiness>
        extends AbstractManagerElementCaching<UUID, V>
        implements IBusinessProvider {

    private final AssetListingManager listingManager;
    protected final IBusinessContextHandler visitStateProvider;

    public AbstractBusinessManager(String pluginName,
                                   Logger logger,
                                   ManagerConfig config,
                                   File pluginDir,
                                   IShutdownHandle shutdownHandle,
                                   ISerializer serializer,
                                   ITypeAsserter asserter,
                                   Injector injector,
                                   Class<V> type,
                                   AssetListingManager listingManager,
                                   IBusinessContextHandler visitStateProvider) {
        super(pluginName, logger, config, new File(pluginDir, "business"), shutdownHandle, serializer, asserter, injector, type);
        this.listingManager = listingManager;
        this.visitStateProvider = visitStateProvider;

        dependsOn(AssetListingManager.class);

        BusinessMediator.registerBusinessProvider(this);
    }

    @Override
    protected UUID fromString(String s) {
        return UUID.fromString(s);
    }

    @Override
    public IBusiness getBusiness(UUID uuid) {
        return get(uuid)
                .map(Reference::get)
                .orElse(null);
    }

    @Override
    public Set<UUID> keys() {
        return keySet();
    }

    @Override
    public void enable() throws Exception {
        super.enable();

        DefaultConfigBuilder builder = new DefaultConfigBuilder(BusinessMediator.getTierConfigs(), listingManager);
        addDefaultConfig(builder);
        builder.updateConfig();
    }

    /**
     * Open a new business of the type defined in {@link #getTierName()}.
     *
     * @param ownerUuid UUID of the owner
     * @param subType   sub-type of the business.
     * @return newly created business
     */
    @Override
    public V openNewBusiness(UUID ownerUuid, String subType) {
        Validation.assertNotNull(ownerUuid);
        Validation.assertNotNull(subType);

        ITier tier = TierRegistry.fromString(getTierName());
        if (tier == null)
            throw new RuntimeException("tier " + getTierName() + " does not exist in the config!");

        if (!tier.verifySubType(subType))
            throw new RuntimeException("tier " + getTierName() + " does not have subType " + subType + " in the config!");

        UUID uuid = UUID.randomUUID();
        getOrNew(uuid)
                .map(Reference::get)
                .ifPresent(business -> {
                    business.setOwnerUuid(ownerUuid);
                    business.replaceTier(tier);
                });

        return get(uuid).map(Reference::get).orElseThrow(RuntimeException::new);
    }

    @Override
    public boolean deleteBusiness(IBusiness business) {
        if (!get(business.getUuid()).isPresent())
            return false;

        delete(business.getUuid());
        return true;
    }

    /**
     * Out of all visiting businesses, filter all businesses with the type that matches
     * with this business manager.
     *
     * @param playerUuid target player to query for
     * @param consumer   consumer for the found businesses
     */
    protected void forEachApplicableBusiness(UUID playerUuid, Consumer<V> consumer) {
        Set<UUID> visiting = visitStateProvider.getUsingBusiness(playerUuid);
        visiting.stream()
                .map(this::get)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .map(Reference::get)
                .filter(Objects::nonNull)
                .forEach(consumer);
    }

    protected abstract void addDefaultConfig(DefaultConfigBuilder defaultConfigBuilder);

    protected class DefaultConfigBuilder {
        private final IKeyValueStorage tierConfigs;
        private final AssetListingManager listingManager;

        private String name;
        private final Map<String, Double> requirement = new HashMap<>();
        private final Map<String, Double> input = new HashMap<>();
        private final Map<String, Double> output = new HashMap<>();
        private long timeToLiveMin = -1L;
        private long timeToLiveMax = -1L;

        public DefaultConfigBuilder(IKeyValueStorage tierConfigs,
                                    AssetListingManager listingManager) {
            this.tierConfigs = tierConfigs;
            this.listingManager = listingManager;
        }

        public DefaultConfigBuilder name(String name) {
            this.name = name;
            return this;
        }

        public DefaultConfigBuilder putRequirement(AssetSignature signature, double amount) {
            listingManager.newListing(signature);
            requirement.put(listingManager.fromSignature(signature).getKey().toString(), amount);
            return this;
        }

        public DefaultConfigBuilder putInput(AssetSignature signature, double amount) {
            listingManager.newListing(signature);
            input.put(listingManager.fromSignature(signature).getKey().toString(), amount);
            return this;
        }

        public DefaultConfigBuilder putOutput(AssetSignature signature, double amount) {
            listingManager.newListing(signature);
            output.put(listingManager.fromSignature(signature).getKey().toString(), amount);
            return this;
        }

        public DefaultConfigBuilder setTimeToLiveMin(long timeToLiveMin) {
            this.timeToLiveMin = timeToLiveMin;
            return this;
        }

        public DefaultConfigBuilder setTimeToLiveMax(long timeToLiveMax) {
            this.timeToLiveMax = timeToLiveMax;
            return this;
        }

        private void updateConfig() {
            Validation.assertNotNull(name);
            if (tierConfigs.get(name + "." + ITier.DEFAULT_SUB_TYPE).isPresent())
                return;

            tierConfigs.put(name + "." + ITier.DEFAULT_SUB_TYPE + "." + TierAdapter.REQUIREMENT, requirement);
            tierConfigs.put(name + "." + ITier.DEFAULT_SUB_TYPE + "." + TierAdapter.INPUT, input);
            tierConfigs.put(name + "." + ITier.DEFAULT_SUB_TYPE + "." + TierAdapter.OUTPUT, output);
            tierConfigs.put(name + "." + ITier.DEFAULT_SUB_TYPE + "." + TierAdapter.TIME_TO_LIVE_MIN, timeToLiveMin);
            tierConfigs.put(name + "." + ITier.DEFAULT_SUB_TYPE + "." + TierAdapter.TIME_TO_LIVE_MAX, timeToLiveMax);
        }
    }
}
