package io.github.wysohn.realeconomy.api.smartinv.gui;

import fr.minuskube.inv.content.InventoryContents;
import io.github.wysohn.rapidframework3.core.language.ManagerLanguage;
import io.github.wysohn.rapidframework3.interfaces.paging.DataProvider;
import io.github.wysohn.realeconomy.manager.asset.Asset;
import io.github.wysohn.realeconomy.manager.asset.Item;
import io.github.wysohn.realeconomy.manager.asset.signature.ItemStackSignature;
import io.github.wysohn.realeconomy.manager.banking.bank.AbstractBank;
import io.github.wysohn.realeconomy.manager.user.User;
import io.github.wysohn.realeconomy.mediator.BankingMediator;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Server;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFactory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataAdapterContext;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.junit.Before;
import org.junit.Test;
import org.mockito.internal.util.reflection.Whitebox;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;

import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;

public class TradeAccountGUITest {

    private TradeAccountGUI tradeAccountGUI;
    private Function<Player, User> function;
    private NamespacedKey namespacedKey;
    private BankingMediator bankingMediator;
    private ManagerLanguage language;

    @Before
    public void init() {
        language = mock(ManagerLanguage.class);
        bankingMediator = mock(BankingMediator.class);
        namespacedKey = new NamespacedKey("test", "ser");
        function = mock(Function.class);

        tradeAccountGUI = new TradeAccountGUI(language, bankingMediator, namespacedKey, function);
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
        Server server = mock(Server.class);
        Field field = Bukkit.class.getDeclaredField("server");
        field.setAccessible(true);
        field.set(null, server);

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
        Server server = mock(Server.class);
        Field field = Bukkit.class.getDeclaredField("server");
        field.setAccessible(true);
        field.set(null, server);

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
        when(bank.accountAssetProvider(eq(user))).thenReturn(dataProvider);
        when(dataProvider.size()).thenReturn(46);
        when(dataProvider.get(anyInt(), anyInt())).thenReturn(assetList);
        when(server.getItemFactory()).thenReturn(itemFactory);
        when(itemFactory.getItemMeta(any())).thenReturn(meta);
        when(meta.getPersistentDataContainer()).thenReturn(persistentDataContainer);

        tradeAccountGUI.init(player, contents);
        tradeAccountGUI.update(player, contents);

        for (int i = 0; i < 45; i++)
            verify(dataProvider).get(i, 45);
        verify(dataProvider, never()).get(45, 45);
    }

    @Test
    public void testUpdateTwoPagesNext() throws Exception {
        Server server = mock(Server.class);
        Field field = Bukkit.class.getDeclaredField("server");
        field.setAccessible(true);
        field.set(null, server);

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
        when(bank.accountAssetProvider(eq(user))).thenReturn(dataProvider);
        when(dataProvider.size()).thenReturn(46);
        when(dataProvider.get(anyInt(), anyInt())).thenReturn(assetList);
        when(server.getItemFactory()).thenReturn(itemFactory);
        when(itemFactory.getItemMeta(any())).thenReturn(meta);
        when(meta.getPersistentDataContainer()).thenReturn(persistentDataContainer);

        Whitebox.setInternalState(tradeAccountGUI, "page", 1);
        tradeAccountGUI.init(player, contents);
        tradeAccountGUI.update(player, contents);

        verify(dataProvider).get(0, 45);
        for (int i = 1; i < 45; i++)
            verify(dataProvider, never()).get(i, 45);
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
            return null;
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
}