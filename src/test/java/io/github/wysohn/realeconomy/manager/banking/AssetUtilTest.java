package io.github.wysohn.realeconomy.manager.banking;

import io.github.wysohn.rapidframework3.interfaces.paging.DataProvider;
import io.github.wysohn.realeconomy.manager.asset.Asset;
import io.github.wysohn.realeconomy.manager.asset.PhysicalAsset;
import io.github.wysohn.realeconomy.manager.asset.signature.AssetSignature;
import io.github.wysohn.realeconomy.manager.asset.signature.ItemStackSignature;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class AssetUtilTest {

    @Test
    public void addAsset() {
        List<Asset> ownedAssets = new ArrayList<>();
        Asset asset1 = mock(Asset.class);
        PhysicalAsset asset2 = mock(PhysicalAsset.class);
        Asset asset3 = mock(Asset.class);

        Asset asset1Clone = mock(Asset.class);
        PhysicalAsset asset2Clone = mock(PhysicalAsset.class);
        Asset asset3Clone = mock(Asset.class);

        AssetSignature asset1Signature = mock(AssetSignature.class);
        ItemStackSignature asset2Signature = mock(ItemStackSignature.class);
        AssetSignature asset3Signature = mock(AssetSignature.class);

        when(asset1.clone()).thenReturn(asset1Clone);
        when(asset1Clone.getSignature()).thenReturn(asset1Signature);
        when(asset2.clone()).thenReturn(asset2Clone);
        when(asset2Clone.getSignature()).thenReturn(asset2Signature);
        when(asset3.clone()).thenReturn(asset3Clone);
        when(asset3Clone.getSignature()).thenReturn(asset3Signature);

        AssetUtil.addAsset(ownedAssets, asset3);
        AssetUtil.addAsset(ownedAssets, asset1);
        AssetUtil.addAsset(ownedAssets, asset2);

        assertTrue(ownedAssets.contains(asset1Clone));
        assertTrue(ownedAssets.contains(asset2Clone));
        assertTrue(ownedAssets.contains(asset3Clone));

        DataProvider<Asset> dataProvider = AssetUtil.assetDataProvider(ownedAssets);
        assertEquals(ownedAssets, dataProvider.get(0, 3));

        assertEquals(1, AssetUtil.removeAsset(ownedAssets, asset1Signature, 1));
        when(asset2Clone.getAmount()).thenReturn(364);
        assertEquals(300, AssetUtil.removeAsset(ownedAssets, asset2Signature, 300));
        assertEquals(1, AssetUtil.removeAsset(ownedAssets, asset3Signature, 1));
        when(asset2Clone.getAmount()).thenReturn(64);
        assertEquals(64, AssetUtil.removeAsset(ownedAssets, asset2Signature, 100));

        AssetUtil.addAsset(ownedAssets, asset2);
        when(asset2Clone.getAmount()).thenReturn(100);
        assertEquals(100, AssetUtil.removeAsset(ownedAssets, asset2Signature, 100));

        assertEquals(0, ownedAssets.size());
    }
}