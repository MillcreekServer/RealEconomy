package io.github.wysohn.realeconomy.interfaces.business;

import io.github.wysohn.rapidframework3.interfaces.IPluginObject;
import io.github.wysohn.realeconomy.interfaces.IVisitable;
import io.github.wysohn.realeconomy.interfaces.banking.IAssetHolder;
import io.github.wysohn.realeconomy.manager.asset.signature.AssetSignature;

import java.util.Map;
import java.util.UUID;

public interface IBusiness extends IAssetHolder, IVisitable, IPluginObject {
    UUID getOwnerUuid();

    void setOwnerUuid(UUID ownerUuid);

    ITier currentTier();

    void replaceTier(ITier tier);

    boolean isEstablished();

    boolean endOfLife();

    Map<AssetSignature, Double> getCurrentProgress();

    Map<AssetSignature, Double> getProductionStorage();

    void init();

    void update();

    void stop();
}
