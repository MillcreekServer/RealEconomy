package io.github.wysohn.realeconomy.manager.asset.listing;

import io.github.wysohn.rapidframework3.core.caching.CachedElement;
import io.github.wysohn.realeconomy.manager.asset.Asset;
import io.github.wysohn.realeconomy.manager.asset.signature.AssetSignature;

import java.util.Map;
import java.util.UUID;

public class AssetListing extends CachedElement<UUID> {
    private AssetSignature signature;
    private UUID issuerUuid;

    private AssetListing() {
        this(null);
    }

    public AssetListing(UUID key) {
        super(key);
    }

    void setSignature(AssetSignature signature) {
        this.signature = signature;

        notifyObservers();
    }

    public AssetSignature getSignature() {
        return signature;
    }

    public void setIssuerUuid(UUID issuerUuid) {
        this.issuerUuid = issuerUuid;

        notifyObservers();
    }

    public UUID getIssuerUuid() {
        return issuerUuid;
    }

    public Asset create(Map<String, Object> metaData) {
        return signature.create(metaData);
    }
}
