package io.github.wysohn.realeconomy.manager.asset.listing;

import io.github.wysohn.realeconomy.interfaces.banking.IOrderIssuer;
import io.github.wysohn.realeconomy.interfaces.banking.IOrderIssuerProvider;
import io.github.wysohn.realeconomy.manager.currency.Currency;
import io.github.wysohn.realeconomy.manager.currency.CurrencyManager;

import java.lang.ref.Reference;
import java.util.UUID;

/**
 * type (sell/buy)
 * price
 * currency
 */
public class Order implements Comparable<Order> {
    private transient final IOrderIssuerProvider orderIssuerProvider;
    private transient final CurrencyManager currencyManager;

    private final OrderId orderId;

    private final UUID issuerUuid;
    private final OrderType type;
    private final double price;
    private final UUID currencyUuid;

    private int amount;
    private final int max;

    public Order(IOrderIssuerProvider orderIssuerProvider,
                 CurrencyManager currencyManager,
                 UUID issuerUuid,
                 OrderType type, double price, UUID currencyUuid, int stock) {
        this.orderIssuerProvider = orderIssuerProvider;
        this.currencyManager = currencyManager;
        this.orderId = OrderId.randomId();
        this.issuerUuid = issuerUuid;
        this.type = type;
        this.price = price;
        this.currencyUuid = currencyUuid;
        this.amount = stock;
        this.max = stock;
    }

    public OrderId getOrderId() {
        return orderId;
    }

    public IOrderIssuer getIssuer() {
        return orderIssuerProvider.get(issuerUuid);
    }

    public OrderType getType() {
        return type;
    }

    public double getPrice() {
        return price;
    }

    public Currency getCurrency() {
        return currencyManager.get(currencyUuid).map(Reference::get).orElse(null);
    }

    public int getAmount() {
        return amount;
    }

    public void setAmount(int amount) {
        this.amount = amount;
    }

    public int getMax() {
        return max;
    }

    @Override
    public int compareTo(Order o) {
        if (currencyUuid.equals(o.currencyUuid)) {
            return Double.compare(price, o.price);
        } else {
            // if different currency, don't compare price
            return currencyUuid.compareTo(o.currencyUuid);
        }
    }
}