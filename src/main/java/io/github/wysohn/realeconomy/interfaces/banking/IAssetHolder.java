package io.github.wysohn.realeconomy.interfaces.banking;

import io.github.wysohn.rapidframework3.interfaces.paging.DataProvider;
import io.github.wysohn.rapidframework3.utils.Pair;
import io.github.wysohn.realeconomy.manager.asset.signature.AssetSignature;

public interface IAssetHolder {
    /**
     * Add new asset to this holder. This method behave differently depending on the state provided
     * by {@link AssetSignature#isPhysical()}. If it is a physical asset, the amount matters since
     * we need to be able to count the asset. However, if it is not a physical asset, the amount
     * is ignored (a.k.a treated as 1 always) as it does not make sense to count a currency asset.
     * (ex. If there were a check worth 1000 dollars from A and a check worth 1000 dollars from B,
     * we don't combine and count them as 2000 dollars check)
     * <p>
     * Currency asset includes, Bond, Loan, Check, etc. which are the measurements itself so cannot
     * be combined.
     *
     * @param signature the signature
     * @param amount    amount to add
     * @return
     */
    boolean addAsset(AssetSignature signature, int amount);

    default boolean addAsset(AssetSignature signature) {
        return addAsset(signature, 1);
    }

    /**
     * Get number of asset this holder is holding. For non-physical asset, it works as boolean
     * method. 0 if asset does not exist, and 1 if asset exists. For physical assets, it
     * simply returns number of asset the holder is holding.
     *
     * @param signature
     * @return
     */
    int getAssetAmount(AssetSignature signature);

    /**
     * Remove the asset from this holder.
     *
     * @param signature
     * @param amount
     * @return the amount of assets removed. If there were sufficient asset exist in the holder's container,
     * this value should match with the given amount. 0 if nothing has done.
     */
    int removeAsset(AssetSignature signature, int amount);

    default int removeAsset(AssetSignature signature) {
        return removeAsset(signature, 1);
    }

    DataProvider<Pair<AssetSignature, Integer>> assetDataProvider();
}
