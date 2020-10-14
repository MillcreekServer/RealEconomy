package io.github.wysohn.realeconomy.mediator;

import io.github.wysohn.rapidframework3.core.main.Mediator;
import io.github.wysohn.rapidframework3.interfaces.paging.DataProvider;
import io.github.wysohn.rapidframework3.utils.Validation;
import io.github.wysohn.realeconomy.interfaces.banking.IOrderIssuer;
import io.github.wysohn.realeconomy.interfaces.banking.IOrderIssuerProvider;
import io.github.wysohn.realeconomy.manager.asset.listing.AssetListingManager;
import io.github.wysohn.realeconomy.manager.asset.listing.OrderInfo;
import io.github.wysohn.realeconomy.manager.asset.listing.OrderType;
import io.github.wysohn.realeconomy.manager.asset.signature.AssetSignature;
import io.github.wysohn.realeconomy.manager.currency.Currency;
import io.github.wysohn.realeconomy.manager.currency.CurrencyManager;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.sql.SQLException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Singleton
public class TradeMediator extends Mediator {
    private final ExecutorService tradeExecutor = Executors.newSingleThreadExecutor();

    private final CurrencyManager currencyManager;
    private final AssetListingManager assetListingManager;
    private final IOrderIssuerProvider orderIssuerProvider;

    private TradeBroker tradeBroker;

    @Inject
    public TradeMediator(CurrencyManager currencyManager,
                         AssetListingManager assetListingManager,
                         IOrderIssuerProvider orderIssuerProvider) {
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
            } catch (SQLException ex) {
                ex.printStackTrace();
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
            } catch (SQLException ex) {
                ex.printStackTrace();
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

        }
    }
}
