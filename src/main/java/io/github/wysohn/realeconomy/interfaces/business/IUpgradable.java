package io.github.wysohn.realeconomy.interfaces.business;

import io.github.wysohn.realeconomy.interfaces.business.upgrades.IUpgrade;

import java.util.Map;

public interface IUpgradable {
    Map<IUpgrade, Integer> getUpgrades();

    int getUpgrade(IUpgrade upgrade);

    void setUpgrade(IUpgrade upgrade, int level);
}
