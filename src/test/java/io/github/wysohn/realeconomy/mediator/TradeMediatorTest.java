package io.github.wysohn.realeconomy.mediator;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Module;
import com.google.inject.Provides;
import com.google.inject.multibindings.ProvidesIntoSet;
import io.github.wysohn.rapidframework3.bukkit.testutils.manager.AbstractBukkitManagerTest;
import io.github.wysohn.rapidframework3.core.main.ManagerConfig;
import io.github.wysohn.rapidframework3.interfaces.IMemento;
import io.github.wysohn.rapidframework3.testmodules.MockLoggerModule;
import io.github.wysohn.rapidframework3.testmodules.MockShutdownModule;
import io.github.wysohn.realeconomy.inject.annotation.MaxCapital;
import io.github.wysohn.realeconomy.inject.annotation.MinCapital;
import io.github.wysohn.realeconomy.interfaces.banking.IBankOwnerProvider;
import io.github.wysohn.realeconomy.interfaces.banking.IBankUser;
import io.github.wysohn.realeconomy.interfaces.banking.IBankUserProvider;
import io.github.wysohn.realeconomy.manager.asset.signature.ItemStackSignature;
import io.github.wysohn.realeconomy.manager.banking.BankingTypeRegistry;
import io.github.wysohn.realeconomy.manager.banking.bank.CentralBank;
import io.github.wysohn.realeconomy.manager.currency.Currency;
import io.github.wysohn.realeconomy.manager.currency.CurrencyManager;
import io.github.wysohn.realeconomy.manager.listing.AssetListing;
import io.github.wysohn.realeconomy.manager.listing.AssetListingManager;
import io.github.wysohn.realeconomy.manager.listing.TradeInfo;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.Before;
import org.junit.Test;

import java.lang.ref.WeakReference;
import java.math.BigDecimal;
import java.util.*;
import java.util.function.Consumer;
import java.util.logging.Logger;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

public class TradeMediatorTest extends AbstractBukkitManagerTest {

    List<Module> moduleList = new LinkedList<>();
    private ManagerConfig config;
    private Logger logger;
    private CurrencyManager currencyManager;

    @Before
    public void init() throws Exception {
        config = mock(ManagerConfig.class);
        logger = mock(Logger.class);
        currencyManager = mock(CurrencyManager.class);

        FileConfiguration materialsSec = new YamlConfiguration();
        materialsSec.set(Material.APPLE.name(), "food");
        materialsSec.set(Material.DIAMOND.name(), "ore");

        when(config.get(eq(TradeMediator.MATERIALS))).thenReturn(Optional.of(materialsSec));
        when(config.get(eq(materialsSec), anyString())).then(invocation -> {
            String key = (String) invocation.getArguments()[1];
            return Optional.ofNullable(materialsSec.get(key));
        });
        when(config.get(eq(TradeMediator.DENY_LIST))).thenReturn(Optional.empty());

        moduleList.add(new MockLoggerModule());
        moduleList.add(new AbstractModule() {
            @Provides
            ManagerConfig config() {
                return config;
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
            SimulationMediator simulationMediator() {
                return mock(SimulationMediator.class);
            }

            @Provides
            CurrencyManager currencyManager() {
                return currencyManager;
            }

            @ProvidesIntoSet
            IBankOwnerProvider bankOwnerProvider() {
                return mock(IBankOwnerProvider.class);
            }
        });
        moduleList.add(new MockShutdownModule(() -> {
        }));
    }

    @Test
    public void testBrokerBuyerNotExist() throws Exception {
        CurrencyManager currencyManager = mock(CurrencyManager.class);
        AssetListingManager assetListingManager = mock(AssetListingManager.class);
        IBankUserProvider bankUserProvider = mock(IBankUserProvider.class);

        TradeMediator.TradeBroker tradeBroker = new TradeMediator.TradeBroker(assetListingManager,
                new HashSet<IBankUserProvider>() {{
                    add(bankUserProvider);
                }},
                currencyManager,
                logger);

        doAnswer(invocation -> {
            Consumer<TradeInfo> consumer = (Consumer<TradeInfo>) invocation.getArguments()[0];

            consumer.accept(TradeInfo.create(35,
                    UUID.randomUUID(),
                    3000.55,
                    244,
                    232,
                    UUID.randomUUID(),
                    3099.34,
                    50,
                    UUID.randomUUID(),
                    UUID.randomUUID(),
                    1));

            return null;
        }).when(assetListingManager).peekMatchingOrder(any(Consumer.class));

        tradeBroker.processOrder();

        verify(assetListingManager, atLeast(1)).peekMatchingOrder(any(Consumer.class));
    }

    @Test
    public void testBrokerSellerNotExist() throws Exception {
        CurrencyManager currencyManager = mock(CurrencyManager.class);
        AssetListingManager assetListingManager = mock(AssetListingManager.class);
        IBankUserProvider bankUserProvider = mock(IBankUserProvider.class);

        TradeMediator.TradeBroker tradeBroker = new TradeMediator.TradeBroker(assetListingManager,
                new HashSet<IBankUserProvider>() {{
                    add(bankUserProvider);
                }},
                currencyManager,
                logger);

        UUID buyerUuid = UUID.randomUUID();
        UUID sellerUuid = UUID.randomUUID();
        doAnswer(invocation -> {
            Consumer<TradeInfo> consumer = (Consumer<TradeInfo>) invocation.getArguments()[0];

            consumer.accept(TradeInfo.create(35,
                    sellerUuid,
                    3000.55,
                    244,
                    232,
                    buyerUuid,
                    3099.34,
                    50,
                    UUID.randomUUID(),
                    UUID.randomUUID(),
                    1));

            return null;
        }).when(assetListingManager).peekMatchingOrder(any(Consumer.class));

        when(bankUserProvider.get(eq(buyerUuid))).thenReturn(mock(IBankUser.class));

        tradeBroker.processOrder();

        verify(assetListingManager, atLeast(1)).peekMatchingOrder(any(Consumer.class));
    }

    @Test
    public void testBrokerCurrencyNotExist() throws Exception {
        CurrencyManager currencyManager = mock(CurrencyManager.class);
        AssetListingManager assetListingManager = mock(AssetListingManager.class);
        IBankUserProvider bankUserProvider = mock(IBankUserProvider.class);

        TradeMediator.TradeBroker tradeBroker = new TradeMediator.TradeBroker(assetListingManager,
                new HashSet<IBankUserProvider>() {{
                    add(bankUserProvider);
                }},
                currencyManager,
                logger);

        UUID buyerUuid = UUID.randomUUID();
        UUID sellerUuid = UUID.randomUUID();
        UUID currencyUuid = UUID.randomUUID();
        doAnswer(invocation -> {
            Consumer<TradeInfo> consumer = (Consumer<TradeInfo>) invocation.getArguments()[0];

            consumer.accept(TradeInfo.create(35,
                    sellerUuid,
                    3000.55,
                    244,
                    232,
                    buyerUuid,
                    3099.34,
                    50,
                    currencyUuid,
                    UUID.randomUUID(),
                    1));

            return null;
        }).when(assetListingManager).peekMatchingOrder(any(Consumer.class));

        when(bankUserProvider.get(eq(buyerUuid))).thenReturn(mock(IBankUser.class));
        when(bankUserProvider.get(eq(sellerUuid))).thenReturn(mock(IBankUser.class));
        when(currencyManager.get(eq(currencyUuid))).thenReturn(Optional.empty());

        tradeBroker.processOrder();

        verify(assetListingManager, atLeast(1)).peekMatchingOrder(any(Consumer.class));
        verify(assetListingManager, times(2)).cancelOrder(anyInt(), any(), any());
        verify(assetListingManager).commitOrders();
    }

    @Test
    public void testBuyerAccountNotExist() throws Exception {
        CurrencyManager currencyManager = mock(CurrencyManager.class);
        AssetListingManager assetListingManager = mock(AssetListingManager.class);
        IBankUserProvider bankUserProvider = mock(IBankUserProvider.class);

        TradeMediator.TradeBroker tradeBroker = new TradeMediator.TradeBroker(assetListingManager,
                new HashSet<IBankUserProvider>() {{
                    add(bankUserProvider);
                }},
                currencyManager,
                logger);

        UUID buyerUuid = UUID.randomUUID();
        UUID sellerUuid = UUID.randomUUID();
        UUID currencyUuid = UUID.randomUUID();
        Currency currency = mock(Currency.class);
        doAnswer(invocation -> {
            Consumer<TradeInfo> consumer = (Consumer<TradeInfo>) invocation.getArguments()[0];

            consumer.accept(TradeInfo.create(35,
                    sellerUuid,
                    3000.55,
                    244,
                    232,
                    buyerUuid,
                    3099.34,
                    50,
                    currencyUuid,
                    UUID.randomUUID(),
                    1));

            return null;
        }).when(assetListingManager).peekMatchingOrder(any(Consumer.class));

        when(bankUserProvider.get(eq(buyerUuid))).thenReturn(mock(IBankUser.class));
        when(bankUserProvider.get(eq(sellerUuid))).thenReturn(mock(IBankUser.class));
        when(currencyManager.get(eq(currencyUuid))).thenReturn(Optional.of(new WeakReference<>(currency)));
        when(currency.ownerBank()).thenReturn(mock(CentralBank.class));

        tradeBroker.processOrder();

        verify(assetListingManager, atLeast(1)).peekMatchingOrder(any(Consumer.class));
        verify(assetListingManager, times(1)).cancelOrder(anyInt(), any(), any());
    }

    @Test
    public void testSellerAccountNotExist() throws Exception {
        CurrencyManager currencyManager = mock(CurrencyManager.class);
        AssetListingManager assetListingManager = mock(AssetListingManager.class);
        IBankUserProvider bankUserProvider = mock(IBankUserProvider.class);

        TradeMediator.TradeBroker tradeBroker = new TradeMediator.TradeBroker(assetListingManager,
                new HashSet<IBankUserProvider>() {{
                    add(bankUserProvider);
                }},
                currencyManager,
                logger);

        UUID buyerUuid = UUID.randomUUID();
        IBankUser buyer = mock(IBankUser.class);
        UUID sellerUuid = UUID.randomUUID();
        IBankUser seller = mock(IBankUser.class);
        UUID currencyUuid = UUID.randomUUID();
        Currency currency = mock(Currency.class);
        CentralBank bank = mock(CentralBank.class);
        doAnswer(invocation -> {
            Consumer<TradeInfo> consumer = (Consumer<TradeInfo>) invocation.getArguments()[0];

            consumer.accept(TradeInfo.create(35,
                    sellerUuid,
                    3000.55,
                    244,
                    232,
                    buyerUuid,
                    3099.34,
                    50,
                    currencyUuid,
                    UUID.randomUUID(),
                    1));

            return null;
        }).when(assetListingManager).peekMatchingOrder(any(Consumer.class));

        when(bankUserProvider.get(eq(buyerUuid))).thenReturn(buyer);
        when(bankUserProvider.get(eq(sellerUuid))).thenReturn(seller);
        when(currencyManager.get(eq(currencyUuid))).thenReturn(Optional.of(new WeakReference<>(currency)));
        when(currency.ownerBank()).thenReturn(bank);
        when(bank.hasAccount(eq(buyer), eq(BankingTypeRegistry.TRADING))).thenReturn(true);

        tradeBroker.processOrder();

        verify(assetListingManager, atLeast(1)).peekMatchingOrder(any(Consumer.class));
        verify(assetListingManager, times(1)).cancelOrder(anyInt(), any(), any());
    }

    @Test
    public void testBuyerOrderIdFail() throws Exception {
        CurrencyManager currencyManager = mock(CurrencyManager.class);
        AssetListingManager assetListingManager = mock(AssetListingManager.class);
        IBankUserProvider bankUserProvider = mock(IBankUserProvider.class);

        TradeMediator.TradeBroker tradeBroker = new TradeMediator.TradeBroker(assetListingManager,
                new HashSet<IBankUserProvider>() {{
                    add(bankUserProvider);
                }},
                currencyManager,
                logger);

        UUID buyerUuid = UUID.randomUUID();
        IBankUser buyer = mock(IBankUser.class);
        UUID sellerUuid = UUID.randomUUID();
        IBankUser seller = mock(IBankUser.class);
        UUID currencyUuid = UUID.randomUUID();
        Currency currency = mock(Currency.class);
        CentralBank bank = mock(CentralBank.class);
        doAnswer(invocation -> {
            Consumer<TradeInfo> consumer = (Consumer<TradeInfo>) invocation.getArguments()[0];

            consumer.accept(TradeInfo.create(35,
                    sellerUuid,
                    3000.55,
                    244,
                    232,
                    buyerUuid,
                    3099.34,
                    50,
                    currencyUuid,
                    UUID.randomUUID(),
                    1));

            return null;
        }).when(assetListingManager).peekMatchingOrder(any(Consumer.class));

        when(bankUserProvider.get(eq(buyerUuid))).thenReturn(buyer);
        when(bankUserProvider.get(eq(sellerUuid))).thenReturn(seller);
        when(currencyManager.get(eq(currencyUuid))).thenReturn(Optional.of(new WeakReference<>(currency)));
        when(currency.ownerBank()).thenReturn(bank);
        when(bank.hasAccount(eq(buyer), eq(BankingTypeRegistry.TRADING))).thenReturn(true);
        when(bank.hasAccount(eq(seller), eq(BankingTypeRegistry.TRADING))).thenReturn(true);

        tradeBroker.processOrder();

        verify(assetListingManager, atLeast(1)).peekMatchingOrder(any(Consumer.class));
        verify(assetListingManager, times(1)).cancelOrder(anyInt(), any(), any());
    }

    @Test
    public void testSellerOrderIdFail() throws Exception {
        CurrencyManager currencyManager = mock(CurrencyManager.class);
        AssetListingManager assetListingManager = mock(AssetListingManager.class);
        IBankUserProvider bankUserProvider = mock(IBankUserProvider.class);

        TradeMediator.TradeBroker tradeBroker = new TradeMediator.TradeBroker(assetListingManager,
                new HashSet<IBankUserProvider>() {{
                    add(bankUserProvider);
                }},
                currencyManager,
                logger);

        UUID buyerUuid = UUID.randomUUID();
        IBankUser buyer = mock(IBankUser.class);
        UUID sellerUuid = UUID.randomUUID();
        IBankUser seller = mock(IBankUser.class);
        UUID currencyUuid = UUID.randomUUID();
        Currency currency = mock(Currency.class);
        CentralBank bank = mock(CentralBank.class);
        doAnswer(invocation -> {
            Consumer<TradeInfo> consumer = (Consumer<TradeInfo>) invocation.getArguments()[0];

            consumer.accept(TradeInfo.create(35,
                    sellerUuid,
                    3000.55,
                    244,
                    232,
                    buyerUuid,
                    3099.34,
                    50,
                    currencyUuid,
                    UUID.randomUUID(),
                    1));

            return null;
        }).when(assetListingManager).peekMatchingOrder(any(Consumer.class));

        when(bankUserProvider.get(eq(buyerUuid))).thenReturn(buyer);
        when(bankUserProvider.get(eq(sellerUuid))).thenReturn(seller);
        when(currencyManager.get(eq(currencyUuid))).thenReturn(Optional.of(new WeakReference<>(currency)));
        when(currency.ownerBank()).thenReturn(bank);
        when(bank.hasAccount(eq(buyer), eq(BankingTypeRegistry.TRADING))).thenReturn(true);
        when(bank.hasAccount(eq(seller), eq(BankingTypeRegistry.TRADING))).thenReturn(true);
        when(buyer.hasOrderId(any(), anyInt())).thenReturn(true);

        tradeBroker.processOrder();

        verify(assetListingManager, atLeast(1)).peekMatchingOrder(any(Consumer.class));
        verify(assetListingManager, times(1)).cancelOrder(anyInt(), any(), any());
    }

    @Test
    public void testTransactionNoAssets() throws Exception {
        CurrencyManager currencyManager = mock(CurrencyManager.class);
        AssetListingManager assetListingManager = mock(AssetListingManager.class);
        IBankUserProvider bankUserProvider = mock(IBankUserProvider.class);

        TradeMediator.TradeBroker tradeBroker = new TradeMediator.TradeBroker(assetListingManager,
                new HashSet<IBankUserProvider>() {{
                    add(bankUserProvider);
                }},
                currencyManager,
                logger);

        UUID buyerUuid = UUID.randomUUID();
        IBankUser buyer = mock(IBankUser.class);
        UUID sellerUuid = UUID.randomUUID();
        IBankUser seller = mock(IBankUser.class);
        UUID currencyUuid = UUID.randomUUID();
        Currency currency = mock(Currency.class);
        CentralBank bank = mock(CentralBank.class);
        AssetListing listing = mock(AssetListing.class);
        UUID listingUuid = UUID.randomUUID();

        IMemento buyerState = mock(IMemento.class);
        IMemento sellerState = mock(IMemento.class);
        IMemento bankState = mock(IMemento.class);

        doAnswer(invocation -> {
            Consumer<TradeInfo> consumer = (Consumer<TradeInfo>) invocation.getArguments()[0];

            consumer.accept(TradeInfo.create(35,
                    sellerUuid,
                    3000.55,
                    244,
                    232,
                    buyerUuid,
                    3099.34,
                    50,
                    currencyUuid,
                    listingUuid,
                    1));

            return null;
        }).when(assetListingManager).peekMatchingOrder(any(Consumer.class));

        when(bankUserProvider.get(eq(buyerUuid))).thenReturn(buyer);
        when(bankUserProvider.get(eq(sellerUuid))).thenReturn(seller);
        when(currencyManager.get(eq(currencyUuid))).thenReturn(Optional.of(new WeakReference<>(currency)));
        when(currency.ownerBank()).thenReturn(bank);
        when(bank.hasAccount(eq(buyer), eq(BankingTypeRegistry.TRADING))).thenReturn(true);
        when(bank.hasAccount(eq(seller), eq(BankingTypeRegistry.TRADING))).thenReturn(true);
        when(buyer.hasOrderId(any(), anyInt())).thenReturn(true);
        when(seller.hasOrderId(any(), anyInt())).thenReturn(true);
        when(assetListingManager.get(eq(listingUuid))).thenReturn(Optional.of(new WeakReference<>(listing)));
        when(buyer.saveState()).thenReturn(buyerState);
        when(seller.saveState()).thenReturn(sellerState);
        when(bank.saveState()).thenReturn(bankState);

        tradeBroker.processOrder();

        verify(assetListingManager, atLeast(1)).peekMatchingOrder(any(Consumer.class));
        verify(buyer).restoreState(eq(buyerState));
        verify(seller).restoreState(eq(sellerState));
        verify(bank).restoreState(eq(bankState));
    }

    @Test
    public void testTransactionConcurrent() throws Exception {
        CurrencyManager currencyManager = mock(CurrencyManager.class);
        AssetListingManager assetListingManager = mock(AssetListingManager.class);
        IBankUserProvider bankUserProvider = mock(IBankUserProvider.class);

        TradeMediator.TradeBroker tradeBroker = new TradeMediator.TradeBroker(assetListingManager,
                new HashSet<IBankUserProvider>() {{
                    add(bankUserProvider);
                }},
                currencyManager,
                logger);

        UUID buyerUuid = UUID.randomUUID();
        IBankUser buyer = mock(IBankUser.class);
        UUID sellerUuid = UUID.randomUUID();
        IBankUser seller = mock(IBankUser.class);
        UUID currencyUuid = UUID.randomUUID();
        Currency currency = mock(Currency.class);
        CentralBank bank = new CentralBank(UUID.randomUUID());
        AssetListing listing = mock(AssetListing.class);
        UUID listingUuid = UUID.randomUUID();
        ItemStackSignature sign = new ItemStackSignature(Material.DIAMOND);

        when(currency.getKey()).thenReturn(currencyUuid);
        when(listing.getSignature()).thenReturn(sign);
        IMemento buyerState = mock(IMemento.class);
        IMemento sellerState = mock(IMemento.class);
        when(buyer.getUuid()).thenReturn(buyerUuid);
        when(seller.getUuid()).thenReturn(sellerUuid);
        addFakeObserver(bank);
        Guice.createInjector(moduleList).injectMembers(bank);

        doAnswer(invocation -> {
            Consumer<TradeInfo> consumer = (Consumer<TradeInfo>) invocation.getArguments()[0];

            consumer.accept(TradeInfo.create(35,
                    sellerUuid,
                    3000.55,
                    244,
                    232,
                    buyerUuid,
                    3099.34,
                    5,
                    currencyUuid,
                    listingUuid,
                    1));

            return null;
        }).when(assetListingManager).peekMatchingOrder(any(Consumer.class));

        bank.putAccount(buyer, BankingTypeRegistry.TRADING);
        bank.putAccount(seller, BankingTypeRegistry.TRADING);
        bank.addAccountAsset(seller, sign.asset(100.0));
        bank.depositAccount(buyer, BankingTypeRegistry.TRADING, 3000.55 * 5, currency);
        bank.depositAccount(seller, BankingTypeRegistry.TRADING, 12345.678, currency);

        when(bankUserProvider.get(eq(buyerUuid))).thenReturn(buyer);
        when(bankUserProvider.get(eq(sellerUuid))).thenReturn(seller);
        when(currencyManager.get(eq(currencyUuid))).thenReturn(Optional.of(new WeakReference<>(currency)));
        when(currency.ownerBank()).thenReturn(bank);
        when(buyer.hasOrderId(any(), anyInt())).thenReturn(true);
        when(seller.hasOrderId(any(), anyInt())).thenReturn(true);
        when(assetListingManager.get(eq(listingUuid))).thenReturn(Optional.of(new WeakReference<>(listing)));
        when(buyer.saveState()).thenReturn(buyerState);
        when(seller.saveState()).thenReturn(sellerState);

        Thread thread1 = new Thread(() -> {
            // run it 100 times
            // only the first trade will success since the buyer has just enough money for 5 diamonds
            for (int i = 0; i < 100; i++) {
                tradeBroker.processOrder();
            }
        });
        Thread thread2 = new Thread(() -> {
            // save total of 45150 to account
            for (int i = 0; i < 300; i++) {
                bank.depositAccount(seller, BankingTypeRegistry.TRADING, i + 1, currency);
            }
        });
        Thread thread3 = new Thread(() -> {
            // take out total of 5050 to account
            for (int i = 0; i < 100; i++) {
                bank.withdrawAccount(seller, BankingTypeRegistry.TRADING, i + 1, currency);
            }
        });

        thread1.start();
        thread2.start();
        thread3.start();

        thread1.join();
        thread2.join();
        thread3.join();

        verify(assetListingManager, atLeast(1)).peekMatchingOrder(any(Consumer.class));

        // we should have balance only for
        //   initial amount + the diamonds sold + amount difference of thread2 and thread3
        // if we have more or less than it suppose to be, probably race condition happened
        assertEquals(BigDecimal.valueOf(12345.678 + 3000.55 * 5 + (45150 - 5050)),
                bank.balanceOfAccount(seller, BankingTypeRegistry.TRADING, currency));
    }
}