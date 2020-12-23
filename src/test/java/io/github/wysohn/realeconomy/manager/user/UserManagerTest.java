package io.github.wysohn.realeconomy.manager.user;

import com.google.inject.Module;
import com.google.inject.*;
import io.github.wysohn.rapidframework3.core.inject.module.PluginInfoModule;
import io.github.wysohn.rapidframework3.core.inject.module.TaskSupervisorModule;
import io.github.wysohn.rapidframework3.core.inject.module.TypeAsserterModule;
import io.github.wysohn.rapidframework3.core.language.ManagerLanguage;
import io.github.wysohn.rapidframework3.interfaces.io.IPluginResourceProvider;
import io.github.wysohn.rapidframework3.interfaces.plugin.ITaskSupervisor;
import io.github.wysohn.rapidframework3.interfaces.serialize.ISerializer;
import io.github.wysohn.rapidframework3.testmodules.*;
import io.github.wysohn.rapidframework3.utils.Pair;
import io.github.wysohn.realeconomy.inject.annotation.MaxCapital;
import io.github.wysohn.realeconomy.inject.annotation.MinCapital;
import io.github.wysohn.realeconomy.inject.annotation.NamespaceKeyCheckBalance;
import io.github.wysohn.realeconomy.inject.annotation.NamespaceKeyCheckCurrency;
import io.github.wysohn.realeconomy.inject.module.NamespacedKeyModule;
import io.github.wysohn.realeconomy.inject.module.OrderPlacementHandlerModule;
import io.github.wysohn.realeconomy.inject.module.OrderSQLModule;
import io.github.wysohn.realeconomy.interfaces.currency.ICurrencyOwnerProvider;
import io.github.wysohn.realeconomy.main.RealEconomy;
import io.github.wysohn.realeconomy.manager.asset.listing.AssetListingManager;
import io.github.wysohn.realeconomy.manager.banking.CentralBankingManager;
import io.github.wysohn.realeconomy.manager.banking.bank.CentralBank;
import io.github.wysohn.realeconomy.manager.currency.Currency;
import io.github.wysohn.realeconomy.manager.currency.CurrencyManager;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Server;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.lang.ref.Reference;
import java.math.BigDecimal;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

public class UserManagerTest {
    @Inject
    @NamespaceKeyCheckCurrency
    private NamespacedKey keyCurrency;
    @Inject
    @NamespaceKeyCheckBalance
    private NamespacedKey keyBalance;

    private final List<Module> moduleList = new LinkedList<>();

    private ISerializer mockSerializer;
    private RealEconomy mockMain;
    private ITaskSupervisor taskSupervisor;
    private CentralBankingManager centralBankingManager;
    private ICurrencyOwnerProvider currencyOwnerProvider;
    private ManagerLanguage lang;
    private AssetListingManager listingManager;

    @Before
    public void init() {
        Logger logger = mock(Logger.class);
        Server server = mock(Server.class);
        when(server.getLogger()).thenReturn(logger);

        mockMain = new RealEconomy(server);
        mockSerializer = mock(ISerializer.class);
        taskSupervisor = mock(ITaskSupervisor.class);
        centralBankingManager = mock(CentralBankingManager.class);
        currencyOwnerProvider = mock(ICurrencyOwnerProvider.class);
        lang = mock(ManagerLanguage.class);
        listingManager = mock(AssetListingManager.class);

        moduleList.add(new PluginInfoModule("test", "test", "test"));
        moduleList.add(new TypeAsserterModule());
        moduleList.add(new MockLoggerModule());
        moduleList.add(new MockConfigModule(Pair.of(CurrencyManager.KEY_MAX_LEN, 3),
                Pair.of(UserManager.DROP_CURRENCY_ON_DEATH, true),
                Pair.of(UserManager.CHECK_PICKUP_BY_PLAYER_ONLY, false)));
        moduleList.add(new MockPluginDirectoryModule());
        moduleList.add(new MockSerializerModule(mockSerializer));
        moduleList.add(new MockShutdownModule(() -> {

        }));
        moduleList.add(new TaskSupervisorModule(taskSupervisor));

        moduleList.add(new NamespacedKeyModule());
        moduleList.add(new OrderSQLModule());
        moduleList.add(new OrderPlacementHandlerModule());
        moduleList.add(new AbstractModule() {
            @Provides
            RealEconomy realEconomy() {
                return mockMain;
            }

            @Provides
            @MaxCapital
            BigDecimal max() {
                return BigDecimal.valueOf(Double.MAX_VALUE);
            }

            @Provides
            @MinCapital
            BigDecimal min() {
                return BigDecimal.valueOf(-Double.MAX_VALUE);
            }

            @Provides
            CentralBankingManager centralBankingManager() {
                return centralBankingManager;
            }

            @Provides
            ICurrencyOwnerProvider currencyOwnerProvider() {
                return currencyOwnerProvider;
            }

            @Provides
            ManagerLanguage language() {
                return lang;
            }

            @Provides
            AssetListingManager listingManager() {
                return listingManager;
            }

            @Provides
            IPluginResourceProvider resourceProvider() {
                File folder = new File("src/main/resources/");
                return name -> {
                    try {
                        return new FileInputStream(new File(folder, name));
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    }
                    return null;
                };
            }
        });
    }

    @Test
    public void onDeath() {
        UserManager manager = Guice.createInjector(moduleList).getInstance(UserManager.class);

        PlayerDeathEvent event = mock(PlayerDeathEvent.class);
        Player entity = mock(Player.class);
        UUID uuid = UUID.randomUUID();
        PlayerInventory inventory = mock(PlayerInventory.class);
        ItemStack[] currentItems = new ItemStack[4 * 9 + 6]; // size may differ but not necessary when testing
        currentItems[0] = new ItemStack(Material.DIAMOND);
        currentItems[33] = new ItemStack(Material.SKELETON_SKULL);
        currentItems[21] = new ItemStack(Material.APPLE, 23);

        manager.getOrNew(uuid).map(Reference::get).ifPresent(user -> user.setSender(entity));
        when(event.getEntity()).thenReturn(entity);
        when(entity.getUniqueId()).thenReturn(uuid);
        when(entity.getInventory()).thenReturn(inventory);
        when(inventory.getContents()).thenReturn(currentItems);

        manager.onDeath(event);

        // It's rather too difficult to simulate all the Bukkit methods, so at this point, do the test
        // (itemizeCurrencies) in the game.
    }

    @Test
    public void onPickupCheck() {
        UserManager manager = Guice.createInjector(moduleList).getInstance(UserManager.class);

        EntityPickupItemEvent event = mock(EntityPickupItemEvent.class);

        // zombie picked up the item
        when(event.getEntityType()).thenReturn(EntityType.ZOMBIE);

        manager.onPickupCheck(event);

        verify(event, never()).setCancelled(anyBoolean());
    }

    @Test
    public void onPickupCheck2() {
        Injector injector = Guice.createInjector(moduleList);
        UserManager manager = injector.getInstance(UserManager.class);
        CurrencyManager currencyManager = injector.getInstance(CurrencyManager.class);
        injector.injectMembers(this);

        EntityPickupItemEvent event = mock(EntityPickupItemEvent.class);
        Player entity = mock(Player.class);
        UUID uuid = UUID.randomUUID();
        Item item = mock(Item.class);
        ItemStack itemStack = mock(ItemStack.class);
        ItemMeta meta = mock(ItemMeta.class);
        PersistentDataContainer container = mock(PersistentDataContainer.class);
        CentralBank centralBank = mock(CentralBank.class);
        currencyManager.newCurrency("dollar", "ABC", centralBank);
        Currency currency = currencyManager.get("dollar").map(Reference::get).orElseThrow(RuntimeException::new);
        UUID currencyUuid = currency.getKey();

        manager.getOrNew(uuid).map(Reference::get).ifPresent(user -> user.setSender(entity));
        when(event.getEntity()).thenReturn(entity);
        when(entity.getUniqueId()).thenReturn(uuid);
        // player picked up the item
        when(event.getEntityType()).thenReturn(EntityType.PLAYER);
        when(event.getItem()).thenReturn(item);
        when(item.getItemStack()).thenReturn(itemStack);
        when(itemStack.getItemMeta()).thenReturn(meta);
        when(meta.getPersistentDataContainer()).thenReturn(container);
        when(container.has(eq(keyCurrency), eq(PersistentDataType.STRING)))
                .thenReturn(true);
        when(container.has(eq(keyBalance), eq(PersistentDataType.STRING)))
                .thenReturn(true);
        when(container.get(eq(keyCurrency), eq(PersistentDataType.STRING)))
                .thenReturn(currencyUuid.toString());
        when(container.get(eq(keyBalance), eq(PersistentDataType.STRING)))
                .thenReturn(BigDecimal.valueOf(36753.45).toString());

        User user = manager.get(uuid).map(Reference::get).orElseThrow(RuntimeException::new);
        assertEquals(BigDecimal.ZERO, user.balance(currency));
        manager.onPickupCheck(event);
        verify(event).setCancelled(eq(true));
        verify(item).remove();
        assertEquals(BigDecimal.valueOf(36753.45), user.balance(currency));
    }
}