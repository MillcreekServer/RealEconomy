package io.github.wysohn.realeconomy.manager.banking;

import io.github.wysohn.realeconomy.manager.asset.Asset;
import io.github.wysohn.realeconomy.manager.asset.signature.AssetSignature;
import io.github.wysohn.realeconomy.manager.asset.signature.ElectricitySignature;
import io.github.wysohn.realeconomy.manager.asset.signature.ItemStackSignature;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Server;
import org.bukkit.inventory.ItemFactory;
import org.bukkit.inventory.ItemStack;
import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class AssetUtilTest {

    @Before
    public void init() throws Exception {
        Server server = mock(Server.class);
        Field field = Bukkit.class.getDeclaredField("server");
        field.setAccessible(true);
        field.set(null, server);

        ItemFactory itemFactory = mock(ItemFactory.class);
        when(server.getItemFactory()).thenReturn(itemFactory);
    }

    @Test
    public void addAsset() {
        List<Asset> ownedAssets = new ArrayList<>();

        ItemStackSignature itemStackSignature = new ItemStackSignature(new ItemStack(Material.DIAMOND));
        ElectricitySignature electricitySignature = new ElectricitySignature();

        AssetUtil.addAsset(ownedAssets, itemStackSignature.create(new HashMap<String, Object>() {{
            put(AssetSignature.KEY_NUMERIC_MEASURE, 33.0);
        }}));
        AssetUtil.addAsset(ownedAssets, electricitySignature.create(new HashMap<String, Object>() {{
            put(AssetSignature.KEY_NUMERIC_MEASURE, 1088.443);
        }}));
        assertEquals(2, AssetUtil.assetDataProvider(ownedAssets).size());

        AssetUtil.removeAsset(ownedAssets, new ItemStackSignature(new ItemStack(Material.DIAMOND)), 15);
        AssetUtil.removeAsset(ownedAssets, new ElectricitySignature(), 392.44);
        assertEquals(33.0 - 15.0, ownedAssets.get(0).getNumericalMeasure(), 0.00001);
        assertEquals(1088.443 - 392.44, ownedAssets.get(1).getNumericalMeasure(), 0.00001);
        assertEquals(2, AssetUtil.assetDataProvider(ownedAssets).size());

        AssetUtil.addAsset(ownedAssets, itemStackSignature.create(new HashMap<String, Object>() {{
            put(AssetSignature.KEY_NUMERIC_MEASURE, 64.0);
        }}));
        AssetUtil.addAsset(ownedAssets, electricitySignature.create(new HashMap<String, Object>() {{
            put(AssetSignature.KEY_NUMERIC_MEASURE, 10000.0);
        }}));
        assertEquals(4, AssetUtil.assetDataProvider(ownedAssets).size());

        AssetUtil.removeAsset(ownedAssets, new ItemStackSignature(new ItemStack(Material.DIAMOND)), 48);
        AssetUtil.removeAsset(ownedAssets, new ElectricitySignature(), 3000.0);
        assertEquals(33.0 - 15.0 + 64 - 48, ownedAssets.get(0).getNumericalMeasure(), 0.00001);
        assertEquals(1088.443 - 392.44 + 10000.0 - 3000.0, ownedAssets.get(1).getNumericalMeasure(), 0.00001);
        assertEquals(2, AssetUtil.assetDataProvider(ownedAssets).size());

        AssetUtil.removeAsset(ownedAssets, new ItemStackSignature(new ItemStack(Material.DIAMOND)), 33.0 - 15.0 + 64 - 48);
        AssetUtil.removeAsset(ownedAssets, new ElectricitySignature(), 1088.443 - 392.44 + 10000.0 - 3000.0);
        assertEquals(0, AssetUtil.assetDataProvider(ownedAssets).size());
    }
}