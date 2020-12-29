package io.github.wysohn.realeconomy.interfaces.banking;

import io.github.wysohn.rapidframework3.interfaces.entity.IEntitySnapshot;
import io.github.wysohn.rapidframework3.interfaces.paging.DataProvider;
import io.github.wysohn.realeconomy.manager.asset.Asset;
import io.github.wysohn.realeconomy.manager.asset.signature.AssetSignature;

/**
 * Represent any store that contains 'virtual' assets.
 * Anything hold by this will be virtual, so it has no real world form (yet).
 * For example, a bank may be a IAssetHolder, and it may contain virtual items
 * which doesn't exist in reality.
 * <p>
 * If {@link io.github.wysohn.realeconomy.interfaces.IFinancialEntity} is the 'real' asset storage,
 * this class is the 'virtual' asset storage.
 */
public interface IAssetHolder extends IEntitySnapshot {
    /**
     * Add asset to this holder.
     */
    void addAsset(Asset asset);

    /**
     * Remove the asset from this holder.
     *
     * @param signature
     * @param amount
     * @return the amount of assets removed. If there were sufficient asset exist
     * in the holder's container, this value should match with the given amount.
     * 0 if nothing has done.
     */
    int removeAsset(AssetSignature signature, int amount);

    DataProvider<Asset> assetDataProvider();
}
