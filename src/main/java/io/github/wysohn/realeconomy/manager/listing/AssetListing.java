package io.github.wysohn.realeconomy.manager.listing;

import io.github.wysohn.rapidframework3.core.caching.CachedElement;
import io.github.wysohn.realeconomy.manager.asset.Asset;
import io.github.wysohn.realeconomy.manager.asset.signature.AssetSignature;

import javax.persistence.Column;
import java.util.Map;
import java.util.UUID;

public class AssetListing extends CachedElement<UUID> {
    @Column
    private AssetSignature signature;

    private AssetListing() {
        this((UUID) null);
    }

    private AssetListing(AssetListing copy){
        super(copy.getKey());
        signature = copy.signature;
    }

    public AssetListing(UUID key) {
        super(key);
    }

    void setSignature(AssetSignature signature) {
        mutate(() -> this.signature = signature);
    }

    public AssetSignature getSignature() {
        return read(() -> signature);
    }

    public Asset create(Map<String, Object> metaData) {
        return read(() -> signature.asset(metaData));
    }

    @Override
    public String toString() {
        return read(() -> signature.toString());
    }
}
