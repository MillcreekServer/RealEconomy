package io.github.wysohn.realeconomy.manager.asset;

import io.github.wysohn.realeconomy.manager.asset.signature.AssetSignature;

import java.util.UUID;

public class Check extends Asset {
    private Check() {
        super(null, null);
    }

    public Check(UUID key, AssetSignature signature) {
        super(key, signature);
    }

    public Check(AssetSignature signature) {
        super(signature);
    }

    @Override
    public Asset clone() {
        throw new RuntimeException();
    }
}
