package io.github.wysohn.realeconomy.manager.account;

import com.google.inject.Injector;
import io.github.wysohn.rapidframework3.core.caching.AbstractManagerElementCaching;
import io.github.wysohn.rapidframework3.core.database.Databases;
import io.github.wysohn.rapidframework3.core.main.ManagerConfig;
import io.github.wysohn.rapidframework3.interfaces.plugin.IShutdownHandle;
import io.github.wysohn.rapidframework3.interfaces.serialize.ISerializer;

import java.io.File;
import java.util.UUID;
import java.util.logging.Logger;

public class AccountManager extends AbstractManagerElementCaching<UUID, Account> {
    public AccountManager(
            String pluginName,
            Logger logger,
            ManagerConfig config,
            File pluginDir,
            IShutdownHandle shutdownHandle,
            ISerializer serializer,
            Injector injector, Class<Account> type) {
        super(pluginName, logger, config, pluginDir, shutdownHandle, serializer, injector, type);
    }

    @Override
    protected Databases.DatabaseFactory createDatabaseFactory() {
        return getDatabaseFactory("account");
    }

    @Override
    protected UUID fromString(String s) {
        return UUID.fromString(s);
    }

    @Override
    protected Account newInstance(UUID uuid) {
        return new Account(uuid);
    }
}
