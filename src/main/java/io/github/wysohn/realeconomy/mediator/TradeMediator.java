package io.github.wysohn.realeconomy.mediator;

import io.github.wysohn.rapidframework3.core.main.Mediator;
import io.github.wysohn.rapidframework3.utils.Validation;
import io.github.wysohn.realeconomy.interfaces.banking.IOrderIssuer;
import io.github.wysohn.realeconomy.interfaces.banking.IOrderIssuerProvider;
import io.github.wysohn.realeconomy.manager.asset.listing.AssetListingManager;
import io.github.wysohn.realeconomy.manager.asset.listing.Order;
import io.github.wysohn.realeconomy.manager.asset.listing.OrderId;
import io.github.wysohn.realeconomy.manager.asset.signature.AssetSignature;
import io.github.wysohn.realeconomy.manager.currency.Currency;
import io.github.wysohn.realeconomy.manager.currency.CurrencyManager;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.lang.ref.Reference;

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

    public OrderId listAsset(IOrderIssuer issuer,
                             AssetSignature signature,
                             int stock, double price, Currency currency) {
        Validation.assertNotNull(issuer);
        Validation.assertNotNull(signature);
        Validation.validate(stock, s -> s > 0, "Negative or 0 stock not allowed.");
        Validation.validate(price, p -> p > 0.0, "Negative or 0.0 price not allowed.");
        Validation.assertNotNull(currency);

        Order order = new Order(
                issuer.getUuid(),
                price,
                currency.getKey(),
                stock);

        assetListingManager.getOrNew(signature)
                .map(Reference::get)
                .ifPresent(assetListing -> assetListing.addSell(order));

        return order.getOrderId();
    }
}
