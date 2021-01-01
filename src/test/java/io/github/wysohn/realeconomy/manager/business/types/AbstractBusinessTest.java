package io.github.wysohn.realeconomy.manager.business.types;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Module;
import com.google.inject.Provides;
import io.github.wysohn.rapidframework3.bukkit.testutils.manager.AbstractBukkitManagerTest;
import io.github.wysohn.rapidframework3.core.inject.module.GsonSerializerModule;
import io.github.wysohn.rapidframework3.core.inject.module.TypeAsserterModule;
import io.github.wysohn.rapidframework3.interfaces.IMemento;
import io.github.wysohn.realeconomy.interfaces.business.tiers.ITier;
import io.github.wysohn.realeconomy.manager.CustomTypeAdapters;
import io.github.wysohn.realeconomy.manager.asset.Asset;
import io.github.wysohn.realeconomy.manager.asset.signature.AssetSignature;
import io.github.wysohn.realeconomy.manager.asset.signature.DurationSignature;
import io.github.wysohn.realeconomy.manager.asset.signature.ElectricitySignature;
import io.github.wysohn.realeconomy.manager.asset.signature.ItemStackSignature;
import io.github.wysohn.realeconomy.manager.business.tiers.TierInfoMap;
import io.github.wysohn.realeconomy.manager.listing.AssetListingManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.ItemFactory;
import org.bukkit.inventory.ItemStack;
import org.junit.Before;
import org.junit.Test;

import java.util.*;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class AbstractBusinessTest extends AbstractBukkitManagerTest {
    private final List<Module> moduleList = new LinkedList<>();
    private AssetListingManager listingManager;

    @Before
    public void init() throws Exception {
        ItemFactory itemFactory = mock(ItemFactory.class);
        when(Bukkit.getItemFactory()).thenReturn(itemFactory);
        when(itemFactory.equals(any(), any())).thenReturn(false);

        listingManager = mock(AssetListingManager.class);

        moduleList.add(new GsonSerializerModule(
                CustomTypeAdapters.ACCOUNT,
                CustomTypeAdapters.BANKING_TYPE,
                CustomTypeAdapters.ASSET,
                CustomTypeAdapters.ASSET_SIGNATURE
        ));
        moduleList.add(new TypeAsserterModule());
        moduleList.add(new AbstractModule() {
            @Provides
            AssetListingManager listingManager() {
                return listingManager;
            }
        });
    }

    public static void prepareConfigs(AssetListingManager listingManager,
                                      ITier tier,
                                      Map<AssetSignature, Double> requirementMap,
                                      Map<AssetSignature, Double> inputsMap,
                                      Map<AssetSignature, Double> outputsMap) {
        TierInfoMap requirement = mock(TierInfoMap.class);
        TierInfoMap inputs = mock(TierInfoMap.class);
        TierInfoMap outputs = mock(TierInfoMap.class);

        when(tier.requirement(any())).thenReturn(requirement);
        when(tier.inputs(any())).thenReturn(inputs);
        when(tier.outputs(any())).thenReturn(outputs);

        when(requirement.getAll(eq(listingManager))).thenReturn(requirementMap);
        when(inputs.getAll(eq(listingManager))).thenReturn(inputsMap);
        when(outputs.getAll(eq(listingManager))).thenReturn(outputsMap);
    }

    @Test
    public void testEstablishment() {
        ITier tier = mock(ITier.class);
        when(tier.timeToLiveMax()).thenReturn(-1L);

        TempBusiness tempBusiness = new TempBusiness(UUID.randomUUID());
        addFakeObserver(tempBusiness);
        tempBusiness.replaceTier(tier);
        Guice.createInjector(moduleList).injectMembers(tempBusiness);

        prepareConfigs(listingManager,
                tier,
                new HashMap<AssetSignature, Double>() {{
                    put(new ItemStackSignature(new ItemStack(Material.STONE)), 23.0);
                    put(new ElectricitySignature(), 10000.35);
                    put(new DurationSignature(), 50.0);
                }}, new HashMap<AssetSignature, Double>() {{

                }}, new HashMap<AssetSignature, Double>() {{

                }});
        tempBusiness.addAsset(new ItemStackSignature(new ItemStack(Material.STONE)).asset(100.0));
        tempBusiness.addAsset(new ElectricitySignature().asset(50000.0));

        tempBusiness.init();

        tempBusiness.update(); // resources filled
        assertEquals(23.0, tempBusiness.getCurrentProgress().get(new ItemStackSignature(new ItemStack(Material.STONE))), 0.00001);
        assertEquals(10000.35, tempBusiness.getCurrentProgress().get(new ElectricitySignature()), 0.00001);

        // duration
        int seconds = 0;
        while (!tempBusiness.isEstablished()) {
            tempBusiness.update();
            seconds++;
            assertEquals(seconds, tempBusiness.getCurrentProgress().get(new DurationSignature()), 0.00001);
        }

        assertEquals(23.0, tempBusiness.getCurrentProgress().get(new ItemStackSignature(new ItemStack(Material.STONE))), 0.00001);
        assertEquals(10000.35, tempBusiness.getCurrentProgress().get(new ElectricitySignature()), 0.00001);
        assertEquals(50.0, tempBusiness.getCurrentProgress().get(new DurationSignature()), 0.00001);
    }

    @Test
    public void testProduction() {
        ITier tier = mock(ITier.class);
        when(tier.timeToLiveMax()).thenReturn(-1L);

        TempBusiness tempBusiness = new TempBusiness(UUID.randomUUID());
        addFakeObserver(tempBusiness);
        tempBusiness.replaceTier(tier);
        Guice.createInjector(moduleList).injectMembers(tempBusiness);
        addFakeObserver(tempBusiness);

        // basically, it's a furnace using electricity
        prepareConfigs(listingManager,
                tier,
                new HashMap<AssetSignature, Double>() {{
                }}, new HashMap<AssetSignature, Double>() {{
                    put(new ItemStackSignature(new ItemStack(Material.COBBLESTONE)), 5.0);
                    put(new ElectricitySignature(), 22.68);
                }}, new HashMap<AssetSignature, Double>() {{
                    put(new ItemStackSignature(new ItemStack(Material.STONE)), 5.0);
                }});

        tempBusiness.init();

        // not enough electricity
        tempBusiness.addAsset(new ItemStackSignature(new ItemStack(Material.COBBLESTONE)).asset(64.0));
        tempBusiness.addAsset(new ElectricitySignature().asset(14.33));

        tempBusiness.update();
        assertEquals(5.0, tempBusiness.getProductionMaterials().get(new ItemStackSignature(new ItemStack(Material.COBBLESTONE))), 0.00001);
        assertEquals(14.33, tempBusiness.getProductionMaterials().getOrDefault(new ElectricitySignature(), 0.0), 0.00001);

        // now has enough resources, so resources in the storage will be consumed
        tempBusiness.addAsset(new ElectricitySignature().asset(10000.0));

        tempBusiness.update();
        assertEquals(0.0, tempBusiness.getProductionMaterials().get(new ItemStackSignature(new ItemStack(Material.COBBLESTONE))), 0.00001);
        assertEquals(0.0, tempBusiness.getProductionMaterials().getOrDefault(new ElectricitySignature(), 0.0), 0.00001);

        // 10 seconds passed
        for (int i = 0; i < 10; i++)
            tempBusiness.update();

        // 5 stones for the update() above, and 50 stones for 10 seconds production
        assertEquals(5 + 50, tempBusiness.removeAsset(new ItemStackSignature(new ItemStack(Material.STONE)), 100).stream()
                .map(Asset::getNumericalMeasure).reduce(Double::sum).orElse(0.0), 0.00001);
    }

    public static class TempBusiness extends AbstractBusiness {
        public TempBusiness(UUID key) {
            super(key);
        }

        @Override
        public IMemento saveState() {
            return null;
        }

        @Override
        public void restoreState(IMemento savedState) {

        }
    }
}