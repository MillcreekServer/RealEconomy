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
import io.github.wysohn.realeconomy.interfaces.business.IVisitStateProvider;
import io.github.wysohn.realeconomy.mediator.BusinessMediator;

import javax.inject.Named;
import java.io.File;
import java.lang.ref.Reference;
import java.util.*;
import java.util.logging.Logger;

@Singleton
public class ChunkClaimManager
        extends AbstractManagerElementCaching<SimpleChunkLocation, ChunkClaim>
        implements IVisitStateProvider {

    private final Map<UUID, SimpleChunkLocation> businessToChunk = new HashMap<>();
    private final Map<SimpleChunkLocation, IBusiness> chunkToBusiness = new HashMap<>();

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

        BusinessMediator.registerVisitorStateProvider(this);
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
    public Set<IBusiness> getUsingBusiness(UUID memberUuid) {
        SimpleLocation location = ManagerPlayerLocation.getCurrentBlockLocation(memberUuid);
        SimpleChunkLocation chunk = new SimpleChunkLocation(location);
        if (chunkToBusiness.containsKey(chunk))
            return Collections.singleton(chunkToBusiness.get(chunk));
        else
            return Collections.emptySet();
    }

    @Override
    public boolean isMember(IBusiness business, UUID memberUuid) {
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

    public IBusiness getBusinessFromChunk(SimpleChunkLocation chunk) {
        Validation.assertNotNull(chunk);

        return chunkToBusiness.get(chunk);
    }

    public SimpleChunkLocation getChunkOfBusiness(IBusiness business) {
        Validation.assertNotNull(business);

        return businessToChunk.get(business.getUuid());
    }

    public void updateMapping(SimpleChunkLocation chunkLocation, IBusiness business) {
        Validation.assertNotNull(chunkLocation);
        Validation.assertNotNull(business);

        businessToChunk.put(business.getUuid(), chunkLocation);
        chunkToBusiness.put(chunkLocation, business);
    }

    public void removeMapping(SimpleChunkLocation chunkLocation, IBusiness business) {
        Validation.assertNotNull(chunkLocation);
        Validation.assertNotNull(business);

        businessToChunk.remove(business.getUuid());
        chunkToBusiness.remove(chunkLocation);
    }
}
