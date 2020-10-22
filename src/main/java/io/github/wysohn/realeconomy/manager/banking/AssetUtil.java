package io.github.wysohn.realeconomy.manager.banking;

import io.github.wysohn.rapidframework3.core.paging.DataProviderProxy;
import io.github.wysohn.rapidframework3.interfaces.paging.DataProvider;
import io.github.wysohn.realeconomy.manager.asset.Asset;
import io.github.wysohn.realeconomy.manager.asset.PhysicalAsset;
import io.github.wysohn.realeconomy.manager.asset.signature.AssetSignature;

import java.util.List;
import java.util.ListIterator;
import java.util.Objects;

public class AssetUtil {

    public static void addAsset(List<Asset> ownedAssets, Asset asset) {
        ownedAssets.add(asset.clone());
    }

    public static int removeAsset(List<Asset> ownedAssets, AssetSignature signature, int amount) {
        int remove = amount;

        ListIterator<Asset> assetListIterator = ownedAssets.listIterator();
        while (assetListIterator.hasNext() && remove > 0) {
            Asset asset = assetListIterator.next();
            if (!Objects.equals(asset.getSignature(), signature))
                continue;

            if (asset instanceof PhysicalAsset) {
                int currentAmount = ((PhysicalAsset) asset).getAmount();
                if (currentAmount > remove) {
                    ((PhysicalAsset) asset).setAmount(currentAmount - remove);
                    remove = 0;
                    break;
                } else if (currentAmount == remove) {
                    assetListIterator.remove();
                    remove = 0;
                    break;
                } else {
                    assetListIterator.remove();
                    remove -= currentAmount;
                }
            } else {
                assetListIterator.remove();
                remove--;
            }
        }

        return amount - remove;
    }

    public static DataProvider<Asset> assetDataProvider(List<Asset> ownedAssets) {
        return new DataProviderProxy<>(ownedAssets, 1000L);
    }
}
