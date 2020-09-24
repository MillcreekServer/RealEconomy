package io.github.wysohn.realeconomy.main;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import io.github.wysohn.rapidframework3.bukkit.main.AbstractBukkitPlugin;
import io.github.wysohn.rapidframework3.core.command.SubCommand;
import io.github.wysohn.rapidframework3.core.command.TabCompleters;
import io.github.wysohn.rapidframework3.core.exceptions.InvalidArgumentException;
import io.github.wysohn.rapidframework3.core.inject.module.GsonSerializerModule;
import io.github.wysohn.rapidframework3.core.inject.module.LanguagesModule;
import io.github.wysohn.rapidframework3.core.inject.module.ManagerModule;
import io.github.wysohn.rapidframework3.core.inject.module.MediatorModule;
import io.github.wysohn.rapidframework3.core.main.PluginMainBuilder;
import io.github.wysohn.rapidframework3.core.player.AbstractPlayerWrapper;
import io.github.wysohn.rapidframework3.interfaces.ICommandSender;
import io.github.wysohn.rapidframework3.interfaces.command.IArgumentMapper;
import io.github.wysohn.realeconomy.inject.module.BankOwnerProviderModule;
import io.github.wysohn.realeconomy.inject.module.MaxCapitalModule;
import io.github.wysohn.realeconomy.inject.module.ServerBankModule;
import io.github.wysohn.realeconomy.inject.module.TransactionHandlerModule;
import io.github.wysohn.realeconomy.manager.CustomTypeAdapters;
import io.github.wysohn.realeconomy.manager.banking.CentralBankingManager;
import io.github.wysohn.realeconomy.manager.banking.bank.AbstractBank;
import io.github.wysohn.realeconomy.manager.currency.Currency;
import io.github.wysohn.realeconomy.manager.currency.CurrencyManager;
import io.github.wysohn.realeconomy.manager.user.User;
import io.github.wysohn.realeconomy.manager.user.UserManager;
import io.github.wysohn.realeconomy.mediator.BankingMediator;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.java.JavaPluginLoader;

import java.lang.ref.Reference;
import java.math.BigDecimal;
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
                .addArgumentMapper(0, mapUser())
                .addArgumentMapper(1, mapAmount())
                .addArgumentMapper(2, mapCurrency())
                .action((sender, args) -> {
                    Optional<User> optTarget = args.get(0);
                    Optional<BigDecimal> optAmount = args.get(1);
                    Optional<Currency> optCurrency = args.get(2);

                    if (!optTarget.isPresent() || !optAmount.isPresent() || !optCurrency.isPresent())
                        return true;

                    User target = optTarget.get();
                    BigDecimal amount = optAmount.get();
                    Currency currency = optCurrency.get();

                    processSend(getUser(sender).orElseThrow(RuntimeException::new),
                            target,
                            amount,
                            currency);

                    return true;
                })
        );
        list.add(new SubCommand.Builder("give", -1)
                .addTabCompleter(0, TabCompleters.PLAYER)
                .addTabCompleter(1, TabCompleters.hint("<amount>"))
                .addTabCompleter(2, TabCompleters.hint("<currency>"))
                .addArgumentMapper(0, mapUser())
                .addArgumentMapper(1, mapAmount())
                .addArgumentMapper(2, mapCurrency())
                .action((sender, args) -> {
                    Optional<User> optTarget = args.get(0);
                    Optional<BigDecimal> optAmount = args.get(1);
                    Optional<Currency> optCurrency = args.get(2);

                    if (!optTarget.isPresent() || !optAmount.isPresent() || !optCurrency.isPresent())
                        return true;

                    User target = optTarget.get();
                    BigDecimal amount = optAmount.get();
                    Currency currency = optCurrency.get();

                    processSend(null,
                            target,
                            amount,
                            currency);
                    return true;
                })
        );
        list.add(new SubCommand.Builder("take", -1)
                .addTabCompleter(0, TabCompleters.PLAYER)
                .addTabCompleter(1, TabCompleters.hint("<amount>"))
                .addTabCompleter(2, TabCompleters.hint("<currency>"))
                .addArgumentMapper(0, mapUser())
                .addArgumentMapper(1, mapAmount())
                .addArgumentMapper(2, mapCurrency())
                .action((sender, args) -> {
                    Optional<User> optTarget = args.get(0);
                    Optional<BigDecimal> optAmount = args.get(1);
                    Optional<Currency> optCurrency = args.get(2);

                    if (!optTarget.isPresent() || !optAmount.isPresent() || !optCurrency.isPresent())
                        return true;

                    User target = optTarget.get();
                    BigDecimal amount = optAmount.get();
                    Currency currency = optCurrency.get();

                    processSend(target,
                            null,
                            amount,
                            currency);
                    return true;
                })
        );
    }

    private void processSend(User from, User to, BigDecimal amount, Currency currency) {
        getMain().getMediator(BankingMediator.class).ifPresent(bankingMediator -> {
            switch (bankingMediator.send(from, to, amount, currency)) {
                case NO_OWNER:
                    break;
                case FROM_INSUFFICIENT:
                    break;
                case TO_DEPOSIT_REFUSED:
                    break;
                case OK:
                    break;
            }
        });
    }

    private IArgumentMapper<Currency> mapCurrency() {
        return s -> Optional.of(s).flatMap(this::getCurrency)
                .orElseThrow(() -> new InvalidArgumentException(RealEconomyLangs.Command_Common_CurrencyNotFound, (sen, langman) ->
                        langman.addString(s)));
    }

    private IArgumentMapper<BigDecimal> mapAmount() {
        return s -> Optional.of(s)
                .map(Double::parseDouble)
                .filter(value -> value > 0.0)
                .map(BigDecimal::valueOf)
                .orElse(BigDecimal.ZERO);
    }

    private IArgumentMapper<User> mapUser() {
        return s -> Optional.of(s)
                .flatMap(this::getUser)
                .orElseThrow(() -> new InvalidArgumentException(RealEconomyLangs.Command_Common_UserNotFound, (sen, langman) ->
                        langman.addString(s)));
    }

    private Optional<Currency> getCurrency(String currencyName) {
        return getMain().getManager(CurrencyManager.class)
                .flatMap(currencyManager -> currencyManager.get(currencyName))
                .map(Reference::get);
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

    private Optional<User> getUser(String name) {
        return Optional.ofNullable(name)
                .map(Bukkit::getOfflinePlayer)
                .map(OfflinePlayer::getUniqueId)
                .flatMap(this::getUser);
    }
}
