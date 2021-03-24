package io.github.wysohn.realeconomy.manager.asset;

import io.github.wysohn.realeconomy.manager.asset.signature.AssetSignature;
import io.github.wysohn.realeconomy.manager.asset.signature.ItemStackSignature;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.Optional;
import java.util.UUID;

public class Item extends PhysicalAsset {
    private Item() {
        super(null, null);
    }

    public Item(UUID key, AssetSignature signature) {
        super(key, signature);
    }

    @Override
    public ItemStack getIcon() {
        if(getSignature() instanceof ItemStackSignature) {
            return Optional.ofNullable(getSignature())
                    .map(ItemStackSignature.class::cast)
                    .map(ItemStackSignature::getItemStack)
                    .map(ItemStack::clone)
                    .orElse(new ItemStack(Material.BARRIER));
        } else { // well this is not likely possible, but who knows
            return super.getIcon();
        }
    }

    @Override
    public Asset clone() {
        Item item = new Item(getUuid(), getSignature());
        item.setAmount(this.getAmount());
        return item;
    }
}
