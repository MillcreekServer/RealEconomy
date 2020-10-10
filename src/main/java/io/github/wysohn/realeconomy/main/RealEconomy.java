package io.github.wysohn.realeconomy.main;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import io.github.wysohn.rapidframework3.bukkit.main.AbstractBukkitPlugin;
import io.github.wysohn.rapidframework3.core.command.ArgumentMappers;
import io.github.wysohn.rapidframework3.core.command.SubCommand;
import io.github.wysohn.rapidframework3.core.command.TabCompleters;
import io.github.wysohn.rapidframework3.core.exceptions.InvalidArgumentException;
import io.github.wysohn.rapidframework3.core.inject.module.*;
import io.github.wysohn.rapidframework3.core.language.DefaultLangs;
import io.github.wysohn.rapidframework3.core.language.Pagination;
import io.github.wysohn.rapidframework3.core.main.PluginMainBuilder;
import io.github.wysohn.rapidframework3.core.message.MessageBuilder;
import io.github.wysohn.rapidframework3.core.player.AbstractPlayerWrapper;
import io.github.wysohn.rapidframework3.interfaces.ICommandSender;
import io.github.wysohn.rapidframework3.interfaces.command.IArgumentMapper;
import io.github.wysohn.rapidframework3.utils.Pair;
import io.github.wysohn.realeconomy.inject.module.BankOwnerProviderModule;
import io.github.wysohn.realeconomy.inject.module.CapitalLimitModule;
import io.github.wysohn.realeconomy.inject.module.NamespacedKeyModule;
import io.github.wysohn.realeconomy.inject.module.TransactionHandlerModule;
import io.github.wysohn.realeconomy.manager.CustomTypeAdapters;
import io.github.wysohn.realeconomy.manager.banking.CentralBankingManager;
import io.github.wysohn.realeconomy.manager.banking.bank.AbstractBank;
import io.github.wysohn.realeconomy.manager.banking.bank.CentralBank;
import io.github.wysohn.realeconomy.manager.currency.Currency;
import io.github.wysohn.realeconomy.manager.currency.CurrencyManager;
import io.github.wysohn.realeconomy.manager.user.User;
import io.github.wysohn.realeconomy.manager.user.UserManager;
import io.github.wysohn.realeconomy.mediator.BankingMediator;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.Server;

import java.lang.ref.Reference;
import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public class RealEconomy extends AbstractBukkitPlugin {

    public RealEconomy() {
    }

    public RealEconomy(Server mockServer) {
        super(mockServer);
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
                CentralBankingManager.class,
                CurrencyManager.class,
                UserManager.class
        ));
        pluginMainBuilder.addModule(new MediatorModule(
                BankingMediator.class
        ));
        pluginMainBuilder.addModule(new BankOwnerProviderModule(
                //TODO users
        ));
        pluginMainBuilder.addModule(new GsonSerializerModule(
                CustomTypeAdapters.ACCOUNT,
                CustomTypeAdapters.BANKING_TYPE,
                CustomTypeAdapters.ASSET,
                CustomTypeAdapters.ASSET_SIGNATURE
        ));
        pluginMainBuilder.addModule(new TypeAsserterModule());
        pluginMainBuilder.addModule(new CapitalLimitModule());
        pluginMainBuilder.addModule(new TransactionHandlerModule());
        pluginMainBuilder.addModule(new NamespacedKeyModule());
        //TODO and some other modules as your need...
    }

    @Override
    protected void registerCommands(List<SubCommand.Builder> list) {
        list.add(new SubCommand.Builder("balance", -1)
                .withAlias("bal")
                .withDescription(RealEconomyLangs.Command_Balance_Desc)
                .addUsage(RealEconomyLangs.Command_Balance_Usage)
                .addTabCompleter(0, TabCompleters.hint("[page]"))
                .addArgumentMapper(0, ArgumentMappers.INTEGER)
                .action((sender, args) -> {
                    int page = args.get(0)
                            .map(Integer.class::cast)
                            .filter(val -> val > 0)
                            .map(val -> val - 1)
                            .orElse(0);

                    User target = getUser(sender.getUuid()).orElse(null);
                    if (target == null) {
                        return true;
                    }

                    new Pagination<>(getMain().lang(),
                            target.balancesPagination(),
                            7,
                            getMain().lang().parseFirst(sender, RealEconomyLangs.Wallet),
                            "/realeconomy balance").show(sender, page, (sen, pair, i) ->
                            MessageBuilder.forMessage(toFormattedBalance(pair))
                                    .append(" ")
                                    .append(toCurrencyName(pair))
                                    .build());

                    return true;
                })
        );
        list.add(new SubCommand.Builder("pay", 3)
                .withDescription(RealEconomyLangs.Command_Balance_Desc)
                .addUsage(RealEconomyLangs.Command_Balance_Usage)
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

                    processSend(sender,
                            getUser(sender).orElseThrow(RuntimeException::new),
                            target,
                            amount,
                            currency);

                    return true;
                })
        );
        list.add(new SubCommand.Builder("currencies", -1)
                .withDescription(RealEconomyLangs.Command_Currencies_Desc)
                .addUsage(RealEconomyLangs.Command_Currencies_Usage)
                .addTabCompleter(0, TabCompleters.hint("[page]"))
                .addArgumentMapper(0, ArgumentMappers.INTEGER)
                .action((sender, args) -> {
                    getMain().getManager(CurrencyManager.class).ifPresent(currencyManager -> {
                        int page = args.get(0)
                                .map(Integer.class::cast)
                                .filter(val -> val > 0)
                                .map(val -> val - 1)
                                .orElse(0);

                        new Pagination<>(getMain().lang(),
                                currencyManager.currenciesPagination(),
                                7,
                                getMain().lang().parseFirst(sender, RealEconomyLangs.Currencies),
                                "/realeconomy currencies").show(sender, page, (sen, currency, i) ->
                                MessageBuilder.forMessage(Objects.toString(currency))
                                        .append(":")
                                        .append(currency.getCode())
                                        .append(" ")
                                        .append(Objects.toString(currency.ownerBank()))
                                        .build());
                    });
                    return true;
                }));
        list.add(new SubCommand.Builder("give", -1)
                .withDescription(RealEconomyLangs.Command_Give_Desc)
                .addUsage(RealEconomyLangs.Command_Give_Usage)
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

                    processSend(sender,
                            null,
                            target,
                            amount,
                            currency);
                    return true;
                })
        );
        list.add(new SubCommand.Builder("take", -1)
                .withDescription(RealEconomyLangs.Command_Take_Desc)
                .addUsage(RealEconomyLangs.Command_Take_Usage)
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

                    processSend(sender,
                            target,
                            null,
                            amount,
                            currency);
                    return true;
                })
        );
        list.add(new SubCommand.Builder("bank", -1)
                .withDescription(RealEconomyLangs.Command_Bank_Desc)
                .addUsage(RealEconomyLangs.Command_Bank_Usage)
                .addTabCompleter(0, TabCompleters.hint("<bank name>"))
                .addTabCompleter(1, TabCompleters.simple("info"))
                .addArgumentMapper(0, s -> Optional.of(s)
                        .map(RealEconomy.this::getCentralBank)
                        .orElseThrow(() -> new InvalidArgumentException(RealEconomyLangs.Command_Common_BankNotFound,
                                (sen, man) -> man.addString(s))))
                .addArgumentMapper(1, ArgumentMappers.STRING)
                .action((sender, args) -> {
                    CentralBank centralBank = args.get(0)
                            .map(CentralBank.class::cast)
                            .orElse(null);
                    String action = args.get(1)
                            .map(String.class::cast)
                            .orElse(null);
                    if (centralBank == null)
                        return true;
                    if (action == null)
                        return false;

                    switch (action) {
                        case "info":
                            getMain().lang().sendMessage(sender, DefaultLangs.General_Line);
                            getMain().lang().sendMessage(sender, DefaultLangs.General_Header, (sen, man) ->
                                    man.addString(centralBank.getStringKey()));
                            getMain().lang().sendProperty(sender, centralBank);
                            getMain().lang().sendMessage(sender, DefaultLangs.General_Line);
                            break;
                        default:
                            return false;
                    }

                    return true;
                }));

        getMain().comm().linkMainCommand("balance", "realeconomy", "balance");
        getMain().comm().linkMainCommand("pay", "realeconomy", "pay");
    }

    private CentralBank getCentralBank(String name) {
        return getMain().getManager(CentralBankingManager.class)
                .flatMap(centralBankingManager -> centralBankingManager.get(name))
                .map(Reference::get)
                .orElse(null);
    }


    private String toCurrencyName(Pair<UUID, BigDecimal> pair) {
        return getCurrency(pair.key)
                .map(Object::toString)
                .orElse("[?]");
    }

    private String toFormattedBalance(Pair<UUID, BigDecimal> pair) {
        return Metrics.df.format(pair.value);
    }

    private void processSend(ICommandSender sender, User from, User to, BigDecimal amount, Currency currency) {
        if (Objects.equals(from, to)) {
            getMain().lang().sendMessage(sender, RealEconomyLangs.Command_Common_SenderReceiverSame);
            return;
        }

        getMain().getMediator(BankingMediator.class).ifPresent(bankingMediator -> {
            switch (bankingMediator.send(from, to, amount, currency)) {
                case NO_OWNER:
                    getMain().lang().sendMessage(sender, RealEconomyLangs.Command_Common_NoCurrencyOwner, (sen, man) ->
                            man.addString(currency.toString()));
                    break;
                case FROM_WITHDRAW_REFUSED:
                    getMain().lang().sendMessage(sender, RealEconomyLangs.Command_Common_WithdrawRefused);
                    break;
                case TO_DEPOSIT_REFUSED:
                    getMain().lang().sendMessage(sender, RealEconomyLangs.Command_Common_DepositRefused);
                    break;
                case OK:
                    sendResultMessage(sender, from, to, amount, currency);
                    if (from != null && sender != from)
                        sendResultMessage(from, from, to, amount, currency);
                    if (to != null && sender != to)
                        sendResultMessage(to, from, to, amount, currency);
                    break;
            }
        });
    }

    private void sendResultMessage(ICommandSender sender, User from, User to, BigDecimal amount, Currency currency) {
        getMain().lang().sendMessage(sender, RealEconomyLangs.Command_Common_SendSuccess, (sen, man) ->
                man.addString(Objects.toString(from == null ? currency.ownerBank() : from))
                        .addString(Metrics.df.format(amount))
                        .addString(Objects.toString(currency))
                        .addString(Objects.toString(to == null ? currency.ownerBank() : to)));
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

    private Optional<Currency> getCurrency(UUID currencyUuid) {
        return getMain().getManager(CurrencyManager.class)
                .flatMap(currencyManager -> currencyManager.get(currencyUuid))
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
