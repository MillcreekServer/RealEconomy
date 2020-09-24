package io.github.wysohn.realeconomy.main;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import io.github.wysohn.rapidframework3.bukkit.main.AbstractBukkitPlugin;
import io.github.wysohn.rapidframework3.core.command.ArgumentMappers;
import io.github.wysohn.rapidframework3.core.command.SubCommand;
import io.github.wysohn.rapidframework3.core.command.TabCompleters;
import io.github.wysohn.rapidframework3.core.inject.module.GsonSerializerModule;
import io.github.wysohn.rapidframework3.core.inject.module.LanguagesModule;
import io.github.wysohn.rapidframework3.core.inject.module.ManagerModule;
import io.github.wysohn.rapidframework3.core.inject.module.MediatorModule;
import io.github.wysohn.rapidframework3.core.main.PluginMainBuilder;
import io.github.wysohn.rapidframework3.core.player.AbstractPlayerWrapper;
import io.github.wysohn.rapidframework3.interfaces.ICommandSender;
import io.github.wysohn.realeconomy.inject.module.BankOwnerProviderModule;
import io.github.wysohn.realeconomy.inject.module.MaxCapitalModule;
import io.github.wysohn.realeconomy.inject.module.ServerBankModule;
import io.github.wysohn.realeconomy.inject.module.TransactionHandlerModule;
import io.github.wysohn.realeconomy.manager.CustomTypeAdapters;
import io.github.wysohn.realeconomy.manager.banking.CentralBankingManager;
import io.github.wysohn.realeconomy.manager.banking.bank.AbstractBank;
import io.github.wysohn.realeconomy.manager.user.User;
import io.github.wysohn.realeconomy.manager.user.UserManager;
import io.github.wysohn.realeconomy.mediator.BankingMediator;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.java.JavaPluginLoader;

import java.lang.ref.Reference;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class RealEconomy extends AbstractBukkitPlugin {
    public RealEconomy() {
    }

    private RealEconomy(JavaPluginLoader mockLoader) {
        super(mockLoader);
    }

    /**
     * @param mockLoader
     * @deprecated use for test only
     */
    public static RealEconomy mainForTest(JavaPluginLoader mockLoader) {
        return new RealEconomy(mockLoader);
    }

    @Override
    protected void init(PluginMainBuilder pluginMainBuilder) {
        pluginMainBuilder.addModule(new AbstractModule() {
            @Provides
            RealEconomy realEconomy() {
                return RealEconomy.this;
            }
        });
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
    protected void registerCommands(List<SubCommand.Builder> list) {
        list.add(new SubCommand.Builder("balance", -1)
                .addTabCompleter(0, TabCompleters.PLAYER)
                .action((sender, args) -> {
                    User target = args.get(0)
                            .map(String.class::cast)
                            .map(Bukkit::getOfflinePlayer)
                            .map(OfflinePlayer::getUniqueId)
                            .flatMap(this::getUser)
                            .orElseGet(() -> getUser(sender.getUuid()).orElse(null));
                    if (target == null) {
                        //TODO
                        return true;
                    }

                    target.forEachBalance((uuid, balance) -> {
                        //TODO
                    }, true);

                    return true;
                })
        );
        list.add(new SubCommand.Builder("pay", 3)
                .addTabCompleter(0, TabCompleters.PLAYER)
                .addTabCompleter(1, TabCompleters.hint("<amount>"))
                .addTabCompleter(2, TabCompleters.hint("<currency>"))
                .addArgumentMapper(1, ArgumentMappers.DOUBLE)
                .action((sender, args) -> {
                    User target = args.get(0)
                            .map(String.class::cast)
                            .map(Bukkit::getOfflinePlayer)
                            .map(OfflinePlayer::getUniqueId)
                            .flatMap(this::getUser)
                            .orElse(null);
                    if (target == null) {
                        //TODO
                        return true;
                    }


                    return true;
                })
        );
        list.add(new SubCommand.Builder("give", -1)
                .addTabCompleter(0, TabCompleters.PLAYER)
                .addTabCompleter(1, TabCompleters.hint("<amount>"))
                .addTabCompleter(2, TabCompleters.hint("<currency>"))
                .action((sender, args) -> {
                    User target = args.get(0)
                            .map(String.class::cast)
                            .map(Bukkit::getOfflinePlayer)
                            .map(OfflinePlayer::getUniqueId)
                            .flatMap(this::getUser)
                            .orElse(null);
                    if (target == null) {
                        //TODO
                        return true;
                    }


                    return true;
                })
        );
        list.add(new SubCommand.Builder("take", -1)
                .addTabCompleter(0, TabCompleters.PLAYER)
                .addTabCompleter(1, TabCompleters.hint("<amount>"))
                .addTabCompleter(2, TabCompleters.hint("<currency>"))
                .action((sender, args) -> {
                    User target = args.get(0)
                            .map(String.class::cast)
                            .map(Bukkit::getOfflinePlayer)
                            .map(OfflinePlayer::getUniqueId)
                            .flatMap(this::getUser)
                            .orElse(null);
                    if (target == null) {
                        //TODO
                        return true;
                    }

                    return true;
                })
        );
    }

    private AbstractBank getCurrentBank(ICommandSender sender) {
        return getMain().getMediator(BankingMediator.class)
                .flatMap(bankingMediator -> getUser(sender).map(bankingMediator::getUsingBank))
                .orElseThrow(() -> new RuntimeException("Bank does not exist. But how?"));
    }

    @Override
    protected Optional<? extends AbstractPlayerWrapper> getPlayerWrapper(UUID uuid) {
        return getUser(uuid);
    }

    private Optional<User> getUser(ICommandSender sender) {
        return Optional.ofNullable(sender)
                .map(ICommandSender::getUuid)
                .flatMap(this::getUser);
    }

    private Optional<User> getUser(UUID uuid) {
        return getMain().getManager(UserManager.class)
                .flatMap(userManager -> userManager.get(uuid))
                .map(Reference::get);
    }
}
