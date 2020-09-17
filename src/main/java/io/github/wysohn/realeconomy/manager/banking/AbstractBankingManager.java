package io.github.wysohn.realeconomy.manager.banking;

import com.google.inject.Injector;
import io.github.wysohn.rapidframework3.core.caching.AbstractManagerElementCaching;
import io.github.wysohn.rapidframework3.core.main.ManagerConfig;
import io.github.wysohn.rapidframework3.interfaces.plugin.IShutdownHandle;
import io.github.wysohn.rapidframework3.interfaces.serialize.ISerializer;
import io.github.wysohn.realeconomy.manager.banking.bank.AbstractBank;

import java.io.File;
import java.util.UUID;
import java.util.logging.Logger;

public abstract class AbstractBankingManager<Bank extends AbstractBank>
        extends AbstractManagerElementCaching<UUID, Bank> {
    public AbstractBankingManager(
            String pluginName,
            Logger logger,
            ManagerConfig config,
            File pluginDir,
            IShutdownHandle shutdownHandle,
            ISerializer serializer,
            Injector injector,
            Class<Bank> type) {
        super(pluginName, logger, config, pluginDir, shutdownHandle, serializer, injector, type);
    }

    @Override
    protected UUID fromString(String s) {
        return UUID.fromString(s);
    }
}
