package io.github.wysohn.realeconomy.manager.asset.signature;

import io.github.wysohn.rapidframework3.bukkit.manager.common.message.BukkitMessageBuilder;
import io.github.wysohn.rapidframework3.bukkit.utils.InventoryUtil;
import io.github.wysohn.rapidframework3.core.language.ManagerLanguage;
import io.github.wysohn.rapidframework3.core.message.Message;
import io.github.wysohn.rapidframework3.interfaces.ICommandSender;
import io.github.wysohn.realeconomy.manager.asset.Asset;
import io.github.wysohn.realeconomy.manager.asset.Item;
import io.github.wysohn.realeconomy.manager.listing.AssetListing;
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
 * {@link AssetListing}.
 * As stated in {@link AssetSignature}, it should not contain any mutable attributes.
 * <p>
 * When creating Asset, {@link AssetSignature#KEY_NUMERIC_MEASURE} must be in meta info
 */
public class ItemStackSignature extends PhysicalAssetSignature {
    private final ItemStack itemStack;

    public ItemStackSignature(ItemStack itemStack) {
        this.itemStack = Objects.requireNonNull(itemStack).clone();
        this.itemStack.setAmount(1);
    }

    public ItemStackSignature(Material material) {
        this.itemStack = new ItemStack(Objects.requireNonNull(material));
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
        return itemStack == null ? new ItemStack(Material.AIR) : itemStack;
    }

    @Override
    public AssetSignature clone() {
        return new ItemStackSignature(itemStack == null ? new ItemStack(Material.AIR) : itemStack);
    }

    @Override
    public Asset asset(Map<String, Object> metaData) {
        Item item = new Item(UUID.randomUUID(), this);
        // fail early if amount is not set
        item.setNumericalMeasure((double) Objects.requireNonNull(metaData.get(KEY_NUMERIC_MEASURE)));
        return item;
    }

    @Override
    public Message[] toMessage(ManagerLanguage lang, ICommandSender sender) {
        return BukkitMessageBuilder.forBukkitMessage(Optional.of(itemStack)
                .map(ItemStack::getType)
                .map(Objects::toString)
                .orElse(""))
                .withHoverShowItem(itemStack)
                .build();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ItemStackSignature that = (ItemStackSignature) o;
        return InventoryUtil.areSimilar(itemStack, that.itemStack);
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

    @Override
    public String toString() {
        return itemStack.getType().name();
    }
}
