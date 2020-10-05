package io.github.wysohn.realeconomy.manager.asset.signature;

import java.util.UUID;

public abstract class PhysicalAssetSignature extends AssetSignature {
    public PhysicalAssetSignature(UUID issuerUuid) {
        super(issuerUuid);
    }
}
