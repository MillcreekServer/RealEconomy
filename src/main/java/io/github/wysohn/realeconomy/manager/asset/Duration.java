package io.github.wysohn.realeconomy.manager.asset;

import io.github.wysohn.realeconomy.manager.asset.signature.AssetSignature;

import java.util.UUID;

/**
 * Duration in seconds
 */
public class Duration extends Asset {
    private double timeSeconds = 0.0;

    private Duration() {
        super(null, null);
    }

    public Duration(UUID key, AssetSignature signature) {
        super(key, signature);
    }

    @Override
    public double getNumericalMeasure() {
        return timeSeconds;
    }

    @Override
    public void setNumericalMeasure(double value) {
        this.timeSeconds = value;
    }

    @Override
    public Asset clone() {
        Duration duration = new Duration(getUuid(), getSignature());
        duration.timeSeconds = timeSeconds;
        return duration;
    }
}
