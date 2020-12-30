package io.github.wysohn.realeconomy.api.smartinv.gui;

import copy.com.google.gson.Gson;
import copy.com.google.gson.GsonBuilder;
import fr.minuskube.inv.ClickableItem;
import fr.minuskube.inv.ItemClickData;
import fr.minuskube.inv.content.InventoryContents;
import fr.minuskube.inv.content.InventoryProvider;
import fr.minuskube.inv.content.SlotPos;
import io.github.wysohn.rapidframework3.bukkit.utils.InventoryUtil;
import io.github.wysohn.rapidframework3.core.language.ManagerLanguage;
import io.github.wysohn.rapidframework3.core.serialize.BukkitConfigurationSerializer;
import io.github.wysohn.rapidframework3.interfaces.ICommandSender;
import io.github.wysohn.rapidframework3.interfaces.IMemento;
import io.github.wysohn.rapidframework3.interfaces.paging.DataProvider;
import io.github.wysohn.rapidframework3.utils.FailSensitiveTask;
import io.github.wysohn.realeconomy.interfaces.IFinancialEntity;
import io.github.wysohn.realeconomy.interfaces.banking.IAssetHolder;
import io.github.wysohn.realeconomy.main.RealEconomyLangs;
import io.github.wysohn.realeconomy.manager.CustomTypeAdapters;
import io.github.wysohn.realeconomy.manager.asset.Asset;
import io.github.wysohn.realeconomy.manager.asset.PhysicalAsset;
import io.github.wysohn.realeconomy.manager.asset.signature.AssetSignature;
import io.github.wysohn.realeconomy.manager.asset.signature.ItemStackSignature;
import io.github.wysohn.realeconomy.mediator.TradeMediator;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.lang.reflect.Modifier;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * The GUI class that helps user to transfer asset from/to the asset store.
 */
public class AssetTransferGUI implements InventoryProvider {
    private static final int ITEMS_PER_PAGE = 45;

    private final ManagerLanguage lang;
    private final TradeMediator tradeMediator;
    private final NamespacedKey keySerialized;
    private final Function<Player, ICommandSender> commandSenderFn;
    private final IAssetHolder assetStore;
    private final IFinancialEntity targetToSendAsset;
    private final Predicate<Player> playerPredicate;

    private DataProvider<Asset> dataProvider;
    private int page = 0;
    public static final int PAGE_MAX = 128;

    /**
     * Regardless of who is interacting with the GUI, the transaction will always respect the
     * provided 'assetStore' and 'targetToSendAsset' instances. Usually, the 'assetStore' is the storage
     * owned by the user, and 'targetToSendAsset' is the user itself.
     * <p>
     * In this sense, user is perceiving the 'assetStore' as the inventory, and the 'targetToSendAsset'
     * is the inventory of user itself. You may imagine that the 'assetStore' is like a chest, and
     * 'targetToSendAsset' is the inventory of the player who is using it.
     * <p>
     * The reason why this class is allowing such behavior is that in this way,
     * this GUI can be re-used to manage the storage that is not belonging to the user. For example,
     * simply by replacing the 'assetStore' to the CentralBank, it can be reused to edit the contents
     * of the bank, not the TRADING account of the user.
     *
     * @param lang
     * @param tradeMediator
     * @param keySerialized
     * @param commandSenderFn
     * @param assetStore        the storage to take assets from
     * @param targetToSendAsset target financial entity to send asset for
     * @param playerPredicate   use this if you want to close the GUI without the user exclusively
     *                          closing it by pressing esc key. For example, the user may no longer
     *                          allowed to interact with the content of the 'assetStore' while
     *                          the GUI is open.
     */
    public AssetTransferGUI(ManagerLanguage lang,
                            TradeMediator tradeMediator,
                            NamespacedKey keySerialized,
                            Function<Player, ICommandSender> commandSenderFn,
                            IAssetHolder assetStore,
                            IFinancialEntity targetToSendAsset,
                            Predicate<Player> playerPredicate) {
        this.lang = lang;
        this.tradeMediator = tradeMediator;
        this.keySerialized = keySerialized;
        this.commandSenderFn = commandSenderFn;
        this.assetStore = assetStore;
        this.targetToSendAsset = targetToSendAsset;
        this.playerPredicate = playerPredicate;
    }

    @Override
    public void init(Player player, InventoryContents inventoryContents) {
        dataProvider = assetStore.assetDataProvider();
    }

    @Override
    public void update(Player player, InventoryContents inventoryContents) {
        if (!playerPredicate.test(player)) {
            player.closeInventory();
            return;
        }

        if (dataProvider == null)
            return;

        updateContents(commandSenderFn.apply(player), inventoryContents);

        inventoryContents.set(SlotPos.of(5, 3), ClickableItem.from(pageButton(lang,
                commandSenderFn.apply(player),
                true),
                data -> page = Math.max(0, Math.min(PAGE_MAX, page - 1))));
        inventoryContents.set(SlotPos.of(5, 4), ClickableItem.from(homeButton(lang,
                commandSenderFn.apply(player),
                page + 1),
                data -> page = 0));
        inventoryContents.set(SlotPos.of(5, 5), ClickableItem.from(pageButton(lang,
                commandSenderFn.apply(player),
                false),
                data -> page = Math.max(0, Math.min(PAGE_MAX, page + 1))));
    }

    private void updateContents(ICommandSender sender, InventoryContents inventoryContents) {
        int size = dataProvider.size();
        List<Asset> assets;
        if (page * ITEMS_PER_PAGE < size) {
            assets = dataProvider.get(page, ITEMS_PER_PAGE);
        } else {
            assets = Collections.emptyList();
        }

        for (int i = 0; i < ITEMS_PER_PAGE; i++) {
            inventoryContents.setEditable(SlotPos.of(i / 9, i % 9), true);

            if (i < assets.size()) {
                inventoryContents.set(i, ClickableItem.from(assetToItem(keySerialized,
                        lang,
                        sender,
                        assets.get(i)), this::clickedSlot));
            } else {
                inventoryContents.set(i, ClickableItem.from(new ItemStack(Material.AIR), this::clickedSlot));
            }
        }
    }

    private void clickedSlot(ItemClickData data) {
        if (!(data.getEvent() instanceof InventoryClickEvent))
            throw new RuntimeException("Not a click event. How?");

        InventoryClickEvent event = (InventoryClickEvent) data.getEvent();
        ItemStack slot = event.getCurrentItem();
        ItemStack cursor = event.getCursor();

        if (cursor != null
                && tradeMediator.isDeniedType(cursor.getType())
                && event.getAction() == InventoryAction.PLACE_ALL) {
            //trying to place illegal item into GUI. Stop right away.
            event.setCancelled(true);
            return;
        }

        // cancel other than simple pickup/place event for easy implementation
        if (event.getAction() != InventoryAction.PLACE_ALL
                && event.getAction() != InventoryAction.PICKUP_ALL) {
            event.setCancelled(true);
            return;
        }

        IMemento mementoFrom = assetStore.saveState();
        IMemento mementoTo = targetToSendAsset.saveState();
        FailSensitiveTask.of(() -> {
            if (cursor == null || cursor.getType() == Material.AIR) {
                // empty slot and empty item in hand; nothing will happen
                if (slot == null || slot.getType() == Material.AIR)
                    return true;

                // inventory slot (asset) -> cursor
                Asset asset = itemToAsset(keySerialized, slot);
                if (!transferAsset(assetStore, targetToSendAsset, asset))
                    return false;

                // clear the slot
                event.setCurrentItem(null);
            } else {
                // cursor -> asset store
                ItemStackSignature signature = new ItemStackSignature(cursor);
                Asset asset = signature.create(new HashMap<String, Object>() {{
                    put(AssetSignature.KEY_NUMERIC_MEASURE, cursor.getAmount());
                }});
                assetStore.addAsset(asset);
            }

            // since the data is modified, reset the data provider now to see the change
            // and avoid possible duplications
            dataProvider = assetStore.assetDataProvider();
            return true;
        }).handleException(Throwable::printStackTrace).onFail(() -> {
            // if something ever goes wrong, cancel the click event to minimize the impact.
            event.setCancelled(true);

            // also restore state
            targetToSendAsset.restoreState(mementoTo);
            assetStore.restoreState(mementoFrom);
        }).run();
    }

    private boolean transferAsset(IAssetHolder from, IFinancialEntity to, Asset asset) {
        // remove asset from sender
        int assetAmount = assetAmount(asset);
        if (from.removeAsset(asset.getSignature(), assetAmount).size() > 0)
            return false;

        // give it to receiver
        to.realizeAsset(asset);

        return true;
    }

    static ItemStack pageButton(ManagerLanguage lang,
                                ICommandSender sender,
                                boolean left) {
        ItemStack itemStack = new ItemStack(Material.ARROW);
        InventoryUtil.parseFirstToItemTitle(lang,
                sender,
                left ? RealEconomyLangs.GUI_PreviousPage : RealEconomyLangs.GUI_NextPage,
                itemStack);
        return itemStack;
    }

    static ItemStack homeButton(ManagerLanguage lang,
                                ICommandSender sender,
                                int displayPage){
        if(displayPage < 1 || displayPage > 64)
            displayPage = 1;

        ItemStack itemStack = new ItemStack(Material.NETHER_STAR, displayPage);
        InventoryUtil.parseFirstToItemTitle(lang,
                sender,
                RealEconomyLangs.GUI_Home_Title,
                itemStack);
        int finalDisplayPage1 = displayPage;
        InventoryUtil.parseToItemLores(lang,
                sender,
                RealEconomyLangs.GUI_Home_Lore,
                (s, m) -> m.addInteger(finalDisplayPage1),
                itemStack);
        return itemStack;
    }

    static ItemStack assetToItem(NamespacedKey serKey,
                                 ManagerLanguage lang,
                                 ICommandSender sender,
                                 Asset asset) {
        String serialized = GSON.toJson(asset, Asset.class);

        ItemStack itemStack = asset.getIcon();
        ItemMeta meta = Objects.requireNonNull(itemStack.getItemMeta());
        meta.setLore(asset.lore().stream()
                .map(dl -> lang.parseFirst(sender, dl.lang, dl.parser))
                .map(raw -> ChatColor.translateAlternateColorCodes('&', raw))
                .collect(Collectors.toList()));
        PersistentDataContainer persistent = meta.getPersistentDataContainer();
        persistent.set(serKey, PersistentDataType.STRING, serialized);
        itemStack.setItemMeta(meta);

        return itemStack;
    }

    static Asset itemToAsset(NamespacedKey serKey, ItemStack itemStack) {
        ItemMeta meta = Objects.requireNonNull(itemStack.getItemMeta());
        PersistentDataContainer persistent = meta.getPersistentDataContainer();
        String serialized = persistent.get(serKey, PersistentDataType.STRING);
        return GSON.fromJson(serialized, Asset.class);
    }

    static int assetAmount(Asset asset) {
        if (asset instanceof PhysicalAsset) {
            return ((PhysicalAsset) asset).getAmount();
        } else {
            return 1;
        }
    }

    private static final Gson GSON = new GsonBuilder()
            .excludeFieldsWithModifiers(Modifier.STATIC | Modifier.TRANSIENT)
            .registerTypeHierarchyAdapter(ConfigurationSerializable.class, new BukkitConfigurationSerializer())
            .registerTypeAdapter(CustomTypeAdapters.ASSET.key, CustomTypeAdapters.ASSET.value)
            .registerTypeAdapter(CustomTypeAdapters.ASSET_SIGNATURE.key, CustomTypeAdapters.ASSET_SIGNATURE.value)
            .create();
}
