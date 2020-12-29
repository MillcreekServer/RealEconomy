package io.github.wysohn.realeconomy.inject.module;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import io.github.wysohn.rapidframework3.core.paging.DataProviderProxy;
import io.github.wysohn.rapidframework3.core.paging.Range;
import io.github.wysohn.rapidframework3.interfaces.io.IPluginResourceProvider;
import io.github.wysohn.rapidframework3.interfaces.paging.DataProvider;
import io.github.wysohn.rapidframework3.utils.Pair;
import io.github.wysohn.rapidframework3.utils.Validation;
import io.github.wysohn.rapidframework3.utils.sql.SQLSession;
import io.github.wysohn.rapidframework3.utils.trie.StringListTrie;
import io.github.wysohn.realeconomy.inject.annotation.OrderSQL;
import io.github.wysohn.realeconomy.interfaces.banking.IOrderIssuer;
import io.github.wysohn.realeconomy.interfaces.trade.IOrderPlacementHandler;
import io.github.wysohn.realeconomy.main.Metrics;
import io.github.wysohn.realeconomy.manager.currency.Currency;
import io.github.wysohn.realeconomy.manager.listing.OrderInfo;
import io.github.wysohn.realeconomy.manager.listing.OrderType;
import io.github.wysohn.realeconomy.manager.listing.TradeInfo;

import java.io.IOException;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.*;
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
        orderPlacementHandler.INSERT_CATEGORY = Metrics.resourceToString(resourceProvider, "insert_category.sql");
        orderPlacementHandler.INSERT_LOG = Metrics.resourceToString(resourceProvider, "insert_trade_log.sql");
        orderPlacementHandler.INSERT_LISTING_NAME = Metrics.resourceToString(resourceProvider, "insert_listing_name.sql");
        orderPlacementHandler.INSERT_CURRENCY_NAME = Metrics.resourceToString(resourceProvider, "insert_currency_name.sql");
        orderPlacementHandler.UPDATE_BUY = Metrics.resourceToString(resourceProvider, "update_buy_orders.sql");
        orderPlacementHandler.UPDATE_SELL = Metrics.resourceToString(resourceProvider, "update_sell_orders.sql");
        orderPlacementHandler.DELETE_BUY = Metrics.resourceToString(resourceProvider, "delete_buy_order.sql");
        orderPlacementHandler.DELETE_SELL = Metrics.resourceToString(resourceProvider, "delete_sell_order.sql");
        orderPlacementHandler.SELECT_BY_BUY_ID = Metrics.resourceToString(resourceProvider, "select_buy_order_by_id.sql");
        orderPlacementHandler.SELECT_BY_SELL_ID = Metrics.resourceToString(resourceProvider, "select_sell_order_by_id.sql");
        orderPlacementHandler.SELECT_CATEGORIES = Metrics.resourceToString(resourceProvider, "select_categories.sql");
        orderPlacementHandler.SELECT_MATCH_ORDERS
                = Metrics.resourceToString(resourceProvider, "select_match_orders.sql");
        orderPlacementHandler.SELECT_SELL_ORDERS
                = Metrics.resourceToString(resourceProvider, "select_sell_orders.sql");
        orderPlacementHandler.SELECT_SELL_ORDERS_ALL
                = Metrics.resourceToString(resourceProvider, "select_sell_orders_all.sql");

        List<Pair<String, Integer>> list = orderSql.query(orderPlacementHandler.SELECT_CATEGORIES, pstmt -> {
        }, rs -> {
            int category_id = 0;
            try {
                category_id = rs.getInt("category_id");
                String category_value = rs.getString("category_value");
                return Pair.of(category_value, category_id);
            } catch (SQLException ex) {
                ex.printStackTrace();
                return null;
            }
        });
        list.stream()
                .filter(Objects::nonNull)
                .forEach(pair -> {
                    orderPlacementHandler.categoryIdMap.put(pair.key, pair.value);
                    orderPlacementHandler.categoryTrie.insert(pair.key);
                });

        return orderPlacementHandler;
    }

    private static class OrderPlacementHandler implements IOrderPlacementHandler {
        private final Map<String, Integer> categoryIdMap = new HashMap<>();
        private final StringListTrie categoryTrie = new StringListTrie();

        private final SQLSession ordersSession;
        private final Map<Integer, DataProvider<OrderInfo>> dataProviderMap = new HashMap<>();

        private String INSERT_BUY;
        private String INSERT_SELL;
        private String INSERT_CATEGORY;
        private String INSERT_LOG;
        private String INSERT_LISTING_NAME;
        private String INSERT_CURRENCY_NAME;
        private String UPDATE_BUY;
        private String UPDATE_SELL;
        private String DELETE_BUY;
        private String DELETE_SELL;
        private String SELECT_BY_BUY_ID;
        private String SELECT_BY_SELL_ID;
        private String SELECT_CATEGORIES;
        private String SELECT_MATCH_ORDERS;
        private String SELECT_SELL_ORDERS;
        private String SELECT_SELL_ORDERS_ALL;

        public OrderPlacementHandler(SQLSession ordersSession) {
            this.ordersSession = ordersSession;
        }

        @Override
        public void addOrder(UUID listingUuid,
                             String category,
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

            int categoryId = getCategoryId(category);
            ordersSession.execute(sql, pstmt -> {
                try {
                    pstmt.setString(1, listingUuid.toString());
                    pstmt.setInt(2, categoryId);
                    pstmt.setTimestamp(3, new Timestamp(System.currentTimeMillis()));
                    pstmt.setString(4, issuer.getUuid().toString());
                    pstmt.setDouble(5, price);
                    pstmt.setString(6, currency.getKey().toString());
                    pstmt.setInt(7, stock);
                    pstmt.setInt(8, stock);
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }
            }, index -> issuer.addOrderId(type, index.intValue()));
        }

        private int getCategoryId(String category) {
            if (!categoryIdMap.containsKey(category)) {
                ordersSession.execute(INSERT_CATEGORY, pstmt -> {
                    try {
                        pstmt.setString(1, category);
                    } catch (SQLException ex) {
                        ex.printStackTrace();
                    }
                }, key -> {
                    categoryIdMap.put(category, key.intValue());
                    categoryTrie.insert(category);
                });
            }

            return categoryIdMap.get(category);
        }

        @Override
        public OrderInfo getInfo(int orderId, OrderType type) throws SQLException {
            String sql;
            if (type == OrderType.BUY) {
                sql = SELECT_BY_BUY_ID;
            } else if (type == OrderType.SELL) {
                sql = SELECT_BY_SELL_ID;
            } else {
                throw new RuntimeException("Unknown order type " + type);
            }

            return ordersSession.query(sql, pstmt -> {
                try {
                    pstmt.setInt(1, orderId);
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }
            }, resultSet -> {
                try {
                    return OrderInfo.read(resultSet);
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }

                return null;
            }).stream().findFirst().orElse(null);
        }

        @Override
        public void editOrder(int orderId, OrderType type, int newAmount) throws SQLException {
            String sql;
            if (type == OrderType.BUY) {
                sql = UPDATE_BUY;
            } else if (type == OrderType.SELL) {
                sql = UPDATE_SELL;
            } else {
                throw new RuntimeException("Unknown order type " + type);
            }

            ordersSession.execute(sql, pstmt -> {
                try {
                    pstmt.setInt(1, newAmount);
                    pstmt.setInt(2, orderId);
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }
            }, index -> {
            });
        }

        @Override
        public void logOrder(UUID listingUuid,
                             int categoryId,
                             UUID seller,
                             UUID buyer,
                             double price,
                             UUID currency,
                             int amount) throws SQLException {
            String sql = INSERT_LOG;

            ordersSession.execute(sql, pstmt -> {
                try {
                    pstmt.setString(1, listingUuid.toString());
                    pstmt.setInt(2, categoryId);
                    pstmt.setTimestamp(3, new Timestamp(System.currentTimeMillis()));
                    pstmt.setString(4, seller.toString());
                    pstmt.setString(5, buyer.toString());
                    pstmt.setDouble(6, price);
                    pstmt.setString(7, currency.toString());
                    pstmt.setInt(8, amount);
                } catch (SQLException throwables) {
                    throwables.printStackTrace();
                }
            }, index -> {
            });
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
        }

        @Override
        public void commitOrders() throws SQLException {
            ordersSession.commit();
        }

        @Override
        public void rollbackOrders() throws SQLException {
            ordersSession.rollback();
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
        public StringListTrie categoryList() {
            return categoryTrie;
        }

        @Override
        public void setListingName(UUID listingUuid, String name) {
            Validation.assertNotNull(listingUuid);
            Validation.assertNotNull(name);

            String sql = INSERT_LISTING_NAME;

            ordersSession.execute(sql, (pstmt) -> {
                try {
                    pstmt.setString(1, listingUuid.toString());
                    pstmt.setString(2, name);
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }
            }, index -> {
            });
        }

        @Override
        public void setCurrencyName(UUID currencyUuid, String full, String shorter) {
            Validation.assertNotNull(currencyUuid);
            Validation.assertNotNull(full);
            Validation.assertNotNull(shorter);

            String sql = INSERT_CURRENCY_NAME;

            ordersSession.execute(sql, (pstmt) -> {
                try {
                    pstmt.setString(1, currencyUuid.toString());
                    pstmt.setString(2, full);
                    pstmt.setString(3, shorter);
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }
            }, index -> {
            });
        }

        @Override
        public DataProvider<OrderInfo> getListedOrderProvider(String category) {
            boolean queryAll = false;
            if (category == null) {
                queryAll = true;
            }

            if (category != null && !categoryIdMap.containsKey(category))
                throw new RuntimeException("Unknown category " + category);
            int categoryId = category == null ? 0 : categoryIdMap.get(category);

            boolean finalQueryAll = queryAll;
            return dataProviderMap.computeIfAbsent(categoryId, c -> {
                OrderDataProvider orderDataProvider = new OrderDataProvider(c, finalQueryAll);
                return new DataProviderProxy<>(orderDataProvider, orderDataProvider);
            });
        }

        private class OrderDataProvider implements Function<Range, List<OrderInfo>>, Supplier<Integer> {
            private static final String COLUMN_COUNT = "rows_count";

            private final int categoryId;
            private final boolean queryAll;

            public OrderDataProvider(int categoryId, boolean queryAll) {
                this.categoryId = categoryId;
                this.queryAll = queryAll;
            }

            @Override
            public Integer get() {
                List<Integer> out = ordersSession.query("SELECT COUNT(" + OrderSQLModule.ORDER_ID + ") as " + COLUMN_COUNT +
                        " FROM sell_orders" +
                        (queryAll ? "" : " WHERE " + OrderSQLModule.CATEGORY_ID + " = ?;"), pstmt -> {
                    try {
                        if (!queryAll)
                            pstmt.setInt(1, categoryId);
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
                            pstmt.setInt(i++, categoryId);
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
