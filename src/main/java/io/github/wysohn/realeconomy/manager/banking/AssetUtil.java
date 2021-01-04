package io.github.wysohn.realeconomy.manager.banking;

import io.github.wysohn.rapidframework3.core.paging.DataProviderProxy;
import io.github.wysohn.rapidframework3.interfaces.paging.DataProvider;
import io.github.wysohn.realeconomy.manager.asset.Asset;
import io.github.wysohn.realeconomy.manager.asset.signature.AssetSignature;

import java.util.*;

public class AssetUtil {

    /**
     * Add the asset to the target list.
     * <p>
     * TODO hopefully, we can stack the same assets as one
     *
     * @param ownedAssets
     * @param asset
     */
    public static void addAsset(List<Asset> ownedAssets, Asset asset) {
        // ignore meaningless assets
        if (asset.getNumericalMeasure() <= 0.0)
            return;

        ownedAssets.add(asset.clone());
    }

    /**
     * Remove the target asset from the list.
     *
     * @param ownedAssets target list
     * @param signature   the signature. Any Asset that has the same signature will be removed.
     * @param amount      amount to be removed.
     * @return the list of Assets that were removed
     */
    public static Collection<Asset> removeAsset(List<Asset> ownedAssets, AssetSignature signature, double amount) {
        double remove = amount;
        Collection<Asset> removed = new LinkedList<>();

        ListIterator<Asset> assetListIterator = ownedAssets.listIterator();
        while (assetListIterator.hasNext() && remove > 0) {
            Asset asset = assetListIterator.next();
            if (!Objects.equals(asset.getSignature(), signature))
                continue;

            double currentAmount = asset.getNumericalMeasure();

            if (currentAmount > remove) {
                asset.setNumericalMeasure(currentAmount - remove);

                // create the portion that was removed
                Asset temp = asset.clone();
                temp.setNumericalMeasure(remove);

                removed.add(temp);
                break;
            } else if (currentAmount == remove) {
                assetListIterator.remove();

                removed.add(asset);
                break;
            } else {
                assetListIterator.remove();

                remove -= currentAmount;
                removed.add(asset);
            }
        }

        return removed;
    }

    /**
     * Create a new DataProvider for the target Asset list.
     * This internally utilizes cache, so by using the same instance, it will
     * query the list only once per second. In other words, it has less overhead,
     * but it may not have the latest information.
     *
     * @param ownedAssets
     * @return
     */
    public static DataProvider<Asset> assetDataProvider(List<Asset> ownedAssets) {
        return new DataProviderProxy<>(ownedAssets, 1000L);
    }
}
