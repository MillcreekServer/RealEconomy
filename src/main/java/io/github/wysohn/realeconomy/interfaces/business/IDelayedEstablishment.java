package io.github.wysohn.realeconomy.interfaces.business;

import io.github.wysohn.rapidframework3.utils.Pair;
import io.github.wysohn.realeconomy.manager.asset.signature.AssetSignature;

import java.util.Map;

public interface IDelayedEstablishment {
    boolean isEstablished();

    Map<AssetSignature, Double> getCurrentProgress();

    default double getProgress(AssetSignature signature) {
        return getProgress(signature, false);
    }

    double getProgress(AssetSignature signature, boolean asRatio);

    void setProgress(Pair<AssetSignature, Double>... progresses);
}
