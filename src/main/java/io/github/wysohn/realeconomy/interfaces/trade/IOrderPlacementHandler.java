package io.github.wysohn.realeconomy.interfaces.trade;

import io.github.wysohn.rapidframework3.interfaces.paging.DataProvider;
import io.github.wysohn.realeconomy.interfaces.banking.IOrderIssuer;
import io.github.wysohn.realeconomy.manager.asset.listing.OrderInfo;
import io.github.wysohn.realeconomy.manager.asset.listing.OrderType;
import io.github.wysohn.realeconomy.manager.asset.listing.TradeInfo;
import io.github.wysohn.realeconomy.manager.currency.Currency;

import java.sql.SQLException;
import java.util.UUID;
import java.util.function.Consumer;

public interface IOrderPlacementHandler {
    /**
     * Add order for sell or purchase.
     * <p>
     * For simplicity, this method must be thread-safe, so the callers do not have to worry about
     * the SQL concurrency issue.
     * {@link #commitOrders()} must be invoked to finalize the transaction.
     *
     * @param listingUuid uuid of asset listing specified in
     *                    {@link io.github.wysohn.realeconomy.manager.asset.listing.AssetListingManager}. This method doesn't manually
     *                    check if the given UUID is actually valid.
     * @param type        order type
     * @param issuer      the order issuer
     * @param price       price
     * @param currency    currency of price
     * @param stock       number of stocks
     * @throws SQLException if some unexpected SQL error occurs.
     */
    void addOrder(UUID listingUuid,
                  OrderType type,
                  IOrderIssuer issuer,
                  double price,
                  Currency currency,
                  int stock) throws SQLException;

    /**
     * Edit the current amount of the order. This can be useful when the order is not fully consumed, yet the order of
     * id must be maintained to give fair opportunity (cause if we just delete the order and then add the
     * order again, the order will be placed at the very end of the queue) for all buyers.
     *
     * @param orderId   the order id (which is stored by {@link IOrderIssuer#addOrderId(OrderType, int)}
     * @param type      the order type
     * @param newAmount new adjusted amount. This does not have any validation, so caller must verify it.
     */
    void editOrder(int orderId,
                   OrderType type,
                   int newAmount) throws SQLException;

    /**
     * Cancel the order that was already scheduled. This is valid only if the order is not yet processed.
     * <p>
     * For simplicity, this method must be thread-safe, so the callers do not have to worry about
     * the SQL concurrency issue.
     * {@link #commitOrders()} must be invoked to finalize the transaction.
     *
     * @param orderId  the order id (which is stored by {@link IOrderIssuer#addOrderId(OrderType, int)}
     * @param type     the order type
     * @param callback callback. If successful, the 'orderId' will be returned; or 0, if failed.
     * @throws SQLException
     */
    void cancelOrder(int orderId,
                     OrderType type,
                     Consumer<Integer> callback) throws SQLException;

    /**
     * Commit the scheduled order operations invoked by
     * {@link #addOrder(UUID, OrderType, IOrderIssuer, double, Currency, int)} and
     * {@link #cancelOrder(int, OrderType, Consumer)}.
     */
    void commitOrders() throws SQLException;

    /**
     * Rollback any changes made by
     * {@link #addOrder(UUID, OrderType, IOrderIssuer, double, Currency, int)} and
     * {@link #cancelOrder(int, OrderType, Consumer)}
     * and rollback to the previous state saved by last {@link #commitOrders()} call.
     */
    void rollbackOrders() throws SQLException;

    /**
     * Get the best buy/sell order pair so that they can be matched up.
     * <p>
     * For simplicity, this method must be thread-safe, so the callers do not have to worry about
     * the SQL concurrency issue.
     *
     * @param consumer
     */
    void peekMatchingOrders(Consumer<TradeInfo> consumer);

    /**
     * Get DataProvider for the currently listed selling orders.
     * <p>
     * For simplicity, this method must be thread-safe, so the callers do not have to worry about
     * the SQL concurrency issue.
     *
     * @param listingUuid
     * @return
     */
    DataProvider<OrderInfo> getListedOrderProvider(UUID listingUuid);

    default DataProvider<OrderInfo> getListedOrderProvider() {
        return getListedOrderProvider(null);
    }
}
