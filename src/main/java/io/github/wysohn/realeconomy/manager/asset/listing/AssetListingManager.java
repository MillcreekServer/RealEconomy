package io.github.wysohn.realeconomy.manager.asset.listing;

import com.google.inject.Injector;
import io.github.wysohn.rapidframework3.core.caching.AbstractManagerElementCaching;
import io.github.wysohn.rapidframework3.core.database.Databases;
import io.github.wysohn.rapidframework3.core.inject.annotations.PluginDirectory;
import io.github.wysohn.rapidframework3.core.inject.annotations.PluginLogger;
import io.github.wysohn.rapidframework3.core.main.ManagerConfig;
import io.github.wysohn.rapidframework3.core.paging.DataProviderProxy;
import io.github.wysohn.rapidframework3.interfaces.io.IPluginResourceProvider;
import io.github.wysohn.rapidframework3.interfaces.paging.DataProvider;
import io.github.wysohn.rapidframework3.interfaces.plugin.IShutdownHandle;
import io.github.wysohn.rapidframework3.interfaces.serialize.ISerializer;
import io.github.wysohn.rapidframework3.interfaces.serialize.ITypeAsserter;
import io.github.wysohn.rapidframework3.utils.Validation;
import io.github.wysohn.rapidframework3.utils.sql.SQLSession;
import io.github.wysohn.realeconomy.inject.annotation.OrderSQL;
import io.github.wysohn.realeconomy.interfaces.banking.IOrderIssuer;
import io.github.wysohn.realeconomy.main.Metrics;
import io.github.wysohn.realeconomy.manager.asset.signature.AssetSignature;
import io.github.wysohn.realeconomy.manager.currency.Currency;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.io.File;
import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.function.Consumer;
import java.util.logging.Logger;

/**
 * id(auto increment)
 * register_date
 * current_stock(int)
 * max_stock(int)
 * signature_type(simple class name)
 * signature_data(json)
 */
@Singleton
public class AssetListingManager extends AbstractManagerElementCaching<UUID, AssetListing> {
    private final IPluginResourceProvider resourceProvider;
    private final SQLSession ordersSession;

    private final Map<AssetSignature, UUID> signatureUUIDMap = new HashMap<>();
    private final Map<UUID, DataProvider<OrderInfo>> dataProviderMap = new HashMap<>();

    private String INSERT_BUY;
    private String INSERT_SELL;
    private String INSERT_LOG;
    private String DELETE_BUY;
    private String DELETE_SELL;
    private String SELECT_MATCH_ORDERS;
    private String SELECT_SELL_ORDERS;


    @Inject
    public AssetListingManager(@Named("pluginName") String pluginName,
                               @PluginLogger Logger logger,
                               ManagerConfig config,
                               @PluginDirectory File pluginDir,
                               IShutdownHandle shutdownHandle,
                               ISerializer serializer,
                               ITypeAsserter asserter,
                               IPluginResourceProvider resourceProvider,
                               @OrderSQL SQLSession orderSql,
                               Injector injector) {
        super(pluginName, logger, config, pluginDir, shutdownHandle, serializer, asserter, injector, AssetListing.class);
        this.resourceProvider = resourceProvider;
        this.ordersSession = orderSql;
    }

    @Override
    protected Databases.DatabaseFactory createDatabaseFactory() {
        return getDatabaseFactory("assetListings");
    }

    @Override
    protected UUID fromString(String s) {
        return UUID.fromString(s);
    }

    @Override
    protected AssetListing newInstance(UUID uuid) {
        return new AssetListing(uuid);
    }

    @Override
    public void enable() throws Exception {
        super.enable();

        INSERT_BUY = Metrics.resourceToString(resourceProvider, "insert_buy_order.sql");
        INSERT_SELL = Metrics.resourceToString(resourceProvider, "insert_sell_order.sql");
        INSERT_LOG = Metrics.resourceToString(resourceProvider, "insert_trade_log.sql");
        DELETE_BUY = Metrics.resourceToString(resourceProvider, "delete_buy_order.sql");
        DELETE_SELL = Metrics.resourceToString(resourceProvider, "delete_sell_order.sql");
        SELECT_MATCH_ORDERS = Metrics.resourceToString(resourceProvider, "select_match_orders.sql");
        SELECT_SELL_ORDERS = Metrics.resourceToString(resourceProvider, "select_sell_orders.sql");

        forEach(listing -> signatureUUIDMap.put(listing.getSignature(), listing.getKey()));
    }

    /**
     * @param key
     * @return
     * @deprecated use {@link #newListing(AssetSignature)}
     */
    @Override
    public Optional<WeakReference<AssetListing>> getOrNew(UUID key) {
        return super.getOrNew(key);
    }

    /**
     * List the given signature to the market to be visible.
     *
     * @param signature
     * @return
     */
    public boolean newListing(AssetSignature signature) {
        if (signatureUUIDMap.containsKey(signature))
            return false;

        super.getOrNew(UUID.randomUUID())
                .map(Reference::get)
                .ifPresent(listing -> {
                    listing.setSignature(signature);
                    signatureUUIDMap.put(signature, listing.getKey());
                });

        return true;
    }

    /**
     * Get listing correspond to the signature. For example, if it's an ItemStackSignature,
     * we expect similar ItemStacks to be show up as one instead of all of them having different
     * instance.
     *
     * @param signature
     * @return
     */
    public AssetListing fromSignature(AssetSignature signature) {
        return Optional.ofNullable(signature)
                .map(signatureUUIDMap::get)
                .flatMap(this::get)
                .map(Reference::get)
                .orElse(null);
    }

    /**
     * Add order for the given signature. The given signature must be listed already using
     * {@link #newListing(AssetSignature)}
     *
     * @param signature asset signature
     * @param type      trade type
     * @param issuer    issuer
     * @param price     price
     * @param currency  currency of price
     * @param stock     amount to purchase/sell
     */
    public void addOrder(AssetSignature signature,
                         OrderType type,
                         IOrderIssuer issuer,
                         double price,
                         Currency currency,
                         int stock) throws SQLException {
        if (!signatureUUIDMap.containsKey(signature))
            throw new RuntimeException("Invalid signature.");

        Validation.assertNotNull(type);
        Validation.assertNotNull(issuer);
        Validation.validate(price, val -> val > 0.0, "Price cannot be 0 or less.");
        Validation.assertNotNull(currency);
        Validation.validate(stock, val -> val > 0, "Stock cannot be 0 or less.");

        AssetListing listing = fromSignature(signature);

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
                pstmt.setString(1, listing.getKey().toString());
                pstmt.setString(2, issuer.getUuid().toString());
                pstmt.setDouble(3, price);
                pstmt.setString(4, currency.getKey().toString());
                pstmt.setInt(5, stock);
                pstmt.setInt(6, stock);
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
        }, issuer::addOrderId);

        ordersSession.commit();
    }

    public void cancelOrder(int orderId, OrderType type, Consumer<Long> callback) throws SQLException {
        Validation.validate(orderId, val -> val > 0, "orderId must be larger than 0.");
        Validation.assertNotNull(type);

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
        }, callback);

        ordersSession.commit();
    }

    public void peekMatchingOrder(Consumer<TradeInfo> consumer) {
        String sql = SELECT_MATCH_ORDERS;

        try (ResultSet resultSet = ordersSession.query(sql, pstmt -> {
        })) {
            if (resultSet.next()) {
                consumer.accept(TradeInfo.read(resultSet));
            } else {
                consumer.accept(null);
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

    public DataProvider<OrderInfo> getListedOrderProvider(Currency currency) {
        return new DataProviderProxy<>(range -> {

        }, () -> {
            try (ResultSet resultSet = ordersSession.query("SELECT COUNT(id) as rows FROM sell_orders;", pstmt -> {
            })) {
                if (resultSet.next()) {
                    return resultSet.getInt("rows");
                } else {
                    return 0;
                }
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
        });
    }

    private class OrderDataProvider implements DataProvider<OrderInfo> {
        private final UUID currencyUuid;
        private final int querySize;

        private final int currentSize = 0;
        private int head = 0;
        private final OrderInfo[] infoCache;
        private long lastUpdate = -1L;

        public OrderDataProvider(UUID currencyUuid, int querySize) {
            this.currencyUuid = currencyUuid;
            this.querySize = querySize;
            this.infoCache = new OrderInfo[querySize];
        }

        public OrderDataProvider(UUID currencyUuid) {
            this(currencyUuid, 1000);
        }

        @Override
        public int size() {


            return currentSize;
        }

        @Override
        public List<OrderInfo> get(int index, int size) {
            // update if
            // 1 second passed, index is less than current range, or index is over the current range.
            if (lastUpdate + 1000L < System.currentTimeMillis()
                    || index < head
                    || index >= head + querySize) {
                if (index < head) {
                    head = Math.max(0, head - querySize);
                } else {
                    head += querySize;
                }

                update();
            }

            return infoCache[index - head];
        }

        private void update() {
            lastUpdate = System.currentTimeMillis();

            Arrays.fill(infoCache, null);
            ordersSession.query(SELECT_SELL_ORDERS, pstmt -> {
                try {
                    pstmt.setString(1, currencyUuid.toString());
                    pstmt.setInt(2, head);
                    pstmt.setInt(3, querySize);
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }
            }, resultSet -> {
                try {
                    int i = 0;
                    while (resultSet.next()) {
                        infoCache[i++] = OrderInfo.read(resultSet);
                    }
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }
            });
        }
    }
}
