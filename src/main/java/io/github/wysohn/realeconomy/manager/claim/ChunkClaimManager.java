package io.github.wysohn.realeconomy.manager.claim;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;
import io.github.wysohn.rapidframework3.bukkit.manager.location.ManagerPlayerLocation;
import io.github.wysohn.rapidframework3.core.caching.AbstractManagerElementCaching;
import io.github.wysohn.rapidframework3.core.database.Databases;
import io.github.wysohn.rapidframework3.core.inject.annotations.PluginDirectory;
import io.github.wysohn.rapidframework3.core.inject.annotations.PluginLogger;
import io.github.wysohn.rapidframework3.core.main.ManagerConfig;
import io.github.wysohn.rapidframework3.data.SimpleChunkLocation;
import io.github.wysohn.rapidframework3.data.SimpleLocation;
import io.github.wysohn.rapidframework3.interfaces.plugin.IShutdownHandle;
import io.github.wysohn.rapidframework3.interfaces.serialize.ISerializer;
import io.github.wysohn.rapidframework3.interfaces.serialize.ITypeAsserter;
import io.github.wysohn.rapidframework3.utils.Validation;
import io.github.wysohn.realeconomy.interfaces.business.IBusiness;
import io.github.wysohn.realeconomy.interfaces.business.IClaimHandler;
import io.github.wysohn.realeconomy.mediator.BusinessMediator;

import javax.inject.Named;
import java.io.File;
import java.lang.ref.Reference;
import java.util.*;
import java.util.logging.Logger;

@Singleton
public class ChunkClaimManager
        extends AbstractManagerElementCaching<SimpleChunkLocation, ChunkClaim>
        implements IClaimHandler {

    public static final String KEY_ENABLE = "business.embeddedClaimManager";

    private final Map<UUID, SimpleChunkLocation> businessToChunk = new HashMap<>();

    private final ManagerConfig config;

    @Inject
    public ChunkClaimManager(@Named("pluginName") String pluginName,
                             @PluginLogger Logger logger,
                             ManagerConfig config,
                             @PluginDirectory File pluginDir,
                             IShutdownHandle shutdownHandle,
                             ISerializer serializer,
                             ITypeAsserter asserter,
                             Injector injector) {
        super(pluginName, logger, config, pluginDir, shutdownHandle, serializer, asserter, injector, ChunkClaim.class);
        this.config = config;

        BusinessMediator.registerClaimHandler(this);
    }

    @Override
    protected Databases.DatabaseFactory createDatabaseFactory() {
        return getDatabaseFactory("chunkClaims");
    }

    @Override
    protected SimpleChunkLocation fromString(String s) {
        return SimpleChunkLocation.valueOf(s);
    }

    @Override
    protected ChunkClaim newInstance(SimpleChunkLocation simpleChunkLocation) {
        return new ChunkClaim(simpleChunkLocation);
    }

    @Override
    public void enable() throws Exception {
        super.enable();

        if (!config.get(KEY_ENABLE).isPresent()) {
            config.put(KEY_ENABLE, true);
        }
    }

    @Override
    public int priority() {
        return Integer.MAX_VALUE;
    }

    private boolean isManagerEnabled() {
        return config.get(KEY_ENABLE)
                .filter(Boolean.class::isInstance)
                .map(Boolean.class::cast)
                .orElse(false);
    }

    @Override
    public Set<UUID> getUsingBusiness(UUID memberUuid) {
        if (!isManagerEnabled())
            return Collections.emptySet();

        SimpleLocation location = ManagerPlayerLocation.getCurrentBlockLocation(memberUuid);
        SimpleChunkLocation chunk = new SimpleChunkLocation(location);
        return get(chunk)
                .map(Reference::get)
                .map(ChunkClaim::getBusinessUuid)
                .map(Collections::singleton)
                .orElseGet(Collections::emptySet);
    }

    @Override
    public boolean isInBusiness(IBusiness business, UUID memberUuid) {
        if (!isManagerEnabled())
            return false;

        SimpleChunkLocation chunk = businessToChunk.get(business.getUuid());
        if (chunk == null)
            return true;

        ChunkClaim claim = get(chunk)
                .map(Reference::get)
                .orElse(null);
        if (claim == null)
            return true;

        return claim.hasMember(memberUuid);
    }

    @Override
    public UUID queryBusiness(SimpleLocation location) {
        if (!isManagerEnabled())
            return null;

        Validation.assertNotNull(location);

        return get(new SimpleChunkLocation(location))
                .map(Reference::get)
                .map(ChunkClaim::getBusinessUuid)
                .orElse(null);
    }

    @Override
    public SimpleLocation getLocationOfBusiness(IBusiness business) {
        if (!isManagerEnabled())
            return null;

        Validation.assertNotNull(business);

        return Optional.ofNullable(businessToChunk.get(business.getUuid()))
                .map(chunk -> new SimpleLocation(chunk.getWorld(),
                        chunk.getI() << 4,
                        0,
                        chunk.getJ() << 4))
                .orElse(null);
    }

    @Override
    public boolean updateMapping(SimpleLocation location, IBusiness business) {
        if (!isManagerEnabled())
            return false;

        Validation.assertNotNull(location);
        Validation.assertNotNull(business);

        SimpleChunkLocation chunk = new SimpleChunkLocation(location);
        getOrNew(chunk).map(Reference::get)
                .ifPresent(chunkClaim -> {
                    chunkClaim.setBusinessUuid(business.getUuid());
                    businessToChunk.put(business.getUuid(), chunk);
                });

        return true;
    }

    @Override
    public boolean removeMapping(SimpleLocation location, IBusiness business) {
        if (!isManagerEnabled())
            return false;

        Validation.assertNotNull(location);
        Validation.assertNotNull(business);

        businessToChunk.remove(business.getUuid());
        delete(new SimpleChunkLocation(location));

        return true;
    }
}
