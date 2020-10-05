package io.github.wysohn.realeconomy.main;

import io.github.wysohn.rapidframework3.bukkit.testutils.SimpleBukkitPluginMainTest;
import org.bukkit.Server;
import org.junit.Test;

public class RealEconomyTest {
    @Test
    public void enable() {
        Server server = new SimpleBukkitPluginMainTest<RealEconomy>() {
            @Override
            public RealEconomy instantiate(Server server) {
                return new RealEconomy(server);
            }
        }.enable();
    }
}