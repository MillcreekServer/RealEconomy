package io.github.wysohn.realeconomy.manager.user;

import com.google.inject.Injector;
import io.github.wysohn.rapidframework3.bukkit.manager.user.AbstractUserManager;
import io.github.wysohn.rapidframework3.core.database.Databases;
import io.github.wysohn.rapidframework3.core.inject.annotations.PluginDirectory;
import io.github.wysohn.rapidframework3.core.inject.annotations.PluginLogger;
import io.github.wysohn.rapidframework3.core.main.ManagerConfig;
import io.github.wysohn.rapidframework3.interfaces.plugin.IShutdownHandle;
import io.github.wysohn.rapidframework3.interfaces.serialize.ISerializer;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.io.File;
import java.util.UUID;
import java.util.logging.Logger;

@Singleton
public class UserManager extends AbstractUserManager<User> {
    @Inject
    public UserManager(
            @Named("pluginName") String pluginName,
            @PluginLogger Logger logger,
            ManagerConfig config,
            @PluginDirectory File pluginDir,
            IShutdownHandle shutdownHandle,
            ISerializer serializer,
            Injector injector) {
        super(pluginName, logger, config, pluginDir, shutdownHandle, serializer, injector, User.class);
    }

    @Override
    protected Databases.DatabaseFactory createDatabaseFactory() {
        return getDatabaseFactory("user");
    }

    @Override
    protected User newInstance(UUID uuid) {
        return new User(uuid);
    }
}
