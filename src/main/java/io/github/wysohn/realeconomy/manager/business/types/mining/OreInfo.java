package io.github.wysohn.realeconomy.manager.business.types.mining;

import org.bukkit.Material;

public class OreInfo {
    public final Material material;
    public final long breakAt;

    public OreInfo(Material material, long breakAt) {
        this.material = material;
        this.breakAt = breakAt;
    }


}
