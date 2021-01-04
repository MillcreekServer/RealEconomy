package io.github.wysohn.realeconomy.main;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import io.github.wysohn.rapidframework3.bukkit.main.AbstractBukkitPlugin;
import io.github.wysohn.rapidframework3.bukkit.manager.location.ManagerPlayerLocation;
import io.github.wysohn.rapidframework3.core.command.ArgumentMappers;
import io.github.wysohn.rapidframework3.core.command.EnumArgumentMapper;
import io.github.wysohn.rapidframework3.core.command.SubCommand;
import io.github.wysohn.rapidframework3.core.command.TabCompleters;
import io.github.wysohn.rapidframework3.core.exceptions.InvalidArgumentException;
import io.github.wysohn.rapidframework3.core.inject.module.*;
import io.github.wysohn.rapidframework3.core.language.DefaultLangs;
import io.github.wysohn.rapidframework3.core.main.PluginMainBuilder;
import io.github.wysohn.rapidframework3.core.message.Message;
import io.github.wysohn.rapidframework3.core.message.MessageBuilder;
import io.github.wysohn.rapidframework3.core.paging.Pagination;
import io.github.wysohn.rapidframework3.core.player.AbstractPlayerWrapper;
import io.github.wysohn.rapidframework3.interfaces.ICommandSender;
import io.github.wysohn.rapidframework3.interfaces.command.CommandAction;
import io.github.wysohn.rapidframework3.interfaces.command.IArgumentMapper;
import io.github.wysohn.rapidframework3.interfaces.command.ITabCompleter;
import io.github.wysohn.rapidframework3.interfaces.paging.DataProvider;
import io.github.wysohn.rapidframework3.utils.Pair;
import io.github.wysohn.rapidframework3.utils.regex.CommonPatterns;
import io.github.wysohn.realeconomy.api.smartinv.SmartInvAPI;
import io.github.wysohn.realeconomy.api.vault.VaultHook;
import io.github.wysohn.realeconomy.inject.module.*;
import io.github.wysohn.realeconomy.interfaces.banking.IBankingType;
import io.github.wysohn.realeconomy.interfaces.business.IBusiness;
import io.github.wysohn.realeconomy.interfaces.business.tiers.ITier;
import io.github.wysohn.realeconomy.manager.CustomTypeAdapters;
import io.github.wysohn.realeconomy.manager.asset.signature.AssetSignature;
import io.github.wysohn.realeconomy.manager.asset.signature.ItemStackSignature;
import io.github.wysohn.realeconomy.manager.banking.BankingTypeRegistry;
import io.github.wysohn.realeconomy.manager.banking.CentralBankingManager;
import io.github.wysohn.realeconomy.manager.banking.TransactionUtil;
import io.github.wysohn.realeconomy.manager.banking.bank.AbstractBank;
import io.github.wysohn.realeconomy.manager.banking.bank.CentralBank;
import io.github.wysohn.realeconomy.manager.business.tiers.TierRegistry;
import io.github.wysohn.realeconomy.manager.business.types.mining.MiningBusinessManager;
import io.github.wysohn.realeconomy.manager.currency.Currency;
import io.github.wysohn.realeconomy.manager.currency.CurrencyManager;
import io.github.wysohn.realeconomy.manager.listing.AssetListing;
import io.github.wysohn.realeconomy.manager.listing.AssetListingManager;
import io.github.wysohn.realeconomy.manager.listing.OrderInfo;
import io.github.wysohn.realeconomy.manager.listing.OrderType;
import io.github.wysohn.realeconomy.manager.user.User;
import io.github.wysohn.realeconomy.manager.user.UserManager;
import io.github.wysohn.realeconomy.mediator.BankingMediator;
import io.github.wysohn.realeconomy.mediator.BusinessMediator;
import io.github.wysohn.realeconomy.mediator.TradeMediator;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.Server;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.lang.ref.Reference;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
                AssetListingManager.class,
                CurrencyManager.class,
                UserManager.class,
                ManagerPlayerLocation.class,

                MiningBusinessManager.class
        ));
        pluginMainBuilder.addModule(new MediatorModule(
                BankingMediator.class,
                TradeMediator.class
        ));
        pluginMainBuilder.addModule(new ExternalAPIModule(
                Pair.of("SmartInvs", SmartInvAPI.class),
                Pair.of("Vault", VaultHook.class)
        ));
        pluginMainBuilder.addModule(new BankOwnerProviderModule(
                //TODO users may own commercial bank later
        ));
        pluginMainBuilder.addModule(new BankUserProviderModule(

        ));
        pluginMainBuilder.addModule(new GsonSerializerModule(
                CustomTypeAdapters.ACCOUNT,
                CustomTypeAdapters.BANKING_TYPE,
                CustomTypeAdapters.ASSET,
                CustomTypeAdapters.ASSET_SIGNATURE,
                CustomTypeAdapters.ORE_INFO
        ));
        pluginMainBuilder.addModule(new TypeAsserterModule());
        pluginMainBuilder.addModule(new CapitalLimitModule());
        pluginMainBuilder.addModule(new OrderSQLModule());
        pluginMainBuilder.addModule(new NamespacedKeyModule());
        pluginMainBuilder.addModule(new OrderPlacementHandlerModule());
        pluginMainBuilder.addModule(new BlockGeneratorModule());
        pluginMainBuilder.addModule(new BusinessConstantsModule());
        pluginMainBuilder.addModule(new VisitStateProviderModule());
        //TODO and some other modules as your need...
    }

    @Override
    protected void registerCommands(List<SubCommand.Builder> list) {
        list.add(new SubCommand.Builder("wallet", -1)
                .withAlias("bal", "balance", "money")
                .withDescription(RealEconomyLangs.Command_Wallet_Desc)
                .addUsage(RealEconomyLangs.Command_Wallet_Usage)
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
                            "/realeconomy wallet").show(sender, page, (sen, pair, i) ->
                            MessageBuilder.forMessage(toFormattedBalance(pair))
                                    .append(" ")
                                    .append(toCurrencyName(pair))
                                    .build());

                    return true;
                })
        );
        list.add(new SubCommand.Builder("pay", 3)
                .withDescription(RealEconomyLangs.Command_Pay_Desc)
                .addUsage(RealEconomyLangs.Command_Pay_Usage)
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
        //TODO this command need rework
        // bank is based on BankingMediator#getUsingBank
        // /bank info [type]
        // /bank deposit <type> <amount>
        // /bank withdraw <type> <amount>
        // /bank assets <type>
        // /bank open <type>
        // /bank close <type>
        list.add(new SubCommand.Builder("bank", -1)
                .withDescription(RealEconomyLangs.Command_Bank_Desc)
                .addUsage(RealEconomyLangs.Command_Bank_Usage)
                .addTabCompleter(0, TabCompleters.simple("info", "deposit", "withdraw", "assets", "open"/*, "close"*/))
                .addTabCompleter(1, TabCompleters.simple(Arrays.stream(BankingTypeRegistry.values())
                        .map(IBankingType::name)
                        .toArray(String[]::new)))
                .addArgumentMapper(0, ArgumentMappers.STRING)
                .addArgumentMapper(1, s -> Optional.of(s)
                        .map(String::toUpperCase)
                        .map(BankingTypeRegistry::fromString)
                        .orElseThrow(() -> new InvalidArgumentException(RealEconomyLangs.Command_Common_InvalidAccountType, (sen, man) ->
                                man.addString(s))))
                .action(new CommandAction() {
                    @Override
                    public boolean execute(ICommandSender sender, SubCommand.Arguments args) {
                        String action = args.get(0)
                                .map(String.class::cast)
                                .orElse(null);
                        if (action == null)
                            return false;

                        AbstractBank bank = getCurrentBank(sender);
                        if(bank == null){
                            getMain().lang().sendMessage(sender, RealEconomyLangs.Command_Common_NotInABank);
                            return true;
                        }

                        IBankingType bankingType = args.get(1)
                                .map(IBankingType.class::cast)
                                .orElse(null);
                        double amount = 0.0;
                        switch (action) {
                            case "info":
                                if(bankingType == null){
                                    info(sender, bank);
                                } else {
                                    getUser(sender).ifPresent(user -> balanceInfo(user, bank, bankingType));
                                }
                                break;
                            case "deposit":
                            case "withdraw":
                                if(bankingType == null)
                                    return false;

                                amount = args.get(2)
                                        .map(String.class::cast)
                                        .filter(str -> CommonPatterns.DOUBLE.matcher(str).matches())
                                        .map(Double::parseDouble)
                                        .orElse(0.0);
                                if(amount <= 0.0){
                                    getMain().lang().sendMessage(sender, RealEconomyLangs.Command_Common_InvalidAmount);
                                    return true;
                                }

                                double finalAmount = amount;
                                if (action.equals("deposit")) {
                                    getUser(sender).ifPresent(user -> deposit(user, bank, finalAmount, bankingType));
                                } else {
                                    getUser(sender).ifPresent(user -> withdraw(user, bank, finalAmount, bankingType));
                                }

                                break;
                            case "assets":
                                if (bankingType == null)
                                    return false;

                                getUser(sender).ifPresent(user -> assets(user, bank, bankingType));
                                break;
                            case "open":
                                if (bankingType == null)
                                    return false;
                                getUser(sender).ifPresent(user -> open(user, bank, bankingType));
                                break;
                            case "close":
                                if (bankingType == null)
                                    return false;
                                //getUser(sender).ifPresent(user -> close(user, bank, bankingType));
                                break;
                            default:
                                return false;
                        }

                        return true;
                    }

                    private void info(ICommandSender sender, AbstractBank bank) {
                        getMain().lang().sendMessage(sender, DefaultLangs.General_Line);
                        getMain().lang().sendMessage(sender, DefaultLangs.General_Header, (sen, man) ->
                                man.addString(bank.getStringKey()));
                        getMain().lang().sendProperty(sender, bank);
                        getMain().lang().sendMessage(sender, DefaultLangs.General_Line);
                    }

                    private void balanceInfo(User sender, AbstractBank bank, IBankingType type) {
                        String translated = getMain().lang().parseFirst(sender, type.lang());
                        getMain().lang().sendMessage(sender, DefaultLangs.General_Line);
                        getMain().lang().sendMessage(sender, DefaultLangs.General_Header, (sen, man) ->
                                man.addString(translated));
                        getMain().getMediator(BankingMediator.class).ifPresent(bankingMediator -> {
                            BigDecimal balance = bankingMediator.balance(bank, sender, type);
                            getMain().lang().sendMessage(sender, RealEconomyLangs.Command_Bank_Balance, (sen, man) ->
                                    man.addString(balance.toString())
                                            .addString(Objects.toString(bank.getBaseCurrency())));
                        });
                        getMain().lang().sendMessage(sender, DefaultLangs.General_Line);
                    }

                    private void deposit(User sender,
                                            AbstractBank bank,
                                            double amount,
                                            IBankingType type) {
                        getMain().getMediator(BankingMediator.class).ifPresent(bankingMediator ->
                                // wallet -> bank account
                                handleResult2(sender, bankingMediator.send(sender, sender, type, BigDecimal.valueOf(amount), bank.getBaseCurrency())));
                    }

                    private void withdraw(User sender,
                                          AbstractBank bank,
                                          double amount,
                                          IBankingType type) {
                        // bank account -> wallet
                        getMain().getMediator(BankingMediator.class).ifPresent(bankingMediator ->
                                handleResult2(sender, bankingMediator.send(sender, type, sender, BigDecimal.valueOf(amount), bank.getBaseCurrency())));
                    }

//                    private void handleResult(ICommandSender sender, BankingMediator.Result result) {
//                        switch (result) {
//                            case NO_CURRENCY_SET:
//                                getMain().lang().sendMessage(sender, RealEconomyLangs.Command_Bank_NoCurrencySet);
//                                break;
//                            case NO_ACCOUNT:
//                                getMain().lang().sendMessage(sender, RealEconomyLangs.Command_Bank_NoAccount);
//                                break;
//                            case FAIL_WITHDRAW:
//                                getMain().lang().sendMessage(sender, RealEconomyLangs.Command_Common_WithdrawRefused);
//                                break;
//                            case FAIL_DEPOSIT:
//                                getMain().lang().sendMessage(sender, RealEconomyLangs.Command_Common_DepositRefused);
//                                break;
//                            case OK:
//                                getMain().lang().sendMessage(sender, RealEconomyLangs.Command_Bank_Success);
//                                break;
//                            default:
//                                sender.sendMessageRaw("&cUndefined result: " + result);
//                                break;
//                        }
//                    }

                    private void handleResult2(ICommandSender sender, TransactionUtil.Result result) {
                        switch (result) {
                            case NO_OWNER:
                                getMain().lang().sendMessage(sender, RealEconomyLangs.Command_Bank_NoCurrencyOwner);
                                break;
                            case TO_DEPOSIT_REFUSED:
                                getMain().lang().sendMessage(sender, RealEconomyLangs.Command_Common_DepositRefused);
                                break;
                            case FROM_WITHDRAW_REFUSED:
                                getMain().lang().sendMessage(sender, RealEconomyLangs.Command_Common_WithdrawRefused);
                                break;
                            case OK:
                                getMain().lang().sendMessage(sender, RealEconomyLangs.Command_Bank_Success);
                                break;
                            default:
                                sender.sendMessageRaw("&cUndefined result: " + result);
                                break;
                        }
                    }

                    private void assets(User sender,
                                        AbstractBank bank,
                                        IBankingType type) {
                        if(!bank.hasAccount(sender, type)){
                            String translate = getMain().lang().parseFirst(sender, type.lang());
                            getMain().lang().sendMessage(sender, RealEconomyLangs.Command_Common_NoAccount, (s, m) ->
                                    m.addString(translate).addString(translate));
                            return;
                        }

                        if (BankingTypeRegistry.TRADING.equals(type)) {
                            getMain().api().getAPI(SmartInvAPI.class).ifPresent(smartInvAPI ->
                                    smartInvAPI.openTradeAccountGUI(bank, sender));
                        } else {
                            getMain().lang().sendMessage(sender, RealEconomyLangs.Command_Bank_Assets_InvalidType);
                        }
                    }

                    private void open(User sender,
                                      AbstractBank bank,
                                      IBankingType type) {
                        getMain().getMediator(BankingMediator.class).ifPresent(bankingMediator -> {
                            if (bankingMediator.openAccount(bank, sender, type)) {
                                getMain().lang().sendMessage(sender, RealEconomyLangs.Command_Bank_Open_Success);
                            } else {
                                getMain().lang().sendMessage(sender, RealEconomyLangs.Command_Bank_Open_AlreadyExist);
                            }
                        });
                    }

//                    private void close(User sender,
//                                         AbstractBank bank,
//                                         IBankingType type){
//                        getMain().getMediator(BankingMediator.class).ifPresent(bankingMediator -> {
//                            if (bankingMediator.(bank, sender, type)){
//
//                            } else {
//
//                            }
//                        });
//                    }
                }));
        list.add(new SubCommand.Builder("items", -1)
                .withDescription(RealEconomyLangs.Command_Items_Desc)
                .addUsage(RealEconomyLangs.Command_Items_Usage)
                .addTabCompleter(0, TabCompleters.hint("[page]"))
                .addTabCompleter(1, new ITabCompleter() {
                    @Override
                    public List<String> getHint() {
                        return TabCompleters.list("...");
                    }

                    @Override
                    public List<String> getCandidates(String part) {
                        return getMain().getManager(AssetListingManager.class)
                                .map(AssetListingManager::getCategoryTrie)
                                .map(trie -> trie.getAllStartsWith(part))
                                .orElseGet(ArrayList::new);
                    }
                })
                .addArgumentMapper(0, ArgumentMappers.INTEGER)
                .addArgumentMapper(1, arg -> getMain().getManager(AssetListingManager.class)
                        .map(AssetListingManager::getCategoryTrie)
                        .filter(trie -> trie.find(arg))
                        .map(trie -> arg)
                        .orElseThrow(() -> new InvalidArgumentException(RealEconomyLangs.Command_Items_InvalidCategory,
                                (s, m) -> m.addString(arg))))
                .action((sender, args) -> {
                    int page = args.get(0)
                            .map(Integer.class::cast)
                            .filter(val -> val > 0)
                            .map(val -> val - 1)
                            .orElse(0);
                    String category = args.get(1)
                            .map(String.class::cast)
                            .orElse(null);

                    getMain().getMediator(TradeMediator.class).ifPresent(tradeMediator -> {
                        DataProvider<OrderInfo> dataProvider = tradeMediator.getPrice(category);
                        //TODO use GUI

                        new Pagination<>(getMain().lang(),
                                dataProvider,
                                7,
                                "items",
                                "/realeconomy items").show(sender, page, (sen, info, i) -> {
                            String other = getMain().lang().parseFirst(sen, RealEconomyLangs.Command_Items_Format, (s, man) ->
                                    man.addDouble(info.getPrice())
                                            .addString(Objects.toString(getCurrency(info.getCurrencyUuid()).orElse(null)))
                                            .addInteger(info.getOrderId()));

                            Message[] message = MessageBuilder.forMessage("[").build();
                            message = Message.concat(message, getSignature(info.getListingUuid()).toMessage(getMain().lang(), sender));
                            message = Message.concat(message, MessageBuilder.forMessage("]").build());
                            message = Message.concat(message, MessageBuilder.forMessage("\u26c1"+info.getAmount()).build());
                            message = Message.concat(message, MessageBuilder.forMessage(other)
                                    .withHoverShowText("/realeconomy buy "+info.getOrderId()+" ")
                                    .withClickSuggestCommand("/realeconomy buy "+info.getOrderId()+" ")
                                    .build());
                            return message;
                        });
                    });
                    return true;
                }));
        list.add(new SubCommand.Builder("buy", 3)
                .withDescription(RealEconomyLangs.Command_Buy_Desc)
                .addUsage(RealEconomyLangs.Command_Buy_Usage)
                .addTabCompleter(0, TabCompleters.hint("<order id>"))
                .addTabCompleter(1, TabCompleters.hint("<price>"))
                .addTabCompleter(2, TabCompleters.hint("<amount>"))
                .addArgumentMapper(0, mapOrderId())
                .addArgumentMapper(1, mapPrice())
                .addArgumentMapper(2, ArgumentMappers.INTEGER)
                .action((sender, args) -> {
                    int orderId = args.get(0).map(Integer.class::cast).orElse(-1);
                    double price = args.get(1).map(Double.class::cast).orElse(-1.0);
                    int amount = args.get(2).map(Integer.class::cast)
                            .filter(val -> val > 0 && val < 65536)
                            .orElse(1);

                    if (orderId < 0 || price < 0.0)
                        return true;

                    AbstractBank bank = getMain().getMediator(BankingMediator.class)
                            .flatMap(bankingMediator -> getUser(sender).map(bankingMediator::getUsingBank))
                            .orElse(null);

                    if (bank == null) {
                        getMain().lang().sendMessage(sender, RealEconomyLangs.Command_Common_NotInABank);
                        return true;
                    }

                    getMain().getMediator(TradeMediator.class).ifPresent(tradeMediator ->
                            getUser(sender).ifPresent(user -> {
                                if (tradeMediator.bidAsset(user,
                                        orderId,
                                        price,
                                        bank.getBaseCurrency(),
                                        amount)) {
                                    // show reminder if the account doesn't have enough currency
                                    if (bank.balanceOfAccount(user, BankingTypeRegistry.TRADING)
                                            .compareTo(BigDecimal.valueOf(price)) < 0) {
                                        getMain().lang().sendMessage(sender, RealEconomyLangs.Command_Buy_NotEnoughCurrency);
                                    } else {
                                        // process one tick later since order listing have some delays
                                        getMain().task().sync(() -> {
                                            getMain().comm().runSubCommand(sender, "orders");
                                        });
                                        getMain().task().sync(() -> {
                                            getMain().lang().sendMessage(sender, RealEconomyLangs.Command_Buy_FailNotice);
                                        });
                                    }
                                } else {
                                    String nameTrading = getMain().lang().parseFirst(RealEconomyLangs.BankingType_Trading);
                                    getMain().lang().sendMessage(sender, RealEconomyLangs.Command_Common_NoAccount, (l, m) ->
                                            m.addString(nameTrading).addString(nameTrading));
                                }
                            }));

                    return true;
                }));
        list.add(new SubCommand.Builder("sell", 1)
                .withDescription(RealEconomyLangs.Command_Sell_Desc)
                .addUsage(RealEconomyLangs.Command_Sell_Usage)
                .addTabCompleter(0, TabCompleters.hint("<price>"))
                .addArgumentMapper(0, mapPrice())
                .action((sender, args) -> {
                    double price = args.get(0).map(Double.class::cast).orElse(-1.0);

                    if (price < 0.0)
                        return true;

                    AbstractBank bank = getMain().getMediator(BankingMediator.class)
                            .flatMap(bankingMediator -> getUser(sender).map(bankingMediator::getUsingBank))
                            .orElse(null);

                    if (bank == null) {
                        getMain().lang().sendMessage(sender, RealEconomyLangs.Command_Common_NotInABank);
                        return true;
                    }

                    getMain().getMediator(TradeMediator.class).ifPresent(tradeMediator ->
                            getUser(sender).ifPresent(user -> {
                                ItemStack itemStack = user.getSender().getInventory().getItemInMainHand();
                                if (itemStack.getType() == Material.AIR) {
                                    getMain().lang().sendMessage(sender, DefaultLangs.General_NothingOnYourHand);
                                    return;
                                }

                                if (tradeMediator.isDeniedType(itemStack.getType())) {
                                    getMain().lang().sendMessage(sender, RealEconomyLangs.Command_Common_DeniedTradeType);
                                    return;
                                }

                                AssetSignature signature = new ItemStackSignature(itemStack);
                                if (tradeMediator.sellAsset(user,
                                        signature,
                                        price,
                                        bank.getBaseCurrency(),
                                        itemStack.getAmount(),
                                        () -> {
                                            user.getSender().getInventory().setItemInMainHand(null);
                                            bank.addAccountAsset(user, signature.asset(new HashMap<String, Object>() {{
                                                put(AssetSignature.KEY_NUMERIC_MEASURE, itemStack.getAmount());
                                            }}));
                                        })){

                                    // process one tick later since order listing have sone delays
                                    getMain().task().sync(() -> getMain().comm().runSubCommand(sender, "orders"));
                                } else {
                                    String nameTrading = getMain().lang().parseFirst(RealEconomyLangs.BankingType_Trading);
                                    getMain().lang().sendMessage(sender, RealEconomyLangs.Command_Common_NoAccount, (l, m) ->
                                            m.addString(nameTrading).addString(nameTrading));
                                }
                            }));

                    return true;
                }));
        list.add(new SubCommand.Builder("orders", -1)
                .withDescription(RealEconomyLangs.Command_Orders_Desc)
                .addUsage(RealEconomyLangs.Command_Orders_Usage)
                .addTabCompleter(0, TabCompleters.hint("[page]"))
                .addArgumentMapper(0, ArgumentMappers.INTEGER)
                .action((sender, args) -> {
                    int page = args.get(0)
                            .map(Integer.class::cast)
                            .filter(val -> val > 0)
                            .map(val -> val - 1)
                            .orElse(0);

                    //TODO use GUI
                    getUser(sender).ifPresent(user -> {
                        Pagination.list(getMain().lang(),
                                Stream.concat(user.getOrderIds(OrderType.BUY).stream()
                                                .map(id -> Pair.of(id, OrderType.BUY)),
                                        user.getOrderIds(OrderType.SELL).stream()
                                                .map(id -> Pair.of(id, OrderType.SELL)))
                                        .collect(Collectors.toList()),
                                7,
                                "Orders",
                                "/realeconomy orders")
                                .show(sender, page, (s, pair, i) -> toOrderDetail(user, pair, i));
                    });
                    return true;
                }));
        list.add(new SubCommand.Builder("cancel", 2)
                .withDescription(RealEconomyLangs.Command_Cancel_Desc)
                .addUsage(RealEconomyLangs.Command_Cancel_Usage, (s, man) ->
                        man.addString("&dBUY &8, &dSELL"))
                .addTabCompleter(0, TabCompleters.simple(Arrays.stream(OrderType.values())
                        .map(Enum::name)
                        .toArray(String[]::new)))
                .addTabCompleter(1, TabCompleters.hint("<order id>"))
                .addArgumentMapper(0, new EnumArgumentMapper<>(OrderType.class, true))
                .addArgumentMapper(1, ArgumentMappers.INTEGER)
                .action((sender, args) -> {
                    OrderType type = args.get(0).map(OrderType.class::cast).orElse(null);
                    int orderId = args.get(1).map(Integer.class::cast).orElse(-1);

                    if (type == null || orderId < 0)
                        return true;

                    getUser(sender).ifPresent(user -> {
                        if (!user.hasOrderId(type, orderId)) {
                            getMain().lang().sendMessage(sender, RealEconomyLangs.Command_Cancel_Ownership);
                            return;
                        }

                        getMain().getMediator(TradeMediator.class).ifPresent(tradeMediator ->
                                tradeMediator.cancelOrder(user, orderId, type));
                        getMain().comm().runSubCommand(sender, "orders");
                    });

                    return true;
                }));
        list.add(new SubCommand.Builder("business", -1)
                .withAlias("bus")
                .withDescription(RealEconomyLangs.Command_Business_Desc)
                .addUsage(RealEconomyLangs.Command_Business_Usage)
                .addTabCompleter(0, TabCompleters.simple("open", "disband", "info",
                        "addmember", "removemember", "tiers"))
                .addArgumentMapper(0, ArgumentMappers.STRING)
                .action(new CommandAction() {
                    @Override
                    public boolean execute(ICommandSender sender, SubCommand.Arguments args) {
                        final BusinessMediator businessMediator = getMain().getMediator(BusinessMediator.class)
                                .orElseThrow(RuntimeException::new);

                        User user = getUser(sender).orElse(null);
                        if (user == null) {
                            sender.sendMessageRaw("User instance not found.");
                            return true;
                        }

                        Optional<String> optAction = args.get(0);
                        if (!optAction.isPresent()) {
                            return false;
                        }

                        switch (optAction.get()) {
                            case "open":
                                ITier tier = args.get(1)
                                        .map(String.class::cast)
                                        .map(TierRegistry::fromString)
                                        .orElse(null);
                                if (tier == null) {
                                    getMain().lang().sendMessage(user, RealEconomyLangs.Command_Business_TierNotFound);
                                    return true;
                                }

                                String subType = args.get(2)
                                        .map(String.class::cast)
                                        .orElse(null);

                                return open(businessMediator, user, tier, subType);
                            case "disband":
                                return disband(businessMediator, user);
                            case "info":
                                return info(businessMediator, user);
                            case "add":
                            case "addmember":
                                IBusiness business = businessMediator.getUsingBusiness(sender.getUuid())
                                        .stream()
                                        .findFirst()
                                        .map(businessMediator::getBusiness)
                                        .orElse(null);
                                if (business == null) {
                                    getMain().lang().sendMessage(user, RealEconomyLangs.Command_Business_Common_NoBusinessUsing);
                                    return true;
                                }

                                Player invitee = args.get(1)
                                        .map(String.class::cast)
                                        .map(Bukkit::getPlayer)
                                        .orElse(null);
                                if (invitee == null) {
                                    getMain().lang().sendMessage(user, DefaultLangs.General_NoSuchPlayer, ((sen, langman) ->
                                            langman.addString(args.getAsString(1))));
                                    return true;
                                }

                                return add(businessMediator, user, business, invitee.getUniqueId());
                            case "remove":
                            case "removemember":
                                IBusiness business2 = businessMediator.getUsingBusiness(sender.getUuid())
                                        .stream()
                                        .findFirst()
                                        .map(businessMediator::getBusiness)
                                        .orElse(null);
                                if (business2 == null) {
                                    getMain().lang().sendMessage(user, RealEconomyLangs.Command_Business_Common_NoBusinessUsing);
                                    return true;
                                }

                                OfflinePlayer invitee2 = args.get(1)
                                        .map(String.class::cast)
                                        .map(Bukkit::getOfflinePlayer)
                                        .orElse(null);
                                if (invitee2 == null) {
                                    getMain().lang().sendMessage(user, DefaultLangs.General_NoSuchPlayer, ((sen, langman) ->
                                            langman.addString(args.getAsString(1))));
                                    return true;
                                }

                                return remove(businessMediator, user, business2, invitee2.getUniqueId());
                            case "tiers":
                                ITier targetTier = args.get(1)
                                        .map(String.class::cast)
                                        .map(TierRegistry::fromString)
                                        .orElse(null);

                                return tiers(user, targetTier);
                            default:
                                return false;
                        }
                    }

                    private boolean open(BusinessMediator businessMediator, User sender, ITier tier, String subType) {
                        if (subType == null)
                            subType = ITier.DEFAULT_SUB_TYPE;

                        if (!tier.verifySubType(subType)) {
                            String displayName = tier.displayName(sender);
                            String finalSubType = subType;
                            getMain().lang().sendMessage(sender, RealEconomyLangs.Command_Business_InvalidSubType, ((sen, langman) ->
                                    langman.addString(finalSubType).addString(displayName)));
                            return true;
                        }

                        switch (businessMediator.openNewBusinessLocation(tier.name(), subType, sender)) {
                            case NO_PROVIDER:
                                getMain().lang().sendMessage(sender, RealEconomyLangs.Command_Business_Open_NoProvider);
                                break;
                            case DUP_LOCATION:
                                getMain().lang().sendMessage(sender, RealEconomyLangs.Command_Business_Open_DuplicatedLocation);
                                break;
                            case OK:
                                getMain().lang().sendMessage(sender, RealEconomyLangs.Command_Business_Open_Ok);
                                break;
                        }

                        return true;
                    }

                    private IBusiness getCurrentBusiness(BusinessMediator businessMediator, User sender) {
                        List<IBusiness> current = businessMediator.getUsingBusiness(sender.getUuid()).stream()
                                .map(businessMediator::getBusiness)
                                .collect(Collectors.toList());

                        if (current.size() < 1) {
                            getMain().lang().sendMessage(sender, RealEconomyLangs.Command_Business_Common_NoBusinessUsing);
                            return null;
                        }

                        if (current.size() > 1) {
                            getMain().lang().sendMessage(sender, RealEconomyLangs.Command_Business_Common_MultipleBusinessUsing);
                        }

                        return current.get(0);
                    }

                    private boolean isOwner(User sender, IBusiness business) {
                        if (!Objects.equals(sender.getUuid(), business.getOwnerUuid())) {
                            getMain().lang().sendMessage(sender, RealEconomyLangs.Command_Business_Common_NotOwner);
                            return false;
                        }
                        return true;
                    }

                    private boolean disband(BusinessMediator businessMediator, User sender) {
                        Optional.ofNullable(getCurrentBusiness(businessMediator, sender)).ifPresent(business -> {
                            if (!isOwner(sender, business))
                                return;

                            if (businessMediator.deleteBusiness(business)) {
                                getMain().lang().sendMessage(sender, RealEconomyLangs.Command_Business_Disband_Ok);
                            } else {
                                getMain().lang().sendMessage(sender, RealEconomyLangs.Command_Business_Disband_Failure);
                            }
                        });

                        return true;
                    }

                    private boolean info(BusinessMediator businessMediator, User sender) {
                        Optional.ofNullable(getCurrentBusiness(businessMediator, sender)).ifPresent(business -> {
                            getMain().lang().sendProperty(sender, business);
                        });
                        return true;
                    }

                    private boolean add(BusinessMediator businessMediator,
                                        User sender,
                                        IBusiness business,
                                        UUID target) {
                        if (!isOwner(sender, business))
                            return false;

                        if (!businessMediator.addMember(business, target)) {
                            getMain().lang().sendMessage(sender, RealEconomyLangs.Command_Business_Add_AlreadyMember);
                            return true;
                        }

                        getMain().lang().sendMessage(sender, RealEconomyLangs.Command_Business_Add_Ok);
                        return true;
                    }

                    private boolean remove(BusinessMediator businessMediator,
                                           User sender,
                                           IBusiness business,
                                           UUID target) {
                        if (!isOwner(sender, business))
                            return false;

                        if (!businessMediator.removeMember(business, target)) {
                            getMain().lang().sendMessage(sender, RealEconomyLangs.Command_Business_Kick_NotMember);
                            return true;
                        }

                        getMain().lang().sendMessage(sender, RealEconomyLangs.Command_Business_Kick_Ok);
                        return true;
                    }

                    private boolean tiers(User sender, ITier tier) {
                        getMain().lang().sendMessage(sender, DefaultLangs.General_Line);
                        /**
                         * mining [hover]
                         * farming [hover]
                         * ...
                         */
                        if (tier == null) {
                            for (ITier value : TierRegistry.values()) {
                                sendTierDetails(sender, value, true);
                            }
                        }
                        /**
                         * mining
                         * - coal
                         * - diamond
                         * - ...
                         */
                        else {
                            sendTierDetails(sender, tier, false);
                        }
                        getMain().lang().sendMessage(sender, DefaultLangs.General_Line);
                        return true;
                    }

                    private void sendTierDetails(User user, ITier tier, boolean useHover) {
                        if (useHover) {
                            MessageBuilder builder = MessageBuilder.forMessage(tier.displayName(user));
                            Optional.ofNullable(tier.description(user))
                                    .ifPresent(arr -> {
                                        StringBuilder desc = new StringBuilder();
                                        for (String msg : arr) {
                                            desc.append(msg);
                                            desc.append('\n');
                                        }
                                        builder.withHoverShowText(desc.toString());
                                    });
                            getMain().lang().sendRawMessage(user, builder.build());
                        } else {
                            user.sendMessageRaw(tier.displayName(user));
                            Optional.ofNullable(tier.description(user)).ifPresent(user::sendMessageRaw);
                        }
                    }
                }));

        getMain().comm().linkMainCommand("bal", "realeconomy", "wallet");
        getMain().comm().linkMainCommand("balance", "realeconomy", "wallet");
        getMain().comm().linkMainCommand("money", "realeconomy", "wallet");
        getMain().comm().linkMainCommand("pay", "realeconomy", "pay");
        getMain().comm().linkMainCommand("bank", "realeconomy", "bank");
        getMain().comm().linkMainCommand("items", "realeconomy", "items");
        getMain().comm().linkMainCommand("shop", "realeconomy", "items");
        getMain().comm().linkMainCommand("buy", "realeconomy", "buy");
        getMain().comm().linkMainCommand("sell", "realeconomy", "sell");
        getMain().comm().linkMainCommand("cancel", "realeconomy", "cancel");
        getMain().comm().linkMainCommand("business", "realeconomy", "business");
    }

    /**
     * SQL Overhead
     *
     * @param sender
     * @param orderPair
     * @param i
     * @return
     */
    private Message[] toOrderDetail(User sender, Pair<Integer, OrderType> orderPair, int i) {
        RealEconomyLangs lang;
        if (orderPair.value == OrderType.BUY) {
            lang = RealEconomyLangs.Command_Orders_Buys;
        } else if (orderPair.value == OrderType.SELL) {
            lang = RealEconomyLangs.Command_Orders_Sells;
        } else {
            throw new RuntimeException("Unknown order type " + orderPair.value);
        }

        OrderInfo orderInfo = getMain().getManager(AssetListingManager.class).map(assetListingManager -> {
            try {
                return assetListingManager.getInfo(orderPair.key, orderPair.value);
            } catch (SQLException ex) {
                ex.printStackTrace();
                return null;
            }
        }).orElse(null);

        if (orderInfo == null) {
            sender.removeOrderId(orderPair.value, orderPair.key);
            return null;
        } else {
            return Message.concat(MessageBuilder.forMessage(getMain().lang().parseFirst(lang, (s, m) ->
                            m.addInteger(orderPair.key)
                                    .addDouble(orderInfo.getPrice())
                                    .addString(Objects.toString(getCurrency(orderInfo.getCurrencyUuid()).orElse(null)))
                                    .addInteger(orderInfo.getAmount())))
                            .append(" &f[")
                            .build(),
                    Optional.ofNullable(getSignature(orderInfo.getListingUuid()))
                            .map(signature -> signature.toMessage(getMain().lang(), sender))
                            .orElse(MessageBuilder.empty()),
                    MessageBuilder.forMessage("]").build(),
                    MessageBuilder.forMessage(" ").append("&c[\u2718]")
                            .withHoverShowText("/eco cancel " + orderPair.value.name() + " " + orderInfo.getOrderId())
                            .withClickRunCommand("/eco cancel " + orderPair.value.name() + " " + orderInfo.getOrderId())
                            .build());
        }
    }

    private AssetSignature getSignature(UUID listingUuid) {
        return getMain().getManager(AssetListingManager.class)
                .flatMap(assetListingManager -> assetListingManager.get(listingUuid))
                .map(Reference::get)
                .map(AssetListing::getSignature)
                .orElse(null);
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

    private IArgumentMapper<Double> mapPrice() {
        return (arg -> {
            if (!CommonPatterns.DOUBLE.matcher(arg).matches())
                throw new InvalidArgumentException(DefaultLangs.General_NotDecimal, (s, m) ->
                        m.addString(arg));

            double price = Double.parseDouble(arg);
            if (price <= 0.0 || BigDecimal.valueOf(price).compareTo(CapitalLimitModule.MAX) > 0)
                throw new InvalidArgumentException(RealEconomyLangs.Command_Common_PriceRange, (s, m) ->
                        m.addDouble(CapitalLimitModule.MAX.doubleValue()));

            return price;
        });
    }

    private IArgumentMapper<Integer> mapOrderId() {
        return (arg -> {
            if (!CommonPatterns.INTEGER.matcher(arg).matches())
                throw new InvalidArgumentException(DefaultLangs.General_NotInteger, (s, m) ->
                        m.addString(arg));

            int orderId = Integer.parseInt(arg);
            if (orderId < 1)
                throw new InvalidArgumentException(RealEconomyLangs.Command_Common_InvalidOrderId);

            return orderId;
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

    private Optional<Currency> getCurrency(UUID currencyUuid) {
        return getMain().getManager(CurrencyManager.class)
                .flatMap(currencyManager -> currencyManager.get(currencyUuid))
                .map(Reference::get);
    }

    private AbstractBank getCurrentBank(ICommandSender sender) {
        return getMain().getMediator(BankingMediator.class)
                .flatMap(bankingMediator -> getUser(sender).map(bankingMediator::getUsingBank))
                .orElse(null);
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
