package io.github.wysohn.realeconomy.interfaces.business;

import io.github.wysohn.rapidframework3.interfaces.IPluginObject;
import io.github.wysohn.realeconomy.interfaces.banking.IAssetHolder;
import io.github.wysohn.realeconomy.interfaces.business.tiers.ITier;

import java.util.UUID;

public interface IBusiness extends IAssetHolder, IPluginObject, IUpgradable, IDelayedEstablishment,
        IProductionStorage, ISimpleStringStore {
    UUID getOwnerUuid();

    void setOwnerUuid(UUID ownerUuid);

    ITier currentTier();

    void replaceTier(ITier tier);

    boolean endOfLife();

    void init();

    void update();

    void stop();

}
