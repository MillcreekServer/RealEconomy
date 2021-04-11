package io.github.wysohn.realeconomy.interfaces.trade;

import io.github.wysohn.rapidframework3.interfaces.paging.DataProvider;
import io.github.wysohn.rapidframework3.utils.trie.StringListTrie;
import io.github.wysohn.realeconomy.interfaces.banking.IOrderIssuer;
import io.github.wysohn.realeconomy.manager.currency.Currency;
import io.github.wysohn.realeconomy.manager.listing.*;

import java.sql.SQLException;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * The helper to process various order transactions.
 * <p>
 * Methods must be implemented in a thread-safe way, yet they are expected to be blocking operation.
 */
public interface IOrderQueryModule {

    /**
     * Get category id of the specified category name.
     * <p>
     * This is always successful as it assigns new id for the category that did not exist.
     *
     * @param category category name
     * @return id of the category
     */
    int getCategoryId(String category);

    /**
     * Add order for sell or purchase.
     * <p>
     * For simplicity, this method must be thread-safe, so the callers do not have to worry about
     * the SQL concurrency issue.
     * {@link #commitOrders()} must be invoked to finalize the transaction.
     *
     * @param listingUuid uuid of asset listing specified in
     *                    {@link AssetListingManager}. This method doesn't manually
     *                    check if the given UUID is actually valid.
     * @param category    category of the asset
     * @param type        order type
     * @param issuer      the order issuer
     * @param price       price
     * @param currency    currency of price
     * @param stock       number of stocks
     * @param temp  mark this order as temporary; flag will be set for this record
     * @throws SQLException if some unexpected SQL error occurs.
     */
    void addOrder(UUID listingUuid,
                  String category,
                  OrderType type,
                  IOrderIssuer issuer,
                  double price,
                  Currency currency,
                  int stock,
                  boolean temp) throws SQLException;

    /**
     * Get current order info. This is merely a snapshot of the order, so the order may or may not be valid
     * when you try to alter the order later.
     *
     * @param orderId order id
     * @param type    type of order to search for
     * @return the info; null if no order exist with the given id
     * @throws SQLException if some unexpected SQL error occurs.
     */
    OrderInfo getInfo(int orderId,
                      OrderType type) throws SQLException;

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
     * Log the given trade information.
     *
     * @param listingUuid uuid of listing
     * @param categoryId  category id
     * @param seller      the one sold the asset
     * @param buyer       the one bought the asset
     * @param price       price of an asset
     * @param currency    currency used to trade
     * @param amount      amount traded
     */
    void logOrder(UUID listingUuid,
                  int categoryId,
                  UUID seller,
                  UUID buyer,
                  double price,
                  UUID currency,
                  int amount) throws SQLException;

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
     * Clean-up all buy orders that is flagged with 'temp' field set true.
     * <p>
     * This will require full table scan, so it's better to use it only in appropriate
     * places like enable/disable step.
     */
    void clearTemporaryBuyOrders() throws SQLException;

    /**
     * Clean-up all sell orders that is flagged with 'temp' field set true.
     * <p>
     * This will require full table scan, so it's better to use it only in appropriate
     * places like enable/disable step.
     */
    void clearTemporarySellOrders() throws SQLException;

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
     * Get String Trie of categories for query.
     *
     * @return
     */
    StringListTrie categoryList();

    /**
     * Method to be used to update the listingUuid mapping.
     * <p>
     * This will not be used by plugin itself,
     * but it can be useful for the cross-platform service that queries the order sql database directly.
     * If not set by this method, the other service that rely on the data of order sql database have no way
     * to identify what items are involved in the transaction.
     *
     * @param listingUuid target listing UUID
     * @param name        the name to be mapped with for the given listing UUID
     */
    void setListingName(UUID listingUuid, String name);

    /**
     * Method to be used to update the currencyUuid mapping. This will
     * <p>
     * This will not be used by plugin itself,
     * but it can be useful for the cross-platform service that queries the order sql database directly.
     * If not set by this method, the other service that rely on the data of order sql database have no way
     * to identify what items are involved in the transaction.
     *
     * @param currencyUuid uuid of currency
     * @param full         full name of currency
     * @param shorter      short currency code
     */
    void setCurrencyName(UUID currencyUuid, String full, String shorter);

    /**
     * Get the price point of the last successful trade within the given period.
     *
     * @param daysPeriod   number of past days to query
     * @param currencyUuid uuid of currency
     * @param listingUuid  uuid of listing
     * @return the price point; null if not found
     */
    PricePoint getLastTradingPrice(int daysPeriod, UUID currencyUuid, UUID listingUuid);

    /**
     * Get the average price trend within the given period.
     *
     * @param daysPeriod   number of past days to query
     * @param currencyUuid uuid of currency
     * @param listingUuid  uuid of listing
     * @return the average price; less than 0.0 if not applicable
     */
    double getLastTradingAverage(int daysPeriod, UUID currencyUuid, UUID listingUuid);

    /**
     * Get lowest ask for the given signature. Users are probably interested
     * in what is the cheapest offer prior to decide at what price to bid on.
     *
     * @param currencyUuid uuid of currency
     * @param listingUuid  uuid of listing
     * @return the lowest ask order; null if found none
     */
    OrderInfo getLowestAsk(UUID currencyUuid, UUID listingUuid);

    /**
     * Get highest bid for the given signature. Users are probably interested
     * in what is the highest price it can offer to decide at what price to sell
     * the asset for.
     *
     * @param currencyUuid uuid of currency
     * @param listingUuid  uuid of listing
     * @return the highest bid order; null if found none
     */
    OrderInfo getHighestBid(UUID currencyUuid, UUID listingUuid);

    /**
     * Get highest price point in given period
     *
     * @param daysPeriod   number of past days to query
     * @param currencyUuid uuid of currency
     * @param listingUuid  uuid of listing
     * @return the price point; null if not found
     */
    PricePoint getHighestPoint(int daysPeriod, UUID currencyUuid, UUID listingUuid);

    /**
     * Get lowest price point in given period
     * @param daysPeriod number of past days to query
     * @param currencyUuid uuid of currency
     * @param listingUuid uuid of listing
     * @return the price point; null if not found
     */
    PricePoint getLowestPoint(int daysPeriod, UUID currencyUuid, UUID listingUuid);

    /**
     * Get DataProvider for the currently listed selling orders.
     * <p>
     * For simplicity, this method must be thread-safe, so the callers do not have to worry about
     * the SQL concurrency issue.
     *
     * @param category
     * @return
     */
    DataProvider<OrderInfo> getListedOrderProvider(String category);

    default DataProvider<OrderInfo> getListedOrderProvider() {
        return getListedOrderProvider(null);
    }
}
