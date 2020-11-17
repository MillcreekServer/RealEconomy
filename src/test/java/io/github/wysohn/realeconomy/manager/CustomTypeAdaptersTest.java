package io.github.wysohn.realeconomy.manager;

import copy.com.google.gson.Gson;
import copy.com.google.gson.GsonBuilder;
import io.github.wysohn.realeconomy.interfaces.banking.IAccount;
import io.github.wysohn.realeconomy.manager.asset.Asset;
import io.github.wysohn.realeconomy.manager.asset.Item;
import io.github.wysohn.realeconomy.manager.asset.signature.ItemStackSignature;
import io.github.wysohn.realeconomy.manager.banking.BankingTypeRegistry;
import io.github.wysohn.realeconomy.manager.banking.account.TradingAccount;
import io.github.wysohn.realeconomy.manager.user.User;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Server;
import org.bukkit.inventory.ItemFactory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.junit.Test;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class CustomTypeAdaptersTest {
    @Test
    public void testAccountSerialization() throws Exception {
        Gson gson = new GsonBuilder()
                .registerTypeAdapter(CustomTypeAdapters.ASSET_SIGNATURE.key, CustomTypeAdapters.ASSET_SIGNATURE.value)
                .registerTypeAdapter(CustomTypeAdapters.ASSET.key, CustomTypeAdapters.ASSET.value)
                .registerTypeAdapter(CustomTypeAdapters.ACCOUNT.key, CustomTypeAdapters.ACCOUNT.value)
                .registerTypeAdapter(CustomTypeAdapters.BANKING_TYPE.key, CustomTypeAdapters.BANKING_TYPE.value)
                .create();

        TradingAccount account = new TradingAccount();
        Item asset = new Item(UUID.randomUUID(), new ItemStackSignature(new ItemStack(Material.DIAMOND)));
        asset.setAmount(1423);
        account.addAsset(asset);
        UUID currencyUuid = UUID.randomUUID();
        account.balances.put(currencyUuid, BigDecimal.valueOf(3020504.55));

        String serialized = gson.toJson(account, IAccount.class);
        IAccount restored = gson.fromJson(serialized, IAccount.class);

        assertEquals(BankingTypeRegistry.TRADING, restored.getType());
        assertEquals(BigDecimal.valueOf(3020504.55), restored.getCurrencyMap().get(currencyUuid));
        assertEquals(1, account.assetDataProvider().size());
        assertEquals(asset, account.assetDataProvider().get(0, 1).get(0));
    }

    @Test
    public void testAssetSerialization() throws Exception {
        Gson gson = new GsonBuilder()
                .registerTypeAdapter(CustomTypeAdapters.ASSET_SIGNATURE.key, CustomTypeAdapters.ASSET_SIGNATURE.value)
                .registerTypeAdapter(CustomTypeAdapters.ASSET.key, CustomTypeAdapters.ASSET.value)
                .create();

        Server server = mock(Server.class);
        Field field = Bukkit.class.getDeclaredField("server");
        field.setAccessible(true);
        field.set(null, server);

        ItemMeta meta = mock(ItemMeta.class);
        ItemFactory itemFactory = mock(ItemFactory.class);

        when(server.getItemFactory()).thenReturn(itemFactory);
        when(itemFactory.getItemMeta(any())).thenReturn(meta);

        User user = mock(User.class);
        UUID uuid = UUID.randomUUID();

        when(user.getUuid()).thenReturn(uuid);

        Item asset = new Item(UUID.randomUUID(), new ItemStackSignature(new ItemStack(Material.DIAMOND)));
        asset.setAmount(24634);

        String serialized = gson.toJson(asset, Asset.class);
        Asset restored = gson.fromJson(serialized, Asset.class);

        assertTrue(restored instanceof Item);
        assertEquals(24634, ((Item) restored).getAmount());
    }
}