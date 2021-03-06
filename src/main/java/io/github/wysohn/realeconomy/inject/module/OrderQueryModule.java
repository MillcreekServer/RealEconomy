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
import io.github.wysohn.realeconomy.interfaces.trade.IOrderQueryModule;
import io.github.wysohn.realeconomy.main.Metrics;
import io.github.wysohn.realeconomy.manager.currency.Currency;
import io.github.wysohn.realeconomy.manager.listing.OrderInfo;
import io.github.wysohn.realeconomy.manager.listing.OrderType;
import io.github.wysohn.realeconomy.manager.listing.PricePoint;
import io.github.wysohn.realeconomy.manager.listing.TradeInfo;

import java.io.IOException;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public class OrderQueryModule extends AbstractModule {
    @Provides
    @Singleton
    IOrderQueryModule orderPlacementHandle(@OrderSQL SQLSession orderSql,
                                           IPluginResourceProvider resourceProvider)
            throws IOException {
        OrderQueryModuleImpl orderPlacementHandler = new OrderQueryModuleImpl(orderSql);
        orderPlacementHandler.INSERT_BUY = Metrics.resourceToString(resourceProvider, "insert_buy_order.sql");
        orderPlacementHandler.INSERT_SELL = Metrics.resourceToString(resourceProvider, "insert_sell_order.sql");
        orderPlacementHandler.INSERT_CATEGORY = Metrics.resourceToString(resourceProvider, "insert_category.sql");
        orderPlacementHandler.INSERT_LOG = Metrics.resourceToString(resourceProvider, "insert_trade_log.sql");
        orderPlacementHandler.INSERT_LISTING_NAME = Metrics.resourceToString(resourceProvider, "insert_listing_name.sql");
        orderPlacementHandler.INSERT_CURRENCY_NAME = Metrics.resourceToString(resourceProvider, "insert_currency_name.sql");
        orderPlacementHandler.UPDATE_BUY = Metrics.resourceToString(resourceProvider, "update_buy_orders.sql");
        orderPlacementHandler.UPDATE_SELL = Metrics.resourceToString(resourceProvider, "update_sell_orders.sql");
        orderPlacementHandler.DELETE_BUY = Metrics.resourceToString(resourceProvider, "delete_buy_order.sql");
        orderPlacementHandler.DELETE_BUY_TEMPS = Metrics.resourceToString(resourceProvider, "delete_buy_temp_orders.sql");
        orderPlacementHandler.DELETE_SELL = Metrics.resourceToString(resourceProvider, "delete_sell_order.sql");
        orderPlacementHandler.DELETE_SELL_TEMPS = Metrics.resourceToString(resourceProvider, "delete_sell_temp_orders.sql");
        orderPlacementHandler.SELECT_BY_BUY_ID = Metrics.resourceToString(resourceProvider, "select_buy_order_by_id.sql");
        orderPlacementHandler.SELECT_BY_SELL_ID = Metrics.resourceToString(resourceProvider, "select_sell_order_by_id.sql");
        orderPlacementHandler.SELECT_CATEGORIES = Metrics.resourceToString(resourceProvider, "select_categories.sql");
        orderPlacementHandler.SELECT_MATCH_ORDERS
                = Metrics.resourceToString(resourceProvider, "select_match_orders.sql");
        orderPlacementHandler.SELECT_BUY_ORDERS
                = Metrics.resourceToString(resourceProvider, "select_buy_orders.sql");
        orderPlacementHandler.SELECT_BUY_ORDERS_ALL
                = Metrics.resourceToString(resourceProvider, "select_buy_orders_all.sql");
        orderPlacementHandler.SELECT_SELL_ORDERS
                = Metrics.resourceToString(resourceProvider, "select_sell_orders.sql");
        orderPlacementHandler.SELECT_SELL_ORDERS_ALL
                = Metrics.resourceToString(resourceProvider, "select_sell_orders_all.sql");
        orderPlacementHandler.SELECT_PRICE_TREND
                = Metrics.resourceToString(resourceProvider, "select_price_trend.sql");
        orderPlacementHandler.SELECT_PRICE_TREND_HIGHEST
                = Metrics.resourceToString(resourceProvider, "select_price_trend_highest.sql");
        orderPlacementHandler.SELECT_PRICE_TREND_LOWEST
                = Metrics.resourceToString(resourceProvider, "select_price_trend_lowest.sql");
        orderPlacementHandler.SELECT_PRICE_TREND_LAST
                = Metrics.resourceToString(resourceProvider, "select_price_trend_last.sql");
        orderPlacementHandler.SELECT_PRICE_TREND_AVG
                = Metrics.resourceToString(resourceProvider, "select_price_trend_avg.sql");
        orderPlacementHandler.SELECT_BUY_ORDER_HIGHEST
                = Metrics.resourceToString(resourceProvider, "select_buy_order_highest.sql");
        orderPlacementHandler.SELECT_SELL_ORDER_LOWEST
                = Metrics.resourceToString(resourceProvider, "select_sell_order_lowest.sql");

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

    private static class OrderQueryModuleImpl implements IOrderQueryModule {
        private final Map<String, Integer> categoryIdMap = new HashMap<>();
        private final StringListTrie categoryTrie = new StringListTrie();

        private final SQLSession ordersSession;
        private final Map<Integer, Map<OrderType, DataProvider<OrderInfo>>> dataProviderMap = new HashMap<>();

        private String INSERT_BUY;
        private String INSERT_SELL;
        private String INSERT_CATEGORY;
        private String INSERT_LOG;
        private String INSERT_LISTING_NAME;
        private String INSERT_CURRENCY_NAME;
        private String UPDATE_BUY;
        private String UPDATE_SELL;
        private String DELETE_BUY;
        private String DELETE_BUY_TEMPS;
        private String DELETE_SELL;
        private String DELETE_SELL_TEMPS;
        private String SELECT_BY_BUY_ID;
        private String SELECT_BY_SELL_ID;
        private String SELECT_CATEGORIES;
        private String SELECT_MATCH_ORDERS;
        private String SELECT_BUY_ORDERS;
        private String SELECT_BUY_ORDERS_ALL;
        private String SELECT_SELL_ORDERS;
        private String SELECT_SELL_ORDERS_ALL;
        private String SELECT_PRICE_TREND;
        private String SELECT_PRICE_TREND_HIGHEST;
        private String SELECT_PRICE_TREND_LOWEST;
        private String SELECT_PRICE_TREND_LAST;
        private String SELECT_PRICE_TREND_AVG;
        private String SELECT_BUY_ORDER_HIGHEST;
        private String SELECT_SELL_ORDER_LOWEST;

        public OrderQueryModuleImpl(SQLSession ordersSession) {
            this.ordersSession = ordersSession;
        }

        public int getCategoryId(String category) {
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
        public void addOrder(UUID listingUuid,
                             String category,
                             OrderType type,
                             IOrderIssuer issuer,
                             double price,
                             Currency currency,
                             int stock,
                             boolean temp) throws SQLException {
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
                    pstmt.setBoolean(9, temp);
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }
            }, index -> issuer.addOrderId(type, index.intValue()));
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
            }).stream().filter(Objects::nonNull).findFirst().orElse(null);
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
        public void clearTemporaryBuyOrders() throws SQLException {
            String sql = DELETE_BUY_TEMPS;

            ordersSession.execute(sql, (pstmt) -> {
            }, index -> {
            });
        }

        @Override
        public void clearTemporarySellOrders() throws SQLException {
            String sql = DELETE_SELL_TEMPS;

            ordersSession.execute(sql, (pstmt) -> {
            }, index -> {
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
        public PricePoint getLastTradingPrice(int daysPeriod, UUID currencyUuid, UUID listingUuid) {
            String sql = SELECT_PRICE_TREND_LAST;

            List<PricePoint> points = ordersSession.query(sql, pstmt -> {
                try {
                    pstmt.setString(1, currencyUuid.toString());
                    pstmt.setString(2, listingUuid.toString());
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }
            }, resultSet -> {
                try {
                    return PricePoint.read(resultSet);
                } catch (SQLException ex) {
                    ex.printStackTrace();
                    return null;
                }
            });

            return points.stream().findFirst().orElse(null);
        }

        @Override
        public double getLastTradingAverage(int daysPeriod, UUID currencyUuid, UUID listingUuid) {
            String sql = SELECT_PRICE_TREND_AVG;

            List<Double> average = ordersSession.query(sql, pstmt -> {
                try {
                    pstmt.setInt(1, daysPeriod);
                    pstmt.setString(2, currencyUuid.toString());
                    pstmt.setString(3, listingUuid.toString());
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }
            }, resultSet -> {
                try {
                    return resultSet.getDouble("average");
                } catch (SQLException ex) {
                    ex.printStackTrace();
                    return null;
                }
            });

            return average.stream().findFirst()
                    .filter(val -> val > 0.0)
                    .orElse(-1.0);
        }

        @Override
        public OrderInfo getLowestAsk(UUID currencyUuid, UUID listingUuid) {
            String sql = SELECT_SELL_ORDER_LOWEST;

            List<OrderInfo> orderInfos = ordersSession.query(sql, pstmt -> {
                try {
                    pstmt.setString(1, currencyUuid.toString());
                    pstmt.setString(2, listingUuid.toString());
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

            return orderInfos.stream().findFirst().orElse(null);
        }

        @Override
        public OrderInfo getHighestBid(UUID currencyUuid, UUID listingUuid) {
            String sql = SELECT_BUY_ORDER_HIGHEST;

            List<OrderInfo> orderInfos = ordersSession.query(sql, pstmt -> {
                try {
                    pstmt.setString(1, currencyUuid.toString());
                    pstmt.setString(2, listingUuid.toString());
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

            return orderInfos.stream().findFirst().orElse(null);
        }

        @Override
        public PricePoint getHighestPoint(int daysPeriod, UUID currencyUuid, UUID listingUuid) {
            String sql = SELECT_PRICE_TREND_HIGHEST;

            List<PricePoint> points = ordersSession.query(sql, pstmt -> {
                try {
                    pstmt.setInt(1, daysPeriod);
                    pstmt.setString(2, currencyUuid.toString());
                    pstmt.setString(3, listingUuid.toString());
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }
            }, resultSet -> {
                try {
                    return PricePoint.read(resultSet);
                } catch (SQLException ex) {
                    ex.printStackTrace();
                    return null;
                }
            });

            return points.stream().filter(Objects::nonNull).findFirst().orElse(null);
        }

        @Override
        public PricePoint getLowestPoint(int daysPeriod, UUID currencyUuid, UUID listingUuid) {
            String sql = SELECT_PRICE_TREND_LOWEST;

            List<PricePoint> points = ordersSession.query(sql, pstmt -> {
                try {
                    pstmt.setInt(1, daysPeriod);
                    pstmt.setString(2, currencyUuid.toString());
                    pstmt.setString(3, listingUuid.toString());
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }
            }, resultSet -> {
                try {
                    return PricePoint.read(resultSet);
                } catch (SQLException ex) {
                    ex.printStackTrace();
                    return null;
                }
            });

            return points.stream().filter(Objects::nonNull).findFirst().orElse(null);
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
                    .filter(Objects::nonNull)
                    .findFirst()
                    .orElse(null));
        }

        public Collection<String> categoryNames() {
            return categoryIdMap.keySet();
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
        public DataProvider<OrderInfo> getListedOrderProvider(OrderType type, String category) {
            boolean queryAll = false;
            if (category == null) {
                queryAll = true;
            }

            if (category != null && !categoryIdMap.containsKey(category))
                throw new RuntimeException("Unknown category " + category);
            int categoryId = category == null ? 0 : categoryIdMap.get(category);

            boolean finalQueryAll = queryAll;

            Map<OrderType, DataProvider<OrderInfo>> typeMap = dataProviderMap.computeIfAbsent(categoryId,
                    c -> new EnumMap<>(OrderType.class));
            switch (type) {
                case BUY:
                    return typeMap.computeIfAbsent(type, t -> {
                        OrderDataProvider orderDataProvider = new OrderDataProvider("buy_orders",
                                SELECT_BUY_ORDERS_ALL,
                                SELECT_BUY_ORDERS,
                                categoryId, finalQueryAll);
                        return new DataProviderProxy<>(orderDataProvider, orderDataProvider);
                    });
                case SELL:
                    return typeMap.computeIfAbsent(type, t -> {
                        OrderDataProvider orderDataProvider = new OrderDataProvider("sell_orders",
                                SELECT_SELL_ORDERS_ALL,
                                SELECT_SELL_ORDERS,
                                categoryId, finalQueryAll);
                        return new DataProviderProxy<>(orderDataProvider, orderDataProvider);
                    });
                default:
                    throw new RuntimeException("Unexpected type " + type);
            }
        }

        private class OrderDataProvider implements Function<Range, List<OrderInfo>>, Supplier<Integer> {
            private static final String COLUMN_COUNT = "rows_count";

            private final String tableName;
            private final String queryAll;
            private final String querySome;
            private final int categoryId;
            private final boolean all;

            public OrderDataProvider(String tableName, String queryAll, String querySome, int categoryId, boolean all) {
                this.queryAll = queryAll;
                this.querySome = querySome;
                this.tableName = tableName;
                this.categoryId = categoryId;
                this.all = all;
            }

            @Override
            public Integer get() {
                List<Integer> out = ordersSession.query("SELECT COUNT(" + OrderSQLModule.LISTING_UUID + ") as " + COLUMN_COUNT +
                        " FROM (" +
                        " SELECT " + OrderSQLModule.LISTING_UUID +
                        " FROM " + tableName +
                        (all ? "" : " WHERE " + OrderSQLModule.CATEGORY_ID + " = ?") +
                        " GROUP BY " + OrderSQLModule.LISTING_UUID +
                        ") tbl", pstmt -> {
                    try {
                        if (!all)
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

                return out.stream().filter(Objects::nonNull).findFirst().orElse(0);
            }

            @Override
            public List<OrderInfo> apply(Range range) {
                String sql = all ? queryAll : querySome;

                return ordersSession.query(sql, pstmt -> {
                    try {
                        int i = 1;
                        if (!all)
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
