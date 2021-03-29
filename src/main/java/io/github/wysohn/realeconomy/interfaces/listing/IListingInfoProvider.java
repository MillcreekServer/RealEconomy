package io.github.wysohn.realeconomy.interfaces.listing;

import io.github.wysohn.realeconomy.manager.asset.signature.AssetSignature;

import java.util.UUID;

public interface IListingInfoProvider {

    void newListing(AssetSignature sign);

    UUID signatureToUuid(AssetSignature sign);

    AssetSignature uuidToSignature(UUID uuid);
}
