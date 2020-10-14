package io.github.wysohn.realeconomy.inject.module;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import io.github.wysohn.rapidframework3.core.paging.DataProviderProxy;
import io.github.wysohn.rapidframework3.core.paging.Range;
import io.github.wysohn.rapidframework3.interfaces.io.IPluginResourceProvider;
import io.github.wysohn.rapidframework3.interfaces.paging.DataProvider;
import io.github.wysohn.rapidframework3.utils.sql.SQLSession;
import io.github.wysohn.realeconomy.inject.annotation.OrderSQL;
import io.github.wysohn.realeconomy.interfaces.banking.IOrderIssuer;
import io.github.wysohn.realeconomy.interfaces.trade.IOrderPlacementHandler;
import io.github.wysohn.realeconomy.main.Metrics;
import io.github.wysohn.realeconomy.manager.asset.listing.OrderInfo;
import io.github.wysohn.realeconomy.manager.asset.listing.OrderType;
import io.github.wysohn.realeconomy.manager.asset.listing.TradeInfo;
import io.github.wysohn.realeconomy.manager.currency.Currency;

import java.io.IOException;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public class OrderPlacementHandlerModule extends AbstractModule {
    @Provides
    @Singleton
    IOrderPlacementHandler orderPlacementHandle(@OrderSQL SQLSession orderSql,
                                                IPluginResourceProvider resourceProvider)
            throws IOException {
        OrderPlacementHandler orderPlacementHandler = new OrderPlacementHandler(orderSql);
        orderPlacementHandler.INSERT_BUY = Metrics.resourceToString(resourceProvider, "insert_buy_order.sql");
        orderPlacementHandler.INSERT_SELL = Metrics.resourceToString(resourceProvider, "insert_sell_order.sql");
        orderPlacementHandler.INSERT_LOG = Metrics.resourceToString(resourceProvider, "insert_trade_log.sql");
        orderPlacementHandler.DELETE_BUY = Metrics.resourceToString(resourceProvider, "delete_buy_order.sql");
        orderPlacementHandler.DELETE_SELL = Metrics.resourceToString(resourceProvider, "delete_sell_order.sql");
        orderPlacementHandler.SELECT_MATCH_ORDERS
                = Metrics.resourceToString(resourceProvider, "select_match_orders.sql");
        orderPlacementHandler.SELECT_SELL_ORDERS
                = Metrics.resourceToString(resourceProvider, "select_sell_orders.sql");
        orderPlacementHandler.SELECT_SELL_ORDERS_ALL
                = Metrics.resourceToString(resourceProvider, "select_sell_orders_all.sql");
        return orderPlacementHandler;
    }

    private static class OrderPlacementHandler implements IOrderPlacementHandler {
        // fake UUID just to store all search data in dataProviderMap
        private static final UUID ALL_LISTINGS_UUID = UUID.fromString("f02a94b0-dde1-4ee3-9496-ea21b27de956");

        private final SQLSession ordersSession;
        private final Map<UUID, DataProvider<OrderInfo>> dataProviderMap = new HashMap<>();

        private String INSERT_BUY;
        private String INSERT_SELL;
        private String INSERT_LOG;
        private String DELETE_BUY;
        private String DELETE_SELL;
        private String SELECT_MATCH_ORDERS;
        private String SELECT_SELL_ORDERS;
        private String SELECT_SELL_ORDERS_ALL;

        public OrderPlacementHandler(SQLSession ordersSession) {
            this.ordersSession = ordersSession;
        }

        @Override
        public void addOrder(UUID listingUuid,
                             OrderType type,
                             IOrderIssuer issuer,
                             double price,
                             Currency currency,
                             int stock) throws SQLException {
            String sql;
            if (type == OrderType.BUY) {
                sql = INSERT_BUY;
            } else if (type == OrderType.SELL) {
                sql = INSERT_SELL;
            } else {
                throw new RuntimeException("Unknown order type " + type);
            }

            ordersSession.execute(sql, pstmt -> {
                try {
                    pstmt.setString(1, listingUuid.toString());
                    pstmt.setLong(2, System.currentTimeMillis());
                    pstmt.setString(3, issuer.getUuid().toString());
                    pstmt.setDouble(4, price);
                    pstmt.setString(5, currency.getKey().toString());
                    pstmt.setInt(6, stock);
                    pstmt.setInt(7, stock);
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }
            }, index -> issuer.addOrderId(type, index.intValue()));

            ordersSession.commit();
        }

        @Override
        public void cancelOrder(int orderId, OrderType type, Consumer<Integer> callback) throws SQLException {
            String sql;
            if (type == OrderType.BUY) {
                sql = DELETE_BUY;
            } else if (type == OrderType.SELL) {
                sql = DELETE_SELL;
            } else {
                throw new RuntimeException("Unknown order type " + type);
            }

            ordersSession.execute(sql, (pstmt) -> {
                try {
                    pstmt.setInt(1, orderId);
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }
            }, index -> {
                if (index > 0)
                    callback.accept(orderId);
                else
                    callback.accept(0);
            });

            ordersSession.commit();
        }

        @Override
        public void peekMatchingOrders(Consumer<TradeInfo> consumer) {
            String sql = SELECT_MATCH_ORDERS;

            List<TradeInfo> infos = ordersSession.query(sql, pstmt -> {
            }, resultSet -> {
                try {
                    return TradeInfo.read(resultSet);
                } catch (SQLException ex) {
                    ex.printStackTrace();
                    return null;
                }
            });

            consumer.accept(infos.stream()
                    .findFirst()
                    .orElse(null));
        }

        @Override
        public DataProvider<OrderInfo> getListedOrderProvider(UUID listingUuid) {
            boolean queryAll = false;
            if (listingUuid == null) {
                queryAll = true;
                // replace so we can use it to store data for all asset listings
                listingUuid = ALL_LISTINGS_UUID;
            }

            boolean finalQueryAll = queryAll;
            return dataProviderMap.computeIfAbsent(listingUuid, c -> {
                OrderDataProvider orderDataProvider = new OrderDataProvider(c, finalQueryAll);
                return new DataProviderProxy<>(orderDataProvider, orderDataProvider);
            });
        }

        private class OrderDataProvider implements Function<Range, List<OrderInfo>>, Supplier<Integer> {
            private static final String COLUMN_COUNT = "rows";

            private final UUID listingUuid;
            private final boolean queryAll;

            public OrderDataProvider(UUID listingUuid, boolean queryAll) {
                this.listingUuid = listingUuid;
                this.queryAll = queryAll;
            }

            @Override
            public Integer get() {
                List<Integer> out = ordersSession.query("SELECT COUNT(" + OrderSQLModule.ORDER_ID + ") as " + COLUMN_COUNT +
                        " FROM sell_orders" +
                        (queryAll ? "" : "WHERE listing_uuid = ?;"), pstmt -> {
                    try {
                        if (!queryAll)
                            pstmt.setString(1, listingUuid.toString());
                    } catch (SQLException ex) {
                        ex.printStackTrace();
                    }
                }, resultSet -> {
                    try {
                        return resultSet.getInt(COLUMN_COUNT);
                    } catch (SQLException ex) {
                        ex.printStackTrace();
                        return 0;
                    }
                });

                return out.stream().findFirst().orElse(0);
            }

            @Override
            public List<OrderInfo> apply(Range range) {
                String sql = queryAll ? SELECT_SELL_ORDERS_ALL : SELECT_SELL_ORDERS;

                return ordersSession.query(sql, pstmt -> {
                    try {
                        int i = 1;
                        if (!queryAll)
                            pstmt.setString(i++, listingUuid.toString());
                        pstmt.setInt(i++, range.index);
                        pstmt.setInt(i++, range.size);
                    } catch (SQLException ex) {
                        ex.printStackTrace();
                    }
                }, resultSet -> {
                    try {
                        return OrderInfo.read(resultSet);
                    } catch (SQLException ex) {
                        ex.printStackTrace();
                        return null;
                    }
                });
            }
        }
    }
}
