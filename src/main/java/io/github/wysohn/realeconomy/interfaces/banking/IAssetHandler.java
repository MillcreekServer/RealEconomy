package io.github.wysohn.realeconomy.interfaces.banking;

import io.github.wysohn.rapidframework3.interfaces.paging.DataProvider;
import io.github.wysohn.realeconomy.manager.asset.Asset;
import io.github.wysohn.realeconomy.manager.asset.signature.AssetSignature;

import java.util.List;

public interface IAssetHandler {
    void addAsset(List<Asset> ownedAssets, Asset asset);

    int removeAsset(List<Asset> ownedAssets, AssetSignature signature, int amount);

    DataProvider<Asset> assetDataProvider(List<Asset> ownedAssets);
}
