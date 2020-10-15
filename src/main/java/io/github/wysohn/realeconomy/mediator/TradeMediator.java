package io.github.wysohn.realeconomy.mediator;

import io.github.wysohn.rapidframework3.core.inject.annotations.PluginLogger;
import io.github.wysohn.rapidframework3.core.main.Mediator;
import io.github.wysohn.rapidframework3.interfaces.IMemento;
import io.github.wysohn.rapidframework3.interfaces.paging.DataProvider;
import io.github.wysohn.rapidframework3.utils.FailSensitiveTaskGeneric;
import io.github.wysohn.rapidframework3.utils.Validation;
import io.github.wysohn.realeconomy.interfaces.banking.IOrderIssuer;
import io.github.wysohn.realeconomy.interfaces.banking.IOrderIssuerProvider;
import io.github.wysohn.realeconomy.manager.asset.listing.AssetListing;
import io.github.wysohn.realeconomy.manager.asset.listing.AssetListingManager;
import io.github.wysohn.realeconomy.manager.asset.listing.OrderInfo;
import io.github.wysohn.realeconomy.manager.asset.listing.OrderType;
import io.github.wysohn.realeconomy.manager.asset.signature.AssetSignature;
import io.github.wysohn.realeconomy.manager.currency.Currency;
import io.github.wysohn.realeconomy.manager.currency.CurrencyManager;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.lang.ref.Reference;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Supplier;
import java.util.logging.Logger;

@Singleton
public class TradeMediator extends Mediator {
    private final ExecutorService tradeExecutor = Executors.newSingleThreadExecutor();

    private final Logger logger;
    private final CurrencyManager currencyManager;
    private final AssetListingManager assetListingManager;
    private final IOrderIssuerProvider orderIssuerProvider;

    private TradeBroker tradeBroker;

    @Inject
    public TradeMediator(@PluginLogger Logger logger,
                         CurrencyManager currencyManager,
                         AssetListingManager assetListingManager,
                         IOrderIssuerProvider orderIssuerProvider) {
        this.logger = logger;
        this.currencyManager = currencyManager;
        this.assetListingManager = assetListingManager;
        this.orderIssuerProvider = orderIssuerProvider;
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
    }

    @Override
    public void disable() throws Exception {

    }

    public DataProvider<OrderInfo> getPrice() {
        return getPrice(null);
    }

    public DataProvider<OrderInfo> getPrice(AssetSignature signature) {
        return assetListingManager.getListedOrderProvider(signature);
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
     */
    public void sellAsset(IOrderIssuer issuer,
                          AssetSignature signature,
                          double price,
                          Currency currency,
                          int stock) {
        Validation.assertNotNull(issuer);
        Validation.assertNotNull(signature);
        Validation.validate(price, p -> p > 0.0, "Negative or 0.0 price not allowed.");
        Validation.assertNotNull(currency);
        Validation.validate(stock, s -> s > 0, "Negative or 0 stock not allowed.");

        assetListingManager.newListing(signature);

        tradeExecutor.submit(() -> {
            try {
                assetListingManager.addOrder(signature,
                        OrderType.SELL,
                        issuer,
                        price,
                        currency,
                        stock);

                assetListingManager.commitOrders();
            } catch (SQLException ex) {
                ex.printStackTrace();
                assetListingManager.rollbackOrders();
            }
        });
    }

    /**
     * Bid to buy the specified asset. This does not necessarily mean that the buyer
     * can buy the asset immediately.
     *
     * @param issuer    the one want to buy asset
     * @param signature asset signature
     * @param price     bidding price
     * @param currency  currency of price
     * @param amount    goal number of assets to purchase
     */
    public void bidAsset(IOrderIssuer issuer,
                         AssetSignature signature,
                         double price,
                         Currency currency,
                         int amount) {
        Validation.assertNotNull(issuer);
        Validation.assertNotNull(signature);
        Validation.validate(price, p -> p > 0.0, "Negative or 0.0 price not allowed.");
        Validation.assertNotNull(currency);
        Validation.validate(amount, s -> s > 0, "Negative or 0 amount not allowed.");

        assetListingManager.newListing(signature);

        tradeExecutor.submit(() -> {
            try {
                assetListingManager.addOrder(signature,
                        OrderType.BUY,
                        issuer,
                        price,
                        currency,
                        amount);

                assetListingManager.commitOrders();
            } catch (SQLException ex) {
                ex.printStackTrace();
                assetListingManager.rollbackOrders();
            }
        });
    }

    private class TradeBroker extends Thread {
        public TradeBroker() {
            setPriority(NORM_PRIORITY - 1);
            setName("RealEconomy - TradeBroker");
        }

        @Override
        public void run() {
            while (!interrupted()) {
                try {
                    Thread.sleep(1000L);
                } catch (InterruptedException e) {
                    logger.info(getName() + " is interrupted.");
                }

                assetListingManager.peekMatchingOrder(tradeInfo -> {
                    // get buy/sell pair
                    IOrderIssuer buyer = orderIssuerProvider.get(tradeInfo.getBuyer());
                    IOrderIssuer seller = orderIssuerProvider.get(tradeInfo.getSeller());

                    // cannot proceed if either trading end is not found
                    if (buyer == null) {
                        // delete order so other orders can be processed.
                        try {
                            assetListingManager.cancelOrder(tradeInfo.getBuyId(), OrderType.BUY, index -> {
                            });
                        } catch (SQLException ex) {
                            ex.printStackTrace();
                        }
                        return;
                    }
                    if (seller == null) {
                        // delete order so other orders can be processed.
                        try {
                            assetListingManager.cancelOrder(tradeInfo.getSellId(), OrderType.BUY, index -> {
                            });
                        } catch (SQLException ex) {
                            ex.printStackTrace();
                        }
                        return;
                    }

                    IMemento buyerState = buyer.saveState();
                    IMemento sellerState = seller.saveState();
                    TradeResult result = FailSensitiveTradeResult.of(() -> {
                        // delete the orders
                        try {
                            assetListingManager.cancelOrder(tradeInfo.getBuyId(), OrderType.BUY, index ->
                                    buyer.removeOrderId(OrderType.BUY, index));
                            assetListingManager.cancelOrder(tradeInfo.getSellId(), OrderType.BUY, index ->
                                    seller.removeOrderId(OrderType.SELL, index));
                        } catch (SQLException ex) {
                            throw new RuntimeException("Trade Info: " + tradeInfo, ex);
                        }

                        // get listing info
                        AssetListing listing = assetListingManager.get(tradeInfo.getListingUuid())
                                .map(Reference::get)
                                .orElse(null);

                        // order exist but listing doesn't? Weird.
                        if (listing == null) {
                            assetListingManager.commitOrders();
                            logger.warning("Found broken orders. They are deleted.");
                            logger.warning("Trade Info: " + tradeInfo);
                            return TradeResult.INVALID_INFO;
                        }
                        AssetSignature signature = listing.getSignature();

                        // amount, price, currency
                        int amount = tradeInfo.getStock(); // use the seller stock
                        double price = tradeInfo.getAsk(); // use the seller defined price
                        Currency currency = currencyManager.get(tradeInfo.getCurrencyUuid())
                                .map(Reference::get)
                                .orElse(null);

                        // weird currency found.
                        if (currency == null) {
                            assetListingManager.commitOrders();
                            logger.warning("Cannot proceed with unknown Currency. Orders are deleted.");
                            logger.warning("Trade Info: " + tradeInfo);
                            return TradeResult.INVALID_INFO;
                        }

                        // take asset from seller
                        int amountsRemoved = 1;
                        if (signature.isPhysical()) // take only if physically present asset
                            amountsRemoved = seller.removeAsset(signature, amount);
                        else
                            amount = 1;

                        // trade only if at least one asset is removed successfully
                        if (amountsRemoved > 0) {
                            BigDecimal payTotal = BigDecimal.valueOf(price).multiply(BigDecimal.valueOf(amountsRemoved));

                            // take currency from buyer
                            if (!buyer.withdraw(payTotal, currency))
                                return TradeResult.WITHDRAW_REFUSED;

                            // give currency to the seller
                            if (!seller.deposit(payTotal, currency))
                                return TradeResult.DEPOSIT_REFUSED;

                            // give asset to the buyer
                            buyer.addAsset(signature, amountsRemoved);

                            // if there are left overs, re-register the sell order
                            int leftOvers = amount - amountsRemoved;
                            if (leftOvers > 0) {
                                try {
                                    assetListingManager.addOrder(signature,
                                            OrderType.SELL,
                                            seller,
                                            price,
                                            currency,
                                            leftOvers);
                                } catch (SQLException ex) {
                                    throw new RuntimeException("Trade Info: " + tradeInfo, ex);
                                }
                            }
                        }

                        // finalize SQL transaction
                        assetListingManager.commitOrders();
                        return TradeResult.OK;
                    }).handleException(Throwable::printStackTrace).onFail(() -> {
                        buyer.restoreState(buyerState);
                        seller.restoreState(sellerState);

                        assetListingManager.rollbackOrders();
                    }).run();

                    //TODO some kind of message queue to inform the trade result
                    //buyer.addResult(result) ??
                });
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

    public enum TradeResult {
        INVALID_INFO, WITHDRAW_REFUSED, DEPOSIT_REFUSED, OK
    }
}
