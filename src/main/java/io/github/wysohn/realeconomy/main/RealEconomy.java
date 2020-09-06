package io.github.wysohn.realeconomy.main;

import io.github.wysohn.rapidframework2.bukkit.main.AbstractBukkitPlugin;
import io.github.wysohn.rapidframework2.bukkit.main.BukkitPluginBridge;
import io.github.wysohn.rapidframework2.core.interfaces.plugin.IPluginManager;
import io.github.wysohn.rapidframework2.core.manager.player.AbstractPlayerWrapper;

import java.io.File;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Logger;

public class RealEconomy extends AbstractBukkitPlugin {
    @Override
    protected BukkitPluginBridge createCore() {
        return new RealEconomyBridge(this);
    }

    @Override
    protected BukkitPluginBridge createCore(String s, String s1, String s2, String s3, Logger logger, File file, IPluginManager iPluginManager) {
        return new RealEconomyBridge(s, s1, s2, s3, logger, file, iPluginManager, this);
    }

    @Override
    protected Optional<? extends AbstractPlayerWrapper> getPlayerWrapper(UUID uuid) {
        throw new RuntimeException("Need wrapper.");
    }
}
