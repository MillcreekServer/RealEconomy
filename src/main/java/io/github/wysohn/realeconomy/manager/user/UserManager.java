package io.github.wysohn.realeconomy.manager.user;

import com.google.inject.Injector;
import io.github.wysohn.rapidframework3.bukkit.manager.user.AbstractUserManager;
import io.github.wysohn.rapidframework3.core.database.Databases;
import io.github.wysohn.rapidframework3.core.inject.annotations.PluginDirectory;
import io.github.wysohn.rapidframework3.core.inject.annotations.PluginLogger;
import io.github.wysohn.rapidframework3.core.main.ManagerConfig;
import io.github.wysohn.rapidframework3.interfaces.IMemento;
import io.github.wysohn.rapidframework3.interfaces.plugin.IShutdownHandle;
import io.github.wysohn.rapidframework3.interfaces.serialize.ISerializer;
import io.github.wysohn.rapidframework3.interfaces.serialize.ITypeAsserter;
import io.github.wysohn.rapidframework3.utils.FailSensitiveTask;
import io.github.wysohn.rapidframework3.utils.Pair;
import io.github.wysohn.realeconomy.inject.annotation.NamespaceKeyCheckBalance;
import io.github.wysohn.realeconomy.inject.annotation.NamespaceKeyCheckCurrency;
import io.github.wysohn.realeconomy.interfaces.banking.IBankUserProvider;
import io.github.wysohn.realeconomy.manager.currency.Currency;
import io.github.wysohn.realeconomy.manager.currency.CurrencyManager;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.io.File;
import java.lang.ref.Reference;
import java.math.BigDecimal;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.logging.Logger;

@Singleton
public class UserManager extends AbstractUserManager<User> {
    private final Logger logger;
    private final ManagerConfig config;
    private final CurrencyManager currencyManager;
    private final NamespacedKey checkCurrencyKey;
    private final NamespacedKey checkBalanceKey;

    private final IBankUserProvider bankUserProvider = (uuid) -> get(uuid)
            .map(Reference::get)
            .orElse(null);

    @Inject
    public UserManager(
            @Named("pluginName") String pluginName,
            @PluginLogger Logger logger,
            ManagerConfig config,
            @PluginDirectory File pluginDir,
            IShutdownHandle shutdownHandle,
            ISerializer serializer,
            ITypeAsserter asserter,
            Injector injector,
            CurrencyManager currencyManager,
            @NamespaceKeyCheckCurrency NamespacedKey checkCurrencyKey,
            @NamespaceKeyCheckBalance NamespacedKey checkBalanceKey) {
        super(pluginName, logger, config, pluginDir, shutdownHandle, serializer, asserter, injector, User.class);

        this.logger = logger;
        this.config = config;
        this.currencyManager = currencyManager;
        this.checkCurrencyKey = checkCurrencyKey;
        this.checkBalanceKey = checkBalanceKey;
    }

    @Override
    protected Databases.DatabaseFactory createDatabaseFactory() {
        return getDatabaseFactory("user");
    }

    @Override
    protected User newInstance(UUID uuid) {
        return new User(uuid);
    }

    @Override
    public void enable() throws Exception {
        super.enable();

        if (!config.get(DROP_CURRENCY_ON_DEATH).isPresent()) {
            config.put(DROP_CURRENCY_ON_DEATH, false);
        }
        if (!config.get(CHECK_PICKUP_BY_PLAYER_ONLY).isPresent()) {
            config.put(CHECK_PICKUP_BY_PLAYER_ONLY, false);
        }
    }

    public IBankUserProvider bankUserProvider(){
        return bankUserProvider;
    }

    /**
     * Convert all currencies into ItemStack with the special PersistentDataContainer.
     * Once the List is returned, the user have empty wallet.
     *
     * @param user user to itemize its wallet
     * @return all the converted items
     */
    private List<ItemStack> itemizeCurrencies(User user) {
        List<Pair<UUID, BigDecimal>> wallet = user.clearWallet();
        List<ItemStack> checks = new LinkedList<>();
        for (Pair<UUID, BigDecimal> pair : wallet) {
            ItemStack check = new ItemStack(Material.PAPER);
            ItemMeta meta = check.getItemMeta();
            PersistentDataContainer dataContainer = meta.getPersistentDataContainer();
            dataContainer.set(checkCurrencyKey, PersistentDataType.STRING, pair.key.toString());
            dataContainer.set(checkBalanceKey, PersistentDataType.STRING, pair.value.toString());
            check.setItemMeta(meta);
            checks.add(check);
        }
        return checks;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onDeath(PlayerDeathEvent event) {
        if (!config.get(DROP_CURRENCY_ON_DEATH)
                .filter(Boolean.class::isInstance)
                .map(Boolean.class::cast)
                .orElse(false))
            return;

        // if keep inventory, keep currencies too
        if (event.getKeepInventory())
            return;

        User user = get(event.getEntity().getUniqueId())
                .map(Reference::get)
                .orElse(null);
        if (user == null)
            return;

        IMemento savedState = user.saveState();
        FailSensitiveTask.of(() -> {
            List<ItemStack> checks = itemizeCurrencies(user);
            event.getDrops().addAll(checks);
            return true;
        }).handleException(Throwable::printStackTrace)
                .onFail(() -> user.restoreState(savedState))
                .run();
    }

    private boolean isCheckItem(ItemStack itemStack) {
        ItemMeta meta = itemStack.getItemMeta();
        PersistentDataContainer dataContainer = meta.getPersistentDataContainer();
        return dataContainer.has(checkBalanceKey, PersistentDataType.STRING)
                && dataContainer.has(checkCurrencyKey, PersistentDataType.STRING);
    }

    private Pair<UUID, BigDecimal> checkItemToCurrency(ItemStack itemStack) {
        ItemMeta meta = itemStack.getItemMeta();
        PersistentDataContainer dataContainer = meta.getPersistentDataContainer();
        String rawBalance = dataContainer.get(checkBalanceKey, PersistentDataType.STRING);
        String rawCurrencyUuid = dataContainer.get(checkCurrencyKey, PersistentDataType.STRING);

        return Pair.of(UUID.fromString(Objects.requireNonNull(rawCurrencyUuid)),
                new BigDecimal(Objects.requireNonNull(rawBalance)));
    }

    @EventHandler
    public void onPickupCheck(EntityPickupItemEvent event) {
        // non-player entity may pick it up (for fun?)
        if (!config.get(CHECK_PICKUP_BY_PLAYER_ONLY)
                .filter(Boolean.class::isInstance)
                .map(Boolean.class::cast)
                .orElse(false) && event.getEntityType() != EntityType.PLAYER) {
            return;
        }

        Item item = event.getItem();
        ItemStack itemStack = item.getItemStack();

        if (!isCheckItem(itemStack))
            return;

        // cancel early, so nobody can pick it up
        event.setCancelled(true);
        // when player only option is on, entity type must be Player in order to proceed.
        if (event.getEntityType() != EntityType.PLAYER)
            return;

        Player player = (Player) event.getEntity();
        User user = get(player.getUniqueId())
                .map(Reference::get)
                .orElse(null);
        if (user == null)
            return;

        Pair<UUID, BigDecimal> currencyPair = checkItemToCurrency(itemStack);
        Currency currency = currencyManager.get(currencyPair.key)
                .map(Reference::get)
                .orElse(null);

        // remove the item early
        item.remove();

        // it's a useless piece of paper if currency doesn't exist...
        if (currency == null) {
            logger.info(player + " has picked up a check, but Currency info was not found.");
            logger.info("currency uuid: " + currencyPair.key + ", balance: " + currencyPair.value);
            logger.info("Item is removed without the player get paid.");
            return;
        }

        // very unlikely, but it may fail to deposit.
        if (!user.deposit(currencyPair.value, currency)) {
            logger.info(player + "'s deposit was refused for some reason.");
            logger.info("currency uuid: " + currencyPair.key + ", balance: " + currencyPair.value);
            logger.info("Item is removed without the player get paid.");
            return;
        }
    }

    public static final String DROP_CURRENCY_ON_DEATH = "currencyDrop.onDeath";
    public static final String CHECK_PICKUP_BY_PLAYER_ONLY = "currencyDrop.pickupByPlayerOnly";
}
