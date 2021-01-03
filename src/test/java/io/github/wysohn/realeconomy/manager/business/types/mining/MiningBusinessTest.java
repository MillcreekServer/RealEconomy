package io.github.wysohn.realeconomy.manager.business.types.mining;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Module;
import com.google.inject.Provides;
import io.github.wysohn.rapidframework3.bukkit.testutils.manager.AbstractBukkitManagerTest;
import io.github.wysohn.rapidframework3.core.inject.module.GsonSerializerModule;
import io.github.wysohn.rapidframework3.core.inject.module.TypeAsserterModule;
import io.github.wysohn.rapidframework3.data.SimpleLocation;
import io.github.wysohn.realeconomy.interfaces.business.IClaimHandler;
import io.github.wysohn.realeconomy.interfaces.business.tiers.ITier;
import io.github.wysohn.realeconomy.interfaces.business.types.mining.IBlockGenerator;
import io.github.wysohn.realeconomy.manager.CustomTypeAdapters;
import io.github.wysohn.realeconomy.manager.asset.Asset;
import io.github.wysohn.realeconomy.manager.asset.Labour;
import io.github.wysohn.realeconomy.manager.asset.signature.AssetSignature;
import io.github.wysohn.realeconomy.manager.asset.signature.ItemStackSignature;
import io.github.wysohn.realeconomy.manager.asset.signature.LabourSignature;
import io.github.wysohn.realeconomy.manager.listing.AssetListingManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemFactory;
import org.bukkit.inventory.ItemStack;
import org.junit.Before;
import org.junit.Test;

import javax.inject.Named;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

import static io.github.wysohn.realeconomy.manager.business.types.AbstractBusinessTest.prepareConfigs;
import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

public class MiningBusinessTest extends AbstractBukkitManagerTest {
    private final List<Module> moduleList = new LinkedList<>();
    private AssetListingManager listingManager;
    private IBlockGenerator blockGenerator;
    private IClaimHandler visitStateProvider;

    @Before
    public void init() throws Exception {
        ItemFactory itemFactory = mock(ItemFactory.class);
        when(Bukkit.getItemFactory()).thenReturn(itemFactory);
        when(itemFactory.equals(any(), any())).thenReturn(false);

        listingManager = mock(AssetListingManager.class);
        blockGenerator = mock(IBlockGenerator.class);
        visitStateProvider = mock(IClaimHandler.class);

        moduleList.add(new GsonSerializerModule(
                CustomTypeAdapters.ACCOUNT,
                CustomTypeAdapters.BANKING_TYPE,
                CustomTypeAdapters.ASSET,
                CustomTypeAdapters.ASSET_SIGNATURE,
                CustomTypeAdapters.ORE_INFO
        ));
        moduleList.add(new TypeAsserterModule());
        moduleList.add(new AbstractModule() {
            @Provides
            AssetListingManager listingManager() {
                return listingManager;
            }

            @Provides
            IBlockGenerator blockGenerator() {
                return blockGenerator;
            }

            @Provides
            @Named("oreRegenDelay")
            long regenDelay() {
                return 0L;
            }
        });
    }

    @Test
    public void testBlockRegen() throws Exception {
        ITier tier = mock(ITier.class);
        when(tier.timeToLiveMax()).thenReturn(-1L);

        MiningBusiness business = new MiningBusiness(UUID.randomUUID());
        addFakeObserver(business);
        business.replaceTier(tier);
        Guice.createInjector(moduleList).injectMembers(business);

        prepareConfigs(listingManager,
                tier,
                new HashMap<AssetSignature, Double>() {{
                }}, new HashMap<AssetSignature, Double>() {{
                    put(new LabourSignature(), 5.0);
                }}, new HashMap<AssetSignature, Double>() {{
                    put(new ItemStackSignature(new ItemStack(Material.COAL_ORE)), 50.0);
                }});

        Player player = mock(Player.class, RETURNS_DEEP_STUBS);
        Block block = mock(Block.class, RETURNS_DEEP_STUBS);
        BlockBreakEvent event = new BlockBreakEvent(block, player);
        when(block.getType()).thenReturn(Material.DIAMOND_ORE);
        when(block.getLocation().getWorld().getName()).thenReturn("world");
        when(block.getLocation().getBlockX()).thenReturn(1);
        when(block.getLocation().getBlockY()).thenReturn(2);
        when(block.getLocation().getBlockZ()).thenReturn(3);

        when(visitStateProvider.isInBusiness(any(), any())).thenReturn(true);
        business.blockBreak(event, visitStateProvider);
        business.init();
        business.update();
        verify(blockGenerator).generateBlockAt(eq(new SimpleLocation("world", 1, 2, 3)),
                eq(Material.DIAMOND_ORE));
    }

    @Test
    public void testBlockBreak() {
        ITier tier = mock(ITier.class);
        when(tier.timeToLiveMax()).thenReturn(-1L);

        MiningBusiness business = new MiningBusiness(UUID.randomUUID());
        addFakeObserver(business);
        business.replaceTier(tier);
        Guice.createInjector(moduleList).injectMembers(business);

        prepareConfigs(listingManager,
                tier,
                new HashMap<AssetSignature, Double>() {{
                }}, new HashMap<AssetSignature, Double>() {{
                }}, new HashMap<AssetSignature, Double>() {{
                    put(new ItemStackSignature(new ItemStack(Material.COAL_ORE)), 50.0);
                }});

        Player player = mock(Player.class, RETURNS_DEEP_STUBS);
        Block block = mock(Block.class, RETURNS_DEEP_STUBS);
        BlockBreakEvent event = new BlockBreakEvent(block, player);
        when(block.getType()).thenReturn(Material.DIAMOND_ORE);
        when(block.getLocation().getWorld().getName()).thenReturn("world");
        when(block.getLocation().getBlockX()).thenReturn(1);
        when(block.getLocation().getBlockY()).thenReturn(2);
        when(block.getLocation().getBlockZ()).thenReturn(3);

        // break 3 diamonds
        when(visitStateProvider.isInBusiness(any(), any())).thenReturn(true);
        business.blockBreak(event, visitStateProvider);
        business.blockBreak(event, visitStateProvider);
        business.blockBreak(event, visitStateProvider);

        List<Asset> assets = business.assetDataProvider().get(0, 3);
        for (int i = 0; i < 3; i++) {
            assertEquals(Labour.class, assets.get(i).getClass());
            assertEquals(240.0, assets.get(i).getNumericalMeasure(), 0.000001);
        }
    }
}