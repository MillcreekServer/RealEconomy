package io.github.wysohn.realeconomy.interfaces.business;

import io.github.wysohn.rapidframework3.utils.Pair;
import io.github.wysohn.realeconomy.manager.asset.signature.AssetSignature;

import java.util.Map;

public interface IProductionStorage {
    Map<AssetSignature, Double> getProductionMaterials();

    default double getProductionMaterial(AssetSignature signature) {
        return getProductionMaterial(signature, false);
    }

    double getProductionMaterial(AssetSignature signature, boolean asRatio);

    void setProductionMaterial(Pair<AssetSignature, Double>... values);
}
