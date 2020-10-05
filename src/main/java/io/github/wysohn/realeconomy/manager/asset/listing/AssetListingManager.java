package io.github.wysohn.realeconomy.manager.asset.listing;

import com.google.inject.Injector;
import io.github.wysohn.rapidframework3.core.caching.AbstractManagerElementCaching;
import io.github.wysohn.rapidframework3.core.database.Databases;
import io.github.wysohn.rapidframework3.core.inject.annotations.PluginDirectory;
import io.github.wysohn.rapidframework3.core.inject.annotations.PluginLogger;
import io.github.wysohn.rapidframework3.core.main.ManagerConfig;
import io.github.wysohn.rapidframework3.interfaces.plugin.IShutdownHandle;
import io.github.wysohn.rapidframework3.interfaces.serialize.ISerializer;
import io.github.wysohn.rapidframework3.interfaces.serialize.ITypeAsserter;
import io.github.wysohn.realeconomy.manager.asset.signature.AssetSignature;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.io.File;
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
public class AssetListingManager extends AbstractManagerElementCaching<AssetSignature, AssetListing> {
    private final ISerializer serializer;

    @Inject
    public AssetListingManager(@Named("pluginName") String pluginName,
                               @PluginLogger Logger logger,
                               ManagerConfig config,
                               @PluginDirectory File pluginDir,
                               IShutdownHandle shutdownHandle,
                               ISerializer serializer,
                               ITypeAsserter asserter,
                               Injector injector) {
        super(pluginName, logger, config, pluginDir, shutdownHandle, serializer, asserter, injector, AssetListing.class);
        this.serializer = serializer;
    }

    @Override
    protected Databases.DatabaseFactory createDatabaseFactory() {
        return getDatabaseFactory("assetListings");
    }

    @Override
    protected AssetSignature fromString(String s) {
        try {
            return serializer.deserializeFromString(AssetSignature.class, s);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    protected AssetListing newInstance(AssetSignature signature) {
        return new AssetListing(signature);
    }
}
