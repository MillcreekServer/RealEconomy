package io.github.wysohn.realeconomy.manager.asset;

import com.google.inject.Injector;
import io.github.wysohn.rapidframework3.core.caching.AbstractManagerElementCaching;
import io.github.wysohn.rapidframework3.core.inject.annotations.PluginDirectory;
import io.github.wysohn.rapidframework3.core.inject.annotations.PluginLogger;
import io.github.wysohn.rapidframework3.core.main.ManagerConfig;
import io.github.wysohn.rapidframework3.interfaces.plugin.IShutdownHandle;
import io.github.wysohn.rapidframework3.interfaces.serialize.ISerializer;

import javax.inject.Named;
import javax.inject.Singleton;
import java.io.File;
import java.util.UUID;
import java.util.logging.Logger;

@Singleton
public abstract class AssetManager<A extends Asset<?>> extends AbstractManagerElementCaching<UUID, A> {
    public AssetManager(
            @Named("pluginName") String pluginName,
            @PluginLogger Logger logger,
            ManagerConfig config,
            @PluginDirectory File pluginDir,
            IShutdownHandle shutdownHandle,
            ISerializer serializer, Injector injector, Class<A> type) {
        super(pluginName, logger, config, pluginDir, shutdownHandle, serializer, injector, type);
    }

    @Override
    protected UUID fromString(String string) {
        return UUID.fromString(string);
    }
}
