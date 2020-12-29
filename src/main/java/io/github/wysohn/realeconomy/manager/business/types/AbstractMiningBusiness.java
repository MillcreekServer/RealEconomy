package io.github.wysohn.realeconomy.manager.business.types;

import io.github.wysohn.realeconomy.interfaces.business.ITier;

import java.util.UUID;

public abstract class AbstractMiningBusiness extends AbstractBusiness {
    public AbstractMiningBusiness(UUID key,
                                  UUID ownerUuid,
                                  ITier tier) {
        super(key, ownerUuid, tier);
    }
}
