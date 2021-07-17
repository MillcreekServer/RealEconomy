package io.github.wysohn.realeconomy.manager.banking;

import com.google.inject.Injector;
import io.github.wysohn.rapidframework3.core.inject.annotations.PluginDirectory;
import io.github.wysohn.rapidframework3.core.inject.annotations.PluginLogger;
import io.github.wysohn.rapidframework3.core.inject.factory.IDatabaseFactoryCreator;
import io.github.wysohn.rapidframework3.core.main.ManagerConfig;
import io.github.wysohn.rapidframework3.interfaces.plugin.IShutdownHandle;
import io.github.wysohn.rapidframework3.interfaces.serialize.ISerializer;
import io.github.wysohn.rapidframework3.interfaces.serialize.ITypeAsserter;
import io.github.wysohn.realeconomy.manager.banking.bank.CentralBank;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.io.File;
import java.util.UUID;
import java.util.logging.Logger;

@Singleton
public class CentralBankingManager extends AbstractBankingManager<CentralBank> {
    @Inject
    public CentralBankingManager(
            @Named("pluginName") String pluginName,
            @PluginLogger Logger logger,
            ManagerConfig config,
            @PluginDirectory File pluginDir,
            IShutdownHandle shutdownHandle,
            IDatabaseFactoryCreator factoryCreator,
            ISerializer serializer,
            ITypeAsserter asserter,
            Injector injector) {
        super(pluginName,
                logger,
                config,
                pluginDir,
                shutdownHandle,
                serializer,
                asserter,
                factoryCreator,
                injector,
                "centralBanks",
                CentralBank.class);
    }

    @Override
    protected CentralBank newInstance(UUID uuid) {
        return new CentralBank(uuid);
    }
}
