package io.github.wysohn.realeconomy.manager.asset.listing;

import java.util.Objects;
import java.util.UUID;

/**
 * type (sell/buy)
 * price
 * currency
 */
public class Order implements Comparable<Order> {
    private final OrderId orderId;

    private final UUID issuerUuid;
    private final double price;
    private final UUID currencyUuid;

    private int amount;
    private final int max;

    private Order() {
        this(null, -0.0, null, -0);
    }

    public Order(UUID issuerUuid,
                 double price,
                 UUID currencyUuid,
                 int stock) {
        this.orderId = OrderId.randomId();
        this.issuerUuid = issuerUuid;
        this.price = price;
        this.currencyUuid = currencyUuid;
        this.amount = stock;
        this.max = stock;
    }

    public OrderId getOrderId() {
        return orderId;
    }

    public UUID getIssuerUuid() {
        return issuerUuid;
    }

    public double getPrice() {
        return price;
    }

    public UUID getCurrencyUuid() {
        return currencyUuid;
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null) return false;
        if (o instanceof OrderId) {
            // this allows direct comparison of Order and OrderId
            return orderId.equals(o);
        } else if (o instanceof Order) {
            Order order = (Order) o;
            return orderId.equals(order.orderId);
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return Objects.hash(orderId);
    }
}