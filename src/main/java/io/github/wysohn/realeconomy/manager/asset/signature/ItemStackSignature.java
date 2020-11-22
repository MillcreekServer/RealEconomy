package io.github.wysohn.realeconomy.manager.asset.signature;

import io.github.wysohn.realeconomy.manager.asset.Asset;
import io.github.wysohn.realeconomy.manager.asset.Item;
import io.github.wysohn.realeconomy.mediator.TradeMediator;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Unlike the original ItemStack, it does not keep track of the amount of items in stack.
 * Number of items must be tracked separately in the
 * {@link io.github.wysohn.realeconomy.manager.asset.listing.AssetListing}.
 * As stated in {@link AssetSignature}, it should not contain any mutable attributes.
 * <p>
 * When creating Asset, {@link PhysicalAssetSignature#KEY_AMOUNT} must be in meta info
 */
public class ItemStackSignature extends PhysicalAssetSignature {
    private final ItemStack itemStack;

    public ItemStackSignature(ItemStack itemStack) {
        this.itemStack = Objects.requireNonNull(itemStack).clone();
        this.itemStack.setAmount(1);
    }

    @Override
    public String category() {
        return TradeMediator.MATERIAL_CATEGORY_MAP.get(itemStack.getType());
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
        return new ItemStackSignature(itemStack == null ? new ItemStack(Material.AIR) : itemStack);
    }

    @Override
    public Asset create(Map<String, Object> metaData) {
        Item item = new Item(UUID.randomUUID(), this);
        // fail early if amount is not set
        item.setAmount((int) Objects.requireNonNull(metaData.get(KEY_AMOUNT)));
        return item;
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
