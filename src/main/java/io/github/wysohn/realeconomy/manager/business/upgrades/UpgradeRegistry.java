package io.github.wysohn.realeconomy.manager.business.upgrades;

import io.github.wysohn.rapidframework3.bukkit.main.AbstractBukkitPlugin;
import io.github.wysohn.rapidframework3.core.language.ManagerLanguage;
import io.github.wysohn.rapidframework3.utils.Validation;
import io.github.wysohn.realeconomy.interfaces.business.upgrades.IUpgrade;
import io.github.wysohn.realeconomy.main.RealEconomy;
import org.bukkit.Bukkit;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class UpgradeRegistry {
    private static final Map<UUID, IUpgrade> REGISTERED_UPGRADES = new HashMap<>();

    public static void register(IUpgrade upgrade) {
        Validation.assertNotNull(upgrade);
        Validation.assertNotNull(upgrade.getUuid());
        Validation.assertNotNull(upgrade.translate());

        if (REGISTERED_UPGRADES.containsKey(upgrade.getUuid()))
            throw new RuntimeException("Duplicated IUpgrade: " + upgrade.getUuid());

        REGISTERED_UPGRADES.put(upgrade.getUuid(), upgrade);

        // not my favorite, but I have no choice
        Optional.ofNullable(Bukkit.getPluginManager())
                .map(pluginManager -> pluginManager.getPlugin("RealEconomy"))
                .map(RealEconomy.class::cast)
                .map(AbstractBukkitPlugin::getMain)
                .flatMap(main -> main.getManager(ManagerLanguage.class))
                .map(lang -> lang.registerLangauge(upgrade.translate()));
    }

    public static IUpgrade fromUuid(UUID uuid) {
        return REGISTERED_UPGRADES.get(uuid);
    }

    public static IUpgrade[] values() {
        return REGISTERED_UPGRADES.values().toArray(new IUpgrade[0]);
    }
}
