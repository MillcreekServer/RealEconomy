package io.github.wysohn.realeconomy.manager.asset;

import io.github.wysohn.realeconomy.manager.asset.signature.AssetSignature;

import java.util.UUID;

public abstract class PhysicalAsset extends Asset {
    private int amount;

    public PhysicalAsset(UUID key,
                         AssetSignature signature) {
        super(key, signature);
    }

    public int getAmount() {
        return amount;
    }

    public void setAmount(int amount) {
        this.amount = amount;

        setLastUpdate(System.currentTimeMillis());
    }
}
