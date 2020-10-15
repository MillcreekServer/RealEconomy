package io.github.wysohn.realeconomy.manager.asset.signature;

import io.github.wysohn.realeconomy.manager.asset.Asset;
import io.github.wysohn.realeconomy.manager.asset.Item;
import org.bukkit.inventory.ItemStack;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Unlike the original ItemStack, it does not keep track of the amount of items in stack.
 * Number of items must be tracked separately in the
 * {@link io.github.wysohn.realeconomy.manager.asset.listing.AssetListing}.
 * As stated in {@link AssetSignature}, it should not contain any mutable attributes.
 */
public class ItemStackSignature extends PhysicalAssetSignature {
    private final ItemStack itemStack;

    public ItemStackSignature(ItemStack itemStack) {
        this.itemStack = itemStack;
    }

    /**
     * The returned ItemStack will share the reference. Must copy it using {@link ItemStack#clone()}
     * if necessary.
     *
     * @return
     */
    public ItemStack getItemStack() {
        return itemStack;
    }

    @Override
    public AssetSignature clone() {
        return new ItemStackSignature(itemStack.clone());
    }

    @Override
    public Asset create(Map<String, Object> metaData) {
        return new Item(UUID.randomUUID(), this);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ItemStackSignature that = (ItemStackSignature) o;
        return itemStack.isSimilar(that.itemStack);
    }

    @Override
    public int hashCode() {
        int hash = 1;

        hash = hash * 31 + itemStack.getType().hashCode();
        hash = hash * 31 + (itemStack.getDurability() & 0xffff);
        hash = hash * 31 + Optional.of(itemStack)
                .map(ItemStack::getItemMeta)
                .map(Object::hashCode)
                .orElse(0);

        return hash;
    }
}
