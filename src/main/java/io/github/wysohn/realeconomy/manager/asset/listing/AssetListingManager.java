package io.github.wysohn.realeconomy.manager.asset.listing;

import com.google.inject.Injector;
import io.github.wysohn.rapidframework3.core.caching.AbstractManagerElementCaching;
import io.github.wysohn.rapidframework3.core.database.Databases;
import io.github.wysohn.rapidframework3.core.inject.annotations.PluginDirectory;
import io.github.wysohn.rapidframework3.core.inject.annotations.PluginLogger;
import io.github.wysohn.rapidframework3.core.main.ManagerConfig;
import io.github.wysohn.rapidframework3.interfaces.paging.DataProvider;
import io.github.wysohn.rapidframework3.interfaces.plugin.IShutdownHandle;
import io.github.wysohn.rapidframework3.interfaces.serialize.ISerializer;
import io.github.wysohn.rapidframework3.interfaces.serialize.ITypeAsserter;
import io.github.wysohn.rapidframework3.utils.Validation;
import io.github.wysohn.realeconomy.interfaces.banking.IOrderIssuer;
import io.github.wysohn.realeconomy.interfaces.trade.IOrderPlacementHandler;
import io.github.wysohn.realeconomy.manager.asset.signature.AssetSignature;
import io.github.wysohn.realeconomy.manager.currency.Currency;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.io.File;
import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
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
    private final IOrderPlacementHandler orderPlacementHandler;
    private final Map<AssetSignature, UUID> signatureUUIDMap = new HashMap<>();

    @Inject
    public AssetListingManager(@Named("pluginName") String pluginName,
                               @PluginLogger Logger logger,
                               ManagerConfig config,
                               @PluginDirectory File pluginDir,
                               IShutdownHandle shutdownHandle,
                               ISerializer serializer,
                               ITypeAsserter asserter,
                               IOrderPlacementHandler orderPlacementHandler,
                               Injector injector) {
        super(pluginName, logger, config, pluginDir, shutdownHandle, serializer, asserter, injector, AssetListing.class);
        this.orderPlacementHandler = orderPlacementHandler;
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
        forEach(listing -> signatureUUIDMap.put(listing.getSignature(), listing.getKey()));
    }

    @Override
    public void disable() throws Exception {
        super.disable();
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
        Validation.assertNotNull(type);
        Validation.assertNotNull(issuer);
        Validation.validate(price, val -> val > 0.0, "Price cannot be 0 or less.");
        Validation.assertNotNull(currency);
        Validation.validate(stock, val -> val > 0, "Stock cannot be 0 or less.");

        if (!signatureUUIDMap.containsKey(signature))
            throw new RuntimeException("Invalid signature.");
        AssetListing listing = fromSignature(signature);

        orderPlacementHandler.addOrder(listing.getKey(),
                type,
                issuer,
                price,
                currency,
                stock);
    }

    public void cancelOrder(int orderId, OrderType type, Consumer<Integer> callback) throws SQLException {
        Validation.validate(orderId, val -> val > 0, "orderId must be larger than 0.");
        Validation.assertNotNull(type);

        orderPlacementHandler.cancelOrder(orderId,
                type,
                callback);
    }

    public void peekMatchingOrder(Consumer<TradeInfo> consumer) {
        orderPlacementHandler.peekMatchingOrders(consumer);
    }

    public DataProvider<OrderInfo> getListedOrderProvider(Currency currency) {
        return orderPlacementHandler.getListedOrderProvider(currency);
    }
}
