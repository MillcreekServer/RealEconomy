package io.github.wysohn.realeconomy.manager.asset.signautre;

import io.github.wysohn.realeconomy.manager.asset.PhysicalAsset;

import java.util.UUID;

public class PhysicalAssetSignature<T> extends AssetSignature<PhysicalAsset<T>> {
    private final T physicalAsset;

    public PhysicalAssetSignature(UUID issuerUuid, T physicalAsset) {
        super(issuerUuid);
        this.physicalAsset = physicalAsset;
    }

    public T getPhysicalAsset() {
        return physicalAsset;
    }
}
