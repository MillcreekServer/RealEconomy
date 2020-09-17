package io.github.wysohn.realeconomy.manager.banking;

import com.google.inject.Injector;
import io.github.wysohn.rapidframework3.core.database.Databases;
import io.github.wysohn.rapidframework3.core.main.ManagerConfig;
import io.github.wysohn.rapidframework3.interfaces.plugin.IShutdownHandle;
import io.github.wysohn.rapidframework3.interfaces.serialize.ISerializer;
import io.github.wysohn.realeconomy.manager.banking.bank.CentralBank;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.File;
import java.util.UUID;
import java.util.logging.Logger;

@Singleton
public class CentralBankingManager extends AbstractBankingManager<CentralBank> {
    @Inject
    public CentralBankingManager(
            String pluginName,
            Logger logger,
            ManagerConfig config,
            File pluginDir,
            IShutdownHandle shutdownHandle,
            ISerializer serializer,
            Injector injector) {
        super(pluginName, logger, config, pluginDir, shutdownHandle, serializer, injector, CentralBank.class);
    }

    @Override
    protected Databases.DatabaseFactory createDatabaseFactory() {
        return getDatabaseFactory("centralBanks");
    }

    @Override
    protected CentralBank newInstance(UUID uuid) {
        return new CentralBank(uuid);
    }
}
