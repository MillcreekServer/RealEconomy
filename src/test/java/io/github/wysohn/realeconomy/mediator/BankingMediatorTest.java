package io.github.wysohn.realeconomy.mediator;

import com.google.inject.Module;
import com.google.inject.*;
import com.google.inject.multibindings.ProvidesIntoSet;
import io.github.wysohn.rapidframework3.bukkit.testutils.manager.AbstractBukkitManagerTest;
import io.github.wysohn.rapidframework3.core.main.ManagerConfig;
import io.github.wysohn.realeconomy.inject.annotation.MaxCapital;
import io.github.wysohn.realeconomy.inject.annotation.MinCapital;
import io.github.wysohn.realeconomy.interfaces.IGovernment;
import io.github.wysohn.realeconomy.interfaces.banking.IAccount;
import io.github.wysohn.realeconomy.interfaces.banking.IBankOwnerProvider;
import io.github.wysohn.realeconomy.interfaces.banking.IBankUser;
import io.github.wysohn.realeconomy.manager.asset.Asset;
import io.github.wysohn.realeconomy.manager.banking.BankingTypeRegistry;
import io.github.wysohn.realeconomy.manager.banking.CentralBankingManager;
import io.github.wysohn.realeconomy.manager.banking.TransactionUtil;
import io.github.wysohn.realeconomy.manager.banking.bank.CentralBank;
import io.github.wysohn.realeconomy.manager.currency.Currency;
import io.github.wysohn.realeconomy.manager.currency.CurrencyManager;
import io.github.wysohn.realeconomy.manager.listing.OrderType;
import io.github.wysohn.realeconomy.manager.listing.TradeInfo;
import io.github.wysohn.realeconomy.manager.user.AbstractBankUser;
import org.junit.Before;
import org.junit.Test;

import java.lang.ref.WeakReference;
import java.math.BigDecimal;
import java.util.*;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.*;

public class BankingMediatorTest extends AbstractBukkitManagerTest {

    List<Module> moduleList = new LinkedList<>();
    private CentralBank serverBank;
    private Currency serverCurrency;
    private UUID serverCurrencyUuid;
    private CurrencyManager currencyManager;
    private CentralBankingManager centralBankingManager;
    private ManagerConfig config;

    @Before
    public void init() {
        UUID bankUuid = UUID.randomUUID();
        serverCurrencyUuid = UUID.randomUUID();
        serverBank = new CentralBank(bankUuid);
        serverCurrency = mock(Currency.class);
        currencyManager = mock(CurrencyManager.class);
        config = mock(ManagerConfig.class);

        when(serverCurrency.getKey()).thenReturn(serverCurrencyUuid);
        when(serverCurrency.ownerBank()).thenReturn(serverBank);
        addFakeObserver(serverBank);
        serverBank.setBaseCurrency(serverCurrency);
        serverBank.setLimitlessPapers(true);

        when(currencyManager.newCurrency(anyString(), anyString(), any())).thenReturn(CurrencyManager.Result.OK);
        centralBankingManager = mock(CentralBankingManager.class);
        when(config.get(eq(BankingMediator.KEY_SERVER_BANK_ENABLE))).thenReturn(Optional.of(true));
        when(currencyManager.get(eq(BankingMediator.SERVER_CURRENCY)))
                .thenReturn(Optional.of(new WeakReference<>(serverCurrency)));
        when(currencyManager.get(eq(serverCurrencyUuid)))
                .thenReturn(Optional.of(new WeakReference<>(serverCurrency)));

        moduleList.add(new AbstractModule() {
            @Provides
            ManagerConfig config() {
                return config;
            }

            @Provides
            CurrencyManager currencyManager() {
                return currencyManager;
            }

            @Provides
            CentralBankingManager centralBankingManager() {
                return centralBankingManager;
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

            @ProvidesIntoSet
            IBankOwnerProvider bankOwnerProvider() {
                return mock(IBankOwnerProvider.class);
            }
        });

        Guice.createInjector(moduleList).injectMembers(serverBank);
    }

    @Test
    public void createCurrency() throws Exception {
        BankingMediator mediator = Guice.createInjector(moduleList).getInstance(BankingMediator.class);
        when(centralBankingManager.getOrNew(any(UUID.class)))
                .thenReturn(Optional.of(new WeakReference<>(serverBank)));
        mediator.enable();

        UUID uuid = UUID.randomUUID();
        IGovernment government = mock(IGovernment.class);
        Currency currency = mock(Currency.class);
        when(government.getUuid()).thenReturn(uuid);
        when(currencyManager.get(anyString())).thenReturn(Optional.of(new WeakReference<>(currency)));
        when(centralBankingManager.get(any(UUID.class)))
                .thenReturn(Optional.of(new WeakReference<>(mock(CentralBank.class))));

        assertEquals(BankingMediator.Result.ALREADY_SET,
                mediator.createCurrency(government, BankingMediator.SERVER_CURRENCY, "USD"));
    }

    @Test
    public void createCurrency2() throws Exception {
        BankingMediator mediator = Guice.createInjector(moduleList).getInstance(BankingMediator.class);
        when(centralBankingManager.getOrNew(any(UUID.class)))
                .thenReturn(Optional.of(new WeakReference<>(serverBank)));
        mediator.enable();

        UUID uuid = UUID.randomUUID();
        IGovernment government = mock(IGovernment.class);
        Currency currency = mock(Currency.class);
        when(government.getUuid()).thenReturn(uuid);
        when(currencyManager.get(anyString())).thenReturn(Optional.of(new WeakReference<>(currency)));
        when(centralBankingManager.get(any(UUID.class))).thenReturn(Optional.empty());
        when(centralBankingManager.getOrNew(any()))
                .thenReturn(Optional.of(new WeakReference<>(mock(CentralBank.class))));

        assertEquals(BankingMediator.Result.OK,
                mediator.createCurrency(government, "dollar", "USD"));
        verify(currencyManager).newCurrency(eq("dollar"), eq("USD"), any(CentralBank.class));
        verify(centralBankingManager).getOrNew(eq(uuid));
    }

    @Test
    public void renameCurrency() throws Exception {
        BankingMediator mediator = Guice.createInjector(moduleList).getInstance(BankingMediator.class);
        when(centralBankingManager.getOrNew(any(UUID.class)))
                .thenReturn(Optional.of(new WeakReference<>(serverBank)));
        mediator.enable();

        when(currencyManager.get(anyString()))
                .thenReturn(Optional.of(new WeakReference<>(mock(Currency.class))));

        assertEquals(BankingMediator.Result.DUP_NAME,
                mediator.renameCurrency(BankingMediator.SERVER_CURRENCY, "other"));
        verify(currencyManager).get(eq("other"));
    }

    @Test
    public void renameCurrency2() throws Exception {
        BankingMediator mediator = Guice.createInjector(moduleList).getInstance(BankingMediator.class);
        when(centralBankingManager.getOrNew(any(UUID.class)))
                .thenReturn(Optional.of(new WeakReference<>(serverBank)));
        mediator.enable();

        when(currencyManager.get(eq("other")))
                .thenReturn(Optional.empty());
        when(currencyManager.get(eq(BankingMediator.SERVER_CURRENCY)))
                .thenReturn(Optional.empty());

        assertEquals(BankingMediator.Result.NOT_FOUND,
                mediator.renameCurrency(BankingMediator.SERVER_CURRENCY, "other"));
    }

    @Test
    public void renameCurrency3() throws Exception {
        BankingMediator mediator = Guice.createInjector(moduleList).getInstance(BankingMediator.class);
        when(centralBankingManager.getOrNew(any(UUID.class)))
                .thenReturn(Optional.of(new WeakReference<>(serverBank)));
        mediator.enable();

        Currency currency = mock(Currency.class);
        when(currencyManager.get(eq("other")))
                .thenReturn(Optional.empty());
        when(currencyManager.get(eq(BankingMediator.SERVER_CURRENCY)))
                .thenReturn(Optional.of(new WeakReference<>(currency)));

        assertEquals(BankingMediator.Result.OK,
                mediator.renameCurrency(BankingMediator.SERVER_CURRENCY, "other"));
    }

    @Test
    public void balance() throws Exception {
        BankingMediator mediator = Guice.createInjector(moduleList).getInstance(BankingMediator.class);
        when(centralBankingManager.getOrNew(any(UUID.class)))
                .thenReturn(Optional.of(new WeakReference<>(serverBank)));
        mediator.enable();

        UUID uuid = UUID.randomUUID();
        UUID currencyUuid = UUID.randomUUID();
        IBankUser user = mock(IBankUser.class);
        IAccount account = mock(IAccount.class);
        Currency currency = mock(Currency.class);
        Map<UUID, BigDecimal> balances = mock(Map.class);

        when(user.getUuid()).thenReturn(uuid);
        when(currency.getKey()).thenReturn(currencyUuid);
        when(account.getCurrencyMap()).thenReturn(balances);

        assertEquals(BigDecimal.valueOf(0.0), serverBank.balanceOfAccount(user, BankingTypeRegistry.CHECKING));
    }

    @Test
    public void testConcurrency() throws Exception {
        Injector injector = Guice.createInjector(moduleList);

        BankingMediator mediator = injector.getInstance(BankingMediator.class);
        when(centralBankingManager.getOrNew(any(UUID.class)))
                .thenReturn(Optional.of(new WeakReference<>(serverBank)));
        mediator.enable();

        UUID uuid1 = UUID.randomUUID();
        TempUser user1 = new TempUser(uuid1);
        injector.injectMembers(user1);
        addFakeObserver(user1);
        UUID uuid2 = UUID.randomUUID();
        TempUser user2 = new TempUser(uuid2);
        injector.injectMembers(user2);
        addFakeObserver(user2);

        serverBank.putAccount(user1, BankingTypeRegistry.TRADING);
        serverBank.putAccount(user2, BankingTypeRegistry.TRADING);
        serverBank.depositAccount(user1, BankingTypeRegistry.TRADING, 500200, serverCurrency);
        serverBank.depositAccount(user2, BankingTypeRegistry.TRADING, 499800, serverCurrency);

        // user 1 sending money to bank
        Thread thread1 = new Thread(() -> {
            for (int i = 0; i < 1000; i++) {
                assertEquals(TransactionUtil.Result.OK,
                        mediator.send(user1, BankingTypeRegistry.TRADING, serverBank, BigDecimal.valueOf(i + 1), serverCurrency));
            }
        });

        // user 2 sending money to bank
        Thread thread2 = new Thread(() -> {
            for (int i = 0; i < 1000; i++) {
                assertEquals(TransactionUtil.Result.OK,
                        mediator.send(user2, BankingTypeRegistry.TRADING, serverBank, BigDecimal.valueOf(i + 1), serverCurrency));
            }
        });

        thread1.start();
        thread2.start();

        thread1.join();
        thread2.join();

        // (1 + 2 + ... + 999) = 499500
        assertEquals(BigDecimal.valueOf(700.0), serverBank.balanceOfAccount(user1, BankingTypeRegistry.TRADING, serverCurrency));
        assertEquals(BigDecimal.valueOf(300.0), serverBank.balanceOfAccount(user2, BankingTypeRegistry.TRADING, serverCurrency));
    }

    @Test
    public void testConcurrency2() throws Exception {
        Injector injector = Guice.createInjector(moduleList);

        BankingMediator mediator = injector.getInstance(BankingMediator.class);
        when(centralBankingManager.getOrNew(any(UUID.class)))
                .thenReturn(Optional.of(new WeakReference<>(serverBank)));
        mediator.enable();

        UUID uuid1 = UUID.randomUUID();
        TempUser user1 = new TempUser(uuid1);
        injector.injectMembers(user1);
        addFakeObserver(user1);
        UUID uuid2 = UUID.randomUUID();
        TempUser user2 = new TempUser(uuid2);
        injector.injectMembers(user2);
        addFakeObserver(user2);

        serverBank.putAccount(user1, BankingTypeRegistry.TRADING);
        serverBank.putAccount(user2, BankingTypeRegistry.TRADING);

        serverBank.depositAccount(user1, BankingTypeRegistry.TRADING, 500500.0, serverCurrency);
        serverBank.depositAccount(user2, BankingTypeRegistry.TRADING, 500500.0, serverCurrency);

        // user 1 sends 125250 to user 2
        Thread thread1 = new Thread(() -> {
            for (int i = 0; i < 500; i++) {
                assertEquals(TransactionUtil.Result.OK,
                        mediator.send(user1, BankingTypeRegistry.TRADING,
                                user2, BankingTypeRegistry.TRADING,
                                BigDecimal.valueOf(i + 1),
                                serverCurrency));
            }
        });

        // user 2 sends 245350 to user 1
        Thread thread2 = new Thread(() -> {
            for (int i = 0; i < 700; i++) {
                assertEquals(TransactionUtil.Result.OK,
                        mediator.send(user2, BankingTypeRegistry.TRADING,
                                user1, BankingTypeRegistry.TRADING,
                                BigDecimal.valueOf(i + 1),
                                serverCurrency));
            }
        });

        thread1.start();
        thread2.start();

        thread1.join();
        thread2.join();

        // (1 + 2 + ... + 1000) = 500500
        // (1 + 2 + ... + 500) = 125250
        // (1 + 2 + ... + 700) = 245350
        assertEquals(BigDecimal.valueOf(500500.0 - 125250 + 245350), serverBank.balanceOfAccount(user1, BankingTypeRegistry.TRADING, serverCurrency));
        assertEquals(BigDecimal.valueOf(500500.0 - 245350 + 125250), serverBank.balanceOfAccount(user2, BankingTypeRegistry.TRADING, serverCurrency));
    }

    @Test
    public void testConcurrency3() throws Exception {
        Injector injector = Guice.createInjector(moduleList);

        BankingMediator mediator = injector.getInstance(BankingMediator.class);
        when(centralBankingManager.getOrNew(any(UUID.class)))
                .thenReturn(Optional.of(new WeakReference<>(serverBank)));
        mediator.enable();

        UUID uuid1 = UUID.randomUUID();
        TempUser user1 = new TempUser(uuid1);
        injector.injectMembers(user1);
        addFakeObserver(user1);
        UUID uuid2 = UUID.randomUUID();
        TempUser user2 = new TempUser(uuid2);
        injector.injectMembers(user2);
        addFakeObserver(user2);
        UUID uuid3 = UUID.randomUUID();
        TempUser user3 = new TempUser(uuid3);
        injector.injectMembers(user3);
        addFakeObserver(user3);

        serverBank.putAccount(user1, BankingTypeRegistry.TRADING);
        serverBank.putAccount(user2, BankingTypeRegistry.TRADING);
        serverBank.putAccount(user3, BankingTypeRegistry.TRADING);
        serverBank.depositAccount(user1, BankingTypeRegistry.TRADING, 125250.0, serverCurrency);
        serverBank.depositAccount(user2, BankingTypeRegistry.TRADING, 125250.0, serverCurrency);

        // user 1 sending to user 2
        Thread thread1 = new Thread(() -> {
            for (int i = 0; i < 1000; i++) {
                assertEquals(TransactionUtil.Result.OK,
                        mediator.send(user1, BankingTypeRegistry.TRADING, user2, BankingTypeRegistry.TRADING, BigDecimal.valueOf(i + 1), serverCurrency));
            }
        });

        // user 2 sending to user 3
        Thread thread2 = new Thread(() -> {
            for (int i = 0; i < 500; i++) {
                assertEquals(TransactionUtil.Result.OK,
                        mediator.send(user2, BankingTypeRegistry.TRADING, user3, BankingTypeRegistry.TRADING, BigDecimal.valueOf(i + 1), serverCurrency));
            }
        });

        thread1.start();
        thread2.start();

        thread1.join();
        thread2.join();

        assertEquals(BigDecimal.valueOf(0.0), serverBank.balanceOfAccount(user1, BankingTypeRegistry.TRADING, serverCurrency));
        assertEquals(BigDecimal.valueOf(125250.0), serverBank.balanceOfAccount(user2, BankingTypeRegistry.TRADING, serverCurrency));
        assertEquals(BigDecimal.valueOf(125250.0), serverBank.balanceOfAccount(user3, BankingTypeRegistry.TRADING, serverCurrency));
    }

    private static class TempUser extends AbstractBankUser {
        public TempUser(UUID key) {
            super(key);
        }

        @Override
        public void handleTransactionResult(TradeInfo info, OrderType type, TradeMediator.TradeResult result) {

        }

        @Override
        public int realizeAsset(Asset asset) {
            return 0;
        }
    }
}