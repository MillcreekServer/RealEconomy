package io.github.wysohn.realeconomy.manager.business.types.mining;

import com.google.inject.Injector;
import com.google.inject.Singleton;
import io.github.wysohn.rapidframework3.core.inject.annotations.PluginDirectory;
import io.github.wysohn.rapidframework3.core.inject.annotations.PluginLogger;
import io.github.wysohn.rapidframework3.core.inject.factory.IDatabaseFactoryCreator;
import io.github.wysohn.rapidframework3.core.main.ManagerConfig;
import io.github.wysohn.rapidframework3.interfaces.plugin.IShutdownHandle;
import io.github.wysohn.rapidframework3.interfaces.serialize.ISerializer;
import io.github.wysohn.rapidframework3.interfaces.serialize.ITypeAsserter;
import io.github.wysohn.realeconomy.interfaces.business.IBusinessContextHandler;
import io.github.wysohn.realeconomy.main.Metrics;
import io.github.wysohn.realeconomy.manager.asset.signature.DurationSignature;
import io.github.wysohn.realeconomy.manager.asset.signature.ElectricitySignature;
import io.github.wysohn.realeconomy.manager.asset.signature.ItemStackSignature;
import io.github.wysohn.realeconomy.manager.asset.signature.LabourSignature;
import io.github.wysohn.realeconomy.manager.business.types.AbstractBusinessManager;
import io.github.wysohn.realeconomy.manager.listing.AssetListingManager;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;

import javax.inject.Inject;
import javax.inject.Named;
import java.io.File;
import java.util.UUID;
import java.util.logging.Logger;

@Singleton
public class MiningBusinessManager extends AbstractBusinessManager<MiningBusiness> implements Listener {
    public static final String TIER_NAME = "mining";

    @Inject
    public MiningBusinessManager(@Named("pluginName") String pluginName,
                                 @PluginLogger Logger logger,
                                 ManagerConfig config,
                                 @PluginDirectory File pluginDir,
                                 IShutdownHandle shutdownHandle,
                                 ISerializer serializer,
                                 ITypeAsserter asserter,
                                 IDatabaseFactoryCreator factoryCreator,
                                 Injector injector,
                                 AssetListingManager listingManager,
                                 IBusinessContextHandler visitStateProvider) {
        super(pluginName,
                logger,
                config,
                pluginDir,
                shutdownHandle,
                serializer,
                asserter,
                factoryCreator,
                injector,
                TIER_NAME,
                MiningBusiness.class,
                listingManager,
                visitStateProvider);
    }

    @Override
    protected MiningBusiness newInstance(UUID uuid) {
        return new MiningBusiness(uuid);
    }

    @Override
    public String getTierName() {
        return TIER_NAME;
    }

    @Override
    protected void addDefaultConfig(DefaultConfigBuilder defaultConfigBuilder) {
        // simple coal mine as default
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
        Player player = event.getPlayer();
        forEachApplicableBusiness(player.getUniqueId(), miningBusiness ->
                miningBusiness.blockBreak(event, visitStateProvider));
    }
}
