package io.github.wysohn.realeconomy.manager.asset;

import io.github.wysohn.rapidframework3.utils.Validation;
import io.github.wysohn.realeconomy.manager.asset.signature.AssetSignature;

import java.util.UUID;

public class Electricity extends UtilityAsset {
    private double storedPower = 0.0;

    private Electricity() {
        super(null, null);
    }

    public Electricity(UUID key, AssetSignature signature) {
        super(key, signature);
    }

    @Override
    public double getNumericalMeasure() {
        return storedPower;
    }

    @Override
    public void setNumericalMeasure(double value) {
        Validation.validate(value, v -> v > 0.0, "Must be non-zero positive.");
        this.storedPower = value;

        setLastUpdate(System.currentTimeMillis());
    }

    @Override
    public Asset clone() {
        Electricity electricity = new Electricity(getUuid(), getSignature());
        electricity.storedPower = storedPower;
        return electricity;
    }
}
