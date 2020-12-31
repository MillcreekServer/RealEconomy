package io.github.wysohn.realeconomy.manager.business;

import io.github.wysohn.rapidframework3.core.inject.annotations.PluginDirectory;
import io.github.wysohn.rapidframework3.core.inject.factory.IStorageFactory;
import io.github.wysohn.rapidframework3.core.main.Manager;
import io.github.wysohn.rapidframework3.core.main.PluginMain;
import io.github.wysohn.rapidframework3.interfaces.store.IKeyValueStorage;
import io.github.wysohn.realeconomy.interfaces.business.IBusiness;
import io.github.wysohn.realeconomy.interfaces.business.IBusinessProvider;
import io.github.wysohn.realeconomy.manager.business.tiers.TierAdapter;
import io.github.wysohn.realeconomy.manager.business.tiers.TierRegistry;
import org.bukkit.Bukkit;
import org.bukkit.event.Listener;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.File;
import java.util.*;

@Singleton
public class BusinessManager extends Manager {
    private static final List<IBusinessProvider> BUSINESSES_PROVIDERS = new LinkedList<>();
    private static IKeyValueStorage tierConfigs;

    private final PluginMain main;
    private final File pluginDir;
    private final IStorageFactory storageFactory;

    @Inject
    public BusinessManager(PluginMain main,
                           @PluginDirectory File pluginDir,
                           IStorageFactory storageFactory) {
        this.main = main;
        this.pluginDir = pluginDir;
        this.storageFactory = storageFactory;

        tierConfigs = storageFactory.create(pluginDir, "tiers.yml");
    }

    /**
     * Register the IBusinessProvider. Note that all the methods should be thread-safe, so that
     * it can be used in the update thread along with the server thread.
     *
     * @param provider provider
     * @throws RuntimeException same provider is registered again
     */
    public static void registerProvider(IBusinessProvider provider) {
        if (BUSINESSES_PROVIDERS.contains(provider))
            throw new RuntimeException("Duplicated provider.");

        BUSINESSES_PROVIDERS.add(provider);
    }

    public static IKeyValueStorage getTierConfigs() {
        return tierConfigs;
    }

    private final long INTERVAL = 1000L;
    private Timer updateTimer;

    @Override
    public void enable() throws Exception {
        tierConfigs.getKeys(false).forEach(name ->
                TierRegistry.register(new TierAdapter(name, tierConfigs)));

        BUSINESSES_PROVIDERS.forEach(provider -> provider.keys().forEach(key ->
                Optional.ofNullable(provider.getBusiness(key))
                        .filter(Listener.class::isInstance)
                        .map(Listener.class::cast)
                        .ifPresent(listener -> Bukkit.getPluginManager().registerEvents(listener, main.getPlatform()))));
    }

    @Override
    public void load() throws Exception {
        if (updateTimer != null)
            updateTimer.cancel();

        BUSINESSES_PROVIDERS.forEach(provider -> provider.keys().forEach(key ->
                Optional.ofNullable(provider.getBusiness(key))
                        .ifPresent(IBusiness::init)));

        updateTimer = new Timer();
        updateTimer.scheduleAtFixedRate(new UpdateTask(), INTERVAL, INTERVAL);
    }

    @Override
    public void disable() throws Exception {
        if (updateTimer != null)
            updateTimer.cancel();

        BUSINESSES_PROVIDERS.forEach(provider -> provider.keys().forEach(key ->
                Optional.ofNullable(provider.getBusiness(key))
                        .ifPresent(IBusiness::stop)));
    }

    private class UpdateTask extends TimerTask {
        @Override
        public void run() {
            BUSINESSES_PROVIDERS.forEach(provider ->
                    provider.keys().forEach(key -> Optional.ofNullable(provider.getBusiness(key))
                            .ifPresent(IBusiness::init)));
        }
    }
}
