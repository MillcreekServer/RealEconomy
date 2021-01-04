package io.github.wysohn.realeconomy.manager.asset;

import io.github.wysohn.realeconomy.manager.asset.signature.AssetSignature;

import java.util.UUID;

public class Bond extends Asset {
    private Bond() {
        super(null, null);
    }

    public Bond(UUID key, AssetSignature signature) {
        super(key, signature);
    }

    public Bond(AssetSignature signature) {
        super(signature);
    }

    @Override
    public double getNumericalMeasure() {
        throw new RuntimeException();
    }

    @Override
    public void setNumericalMeasure(double value) {
        throw new RuntimeException();
    }

    @Override
    public Asset clone() {
        throw new RuntimeException();
    }
}
