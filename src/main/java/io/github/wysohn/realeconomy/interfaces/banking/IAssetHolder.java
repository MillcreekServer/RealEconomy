package io.github.wysohn.realeconomy.interfaces.banking;

import io.github.wysohn.rapidframework3.interfaces.entity.IEntitySnapshot;
import io.github.wysohn.rapidframework3.interfaces.paging.DataProvider;
import io.github.wysohn.realeconomy.manager.asset.Asset;
import io.github.wysohn.realeconomy.manager.asset.signature.AssetSignature;

import java.util.Collection;
import java.util.function.Consumer;

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
     * Count the amount of assets it has. Amount can mean different thing
     * depending on the type of the AssetSignature. If it's an ItemSignature,
     * it will be total amount of the item, but if it's some form of currency,
     * it may return the total amount of currency.
     *
     * @param signature asset signature
     * @return total amount
     */
    double countAsset(AssetSignature signature);

    /**
     * Remove the asset from this holder.
     *
     * @param signature
     * @param amount
     * @return the amount of assets removed. If there were sufficient asset exist
     * in the holder's container, this value should match with the given amount.
     * 0 if nothing has done.
     */
    Collection<Asset> removeAsset(AssetSignature signature, double amount);

    /**
     * Remove an asset at the specified index of this holder's asset container.
     *
     * @param index index
     * @return the Asset removed; null if nothing was removed
     */
    Asset removeAsset(int index);

    DataProvider<Asset> assetDataProvider(Consumer<Runnable> readlock);
}
