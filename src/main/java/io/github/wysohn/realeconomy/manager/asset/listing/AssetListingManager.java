package io.github.wysohn.realeconomy.manager.asset.listing;

import com.google.inject.Injector;
import io.github.wysohn.rapidframework3.core.caching.AbstractManagerElementCaching;
import io.github.wysohn.rapidframework3.core.database.Databases;
import io.github.wysohn.rapidframework3.core.inject.annotations.PluginDirectory;
import io.github.wysohn.rapidframework3.core.inject.annotations.PluginLogger;
import io.github.wysohn.rapidframework3.core.main.ManagerConfig;
import io.github.wysohn.rapidframework3.interfaces.io.IPluginResourceProvider;
import io.github.wysohn.rapidframework3.interfaces.plugin.IShutdownHandle;
import io.github.wysohn.rapidframework3.interfaces.serialize.ISerializer;
import io.github.wysohn.rapidframework3.interfaces.serialize.ITypeAsserter;
import io.github.wysohn.rapidframework3.utils.sql.SQLSession;
import io.github.wysohn.rapidframework3.utils.sql.SQLiteSession;
import io.github.wysohn.realeconomy.interfaces.banking.IOrderIssuer;
import io.github.wysohn.realeconomy.manager.asset.signature.AssetSignature;
import io.github.wysohn.realeconomy.manager.currency.Currency;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
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
    private final ISerializer serializer;
    private final File pluginDir;
    private final IPluginResourceProvider resourceProvider;
    private final IShutdownHandle shutdownHandle;

    private final Map<AssetSignature, UUID> signatureUUIDMap = new HashMap<>();

    private String INSERT_BUY;
    private String INSERT_SELL;
    private String INSERT_LOG;
    private String SELECT_MATCH_ORDERS;
    private String SELECT_SELL_ORDERS;

    private SQLSession ordersSession;

    @Inject
    public AssetListingManager(@Named("pluginName") String pluginName,
                               @PluginLogger Logger logger,
                               ManagerConfig config,
                               @PluginDirectory File pluginDir,
                               IShutdownHandle shutdownHandle,
                               ISerializer serializer,
                               ITypeAsserter asserter,
                               IPluginResourceProvider resourceProvider,
                               Injector injector) {
        super(pluginName, logger, config, pluginDir, shutdownHandle, serializer, asserter, injector, AssetListing.class);
        this.serializer = serializer;
        this.pluginDir = pluginDir;
        this.resourceProvider = resourceProvider;
        this.shutdownHandle = shutdownHandle;
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

        ordersSession = new SQLiteSession(new File(pluginDir, "orders.db"), connection -> {
            try {
                connection.setAutoCommit(false);

                createTable(connection, "create_buy_orders.sql");
                createTable(connection, "create_sell_orders.sql");
                createTable(connection, "create_trade_logs.sql");

                connection.commit();
            } catch (SQLException | IOException ex) {
                ex.printStackTrace();
                shutdownHandle.shutdown();
            }
        });

        INSERT_BUY = resourceToString("insert_buy_order.sql");
        INSERT_SELL = resourceToString("insert_sell_order.sql");
        INSERT_LOG = resourceToString("insert_trade_log.sql");
        SELECT_MATCH_ORDERS = resourceToString("select_match_orders.sql");
        SELECT_SELL_ORDERS = resourceToString("select_sell_orders.sql");

        forEach(listing -> signatureUUIDMap.put(listing.getSignature(), listing.getKey()));
    }

    private void createTable(Connection connection, String filename) throws IOException, SQLException {
        try (PreparedStatement pstmt = connection.prepareStatement(resourceToString(filename))) {
            pstmt.executeUpdate();
        }
    }

    private String resourceToString(String filename) throws IOException {
        StringBuilder builder = new StringBuilder();
        try (InputStreamReader isr = new InputStreamReader(resourceProvider.getResource(filename), StandardCharsets.UTF_8);
             BufferedReader br = new BufferedReader(isr)) {
            String buffer = null;
            while ((buffer = br.readLine()) != null) {
                builder.append(buffer);
            }
        }
        return builder.toString();
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
    public void addOrder(AssetSignature signature, OrderType type, IOrderIssuer issuer,
                         double price,
                         Currency currency,
                         int stock) {
        if (!signatureUUIDMap.containsKey(signature))
            throw new RuntimeException("Invalid signature.");

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
    }

    public void cancelOrder(int orderId, OrderType type) {

    }
}
