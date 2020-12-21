package io.github.wysohn.realeconomy.api.smartinv.gui;

import com.google.common.collect.Multimap;
import fr.minuskube.inv.ItemClickData;
import fr.minuskube.inv.content.InventoryContents;
import io.github.wysohn.rapidframework3.core.language.ManagerLanguage;
import io.github.wysohn.rapidframework3.interfaces.paging.DataProvider;
import io.github.wysohn.realeconomy.interfaces.banking.IBankingType;
import io.github.wysohn.realeconomy.manager.asset.Asset;
import io.github.wysohn.realeconomy.manager.asset.Item;
import io.github.wysohn.realeconomy.manager.asset.signature.ItemStackSignature;
import io.github.wysohn.realeconomy.manager.banking.bank.AbstractBank;
import io.github.wysohn.realeconomy.manager.user.User;
import io.github.wysohn.realeconomy.mediator.BankingMediator;
import io.github.wysohn.realeconomy.mediator.TradeMediator;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Server;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.configuration.serialization.ConfigurationSerialization;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemFactory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.tags.CustomItemTagContainer;
import org.bukkit.persistence.PersistentDataAdapterContext;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.junit.Before;
import org.junit.Test;
import org.mockito.internal.util.reflection.Whitebox;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;
import java.util.function.Function;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;

public class TradeAccountGUITest {

    private TradeAccountGUI tradeAccountGUI;
    private Function<Player, User> function;
    private NamespacedKey namespacedKey;
    private BankingMediator bankingMediator;
    private TradeMediator tradeMediator;
    private ManagerLanguage language;
    private Server server;

    @Before
    public void init() throws Exception {
        language = mock(ManagerLanguage.class);
        bankingMediator = mock(BankingMediator.class);
        tradeMediator = mock(TradeMediator.class);
        namespacedKey = new NamespacedKey("test", "ser");
        function = mock(Function.class);
        server = mock(Server.class, RETURNS_DEEP_STUBS);
        Field field = Bukkit.class.getDeclaredField("server");
        field.setAccessible(true);
        field.set(null, server);

        tradeAccountGUI = new TradeAccountGUI(language, bankingMediator, tradeMediator, namespacedKey, function);

        when(language.parse(any(User.class), any())).thenReturn(new String[]{"Message"});
        when(language.parse(any(User.class), any(), any())).thenReturn(new String[]{"Message"});
        when(language.parseFirst(any(User.class), any())).thenReturn("Message");
        when(language.parseFirst(any(User.class), any(), any())).thenReturn("Message");
        when(server.getUnsafe().getDataVersion()).thenReturn(1);
    }

    @Test
    public void testUpdate() {
        Player player = mock(Player.class);
        InventoryContents contents = mock(InventoryContents.class);
        User user = mock(User.class);
        UUID uuid = UUID.randomUUID();
        AbstractBank bank = mock(AbstractBank.class);
        DataProvider<Asset> dataProvider = mock(DataProvider.class);

        when(player.getUniqueId()).thenReturn(uuid);
        when(user.getUuid()).thenReturn(uuid);
        when(function.apply(eq(player))).thenReturn(user);
        when(bankingMediator.getUsingBank(eq(user))).thenReturn(bank);
        when(bank.accountAssetProvider(eq(user))).thenReturn(dataProvider);

        tradeAccountGUI.init(player, contents);
        tradeAccountGUI.update(player, contents);
    }

    @Test
    public void testUpdateSinglePage() throws Exception {
        List<Asset> assetList = new ArrayList<>();
        assetList.add(new Item(UUID.randomUUID(), new ItemStackSignature(new ItemStack(Material.DIAMOND))));
        ItemMeta meta = mock(ItemMeta.class);

        Player player = mock(Player.class);
        InventoryContents contents = mock(InventoryContents.class);
        User user = mock(User.class);
        UUID uuid = UUID.randomUUID();
        AbstractBank bank = mock(AbstractBank.class);
        DataProvider<Asset> dataProvider = mock(DataProvider.class);
        ItemFactory itemFactory = mock(ItemFactory.class);
        PersistentDataContainer persistentDataContainer = new TempContainer();

        when(player.getUniqueId()).thenReturn(uuid);
        when(user.getUuid()).thenReturn(uuid);
        when(function.apply(eq(player))).thenReturn(user);
        when(bankingMediator.getUsingBank(eq(user))).thenReturn(bank);
        when(bank.hasAccount(eq(user), any(IBankingType.class))).thenReturn(true);
        when(bank.accountAssetProvider(eq(user))).thenReturn(dataProvider);
        when(dataProvider.size()).thenReturn(45);
        when(dataProvider.get(anyInt(), anyInt())).thenReturn(assetList);
        when(server.getItemFactory()).thenReturn(itemFactory);
        when(itemFactory.getItemMeta(any())).thenReturn(meta);
        when(meta.getPersistentDataContainer()).thenReturn(persistentDataContainer);

        tradeAccountGUI.init(player, contents);
        tradeAccountGUI.update(player, contents);

        verify(dataProvider).get(0, 45);
    }

    @Test
    public void testUpdateTwoPages() throws Exception {
        List<Asset> assetList = new ArrayList<>();
        for (int i = 0; i < 46; i++)
            assetList.add(new Item(UUID.randomUUID(), new ItemStackSignature(new ItemStack(Material.DIAMOND))));
        ItemMeta meta = mock(ItemMeta.class);

        Player player = mock(Player.class);
        InventoryContents contents = mock(InventoryContents.class);
        User user = mock(User.class);
        UUID uuid = UUID.randomUUID();
        AbstractBank bank = mock(AbstractBank.class);
        DataProvider<Asset> dataProvider = mock(DataProvider.class);
        ItemFactory itemFactory = mock(ItemFactory.class);
        PersistentDataContainer persistentDataContainer = new TempContainer();

        when(player.getUniqueId()).thenReturn(uuid);
        when(user.getUuid()).thenReturn(uuid);
        when(function.apply(eq(player))).thenReturn(user);
        when(bankingMediator.getUsingBank(eq(user))).thenReturn(bank);
        when(bank.hasAccount(eq(user), any(IBankingType.class))).thenReturn(true);
        when(bank.accountAssetProvider(eq(user))).thenReturn(dataProvider);
        when(dataProvider.size()).thenReturn(46);
        when(dataProvider.get(anyInt(), anyInt())).thenReturn(assetList);
        when(server.getItemFactory()).thenReturn(itemFactory);
        when(itemFactory.getItemMeta(any())).thenReturn(meta);
        when(meta.getPersistentDataContainer()).thenReturn(persistentDataContainer);

        tradeAccountGUI.init(player, contents);
        tradeAccountGUI.update(player, contents);

        verify(dataProvider).get(0, 45);
        verify(dataProvider, never()).get(1, 45);
    }

    @Test
    public void testUpdateTwoPagesNext() throws Exception {
        List<Asset> assetList = new ArrayList<>();
        for (int i = 0; i < 46; i++)
            assetList.add(new Item(UUID.randomUUID(), new ItemStackSignature(new ItemStack(Material.DIAMOND))));
        ItemMeta meta = mock(ItemMeta.class);

        Player player = mock(Player.class);
        InventoryContents contents = mock(InventoryContents.class);
        User user = mock(User.class);
        UUID uuid = UUID.randomUUID();
        AbstractBank bank = mock(AbstractBank.class);
        DataProvider<Asset> dataProvider = mock(DataProvider.class);
        ItemFactory itemFactory = mock(ItemFactory.class);
        PersistentDataContainer persistentDataContainer = new TempContainer();

        when(player.getUniqueId()).thenReturn(uuid);
        when(user.getUuid()).thenReturn(uuid);
        when(function.apply(eq(player))).thenReturn(user);
        when(bankingMediator.getUsingBank(eq(user))).thenReturn(bank);
        when(bank.hasAccount(eq(user), any(IBankingType.class))).thenReturn(true);
        when(bank.accountAssetProvider(eq(user))).thenReturn(dataProvider);
        when(dataProvider.size()).thenReturn(46);
        when(dataProvider.get(anyInt(), anyInt())).thenReturn(assetList);
        when(server.getItemFactory()).thenReturn(itemFactory);
        when(itemFactory.getItemMeta(any())).thenReturn(meta);
        when(meta.getPersistentDataContainer()).thenReturn(persistentDataContainer);

        Whitebox.setInternalState(tradeAccountGUI, "page", 1);
        tradeAccountGUI.init(player, contents);
        tradeAccountGUI.update(player, contents);

        verify(dataProvider).get(1, 45);
    }

    @Test
    public void testClickAddItem() throws Exception {
        List<Asset> assetList = new ArrayList<>();
        Item asset = new Item(UUID.randomUUID(), new ItemStackSignature(new ItemStack(Material.DIAMOND)));
        assetList.add(asset);
        ItemMeta meta = mock(ItemMeta.class);

        Player player = mock(Player.class);
        InventoryContents contents = mock(InventoryContents.class);
        User user = mock(User.class);
        UUID uuid = UUID.randomUUID();
        AbstractBank bank = mock(AbstractBank.class);
        DataProvider<Asset> dataProvider = mock(DataProvider.class);
        ItemFactory itemFactory = mock(ItemFactory.class);
        PersistentDataContainer persistentDataContainer = new TempContainer();
        ItemClickData data = mock(ItemClickData.class);

        when(player.getUniqueId()).thenReturn(uuid);
        when(user.getUuid()).thenReturn(uuid);
        when(function.apply(eq(player))).thenReturn(user);
        when(bankingMediator.getUsingBank(eq(user))).thenReturn(bank);
        when(bank.accountAssetProvider(eq(user))).thenReturn(dataProvider);
        when(dataProvider.size()).thenReturn(46);
        when(dataProvider.get(anyInt(), anyInt())).thenReturn(assetList);
        when(server.getItemFactory()).thenReturn(itemFactory);
        when(itemFactory.getItemMeta(any())).thenReturn(meta);
        when(meta.getPersistentDataContainer()).thenReturn(persistentDataContainer);

        InventoryClickEvent clickEvent = mock(InventoryClickEvent.class);
        InventoryAction action = InventoryAction.PLACE_ALL;
        ItemStack cursor = TradeAccountGUI.assetToItem(namespacedKey, language, user, asset);
        when(data.getEvent()).thenReturn(clickEvent);
        when(clickEvent.getAction()).thenReturn(action);
        when(clickEvent.getCursor()).thenReturn(cursor);

        tradeAccountGUI.init(player, contents);
        tradeAccountGUI.update(player, contents);

        Method method = TradeAccountGUI.class.getDeclaredMethod("clickedSlot",
                Player.class, AbstractBank.class, ItemClickData.class);
        method.setAccessible(true);
        method.invoke(tradeAccountGUI, player, bank, data);
    }

    @Test
    public void testClickTakeItem() throws Exception {
        List<Asset> assetList = new ArrayList<>();
        Item asset = new Item(UUID.randomUUID(), new ItemStackSignature(new ItemStack(Material.DIAMOND)));
        assetList.add(asset);
        ItemMeta meta = mock(ItemMeta.class);

        Player player = mock(Player.class);
        InventoryContents contents = mock(InventoryContents.class);
        User user = mock(User.class);
        UUID uuid = UUID.randomUUID();
        AbstractBank bank = mock(AbstractBank.class);
        DataProvider<Asset> dataProvider = mock(DataProvider.class);
        ItemFactory itemFactory = mock(ItemFactory.class);
        PersistentDataContainer persistentDataContainer = new TempContainer();
        ItemClickData data = mock(ItemClickData.class);

        when(player.getUniqueId()).thenReturn(uuid);
        when(user.getUuid()).thenReturn(uuid);
        when(function.apply(eq(player))).thenReturn(user);
        when(bankingMediator.getUsingBank(eq(user))).thenReturn(bank);
        when(bank.accountAssetProvider(eq(user))).thenReturn(dataProvider);
        when(dataProvider.size()).thenReturn(46);
        when(dataProvider.get(anyInt(), anyInt())).thenReturn(assetList);
        when(server.getItemFactory()).thenReturn(itemFactory);
        when(itemFactory.getItemMeta(any())).thenReturn(meta);
        when(meta.getPersistentDataContainer()).thenReturn(persistentDataContainer);

        InventoryClickEvent clickEvent = mock(InventoryClickEvent.class);
        InventoryAction action = InventoryAction.PLACE_ALL;
        ItemStack slot = TradeAccountGUI.assetToItem(namespacedKey, language, user, asset);
        when(data.getEvent()).thenReturn(clickEvent);
        when(clickEvent.getAction()).thenReturn(action);
        when(clickEvent.getCurrentItem()).thenReturn(slot);

        tradeAccountGUI.init(player, contents);
        tradeAccountGUI.update(player, contents);

        Method method = TradeAccountGUI.class.getDeclaredMethod("clickedSlot",
                Player.class, AbstractBank.class, ItemClickData.class);
        method.setAccessible(true);
        method.invoke(tradeAccountGUI, player, bank, data);
    }

    @Test
    public void testSerialize() throws Exception {
        TestMeta meta = new TestMeta();
        ItemFactory itemFactory = mock(ItemFactory.class);
        meta.container = new TempContainer();

        when(server.getItemFactory()).thenReturn(itemFactory);
        when(server.getUnsafe().getMaterial(anyString(), anyInt())).then(invocation ->
                Material.valueOf((String) invocation.getArguments()[0]));
        when(itemFactory.getItemMeta(any())).thenReturn(meta);
        when(itemFactory.equals(any(), any())).thenReturn(true);

        User user = mock(User.class);
        UUID uuid = UUID.randomUUID();

        when(user.getUuid()).thenReturn(uuid);

        Item asset = new Item(UUID.randomUUID(), new ItemStackSignature(new ItemStack(Material.DIAMOND)));
        asset.setAmount(8853);

        ItemStack itemStack = TradeAccountGUI.assetToItem(namespacedKey, language, user, asset);
        Asset restored = TradeAccountGUI.itemToAsset(namespacedKey, itemStack);

        assertEquals(8853, TradeAccountGUI.assetAmount(restored));
        assertTrue(restored instanceof Item);
        assertEquals(new ItemStack(Material.DIAMOND),
                ((ItemStackSignature)restored.getSignature()).getItemStack());
    }

    private class TempContainer implements PersistentDataContainer {
        private String serialized;

        @Override
        public <T, Z> void set(NamespacedKey key, PersistentDataType<T, Z> type, Z value) {
            serialized = (String) value;
        }

        @Override
        public <T, Z> boolean has(NamespacedKey key, PersistentDataType<T, Z> type) {
            return false;
        }

        @Override
        public <T, Z> Z get(NamespacedKey key, PersistentDataType<T, Z> type) {
            return (Z) serialized;
        }

        @Override
        public <T, Z> Z getOrDefault(NamespacedKey key,
                                     PersistentDataType<T, Z> type,
                                     Z defaultValue) {
            return null;
        }

        @Override
        public Set<NamespacedKey> getKeys() {
            return null;
        }

        @Override
        public void remove(NamespacedKey key) {

        }

        @Override
        public boolean isEmpty() {
            return false;
        }

        @Override
        public PersistentDataAdapterContext getAdapterContext() {
            return null;
        }
    }

    static {
        ConfigurationSerialization.registerClass(TestMeta.class);
    }

    public static class TestMeta implements ItemMeta, Damageable {
        PersistentDataContainer container;

        @Override
        public boolean hasDisplayName() {
            return false;
        }


        @Override
        public String getDisplayName() {
            return null;
        }

        @Override
        public void setDisplayName(String name) {

        }

        @Override
        public boolean hasLocalizedName() {
            return false;
        }


        @Override
        public String getLocalizedName() {
            return null;
        }

        @Override
        public void setLocalizedName(String name) {

        }

        @Override
        public boolean hasLore() {
            return false;
        }

        @Override
        public List<String> getLore() {
            return null;
        }

        @Override
        public void setLore(List<String> lore) {

        }

        @Override
        public boolean hasCustomModelData() {
            return false;
        }

        @Override
        public int getCustomModelData() {
            return 0;
        }

        @Override
        public void setCustomModelData(Integer data) {

        }

        @Override
        public boolean hasEnchants() {
            return false;
        }

        @Override
        public boolean hasEnchant(Enchantment ench) {
            return false;
        }

        @Override
        public int getEnchantLevel(Enchantment ench) {
            return 0;
        }


        @Override
        public Map<Enchantment, Integer> getEnchants() {
            return null;
        }

        @Override
        public boolean addEnchant(Enchantment ench, int level, boolean ignoreLevelRestriction) {
            return false;
        }

        @Override
        public boolean removeEnchant(Enchantment ench) {
            return false;
        }

        @Override
        public boolean hasConflictingEnchant(Enchantment ench) {
            return false;
        }

        @Override
        public void addItemFlags(ItemFlag... itemFlags) {

        }

        @Override
        public void removeItemFlags(ItemFlag... itemFlags) {

        }


        @Override
        public Set<ItemFlag> getItemFlags() {
            return null;
        }

        @Override
        public boolean hasItemFlag(ItemFlag flag) {
            return false;
        }

        @Override
        public boolean isUnbreakable() {
            return false;
        }

        @Override
        public void setUnbreakable(boolean unbreakable) {

        }

        @Override
        public boolean hasAttributeModifiers() {
            return false;
        }

        @Override
        public Multimap<Attribute, AttributeModifier> getAttributeModifiers() {
            return null;
        }


        @Override
        public Multimap<Attribute, AttributeModifier> getAttributeModifiers(EquipmentSlot slot) {
            return null;
        }

        @Override
        public Collection<AttributeModifier> getAttributeModifiers(Attribute attribute) {
            return null;
        }

        @Override
        public boolean addAttributeModifier(Attribute attribute, AttributeModifier modifier) {
            return false;
        }

        @Override
        public void setAttributeModifiers(Multimap<Attribute, AttributeModifier> attributeModifiers) {

        }

        @Override
        public boolean removeAttributeModifier(Attribute attribute) {
            return false;
        }

        @Override
        public boolean removeAttributeModifier(EquipmentSlot slot) {
            return false;
        }

        @Override
        public boolean removeAttributeModifier(Attribute attribute, AttributeModifier modifier) {
            return false;
        }


        @Override
        public CustomItemTagContainer getCustomTagContainer() {
            return null;
        }

        @Override
        public void setVersion(int version) {

        }


        @Override
        public TestMeta clone() {
            TestMeta testMeta = new TestMeta();
            testMeta.container = container;
            return testMeta;
        }


        @Override
        public Map<String, Object> serialize() {
            return new HashMap<>();
        }

        public static TestMeta deserialize(Map<String, Object> map) {
            return new TestMeta();
        }


        @Override
        public PersistentDataContainer getPersistentDataContainer() {
            return container;
        }

        @Override
        public boolean hasDamage() {
            return false;
        }

        @Override
        public int getDamage() {
            return 0;
        }

        @Override
        public void setDamage(int damage) {

        }
    }
}