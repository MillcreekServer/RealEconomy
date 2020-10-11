package io.github.wysohn.realeconomy.mediator;

import io.github.wysohn.rapidframework3.core.main.Mediator;
import io.github.wysohn.rapidframework3.utils.Validation;
import io.github.wysohn.realeconomy.interfaces.banking.IOrderIssuer;
import io.github.wysohn.realeconomy.interfaces.banking.IOrderIssuerProvider;
import io.github.wysohn.realeconomy.manager.asset.listing.AssetListingManager;
import io.github.wysohn.realeconomy.manager.asset.listing.OrderType;
import io.github.wysohn.realeconomy.manager.asset.signature.AssetSignature;
import io.github.wysohn.realeconomy.manager.currency.Currency;
import io.github.wysohn.realeconomy.manager.currency.CurrencyManager;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.sql.SQLException;

@Singleton
public class TradeMediator extends Mediator {
    private final CurrencyManager currencyManager;
    private final AssetListingManager assetListingManager;
    private final IOrderIssuerProvider orderIssuerProvider;

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

    }

    @Override
    public void disable() throws Exception {

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
    public void listAsset(IOrderIssuer issuer,
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
    }
}
