package io.github.wysohn.realeconomy.interfaces.business.types.mining;

import io.github.wysohn.rapidframework3.data.SimpleLocation;
import org.bukkit.Material;

public interface IBlockGenerator {
    void generateBlockAt(SimpleLocation sloc, Material material);
}
