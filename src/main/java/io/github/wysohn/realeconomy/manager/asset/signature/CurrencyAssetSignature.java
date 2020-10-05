package io.github.wysohn.realeconomy.manager.asset.signature;

import java.util.UUID;

public abstract class CurrencyAssetSignature extends AssetSignature {
    private final UUID currencyUuid;
    private final double faceValue;
    private final double rate;

    private CurrencyAssetSignature() {
        this(null, null, 0.0, 0.0);
    }

    public CurrencyAssetSignature(UUID issuerUuid, UUID currencyUuid, double faceValue, double rate) {
        super(issuerUuid);
        this.currencyUuid = currencyUuid;
        this.faceValue = faceValue;
        this.rate = rate;
    }

    public UUID getCurrencyUuid() {
        return currencyUuid;
    }

    public double getFaceValue() {
        return faceValue;
    }

    public double getRate() {
        return rate;
    }
}
