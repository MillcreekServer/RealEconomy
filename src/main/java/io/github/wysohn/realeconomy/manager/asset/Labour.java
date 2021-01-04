package io.github.wysohn.realeconomy.manager.asset;

import io.github.wysohn.realeconomy.manager.asset.signature.AssetSignature;

import java.util.UUID;

public class Labour extends Asset {
    private double labourPoints = 0.0;

    private Labour() {
        super(null, null);
    }

    public Labour(UUID key, AssetSignature signature) {
        super(key, signature);
    }

    @Override
    public double getNumericalMeasure() {
        return labourPoints;
    }

    @Override
    public void setNumericalMeasure(double value) {
        this.labourPoints = value;
    }

    @Override
    public Asset clone() {
        Labour labour = new Labour();
        labour.labourPoints = labourPoints;
        return labour;
    }
}
