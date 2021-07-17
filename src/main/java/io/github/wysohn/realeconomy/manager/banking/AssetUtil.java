package io.github.wysohn.realeconomy.manager.banking;

import io.github.wysohn.rapidframework3.core.paging.DataProviderProxy;
import io.github.wysohn.rapidframework3.interfaces.paging.DataProvider;
import io.github.wysohn.realeconomy.manager.asset.Asset;
import io.github.wysohn.realeconomy.manager.asset.signature.AssetSignature;

import java.util.*;
import java.util.function.Consumer;

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
     * Count total amount of assets it has. O(n)
     * @param ownedAssets
     * @param sign asset signature
     * @return total amount of asset
     */
    public static double countAsset(List<Asset> ownedAssets, AssetSignature sign){
        double count = 0;
        for (Asset ownedAsset : ownedAssets) {
            if(!Objects.equals(sign, ownedAsset.getSignature()))
                continue;

            count += ownedAsset.getNumericalMeasure();
        }
        return count;
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
     * Remove an asset at the specified index. If the specified index is
     * out of bound, it will treat it as of trying to accessing the slot
     * that is empty (in other words, it will return null if index is out of bound).
     *
     * @param ownedAssets the assets list
     * @param index       target index
     * @return the Asset removed at the specified index; null if nothing was removed.
     */
    public static Asset removeAsset(List<Asset> ownedAssets, int index) {
        if (index < 0 || index >= ownedAssets.size())
            return null;

        return ownedAssets.remove(index);
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
    public static DataProvider<Asset> assetDataProvider(List<Asset> ownedAssets, Consumer<Runnable> readlock) {
        return new DataProviderProxy<>(ownedAssets, 1000L);
    }
}
