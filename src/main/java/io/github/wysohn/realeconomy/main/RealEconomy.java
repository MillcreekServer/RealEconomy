package io.github.wysohn.realeconomy.main;

import io.github.wysohn.rapidframework3.bukkit.main.AbstractBukkitPlugin;
import io.github.wysohn.rapidframework3.core.command.SubCommand;
import io.github.wysohn.rapidframework3.core.inject.module.GsonSerializerModule;
import io.github.wysohn.rapidframework3.core.inject.module.LanguagesModule;
import io.github.wysohn.rapidframework3.core.inject.module.ManagerModule;
import io.github.wysohn.rapidframework3.core.inject.module.MediatorModule;
import io.github.wysohn.rapidframework3.core.main.PluginMainBuilder;
import io.github.wysohn.rapidframework3.core.player.AbstractPlayerWrapper;
import io.github.wysohn.realeconomy.inject.module.BankOwnerProviderModule;
import io.github.wysohn.realeconomy.inject.module.MaxCapitalModule;
import io.github.wysohn.realeconomy.inject.module.ServerBankModule;
import io.github.wysohn.realeconomy.inject.module.TransactionHandlerModule;
import io.github.wysohn.realeconomy.manager.CustomTypeAdapters;
import io.github.wysohn.realeconomy.manager.banking.CentralBankingManager;
import io.github.wysohn.realeconomy.mediator.BankingMediator;
import org.bukkit.plugin.java.JavaPluginLoader;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class RealEconomy extends AbstractBukkitPlugin {
    public RealEconomy() {
    }

    private RealEconomy(JavaPluginLoader mockLoader) {
        super(mockLoader);
    }

    @Override
    protected void init(PluginMainBuilder pluginMainBuilder) {
        pluginMainBuilder.addModule(new LanguagesModule(RealEconomyLangs.values()));
        pluginMainBuilder.addModule(new ManagerModule(
                CentralBankingManager.class
        ));
        pluginMainBuilder.addModule(new MediatorModule(
                BankingMediator.class
        ));
        pluginMainBuilder.addModule(new BankOwnerProviderModule(
                //TODO users
        ));
        pluginMainBuilder.addModule(new GsonSerializerModule(
                CustomTypeAdapters.ACCOUNT,
                CustomTypeAdapters.BANKING_TYPE
        ));
        pluginMainBuilder.addModule(new BankOwnerProviderModule());
        pluginMainBuilder.addModule(new ServerBankModule());
        pluginMainBuilder.addModule(new MaxCapitalModule());
        pluginMainBuilder.addModule(new TransactionHandlerModule());
        //TODO and some other modules as your need...
    }

    @Override
    protected void registerCommands(List<SubCommand> list) {
        //TODO register commands
    }

    @Override
    protected Optional<? extends AbstractPlayerWrapper> getPlayerWrapper(UUID uuid) {
        throw new RuntimeException("Need wrapper.");
    }
}
