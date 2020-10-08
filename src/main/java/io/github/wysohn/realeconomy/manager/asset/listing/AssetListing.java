package io.github.wysohn.realeconomy.manager.asset.listing;

import io.github.wysohn.rapidframework3.core.caching.CachedElement;
import io.github.wysohn.rapidframework3.utils.Pair;
import io.github.wysohn.realeconomy.manager.asset.signature.AssetSignature;

import java.util.Comparator;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.function.Predicate;

/**
 * The Asset that is listed to be purchased/sold.
 * <p>
 * This class is not Thread Safe, thus every method provided must be called in synchronous manner.
 * Internally uses PriorityQueue, so each insertion or poll will take O(log n) time. However,
 * query (for smallest/largest value) will take only O(1) due to the characteristic of PriorityQueue.
 */
public class AssetListing extends CachedElement<AssetSignature> {
    private final PriorityQueue<Order> buyOrders = new PriorityQueue<>(Comparator.reverseOrder());
    private final PriorityQueue<Order> sellOrders = new PriorityQueue<>();

    private AssetListing() {
        super(null);
    }

    public AssetListing(AssetSignature signature) {
        super(signature);
    }

    /**
     * Make an order to purchase this listed Asset.
     *
     * @param order the order details.
     * @return always true.
     */
    public boolean addBuy(Order order) {
        boolean add = buyOrders.add(order);
        notifyObservers();
        return add;
    }

    /**
     * Take the highest call from all of the buy orders.
     * This immediately removes the order from the queue, so use peek method instead
     * if you don't want it to be deleted.
     *
     * @return the buy Order; null if there is no buy orders.
     */
    public Order pollBuy() {
        Order poll = buyOrders.poll();
        notifyObservers();
        return poll;
    }

    /**
     * Take the highest call from all of the buy orders, but do not delete it from queue.
     *
     * @return the buy Order; null if there is no buy orders.
     */
    public Order peekBuy() {
        return buyOrders.peek();
    }

    /**
     * Remove all buy orders provided. This method is best used in bulk to maximize
     * the efficiency. For example, instead of calling this method 100 times with 1 item,
     * it's better to call this method 1 time with 100 items. While the prior case take
     * O(K * N) time to delete everything, the rather case takes only O(N).
     * <p>
     * Also, since this is a linear time operation, expect some overhead.
     *
     * @param orderIds buy orders to be delete
     * @return {@link PriorityQueue#bulkRemove(Predicate)} (bulkRemove only exist in JDK 11 and above)
     */
    public boolean cancelBuy(Set<OrderId> orderIds) {
        return buyOrders.removeAll(orderIds);
    }

    /**
     * Check if there is at least one buy order exist.
     *
     * @return true if exist; false if empty.
     */
    public boolean buyAvailable() {
        return !buyOrders.isEmpty();
    }

    /**
     * Make an order to sell.
     *
     * @param order the sell order
     * @return always true.
     */
    public boolean addSell(Order order) {
        boolean add = sellOrders.add(order);
        notifyObservers();
        return add;
    }

    /**
     * Take the cheapest offer from all of the sell orders.
     * This immediately removes the order from the queue, so use peek method instead
     * if you don't want it to be deleted.
     *
     * @return the sell Order; null if there is no sell orders.
     */
    public Order pollSell() {
        Order poll = sellOrders.poll();
        notifyObservers();
        return poll;
    }

    /**
     * Take the cheapest offer from all of the sell orders, but do not remove it from
     * the queue.
     *
     * @return the sell Order; null if there is no sell orders.
     */
    public Order peekSell() {
        return sellOrders.peek();
    }

    /**
     * Remove all sell orders provided. This method is best used in bulk to maximize
     * the efficiency. For example, instead of calling this method 100 times with 1 item,
     * it's better to call this method 1 time with 100 items. While the prior case take
     * O(K * N) time to delete everything, the rather case takes only O(N).
     * <p>
     * Also, since this is a linear time operation, expect some overhead.
     *
     * @param orderIds buy orders to be delete
     * @return {@link PriorityQueue#bulkRemove(Predicate)} (bulkRemove only exist in JDK 11 and above)
     */
    public boolean cancelSell(Set<OrderId> orderIds) {
        return sellOrders.removeAll(orderIds);
    }

    /**
     * Check if at least one sell order exist.
     *
     * @return true if exist; false if empty.
     */
    public boolean sellAvailable() {
        return !sellOrders.isEmpty();
    }

    /**
     * Compare current buy/sell orders and choose the most appropriate (highest buy price and lowest sell price) pair.
     * The Pair contains BUY order and Sell order.
     *
     * @return (BUY / SELL) pair if exist; null if no valid pair exist.
     */
    public Pair<Order, Order> pollIfMatch() {
        Order buyOrder = peekBuy();
        Order sellOrder = peekSell();
        if (buyOrder.getPrice() > sellOrder.getPrice()) {
            return Pair.of(pollBuy(), pollSell());
        } else {
            return null;
        }
    }
}
