package io.github.wysohn.realeconomy.inject.module;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import io.github.wysohn.rapidframework3.interfaces.paging.DataProvider;
import io.github.wysohn.realeconomy.interfaces.banking.IAssetHandler;
import io.github.wysohn.realeconomy.manager.asset.Asset;
import io.github.wysohn.realeconomy.manager.asset.PhysicalAsset;
import io.github.wysohn.realeconomy.manager.asset.signature.AssetSignature;

import java.util.List;
import java.util.ListIterator;
import java.util.Objects;

public class AssetHandlerModule extends AbstractModule {
    @Provides
    @Singleton
    IAssetHandler assetHandler() {
        return new IAssetHandler() {
            @Override
            public void addAsset(List<Asset> ownedAssets, Asset asset) {
                ownedAssets.add(asset);
            }

            @Override
            public int removeAsset(List<Asset> ownedAssets, AssetSignature signature, int amount) {
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

            @Override
            public DataProvider<Asset> assetDataProvider(List<Asset> ownedAssets) {
                return null;
            }
        };
    }
}
