package io.github.wysohn.realeconomy.manager.asset;

import io.github.wysohn.realeconomy.manager.asset.signature.AssetSignature;
import io.github.wysohn.realeconomy.manager.asset.signature.ItemStackSignature;
import org.bukkit.inventory.ItemStack;

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
        if(getSignature() instanceof ItemStackSignature){
            return ((ItemStackSignature) getSignature()).getItemStack().clone();
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
