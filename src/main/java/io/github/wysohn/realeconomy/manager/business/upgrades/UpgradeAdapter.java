package io.github.wysohn.realeconomy.manager.business.upgrades;

import io.github.wysohn.rapidframework3.interfaces.language.ILang;
import io.github.wysohn.rapidframework3.utils.Validation;
import io.github.wysohn.realeconomy.interfaces.business.upgrades.IUpgrade;

import java.util.UUID;

public class UpgradeAdapter implements IUpgrade {
    private final UUID uuid;
    private final ILang translate;

    public UpgradeAdapter(UUID uuid, ILang translate) {
        Validation.assertNotNull(uuid);
        Validation.assertNotNull(translate);

        this.uuid = uuid;
        this.translate = translate;

        UpgradeRegistry.register(this);
    }

    @Override
    public UUID getUuid() {
        return uuid;
    }

    @Override
    public ILang translate() {
        return translate;
    }
}
