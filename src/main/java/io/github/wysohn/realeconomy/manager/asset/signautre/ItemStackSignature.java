package io.github.wysohn.realeconomy.manager.asset.signautre;

import org.bukkit.inventory.ItemStack;

import java.util.UUID;

public class ItemStackSignature extends PhysicalAssetSignature<ItemStack> {
    public ItemStackSignature(UUID issuerUuid, ItemStack physicalAsset) {
        super(issuerUuid, physicalAsset);
    }
}
