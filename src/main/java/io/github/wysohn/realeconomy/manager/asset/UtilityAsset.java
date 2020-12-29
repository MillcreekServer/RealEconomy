package io.github.wysohn.realeconomy.manager.asset;

import io.github.wysohn.realeconomy.manager.asset.signature.AssetSignature;

import java.util.UUID;

public abstract class UtilityAsset extends Asset {

    public UtilityAsset(UUID key, AssetSignature signature) {
        super(key, signature);
    }
}
