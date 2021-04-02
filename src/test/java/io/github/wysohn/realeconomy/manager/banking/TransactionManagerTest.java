package io.github.wysohn.realeconomy.manager.banking;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Provides;
import com.google.inject.multibindings.ProvidesIntoSet;
import io.github.wysohn.rapidframework3.bukkit.testutils.manager.AbstractBukkitManagerTest;
import io.github.wysohn.rapidframework3.interfaces.IMemento;
import io.github.wysohn.realeconomy.inject.annotation.MaxCapital;
import io.github.wysohn.realeconomy.inject.annotation.MinCapital;
import io.github.wysohn.realeconomy.interfaces.IFinancialEntity;
import io.github.wysohn.realeconomy.interfaces.banking.IBankOwnerProvider;
import io.github.wysohn.realeconomy.manager.asset.Asset;
import io.github.wysohn.realeconomy.manager.banking.bank.CentralBank;
import io.github.wysohn.realeconomy.manager.currency.Currency;
import io.github.wysohn.realeconomy.manager.currency.CurrencyManager;
import io.github.wysohn.realeconomy.manager.listing.OrderType;
import io.github.wysohn.realeconomy.manager.listing.TradeInfo;
import io.github.wysohn.realeconomy.manager.user.AbstractBankUser;
import io.github.wysohn.realeconomy.mediator.TradeMediator;
import org.junit.Before;
import org.junit.Test;

import java.lang.ref.WeakReference;
import java.math.BigDecimal;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

public class TransactionManagerTest extends AbstractBukkitManagerTest {
    private VisitingBankManager visitingBankManager;
    private Currency currency;
    private UUID currencyUuid;
    private CurrencyManager currencyManager;
    private final List<AbstractModule> moduleList = new LinkedList<>();
    private CentralBank serverBank;

    @Before
    public void init() {
        visitingBankManager = mock(VisitingBankManager.class);
        currency = mock(Currency.class);
        currencyUuid = UUID.randomUUID();
        currencyManager = mock(CurrencyManager.class);
        serverBank = new CentralBank(UUID.randomUUID());
        addFakeObserver(serverBank);

        moduleList.add(new AbstractModule() {
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
            CurrencyManager currencyManager() {
                return currencyManager;
            }

            @ProvidesIntoSet
            IBankOwnerProvider bankOwnerProvider() {
                return mock(IBankOwnerProvider.class);
            }
        });

        when(visitingBankManager.getUsingBank(any())).thenReturn(serverBank);
        when(currency.ownerBank()).thenReturn(serverBank);
        when(currency.getKey()).thenReturn(currencyUuid);
        when(currencyManager.get(eq(currencyUuid))).thenReturn(Optional.of(new WeakReference<>(currency)));
        Guice.createInjector(moduleList).injectMembers(serverBank);

        serverBank.setBaseCurrency(currency);
    }

    @Test
    public void send1() {
        TransactionManager mediator = new TransactionManager(visitingBankManager);

        IFinancialEntity from = mock(IFinancialEntity.class);
        IFinancialEntity to = mock(IFinancialEntity.class);

        when(from.saveState()).thenReturn(mock(IMemento.class));
        when(to.saveState()).thenReturn(mock(IMemento.class));

        assertEquals(TransactionManager.Result.FROM_WITHDRAW_REFUSED,
                mediator.send(from, to, BigDecimal.valueOf(10342.33), currency));

        verify(from, never()).deposit(any(BigDecimal.class), any(Currency.class));
        verify(from).withdraw(any(BigDecimal.class), any(Currency.class));
        verify(to, never()).deposit(any(BigDecimal.class), any(Currency.class));
        verify(to, never()).withdraw(any(BigDecimal.class), any(Currency.class));
    }

    @Test
    public void send1_1() {
        TransactionManager mediator = new TransactionManager(visitingBankManager);

        IFinancialEntity from = mock(IFinancialEntity.class);
        IFinancialEntity to = mock(IFinancialEntity.class);

        when(from.saveState()).thenReturn(mock(IMemento.class));
        when(to.saveState()).thenReturn(mock(IMemento.class));

        when(from.withdraw(any(), any())).thenReturn(true);

        assertEquals(TransactionManager.Result.TO_DEPOSIT_REFUSED,
                mediator.send(from, to, BigDecimal.valueOf(10342.33), currency));

        verify(from, never()).deposit(any(BigDecimal.class), any(Currency.class));
        verify(from).withdraw(any(BigDecimal.class), any(Currency.class));
        verify(to).deposit(any(BigDecimal.class), any(Currency.class));
        verify(to, never()).withdraw(any(BigDecimal.class), any(Currency.class));
    }

    @Test
    public void send2() {
        TransactionManager mediator = new TransactionManager(visitingBankManager);

        IFinancialEntity from = mock(IFinancialEntity.class);
        IFinancialEntity to = mock(IFinancialEntity.class);
        CentralBank bank = mock(CentralBank.class);

        IMemento fromState = mock(IMemento.class);
        when(from.saveState()).thenReturn(fromState);
        IMemento toState = mock(IMemento.class);
        when(to.saveState()).thenReturn(toState);

        when(currency.ownerBank()).thenReturn(bank);

        assertEquals(TransactionManager.Result.FROM_WITHDRAW_REFUSED,
                mediator.send(from, to, BigDecimal.valueOf(3034.88), currency));

        verify(from, never()).deposit(any(BigDecimal.class), any(Currency.class));
        verify(from, times(1)).withdraw(eq(BigDecimal.valueOf(3034.88)), eq(currency));
        verify(to, never()).deposit(any(BigDecimal.class), any(Currency.class));
        verify(to, never()).withdraw(any(BigDecimal.class), any(Currency.class));

        verify(from).restoreState(eq(fromState));
        verify(to).restoreState(eq(toState));
    }

    @Test
    public void send3() {
        TransactionManager mediator = new TransactionManager(visitingBankManager);

        IFinancialEntity from = mock(IFinancialEntity.class);
        IFinancialEntity to = mock(IFinancialEntity.class);
        CentralBank bank = mock(CentralBank.class);

        IMemento fromState = mock(IMemento.class);
        when(from.saveState()).thenReturn(fromState);
        IMemento toState = mock(IMemento.class);
        when(to.saveState()).thenReturn(toState);

        when(currency.ownerBank()).thenReturn(bank);
        when(from.withdraw(any(BigDecimal.class), any(Currency.class))).thenReturn(true);

        assertEquals(TransactionManager.Result.TO_DEPOSIT_REFUSED,
                mediator.send(from, to, BigDecimal.valueOf(20314.87), currency));

        verify(from, never()).deposit(any(BigDecimal.class), any(Currency.class));
        verify(from, times(1)).withdraw(eq(BigDecimal.valueOf(20314.87)), eq(currency));
        verify(to, times(1)).deposit(eq(BigDecimal.valueOf(20314.87)), eq(currency));
        verify(to, never()).withdraw(any(BigDecimal.class), any(Currency.class));

        verify(from).restoreState(eq(fromState));
        verify(to).restoreState(eq(toState));
    }

    @Test
    public void send4() {
        TransactionManager mediator = new TransactionManager(visitingBankManager);

        IFinancialEntity from = mock(IFinancialEntity.class);
        IFinancialEntity to = mock(IFinancialEntity.class);
        CentralBank bank = mock(CentralBank.class);

        IMemento fromState = mock(IMemento.class);
        when(from.saveState()).thenReturn(fromState);
        IMemento toState = mock(IMemento.class);
        when(to.saveState()).thenReturn(toState);

        when(currency.ownerBank()).thenReturn(bank);
        when(from.withdraw(any(BigDecimal.class), any(Currency.class))).thenReturn(true);
        when(to.deposit(any(BigDecimal.class), any(Currency.class))).thenReturn(true);

        assertEquals(TransactionManager.Result.OK,
                mediator.send(from, to, BigDecimal.valueOf(87943.44), currency));

        verify(from, never()).deposit(any(BigDecimal.class), any(Currency.class));
        verify(from, times(1))
                .withdraw(eq(BigDecimal.valueOf(87943.44)), eq(currency));
        verify(to, times(1))
                .deposit(eq(BigDecimal.valueOf(87943.44)), eq(currency));
        verify(to, never()).withdraw(any(BigDecimal.class), any(Currency.class));

        verify(from, never()).restoreState(eq(fromState));
        verify(to, never()).restoreState(eq(toState));
    }

    @Test
    public void testConcurrency() throws Exception {
        TransactionManager mediator = new TransactionManager(visitingBankManager);

        Injector injector = Guice.createInjector(moduleList);

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
        serverBank.depositAccount(user1, BankingTypeRegistry.TRADING, 500200, currency);
        serverBank.depositAccount(user2, BankingTypeRegistry.TRADING, 499800, currency);

        // user 1 sending money to bank
        Thread thread1 = new Thread(() -> {
            for (int i = 0; i < 1000; i++) {
                assertEquals(TransactionManager.Result.OK,
                        mediator.send(user1, BankingTypeRegistry.TRADING, serverBank, BigDecimal.valueOf(i + 1), currency));
            }
        });

        // user 2 sending money to bank
        Thread thread2 = new Thread(() -> {
            for (int i = 0; i < 1000; i++) {
                assertEquals(TransactionManager.Result.OK,
                        mediator.send(user2, BankingTypeRegistry.TRADING, serverBank, BigDecimal.valueOf(i + 1), currency));
            }
        });

        thread1.start();
        thread2.start();

        thread1.join();
        thread2.join();

        // (1 + 2 + ... + 999) = 499500
        assertEquals(BigDecimal.valueOf(700.0), serverBank.balanceOfAccount(user1, BankingTypeRegistry.TRADING, currency));
        assertEquals(BigDecimal.valueOf(300.0), serverBank.balanceOfAccount(user2, BankingTypeRegistry.TRADING, currency));
    }

    @Test
    public void testConcurrency2() throws Exception {
        TransactionManager mediator = new TransactionManager(visitingBankManager);

        Injector injector = Guice.createInjector(moduleList);

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

        serverBank.depositAccount(user1, BankingTypeRegistry.TRADING, 500500.0, currency);
        serverBank.depositAccount(user2, BankingTypeRegistry.TRADING, 500500.0, currency);

        // user 1 sends 125250 to user 2
        Thread thread1 = new Thread(() -> {
            for (int i = 0; i < 500; i++) {
                assertEquals(TransactionManager.Result.OK,
                        mediator.send(user1, BankingTypeRegistry.TRADING,
                                user2, BankingTypeRegistry.TRADING,
                                BigDecimal.valueOf(i + 1),
                                currency));
            }
        });

        // user 2 sends 245350 to user 1
        Thread thread2 = new Thread(() -> {
            for (int i = 0; i < 700; i++) {
                assertEquals(TransactionManager.Result.OK,
                        mediator.send(user2, BankingTypeRegistry.TRADING,
                                user1, BankingTypeRegistry.TRADING,
                                BigDecimal.valueOf(i + 1),
                                currency));
            }
        });

        thread1.start();
        thread2.start();

        thread1.join();
        thread2.join();

        // (1 + 2 + ... + 1000) = 500500
        // (1 + 2 + ... + 500) = 125250
        // (1 + 2 + ... + 700) = 245350
        assertEquals(BigDecimal.valueOf(500500.0 - 125250 + 245350), serverBank.balanceOfAccount(user1, BankingTypeRegistry.TRADING, currency));
        assertEquals(BigDecimal.valueOf(500500.0 - 245350 + 125250), serverBank.balanceOfAccount(user2, BankingTypeRegistry.TRADING, currency));
    }

    @Test
    public void testConcurrency3() throws Exception {
        TransactionManager mediator = new TransactionManager(visitingBankManager);

        Injector injector = Guice.createInjector(moduleList);

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
        serverBank.depositAccount(user1, BankingTypeRegistry.TRADING, 125250.0, currency);
        serverBank.depositAccount(user2, BankingTypeRegistry.TRADING, 125250.0, currency);

        // user 1 sending to user 2
        Thread thread1 = new Thread(() -> {
            for (int i = 0; i < 1000; i++) {
                assertEquals(TransactionManager.Result.OK,
                        mediator.send(user1, BankingTypeRegistry.TRADING, user2, BankingTypeRegistry.TRADING, BigDecimal.valueOf(i + 1), currency));
            }
        });

        // user 2 sending to user 3
        Thread thread2 = new Thread(() -> {
            for (int i = 0; i < 500; i++) {
                assertEquals(TransactionManager.Result.OK,
                        mediator.send(user2, BankingTypeRegistry.TRADING, user3, BankingTypeRegistry.TRADING, BigDecimal.valueOf(i + 1), currency));
            }
        });

        thread1.start();
        thread2.start();

        thread1.join();
        thread2.join();

        assertEquals(BigDecimal.valueOf(0.0), serverBank.balanceOfAccount(user1, BankingTypeRegistry.TRADING, currency));
        assertEquals(BigDecimal.valueOf(125250.0), serverBank.balanceOfAccount(user2, BankingTypeRegistry.TRADING, currency));
        assertEquals(BigDecimal.valueOf(125250.0), serverBank.balanceOfAccount(user3, BankingTypeRegistry.TRADING, currency));
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