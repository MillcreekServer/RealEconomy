package io.github.wysohn.realeconomy.manager.asset.signautre;

import io.github.wysohn.realeconomy.manager.asset.CurrencyAsset;

import java.util.UUID;

public class CurrencyAssetSignature extends AssetSignature<CurrencyAsset> {
    private final UUID currencyUuid;
    private final double faceValue;
    private final double rate;

    public CurrencyAssetSignature(UUID issuerUuid, UUID currencyUuid, double faceValue, double rate) {
        super(issuerUuid);
        this.currencyUuid = currencyUuid;
        this.faceValue = faceValue;
        this.rate = rate;
    }
}
