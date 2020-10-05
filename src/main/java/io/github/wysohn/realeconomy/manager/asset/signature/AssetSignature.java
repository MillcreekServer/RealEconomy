package io.github.wysohn.realeconomy.manager.asset.signature;

import io.github.wysohn.realeconomy.manager.asset.Asset;

import java.util.Map;
import java.util.UUID;

/**
 * A class that represent the detailed information about an Asset.
 * Children class must be immutable in order to provide concurrent access from multiple threads.
 */
public abstract class AssetSignature {
    private final UUID issuerUuid;

    public AssetSignature(UUID issuerUuid) {
        this.issuerUuid = issuerUuid;
    }

    public UUID getIssuerUuid() {
        return issuerUuid;
    }

    public abstract AssetSignature clone();

    public abstract Asset create(Map<String, Object> metaData);
}
