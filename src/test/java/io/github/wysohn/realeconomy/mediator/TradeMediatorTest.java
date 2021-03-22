package io.github.wysohn.realeconomy.mediator;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Module;
import com.google.inject.Provides;
import com.google.inject.multibindings.ProvidesIntoSet;
import io.github.wysohn.rapidframework3.core.main.ManagerConfig;
import io.github.wysohn.rapidframework3.testmodules.MockLoggerModule;
import io.github.wysohn.realeconomy.inject.annotation.MaxCapital;
import io.github.wysohn.realeconomy.inject.annotation.MinCapital;
import io.github.wysohn.realeconomy.interfaces.banking.IBankUser;
import io.github.wysohn.realeconomy.interfaces.banking.IBankUserProvider;
import io.github.wysohn.realeconomy.manager.asset.Asset;
import io.github.wysohn.realeconomy.manager.asset.signature.AssetSignature;
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
import org.bukkit.inventory.ItemStack;
import org.junit.Before;
import org.junit.Test;

import java.lang.ref.WeakReference;
import java.math.BigDecimal;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

public class TradeMediatorTest {

    List<Module> moduleList = new LinkedList<>();
    private ManagerConfig config;

    @Before
    public void init() throws Exception{
        config = mock(ManagerConfig.class);

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
        });
    }

    @Test
    public void testBroker() throws Exception {
        CurrencyManager currencyManager = mock(CurrencyManager.class);
        AssetListingManager assetListingManager = mock(AssetListingManager.class);
        IBankUserProvider bankUserProvider = mock(IBankUserProvider.class);
        moduleList.add(new AbstractModule() {
            @Provides
            CurrencyManager currencyManager() {
                return currencyManager;
            }

            @Provides
            AssetListingManager assetListingManager() {
                return assetListingManager;
            }

            @ProvidesIntoSet
            IBankUserProvider bankUserProvider() {
                return bankUserProvider;
            }
        });

        TradeMediator mediator = Guice.createInjector(moduleList).getInstance(TradeMediator.class);

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

        mediator.enable();
        mediator.load();
        mediator.tradeBroker.processOrder();
        mediator.disable();

        verify(assetListingManager, atLeast(1)).peekMatchingOrder(any(Consumer.class));
    }

    @Test
    public void testBroker2() throws Exception {
        CurrencyManager currencyManager = mock(CurrencyManager.class);
        AssetListingManager assetListingManager = mock(AssetListingManager.class);
        IBankUserProvider bankUserProvider = mock(IBankUserProvider.class);
        moduleList.add(new AbstractModule() {
            @Provides
            CurrencyManager currencyManager() {
                return currencyManager;
            }

            @Provides
            AssetListingManager assetListingManager() {
                return assetListingManager;
            }

            @ProvidesIntoSet
            IBankUserProvider bankUserProvider() {
                return bankUserProvider;
            }
        });

        TradeMediator mediator = Guice.createInjector(moduleList).getInstance(TradeMediator.class);

        IBankUser buyer = mock(IBankUser.class);
        UUID buyerUuid = UUID.randomUUID();
        IBankUser seller = mock(IBankUser.class);
        UUID sellerUuid = UUID.randomUUID();

        when(buyer.getUuid()).thenReturn(buyerUuid);
        when(seller.getUuid()).thenReturn(sellerUuid);
        when(bankUserProvider.get(eq(buyerUuid))).thenReturn(buyer);
        when(bankUserProvider.get(eq(sellerUuid))).thenReturn(seller);
        when(currencyManager.get(any(UUID.class))).thenReturn(Optional.empty());

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

        mediator.enable();
        mediator.load();
        mediator.tradeBroker.processOrder();
        mediator.disable();

        verify(assetListingManager, atLeast(1)).peekMatchingOrder(any(Consumer.class));
    }

    @Test
    public void testBroker3() throws Exception {
        CurrencyManager currencyManager = mock(CurrencyManager.class);
        AssetListingManager assetListingManager = mock(AssetListingManager.class);
        IBankUserProvider bankUserProvider = mock(IBankUserProvider.class);
        moduleList.add(new AbstractModule() {
            @Provides
            CurrencyManager currencyManager() {
                return currencyManager;
            }

            @Provides
            AssetListingManager assetListingManager() {
                return assetListingManager;
            }

            @ProvidesIntoSet
            IBankUserProvider bankUserProvider() {
                return bankUserProvider;
            }
        });

        TradeMediator mediator = Guice.createInjector(moduleList).getInstance(TradeMediator.class);

        IBankUser buyer = mock(IBankUser.class);
        UUID buyerUuid = UUID.randomUUID();
        IBankUser seller = mock(IBankUser.class);
        UUID sellerUuid = UUID.randomUUID();
        Currency currency = mock(Currency.class);
        UUID currencyUuid = UUID.randomUUID();

        when(buyer.getUuid()).thenReturn(buyerUuid);
        when(seller.getUuid()).thenReturn(sellerUuid);
        when(bankUserProvider.get(eq(buyerUuid))).thenReturn(buyer);
        when(bankUserProvider.get(eq(sellerUuid))).thenReturn(seller);
        when(currencyManager.get(eq(currencyUuid))).thenReturn(Optional.of(new WeakReference<>(currency)));

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

        mediator.enable();
        mediator.load();
        mediator.tradeBroker.processOrder();
        mediator.disable();

        verify(assetListingManager, atLeast(1)).peekMatchingOrder(any(Consumer.class));
    }

    @Test
    public void testBroker4() throws Exception {
        CurrencyManager currencyManager = mock(CurrencyManager.class);
        AssetListingManager assetListingManager = mock(AssetListingManager.class);
        IBankUserProvider bankUserProvider = mock(IBankUserProvider.class);
        moduleList.add(new AbstractModule() {
            @Provides
            CurrencyManager currencyManager() {
                return currencyManager;
            }

            @Provides
            AssetListingManager assetListingManager() {
                return assetListingManager;
            }

            @ProvidesIntoSet
            IBankUserProvider bankUserProvider() {
                return bankUserProvider;
            }
        });

        TradeMediator mediator = Guice.createInjector(moduleList).getInstance(TradeMediator.class);

        IBankUser buyer = mock(IBankUser.class);
        UUID buyerUuid = UUID.randomUUID();
        IBankUser seller = mock(IBankUser.class);
        UUID sellerUuid = UUID.randomUUID();
        Currency currency = mock(Currency.class);
        UUID currencyUuid = UUID.randomUUID();
        CentralBank ownerBank = mock(CentralBank.class);
        UUID bankUuid = UUID.randomUUID();

        when(buyer.getUuid()).thenReturn(buyerUuid);
        when(seller.getUuid()).thenReturn(sellerUuid);
        when(bankUserProvider.get(eq(buyerUuid))).thenReturn(buyer);
        when(bankUserProvider.get(eq(sellerUuid))).thenReturn(seller);
        when(currencyManager.get(eq(currencyUuid))).thenReturn(Optional.of(new WeakReference<>(currency)));
        when(currency.ownerBank()).thenReturn(ownerBank);
        when(ownerBank.getUuid()).thenReturn(bankUuid);

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

        mediator.enable();
        mediator.load();
        mediator.tradeBroker.processOrder();
        mediator.disable();

        verify(assetListingManager, atLeast(1)).peekMatchingOrder(any(Consumer.class));
    }

    @Test
    public void testBroker5() throws Exception {
        CurrencyManager currencyManager = mock(CurrencyManager.class);
        AssetListingManager assetListingManager = mock(AssetListingManager.class);
        IBankUserProvider bankUserProvider = mock(IBankUserProvider.class);
        moduleList.add(new AbstractModule() {
            @Provides
            CurrencyManager currencyManager() {
                return currencyManager;
            }

            @Provides
            AssetListingManager assetListingManager() {
                return assetListingManager;
            }

            @ProvidesIntoSet
            IBankUserProvider bankUserProvider() {
                return bankUserProvider;
            }
        });

        TradeMediator mediator = Guice.createInjector(moduleList).getInstance(TradeMediator.class);

        IBankUser buyer = mock(IBankUser.class);
        UUID buyerUuid = UUID.randomUUID();
        IBankUser seller = mock(IBankUser.class);
        UUID sellerUuid = UUID.randomUUID();
        Currency currency = mock(Currency.class);
        UUID currencyUuid = UUID.randomUUID();
        CentralBank ownerBank = mock(CentralBank.class);
        UUID bankUuid = UUID.randomUUID();

        when(buyer.getUuid()).thenReturn(buyerUuid);
        when(seller.getUuid()).thenReturn(sellerUuid);
        when(bankUserProvider.get(eq(buyerUuid))).thenReturn(buyer);
        when(bankUserProvider.get(eq(sellerUuid))).thenReturn(seller);
        when(currencyManager.get(eq(currencyUuid))).thenReturn(Optional.of(new WeakReference<>(currency)));
        when(currency.ownerBank()).thenReturn(ownerBank);
        when(ownerBank.getUuid()).thenReturn(bankUuid);
        when(ownerBank.hasAccount(eq(buyer), eq(BankingTypeRegistry.TRADING))).thenReturn(true);
        when(ownerBank.hasAccount(eq(seller), eq(BankingTypeRegistry.TRADING))).thenReturn(true);

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

        mediator.enable();
        mediator.load();
        mediator.tradeBroker.processOrder();
        mediator.disable();

        verify(assetListingManager, atLeast(1)).peekMatchingOrder(any(Consumer.class));
    }

    @Test
    public void testBroker6() throws Exception {
        CurrencyManager currencyManager = mock(CurrencyManager.class);
        AssetListingManager assetListingManager = mock(AssetListingManager.class);
        IBankUserProvider bankUserProvider = mock(IBankUserProvider.class);
        moduleList.add(new AbstractModule() {
            @Provides
            CurrencyManager currencyManager() {
                return currencyManager;
            }

            @Provides
            AssetListingManager assetListingManager() {
                return assetListingManager;
            }

            @ProvidesIntoSet
            IBankUserProvider bankUserProvider() {
                return bankUserProvider;
            }
        });

        TradeMediator mediator = Guice.createInjector(moduleList).getInstance(TradeMediator.class);

        IBankUser buyer = mock(IBankUser.class);
        UUID buyerUuid = UUID.randomUUID();
        IBankUser seller = mock(IBankUser.class);
        UUID sellerUuid = UUID.randomUUID();
        Currency currency = mock(Currency.class);
        UUID currencyUuid = UUID.randomUUID();
        CentralBank ownerBank = mock(CentralBank.class);
        UUID bankUuid = UUID.randomUUID();
        AssetListing listing = mock(AssetListing.class);
        UUID listingUuid = UUID.randomUUID();

        when(buyer.getUuid()).thenReturn(buyerUuid);
        when(seller.getUuid()).thenReturn(sellerUuid);
        when(bankUserProvider.get(eq(buyerUuid))).thenReturn(buyer);
        when(bankUserProvider.get(eq(sellerUuid))).thenReturn(seller);
        when(currencyManager.get(eq(currencyUuid))).thenReturn(Optional.of(new WeakReference<>(currency)));
        when(currency.ownerBank()).thenReturn(ownerBank);
        when(ownerBank.getUuid()).thenReturn(bankUuid);
        when(ownerBank.hasAccount(eq(buyer), eq(BankingTypeRegistry.TRADING))).thenReturn(true);
        when(ownerBank.hasAccount(eq(seller), eq(BankingTypeRegistry.TRADING))).thenReturn(true);
        when(listing.getKey()).thenReturn(listingUuid);
        when(assetListingManager.get(eq(listingUuid))).thenReturn(Optional.of(new WeakReference<>(listing)));

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

        mediator.enable();
        mediator.load();
        mediator.tradeBroker.processOrder();
        mediator.disable();

        verify(assetListingManager, atLeast(1)).peekMatchingOrder(any(Consumer.class));
    }

    @Test
    public void testBroker7() throws Exception {
        CurrencyManager currencyManager = mock(CurrencyManager.class);
        AssetListingManager assetListingManager = mock(AssetListingManager.class);
        IBankUserProvider bankUserProvider = mock(IBankUserProvider.class);
        moduleList.add(new AbstractModule() {
            @Provides
            CurrencyManager currencyManager() {
                return currencyManager;
            }

            @Provides
            AssetListingManager assetListingManager() {
                return assetListingManager;
            }

            @ProvidesIntoSet
            IBankUserProvider bankUserProvider() {
                return bankUserProvider;
            }
        });

        TradeMediator mediator = Guice.createInjector(moduleList).getInstance(TradeMediator.class);

        IBankUser buyer = mock(IBankUser.class);
        UUID buyerUuid = UUID.randomUUID();
        IBankUser seller = mock(IBankUser.class);
        UUID sellerUuid = UUID.randomUUID();
        Currency currency = mock(Currency.class);
        UUID currencyUuid = UUID.randomUUID();
        CentralBank ownerBank = mock(CentralBank.class);
        UUID bankUuid = UUID.randomUUID();
        AssetListing listing = mock(AssetListing.class);
        UUID listingUuid = UUID.randomUUID();
        AssetSignature signature = mock(AssetSignature.class);

        when(buyer.getUuid()).thenReturn(buyerUuid);
        when(seller.getUuid()).thenReturn(sellerUuid);
        when(bankUserProvider.get(eq(buyerUuid))).thenReturn(buyer);
        when(bankUserProvider.get(eq(sellerUuid))).thenReturn(seller);
        when(currencyManager.get(eq(currencyUuid))).thenReturn(Optional.of(new WeakReference<>(currency)));
        when(currency.ownerBank()).thenReturn(ownerBank);
        when(ownerBank.getUuid()).thenReturn(bankUuid);
        when(ownerBank.hasAccount(eq(buyer), eq(BankingTypeRegistry.TRADING))).thenReturn(true);
        when(ownerBank.hasAccount(eq(seller), eq(BankingTypeRegistry.TRADING))).thenReturn(true);
        when(listing.getKey()).thenReturn(listingUuid);
        when(listing.getSignature()).thenReturn(signature);
        when(assetListingManager.get(eq(listingUuid))).thenReturn(Optional.of(new WeakReference<>(listing)));

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

        mediator.enable();
        mediator.load();
        mediator.tradeBroker.processOrder();
        mediator.disable();

        verify(assetListingManager, atLeast(1)).peekMatchingOrder(any(Consumer.class));
    }

    @Test
    public void testBroker8() throws Exception {
        CurrencyManager currencyManager = mock(CurrencyManager.class);
        AssetListingManager assetListingManager = mock(AssetListingManager.class);
        IBankUserProvider bankUserProvider = mock(IBankUserProvider.class);
        moduleList.add(new AbstractModule() {
            @Provides
            CurrencyManager currencyManager() {
                return currencyManager;
            }

            @Provides
            AssetListingManager assetListingManager() {
                return assetListingManager;
            }

            @ProvidesIntoSet
            IBankUserProvider bankUserProvider() {
                return bankUserProvider;
            }
        });

        TradeMediator mediator = Guice.createInjector(moduleList).getInstance(TradeMediator.class);

        IBankUser buyer = mock(IBankUser.class);
        UUID buyerUuid = UUID.randomUUID();
        IBankUser seller = mock(IBankUser.class);
        UUID sellerUuid = UUID.randomUUID();
        Currency currency = mock(Currency.class);
        UUID currencyUuid = UUID.randomUUID();
        CentralBank ownerBank = mock(CentralBank.class);
        UUID bankUuid = UUID.randomUUID();
        AssetListing listing = mock(AssetListing.class);
        UUID listingUuid = UUID.randomUUID();
        AssetSignature signature = mock(AssetSignature.class);

        when(buyer.getUuid()).thenReturn(buyerUuid);
        when(seller.getUuid()).thenReturn(sellerUuid);
        when(bankUserProvider.get(eq(buyerUuid))).thenReturn(buyer);
        when(bankUserProvider.get(eq(sellerUuid))).thenReturn(seller);
        when(currencyManager.get(eq(currencyUuid))).thenReturn(Optional.of(new WeakReference<>(currency)));
        when(currency.ownerBank()).thenReturn(ownerBank);
        when(ownerBank.getUuid()).thenReturn(bankUuid);
        when(ownerBank.hasAccount(eq(buyer), eq(BankingTypeRegistry.TRADING))).thenReturn(true);
        when(ownerBank.hasAccount(eq(seller), eq(BankingTypeRegistry.TRADING))).thenReturn(true);
        when(listing.getKey()).thenReturn(listingUuid);
        when(listing.getSignature()).thenReturn(signature);
        when(assetListingManager.get(eq(listingUuid))).thenReturn(Optional.of(new WeakReference<>(listing)));
        when(ownerBank.removeAccountAsset(eq(seller), eq(signature), anyInt())).thenReturn(new LinkedList<Asset>() {{
            add(new ItemStackSignature(new ItemStack(Material.DIAMOND)).asset(1.0));
        }});

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

        mediator.enable();
        mediator.load();
        mediator.tradeBroker.processOrder();
        mediator.disable();

        verify(assetListingManager, atLeast(1)).peekMatchingOrder(any(Consumer.class));
    }

    @Test
    public void testBroker9() throws Exception {
        CurrencyManager currencyManager = mock(CurrencyManager.class);
        AssetListingManager assetListingManager = mock(AssetListingManager.class);
        IBankUserProvider bankUserProvider = mock(IBankUserProvider.class);
        moduleList.add(new AbstractModule() {
            @Provides
            CurrencyManager currencyManager() {
                return currencyManager;
            }

            @Provides
            AssetListingManager assetListingManager() {
                return assetListingManager;
            }

            @ProvidesIntoSet
            IBankUserProvider bankUserProvider() {
                return bankUserProvider;
            }
        });

        TradeMediator mediator = Guice.createInjector(moduleList).getInstance(TradeMediator.class);

        IBankUser buyer = mock(IBankUser.class);
        UUID buyerUuid = UUID.randomUUID();
        IBankUser seller = mock(IBankUser.class);
        UUID sellerUuid = UUID.randomUUID();
        Currency currency = mock(Currency.class);
        UUID currencyUuid = UUID.randomUUID();
        CentralBank ownerBank = mock(CentralBank.class);
        UUID bankUuid = UUID.randomUUID();
        AssetListing listing = mock(AssetListing.class);
        UUID listingUuid = UUID.randomUUID();
        AssetSignature signature = mock(AssetSignature.class);
        Asset asset = mock(Asset.class);

        when(buyer.getUuid()).thenReturn(buyerUuid);
        when(seller.getUuid()).thenReturn(sellerUuid);
        when(bankUserProvider.get(eq(buyerUuid))).thenReturn(buyer);
        when(bankUserProvider.get(eq(sellerUuid))).thenReturn(seller);
        when(currencyManager.get(eq(currencyUuid))).thenReturn(Optional.of(new WeakReference<>(currency)));
        when(currency.ownerBank()).thenReturn(ownerBank);
        when(ownerBank.getUuid()).thenReturn(bankUuid);
        when(ownerBank.hasAccount(eq(buyer), eq(BankingTypeRegistry.TRADING))).thenReturn(true);
        when(ownerBank.hasAccount(eq(seller), eq(BankingTypeRegistry.TRADING))).thenReturn(true);
        when(listing.getKey()).thenReturn(listingUuid);
        when(listing.getSignature()).thenReturn(signature);
        when(assetListingManager.get(eq(listingUuid))).thenReturn(Optional.of(new WeakReference<>(listing)));
        when(ownerBank.removeAccountAsset(eq(seller), eq(signature), anyInt())).thenReturn(new LinkedList<Asset>() {{
            add(new ItemStackSignature(new ItemStack(Material.DIAMOND)).asset(1.0));
        }});
        when(ownerBank.withdrawAccount(any(), any(), any(), any())).thenReturn(true);
        when(ownerBank.depositAccount(any(), any(), any(), any())).thenReturn(true);
        when(signature.asset(anyMap())).thenReturn(asset);

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

        mediator.enable();
        mediator.load();
        mediator.tradeBroker.processOrder();
        mediator.disable();

        verify(assetListingManager, atLeast(1)).peekMatchingOrder(any(Consumer.class));
    }
}