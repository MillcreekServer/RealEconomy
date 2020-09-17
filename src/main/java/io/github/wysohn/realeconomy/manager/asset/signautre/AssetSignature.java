package io.github.wysohn.realeconomy.manager.asset.signautre;

import io.github.wysohn.realeconomy.manager.asset.Asset;

import java.util.UUID;

public class AssetSignature<A extends Asset> {
    private final UUID issuerUuid;

    public AssetSignature(UUID issuerUuid) {
        this.issuerUuid = issuerUuid;
    }
}
