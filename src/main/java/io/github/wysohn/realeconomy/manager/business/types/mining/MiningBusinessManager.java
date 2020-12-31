package io.github.wysohn.realeconomy.manager.business.types.mining;

import com.google.inject.Injector;
import io.github.wysohn.rapidframework3.core.database.Databases;
import io.github.wysohn.rapidframework3.core.inject.annotations.PluginDirectory;
import io.github.wysohn.rapidframework3.core.inject.annotations.PluginLogger;
import io.github.wysohn.rapidframework3.core.main.ManagerConfig;
import io.github.wysohn.rapidframework3.interfaces.plugin.IShutdownHandle;
import io.github.wysohn.rapidframework3.interfaces.serialize.ISerializer;
import io.github.wysohn.rapidframework3.interfaces.serialize.ITypeAsserter;
import io.github.wysohn.realeconomy.main.Metrics;
import io.github.wysohn.realeconomy.manager.asset.signature.DurationSignature;
import io.github.wysohn.realeconomy.manager.asset.signature.ElectricitySignature;
import io.github.wysohn.realeconomy.manager.asset.signature.ItemStackSignature;
import io.github.wysohn.realeconomy.manager.asset.signature.LabourSignature;
import io.github.wysohn.realeconomy.manager.business.types.AbstractBusinessManager;
import io.github.wysohn.realeconomy.manager.listing.AssetListingManager;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;

import javax.inject.Named;
import java.io.File;
import java.util.EnumMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;

public class MiningBusinessManager extends AbstractBusinessManager<MiningBusiness> implements Listener {
    public static final String TIER_NAME = "mining";

    // special blocks that re-generates and also yield more labour points.
    private static final Map<Material, Double> SPECIAL_BLOCKS = new EnumMap<>(Material.class);

    static {
        SPECIAL_BLOCKS.put(Material.COBBLESTONE, 0.5);
        SPECIAL_BLOCKS.put(Material.COAL_ORE, 5.0);
        SPECIAL_BLOCKS.put(Material.DIAMOND_ORE, 240.0);
        SPECIAL_BLOCKS.put(Material.EMERALD_ORE, 300.0);
        SPECIAL_BLOCKS.put(Material.GOLD_ORE, 80.0);
        SPECIAL_BLOCKS.put(Material.IRON_ORE, 10.0);
        SPECIAL_BLOCKS.put(Material.LAPIS_ORE, 120.0);
        SPECIAL_BLOCKS.put(Material.REDSTONE_ORE, 30.0);
    }

    public MiningBusinessManager(@Named("pluginName") String pluginName,
                                 @PluginLogger Logger logger,
                                 ManagerConfig config,
                                 @PluginDirectory File pluginDir,
                                 IShutdownHandle shutdownHandle,
                                 ISerializer serializer,
                                 ITypeAsserter asserter,
                                 Injector injector,
                                 AssetListingManager listingManager) {
        super(pluginName, logger, config, pluginDir, shutdownHandle, serializer,
                asserter, injector, MiningBusiness.class, listingManager);
    }

    @Override
    protected Databases.DatabaseFactory createDatabaseFactory() {
        return getDatabaseFactory(TIER_NAME);
    }

    @Override
    protected MiningBusiness newInstance(UUID uuid) {
        return new MiningBusiness(uuid);
    }

    @Override
    protected void addDefaultConfig(DefaultConfigBuilder defaultConfigBuilder) {
        defaultConfigBuilder.name(TIER_NAME)
                .putRequirement(new DurationSignature(), Metrics.DAY)
                .putRequirement(new ItemStackSignature(new ItemStack(Material.SPRUCE_LOG)), 250.0)
                .putRequirement(new ItemStackSignature(new ItemStack(Material.OAK_LOG)), 250.0)
                .putInput(new ElectricitySignature(), 1000.0)
                .putInput(new DurationSignature(), Metrics.HOUR)
                .putInput(new LabourSignature(), 250.0)
                .putOutput(new ItemStackSignature(new ItemStack(Material.COAL_ORE)), 50.0)
                .setTimeToLiveMin((long) (8 * Metrics.DAY * 1000))
                .setTimeToLiveMax((long) (35 * Metrics.DAY * 1000));
    }

    @EventHandler
    public void onBreak(BlockBreakEvent event) {

    }
}
