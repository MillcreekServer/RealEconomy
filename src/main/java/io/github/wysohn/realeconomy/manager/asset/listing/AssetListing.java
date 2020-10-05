package io.github.wysohn.realeconomy.manager.asset.listing;

import io.github.wysohn.rapidframework3.core.caching.CachedElement;
import io.github.wysohn.rapidframework3.utils.Pair;
import io.github.wysohn.realeconomy.manager.asset.signature.AssetSignature;

import java.util.PriorityQueue;
import java.util.UUID;

/**
 * The Asset that is listed to be purchased/sold.
 * <p>
 * This class is not Thread Safe, thus every method provided must be called in synchronous manner.
 * Internally uses PriorityQueue, so each insertion or poll will take O(log n) time. However,
 * query (for smallest/largest value) will take only O(1) due to the characteristic of PriorityQueue.
 */
public class AssetListing extends CachedElement<AssetSignature> {
    private final PriorityQueue<Order> buyOrders = new PriorityQueue<>();
    private final PriorityQueue<Order> sellOrders = new PriorityQueue<>();

    private AssetListing() {
        super(null);
    }

    public AssetListing(AssetSignature signature) {
        super(signature);
    }

    public boolean addBuy(Order order) {
        boolean add = buyOrders.add(order);
        notifyObservers();
        return add;
    }

    public Order pollBuy(UUID currencyUuid) {
        Order poll = buyOrders.poll();
        notifyObservers();
        return poll;
    }

    public Order peekBuy(UUID currencyUuid) {
        return buyOrders.peek();
    }

    public boolean buyAvailable(UUID currencyUuid) {
        return !buyOrders.isEmpty();
    }

    public boolean addSell(Order order) {
        boolean add = sellOrders.add(order);
        notifyObservers();
        return add;
    }

    public Order pollSell(UUID currencyUuid) {
        Order poll = sellOrders.poll();
        notifyObservers();
        return poll;
    }

    public Order peekSell(UUID currencyUuid) {
        return sellOrders.peek();
    }

    public boolean sellAvailable(UUID currencyUuid) {
        return !sellOrders.isEmpty();
    }

    /**
     * Compare current buy/sell orders and choose the most appropriate (highest buy price and lowest sell price) pair.
     * The Pair contains BUY order and Sell order.
     *
     * @param currencyUuid currency to query
     * @return (BUY / SELL) pair if exist; null if no valid pair exist.
     */
    public Pair<Order, Order> pollIfMatch(UUID currencyUuid) {
        Order buyOrder = peekBuy(currencyUuid);
        Order sellOrder = peekSell(currencyUuid);
        if (buyOrder.getPrice() > sellOrder.getPrice()) {
            return Pair.of(pollBuy(currencyUuid), pollSell(currencyUuid));
        } else {
            return null;
        }
    }
}
