package io.github.wysohn.realeconomy.mediator;

import io.github.wysohn.rapidframework3.core.inject.annotations.PluginLogger;
import io.github.wysohn.rapidframework3.core.main.ManagerConfig;
import io.github.wysohn.rapidframework3.core.main.Mediator;
import io.github.wysohn.rapidframework3.interfaces.IMemento;
import io.github.wysohn.rapidframework3.interfaces.paging.DataProvider;
import io.github.wysohn.rapidframework3.utils.FailSensitiveTaskGeneric;
import io.github.wysohn.rapidframework3.utils.Validation;
import io.github.wysohn.realeconomy.interfaces.banking.IBankUser;
import io.github.wysohn.realeconomy.interfaces.banking.IBankUserProvider;
import io.github.wysohn.realeconomy.interfaces.banking.IOrderIssuer;
import io.github.wysohn.realeconomy.manager.asset.listing.*;
import io.github.wysohn.realeconomy.manager.asset.signature.AssetSignature;
import io.github.wysohn.realeconomy.manager.asset.signature.PhysicalAssetSignature;
import io.github.wysohn.realeconomy.manager.banking.BankingTypeRegistry;
import io.github.wysohn.realeconomy.manager.banking.bank.CentralBank;
import io.github.wysohn.realeconomy.manager.currency.Currency;
import io.github.wysohn.realeconomy.manager.currency.CurrencyManager;
import org.bukkit.Material;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.lang.ref.Reference;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.logging.Logger;

@Singleton
public class TradeMediator extends Mediator {
    public static final Map<Material, String> MATERIAL_CATEGORY_MAP = new EnumMap<>(Material.class);
    public static final String MATERIALS = "materials";
    public static final String MATERIAL_CATEGORY_DEFAULT = "item";

    private final ExecutorService tradeExecutor = Executors.newSingleThreadExecutor();

    private final Logger logger;
    private final ManagerConfig config;
    private final CurrencyManager currencyManager;
    private final AssetListingManager assetListingManager;
    private final Set<IBankUserProvider> bankUserProviders;

    TradeBroker tradeBroker;

    @Inject
    public TradeMediator(@PluginLogger Logger logger,
                         ManagerConfig config,
                         CurrencyManager currencyManager,
                         AssetListingManager assetListingManager,
                         Set<IBankUserProvider> bankUserProviders) {
        this.logger = logger;
        this.config = config;
        this.currencyManager = currencyManager;
        this.assetListingManager = assetListingManager;
        this.bankUserProviders = bankUserProviders;
    }

    @Override
    public void enable() throws Exception {

    }

    @Override
    public void load() throws Exception {
        if (tradeBroker != null)
            tradeBroker.interrupt();
        tradeBroker = new TradeBroker();
        tradeBroker.start();

        if (config.get(MATERIALS).isPresent()) {
            Object materialSection = config.get(MATERIALS).get();
            MATERIAL_CATEGORY_MAP.clear();
            Arrays.stream(Material.values()).forEach(material -> {
                MATERIAL_CATEGORY_MAP.put(material, config.get(materialSection, material.name())
                        .map(String.class::cast)
                        .orElse(MATERIAL_CATEGORY_DEFAULT));
            });
        } else {
            Arrays.stream(Material.values()).forEach(material -> {
                MATERIAL_CATEGORY_MAP.put(material, MATERIAL_CATEGORY_DEFAULT);
                config.put(MATERIALS + "." + material, MATERIAL_CATEGORY_DEFAULT);
            });
        }
    }

    @Override
    public void disable() throws Exception {
        logger.info("Finalizing trade schedules...");
        tradeExecutor.shutdown();
        tradeExecutor.awaitTermination(10, TimeUnit.SECONDS);
        logger.info("Done");

        tradeBroker.interrupt();
    }

    public DataProvider<OrderInfo> getPrice() {
        return getPrice(null);
    }

    public DataProvider<OrderInfo> getPrice(String category) {
        return assetListingManager.getListedOrderProvider(category);
    }

    /**
     * List new asset for sell. Upon successful listing, id of order will be automatically
     * added to the issuer's info.
     *
     * @param issuer    the one issuing this offer
     * @param signature the asset signature
     * @param price     offer price
     * @param currency  currency type of price
     * @param stock     number of stocks to sell
     * @param after     what to do after order is added.
     * @return true if success; false if issuer does not have {@link BankingTypeRegistry#TRADING} account in
     * the currency owner bank.
     */
    public boolean sellAsset(IBankUser issuer,
                             AssetSignature signature,
                             double price,
                             Currency currency,
                             int stock,
                             Runnable after) {
        Validation.assertNotNull(issuer);
        Validation.assertNotNull(signature);
        Validation.validate(price, p -> p > 0.0, "Negative or 0.0 price not allowed.");
        Validation.assertNotNull(currency);
        Validation.validate(stock, s -> s > 0, "Negative or 0 stock not allowed.");

        Validation.assertNotNull(currency.ownerBank());

        if (!currency.ownerBank().hasAccount(issuer, BankingTypeRegistry.TRADING)) {
            return false;
        }

        assetListingManager.newListing(signature);
        tradeExecutor.submit(() -> {
            try {
                assetListingManager.addOrder(signature,
                        OrderType.SELL,
                        issuer,
                        price,
                        currency,
                        stock);

                after.run();

                assetListingManager.commitOrders();
            } catch (SQLException ex) {
                ex.printStackTrace();
                try {
                    assetListingManager.rollbackOrders();
                } catch (SQLException ex2) {
                    ex2.printStackTrace();
                }
            }
        });

        return true;
    }

    /**
     * Bid to buy the specified asset. This does not necessarily mean that the buyer
     * can buy the asset immediately.
     *
     * @param issuer   the one want to buy asset
     * @param orderId  the order id of selling asset
     * @param price    bidding price
     * @param currency currency of price
     * @param amount   goal number of assets to purchase
     * @return true if success; false if issuer does not have {@link BankingTypeRegistry#TRADING} account in
     * the currency owner bank.
     */
    public boolean bidAsset(IBankUser issuer,
                            int orderId,
                            double price,
                            Currency currency,
                            int amount) {
        Validation.assertNotNull(issuer);
        Validation.validate(orderId, id -> id > 0, "Negative or 0 is not allowed for order id.");
        Validation.validate(price, p -> p > 0.0, "Negative or 0.0 price not allowed.");
        Validation.assertNotNull(currency);
        Validation.validate(amount, s -> s > 0, "Negative or 0 amount not allowed.");

        Validation.assertNotNull(currency.ownerBank());

        if (!currency.ownerBank().hasAccount(issuer, BankingTypeRegistry.TRADING)) {
            return false;
        }

        tradeExecutor.submit(() -> {
            try {
                OrderInfo orderInfo = assetListingManager.getInfo(orderId, OrderType.SELL);
                if (orderInfo == null)
                    return;

                UUID listingUuid = orderInfo.getListingUuid();
                AssetListing assetListing = assetListingManager.get(listingUuid)
                        .map(Reference::get)
                        .orElse(null);

                if (assetListing == null)
                    return;

                assetListingManager.addOrder(assetListing.getSignature(),
                        OrderType.BUY,
                        issuer,
                        price,
                        currency,
                        amount);

                assetListingManager.commitOrders();
            } catch (SQLException ex) {
                ex.printStackTrace();
                try {
                    assetListingManager.rollbackOrders();
                } catch (SQLException ex2) {
                    ex2.printStackTrace();
                }
            }
        });

        return true;
    }

    public void cancelOrder(IOrderIssuer issuer, int orderId, OrderType type) {
        Validation.validate(orderId, id -> id > 0, "Negative or 0 is not allowed for order id.");
        Validation.assertNotNull(type);
        Validation.validate(orderId, id -> issuer.hasOrderId(type, id), "Issuer mismatch.");

        tradeExecutor.submit(() -> {
            try {
                assetListingManager.cancelOrder(orderId, type, id -> issuer.removeOrderId(type, id));
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
        });
    }

    class TradeBroker extends Thread {
        public TradeBroker() {
            setPriority(NORM_PRIORITY - 1);
            setName("RealEconomy - TradeBroker");
        }

        @Override
        public void run() {
            while (!interrupted()) {
                processOrder();

                try {
                    Thread.sleep(1000L);
                } catch (InterruptedException e) {
                    logger.info(getName() + " is interrupted.");
                }
            }
        }

        void processOrder() {
            assetListingManager.peekMatchingOrder(tradeInfo -> {
                // skip if no matching orders
                if(tradeInfo == null)
                    return;

                // get buy/sell pair
                IBankUser buyer = bankUserProviders.stream()
                        .map(provider -> provider.get(tradeInfo.getBuyer()))
                        .filter(Objects::nonNull)
                        .findFirst()
                        .orElse(null);
                IBankUser seller = bankUserProviders.stream()
                        .map(provider -> provider.get(tradeInfo.getSeller()))
                        .filter(Objects::nonNull)
                        .findFirst()
                        .orElse(null);

                // cannot proceed if either trading end is not found
                if (buyer == null) {
                    // delete order so other orders can be processed.
                    cancel(tradeInfo.getBuyId(), OrderType.BUY);
                    return;
                }
                if (seller == null) {
                    // delete order so other orders can be processed.
                    cancel(tradeInfo.getSellId(), OrderType.SELL);
                    return;
                }

                Currency currency = currencyManager.get(tradeInfo.getCurrencyUuid())
                        .map(Reference::get)
                        .orElse(null);

                // weird currency found.
                CentralBank bank = null;
                if (currency == null || (bank = currency.ownerBank()) == null) {
                    cancelBoth(tradeInfo);
                    logger.warning("Cannot proceed with unknown Currency or bank not found. Orders are deleted.");
                    logger.warning("Trade Info: " + tradeInfo);
                    return;
                }

                // check if trading account exist
                // usually, this is checked before the order has made, yet
                // account may be deleted for some reason while order is pending
                if (!bank.hasAccount(buyer, BankingTypeRegistry.TRADING)) {
                    // delete order so other orders can be processed.
                    cancel(tradeInfo.getBuyId(), OrderType.BUY);
                    return;
                }
                if (!bank.hasAccount(seller, BankingTypeRegistry.TRADING)) {
                    // delete order so other orders can be processed.
                    cancel(tradeInfo.getSellId(), OrderType.SELL);
                    return;
                }

                IMemento buyerState = buyer.saveState();
                IMemento sellerState = seller.saveState();
                IMemento bankState = bank.saveState();
                CentralBank finalBank = bank;
                TradeResult result = FailSensitiveTradeResult.of(() -> {
                    // get listing info
                    AssetListing listing = assetListingManager.get(tradeInfo.getListingUuid())
                            .map(Reference::get)
                            .orElse(null);

                    // order exist but listing doesn't? Weird.
                    if (listing == null) {
                        logger.warning("Found broken orders. They are deleted.");
                        logger.warning("Trade Info: " + tradeInfo);
                        cancelBoth(tradeInfo);
                        return TradeResult.INVALID_INFO;
                    }
                    AssetSignature signature = listing.getSignature();

                    // amount, price
                    int amount = Math.min(tradeInfo.getStock(), tradeInfo.getAmount()); // use smaller of buy/sell
                    double price = tradeInfo.getAsk(); // use the seller defined price

                    // take asset from seller account
                    int amountsRemoved;
                    if (signature.isPhysical()) // amount varies only for physical assets
                        amountsRemoved = finalBank.removeAccountAsset(seller, signature, amount);
                    else
                        amountsRemoved = finalBank.removeAccountAsset(seller, signature, 1);

                    // trade only if at least one asset is removed successfully
                    if (amountsRemoved > 0) {
                        BigDecimal payTotal = BigDecimal.valueOf(price).multiply(BigDecimal.valueOf(amountsRemoved));

                        // take currency from buyer account
                        if (!finalBank.withdrawAccount(buyer, BankingTypeRegistry.TRADING, payTotal, currency)) {
                            // cancel it since this buyer is unable to pay the initially promised price
                            // give other buyers chance to purchase
                            cancel(tradeInfo.getBuyId(), OrderType.BUY);
                            buyer.removeOrderId(OrderType.BUY, tradeInfo.getBuyId());
                            return TradeResult.WITHDRAW_REFUSED;
                        }

                        // give currency to the seller account
                        if (!finalBank.depositAccount(seller, BankingTypeRegistry.TRADING, payTotal, currency)) {
                            // something wrong with this seller's account, so cancel it
                            // give other listed items the chance to be sold
                            cancel(tradeInfo.getSellId(), OrderType.SELL);
                            seller.removeOrderId(OrderType.SELL, tradeInfo.getSellId());
                            return TradeResult.DEPOSIT_REFUSED;
                        }

                        // give asset to the buyer account
                        finalBank.addAccountAsset(buyer, signature.create(new HashMap<String, Object>() {{
                            put(PhysicalAssetSignature.KEY_AMOUNT, amountsRemoved);
                        }}));

                        // adjust the removed amount
                        try {
                            int newStock = tradeInfo.getStock() - amountsRemoved;
                            if (newStock == 0) {
                                assetListingManager.cancelOrder(tradeInfo.getSellId(), OrderType.SELL, index ->
                                        seller.removeOrderId(OrderType.SELL, index));
                            } else if (newStock > 0) {
                                assetListingManager.editOrder(tradeInfo.getSellId(),
                                        OrderType.SELL,
                                        newStock);
                            } else {
                                throw new RuntimeException("new stock became negative. How?");
                            }

                            int newAmount = tradeInfo.getAmount() - amountsRemoved;
                            if (newAmount == 0) {
                                assetListingManager.cancelOrder(tradeInfo.getBuyId(), OrderType.BUY, index ->
                                        buyer.removeOrderId(OrderType.BUY, index));
                            } else if (newAmount > 0) {
                                assetListingManager.editOrder(tradeInfo.getBuyId(),
                                        OrderType.BUY,
                                        newStock);
                            } else {
                                throw new RuntimeException("new amount became negative. How?");
                            }

                            // log results
                            logTrade(tradeInfo, amountsRemoved);
                        } catch (SQLException ex) {
                            throw new RuntimeException("Trade Info: " + tradeInfo, ex);
                        }
                    } else {
                        // No asset was removed from the seller
                        // in this case, the seller is unable to deliver the promised asset to the buyer
                        // cancel this order so other sellers can get chance to sell their assets.
                        cancel(tradeInfo.getSellId(), OrderType.SELL);
                        seller.removeOrderId(OrderType.SELL, tradeInfo.getSellId());
                        return TradeResult.INSUFFICIENT_ASSETS;
                    }

                    // finalize SQL transaction
                    try {
                        assetListingManager.commitOrders();
                    } catch (SQLException ex) {
                        ex.printStackTrace();
                    }

                    return TradeResult.OK;
                }).handleException(Throwable::printStackTrace).onFail(() -> {
                    buyer.restoreState(buyerState);
                    seller.restoreState(sellerState);
                    finalBank.restoreState(bankState);

                    try {
                        assetListingManager.rollbackOrders();
                    } catch (SQLException ex) {
                        ex.printStackTrace();
                    }
                }).run();

                // since this is an un-handled case, stop the broker
                if (result == null) {
                    tradeBroker.interrupt();
                }

                buyer.handleTransactionResult(tradeInfo, OrderType.BUY, result);
                seller.handleTransactionResult(tradeInfo, OrderType.SELL, result);
            });
        }

        private void cancelBoth(TradeInfo tradeInfo) {
            try {
                assetListingManager.cancelOrder(tradeInfo.getBuyId(), OrderType.BUY, index -> {
                });
                assetListingManager.cancelOrder(tradeInfo.getSellId(), OrderType.SELL, index -> {
                });
                assetListingManager.commitOrders();
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
        }

        private void cancel(int orderId, OrderType type) {
            try {
                assetListingManager.cancelOrder(orderId, type, index -> {
                });
                assetListingManager.commitOrders();
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
        }

        private void logTrade(TradeInfo tradeInfo, int amountTraded) {
            try {
                assetListingManager.logOrder(tradeInfo, amountTraded);
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
        }
    }

    public static class FailSensitiveTradeResult extends FailSensitiveTaskGeneric<FailSensitiveTradeResult, TradeResult> {
        private FailSensitiveTradeResult(Supplier<TradeResult> task,
                                         TradeResult expected) {
            super(task, expected);
        }

        public static FailSensitiveTradeResult of(Supplier<TradeResult> task) {
            return new FailSensitiveTradeResult(task, TradeResult.OK);
        }
    }

    public enum OrderResult {

    }

    public enum TradeResult {
        INVALID_INFO, WITHDRAW_REFUSED, DEPOSIT_REFUSED, INSUFFICIENT_ASSETS, NO_ACCOUNT_BUYER, NO_ACCOUNT_SELLER, OK
    }
}
