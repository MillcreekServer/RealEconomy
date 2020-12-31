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
import io.github.wysohn.realeconomy.interfaces.business.IBusinessProvider;
import io.github.wysohn.realeconomy.interfaces.business.tiers.ITier;
import io.github.wysohn.realeconomy.manager.asset.signature.AssetSignature;
import io.github.wysohn.realeconomy.manager.business.BusinessManager;
import io.github.wysohn.realeconomy.manager.business.tiers.TierAdapter;
import io.github.wysohn.realeconomy.manager.listing.AssetListingManager;

import java.io.File;
import java.lang.ref.Reference;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Logger;

public abstract class AbstractBusinessManager<V extends AbstractBusiness>
        extends AbstractManagerElementCaching<UUID, V>
        implements IBusinessProvider {

    private final AssetListingManager listingManager;

    public AbstractBusinessManager(String pluginName,
                                   Logger logger,
                                   ManagerConfig config,
                                   File pluginDir,
                                   IShutdownHandle shutdownHandle,
                                   ISerializer serializer,
                                   ITypeAsserter asserter,
                                   Injector injector,
                                   Class<V> type,
                                   AssetListingManager listingManager) {
        super(pluginName, logger, config, new File(pluginDir, "business"), shutdownHandle, serializer, asserter, injector, type);
        this.listingManager = listingManager;

        dependsOn(AssetListingManager.class);

        BusinessManager.registerProvider(this);
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
        DefaultConfigBuilder builder = new DefaultConfigBuilder(BusinessManager.getTierConfigs(), listingManager);
        addDefaultConfig(builder);
        builder.updateConfig();
    }

    protected abstract void addDefaultConfig(DefaultConfigBuilder defaultConfigBuilder);

    protected class DefaultConfigBuilder {
        private final IKeyValueStorage tierConfigs;
        private final AssetListingManager listingManager;

        private String name;
        private final Map<UUID, Double> requirement = new HashMap<>();
        private final Map<UUID, Double> fulfillment = new HashMap<>();
        private final Map<UUID, Double> input = new HashMap<>();
        private final Map<UUID, Double> output = new HashMap<>();
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
            requirement.put(listingManager.fromSignature(signature).getKey(), amount);
            return this;
        }

        public DefaultConfigBuilder putFulfillment(AssetSignature signature, double amount) {
            listingManager.newListing(signature);
            fulfillment.put(listingManager.fromSignature(signature).getKey(), amount);
            return this;
        }

        public DefaultConfigBuilder putInput(AssetSignature signature, double amount) {
            listingManager.newListing(signature);
            input.put(listingManager.fromSignature(signature).getKey(), amount);
            return this;
        }

        public DefaultConfigBuilder putOutput(AssetSignature signature, double amount) {
            listingManager.newListing(signature);
            output.put(listingManager.fromSignature(signature).getKey(), amount);
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
