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

    private void prepareConfigs(ITier tier,
                                Map<AssetSignature, Double> requirementMap,
                                Map<AssetSignature, Double> fulfillmentMap,
                                Map<AssetSignature, Double> inputsMap,
                                Map<AssetSignature, Double> outputsMap) {
        TierInfoMap requirement = mock(TierInfoMap.class);
        TierInfoMap fulfillment = mock(TierInfoMap.class);
        TierInfoMap inputs = mock(TierInfoMap.class);
        TierInfoMap outputs = mock(TierInfoMap.class);

        when(tier.requirement()).thenReturn(requirement);
        when(tier.fulfillment()).thenReturn(fulfillment);
        when(tier.inputs()).thenReturn(inputs);
        when(tier.outputs()).thenReturn(outputs);

        when(requirement.getAll(eq(listingManager))).thenReturn(requirementMap);
        when(fulfillment.getAll(eq(listingManager))).thenReturn(fulfillmentMap);
        when(inputs.getAll(eq(listingManager))).thenReturn(inputsMap);
        when(outputs.getAll(eq(listingManager))).thenReturn(outputsMap);
    }

    @Test
    public void testEstablishment() {
        ITier tier = mock(ITier.class);
        when(tier.timeToLive()).thenReturn(-1L);

        TempBusiness tempBusiness = new TempBusiness(UUID.randomUUID(), UUID.randomUUID(), tier);
        Guice.createInjector(moduleList).injectMembers(tempBusiness);

        prepareConfigs(tier,
                new HashMap<AssetSignature, Double>() {{
                    put(new ItemStackSignature(new ItemStack(Material.STONE)), 23.0);
                    put(new ElectricitySignature(), 10000.35);
                }}, new HashMap<AssetSignature, Double>() {{
                    put(new ItemStackSignature(new ItemStack(Material.STONE)), 1.0);
                    put(new ElectricitySignature(), 120.89);
                }}, new HashMap<AssetSignature, Double>() {{

                }}, new HashMap<AssetSignature, Double>() {{

                }});

        // 23 / 1 = 23 seconds required for STONE
        // 10000.35 / 120.89 = 82.7227231 seconds required for electricity

        tempBusiness.init();
        int seconds = 0;
        while (!tempBusiness.isEstablished()) {
            tempBusiness.update();
            seconds++;

            if (seconds == 23) {
                assertEquals(23.0, tempBusiness.getCurrentProgress().get(new ItemStackSignature(new ItemStack(Material.STONE))), 0.00001);
            }
        }

        assertEquals(1.0 * 23, tempBusiness.getCurrentProgress().get(new ItemStackSignature(new ItemStack(Material.STONE))), 0.00001);
        assertEquals(120.89 * 83, tempBusiness.getCurrentProgress().get(new ElectricitySignature()), 0.00001);
        assertEquals(84, seconds);
    }

    @Test
    public void testProduction() {
        ITier tier = mock(ITier.class);
        when(tier.timeToLive()).thenReturn(-1L);

        TempBusiness tempBusiness = new TempBusiness(UUID.randomUUID(), UUID.randomUUID(), tier);
        Guice.createInjector(moduleList).injectMembers(tempBusiness);
        addFakeObserver(tempBusiness);

        // basically, it's a furnace using electricity
        prepareConfigs(tier,
                new HashMap<AssetSignature, Double>() {{
                }}, new HashMap<AssetSignature, Double>() {{
                }}, new HashMap<AssetSignature, Double>() {{
                    put(new ItemStackSignature(new ItemStack(Material.COBBLESTONE)), 5.0);
                    put(new ElectricitySignature(), 22.68);
                }}, new HashMap<AssetSignature, Double>() {{
                    put(new ItemStackSignature(new ItemStack(Material.STONE)), 5.0);
                }});

        tempBusiness.init();

        // not enough electricity
        tempBusiness.addAsset(new ItemStackSignature(new ItemStack(Material.COBBLESTONE)).create(new HashMap<String, Object>() {{
            put(AssetSignature.KEY_NUMERIC_MEASURE, 64.0);
        }}));
        tempBusiness.addAsset(new ElectricitySignature().create(new HashMap<String, Object>() {{
            put(AssetSignature.KEY_NUMERIC_MEASURE, 14.33);
        }}));

        tempBusiness.update();
        assertEquals(5.0, tempBusiness.getProductionMaterials().get(new ItemStackSignature(new ItemStack(Material.COBBLESTONE))), 0.00001);
        assertEquals(14.33, tempBusiness.getProductionMaterials().getOrDefault(new ElectricitySignature(), 0.0), 0.00001);

        // now has enough resources, so resources in the storage will be consumed
        tempBusiness.addAsset(new ElectricitySignature().create(new HashMap<String, Object>() {{
            put(AssetSignature.KEY_NUMERIC_MEASURE, 50.0);
        }}));

        tempBusiness.update();
        assertEquals(5.0, tempBusiness.getProductionMaterials().get(new ItemStackSignature(new ItemStack(Material.COBBLESTONE))), 0.00001);
        assertEquals(14.33, tempBusiness.getProductionMaterials().getOrDefault(new ElectricitySignature(), 0.0), 0.00001);

        // 10 seconds passed
        tempBusiness.addAsset(new ElectricitySignature().create(new HashMap<String, Object>() {{
            put(AssetSignature.KEY_NUMERIC_MEASURE, 100000.0);
        }}));

        for (int i = 0; i < 10; i++)
            tempBusiness.update();

        assertEquals(55, tempBusiness.removeAsset(new ItemStackSignature(new ItemStack(Material.STONE)), 100).stream()
                .map(Asset::getNumericalMeasure).reduce(Double::sum).orElse(0.0), 0.00001);
    }

    private static class TempBusiness extends AbstractBusiness {
        public TempBusiness(UUID key,
                            UUID ownerUuid,
                            ITier tier) {
            super(key, ownerUuid, tier);
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