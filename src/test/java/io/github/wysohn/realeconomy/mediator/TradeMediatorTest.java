package io.github.wysohn.realeconomy.mediator;

import com.google.inject.AbstractModule;
import com.google.inject.Module;
import com.google.inject.Provides;
import io.github.wysohn.rapidframework3.core.main.ManagerConfig;
import io.github.wysohn.rapidframework3.interfaces.IMemento;
import io.github.wysohn.rapidframework3.testmodules.MockLoggerModule;
import io.github.wysohn.rapidframework3.testmodules.MockShutdownModule;
import io.github.wysohn.realeconomy.inject.annotation.MaxCapital;
import io.github.wysohn.realeconomy.inject.annotation.MinCapital;
import io.github.wysohn.realeconomy.interfaces.banking.IBankUser;
import io.github.wysohn.realeconomy.interfaces.banking.IBankUserProvider;
import io.github.wysohn.realeconomy.manager.banking.BankingTypeRegistry;
import io.github.wysohn.realeconomy.manager.banking.bank.CentralBank;
import io.github.wysohn.realeconomy.manager.currency.Currency;
import io.github.wysohn.realeconomy.manager.currency.CurrencyManager;
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

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

public class TradeMediatorTest {

    List<Module> moduleList = new LinkedList<>();
    private ManagerConfig config;
    private Logger logger;

    @Before
    public void init() throws Exception{
        config = mock(ManagerConfig.class);
        logger = mock(Logger.class);

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
    public void testTransaction() throws Exception {
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
        when(seller.hasOrderId(any(), anyInt())).thenReturn(true);

        tradeBroker.processOrder();

        verify(assetListingManager, atLeast(1)).peekMatchingOrder(any(Consumer.class));
    }
}