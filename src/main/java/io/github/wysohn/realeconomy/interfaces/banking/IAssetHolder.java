package io.github.wysohn.realeconomy.interfaces.banking;

import io.github.wysohn.rapidframework3.interfaces.paging.DataProvider;
import io.github.wysohn.realeconomy.manager.asset.Asset;
import io.github.wysohn.realeconomy.manager.asset.signature.AssetSignature;

public interface IAssetHolder {
    /**
     * Add asset to this holder.
     */
    void addAsset(Asset asset);

    /**
     * Remove the asset from this holder.
     *
     * @param signature
     * @param amount
     * @return the amount of assets removed. If there were sufficient asset exist in the holder's container,
     * this value should match with the given amount. 0 if nothing has done.
     */
    int removeAsset(AssetSignature signature, int amount);

    DataProvider<Asset> assetDataProvider();
}
