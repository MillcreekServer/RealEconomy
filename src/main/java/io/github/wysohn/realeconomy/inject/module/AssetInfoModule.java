package io.github.wysohn.realeconomy.inject.module;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import io.github.wysohn.realeconomy.interfaces.listing.IListingInfoProvider;
import io.github.wysohn.realeconomy.manager.asset.signature.AssetSignature;
import io.github.wysohn.realeconomy.manager.listing.AssetListing;
import io.github.wysohn.realeconomy.manager.listing.AssetListingManager;

import java.lang.ref.Reference;
import java.util.UUID;

public class AssetInfoModule extends AbstractModule {
    @Provides
    @Singleton
    IListingInfoProvider assetInfoProvider(AssetListingManager assetListingManager) {
        return new IListingInfoProvider() {
            @Override
            public void newListing(AssetSignature sign) {
                assetListingManager.newListing(sign);
            }

            @Override
            public UUID signatureToUuid(AssetSignature sign) {
                return assetListingManager.signatureToUuid(sign);
            }

            @Override
            public AssetSignature uuidToSignature(UUID uuid) {
                return assetListingManager.get(uuid)
                        .map(Reference::get)
                        .map(AssetListing::getSignature)
                        .orElse(null);
            }
        };
    }
}
