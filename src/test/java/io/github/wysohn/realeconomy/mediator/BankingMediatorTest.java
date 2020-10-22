package io.github.wysohn.realeconomy.mediator;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Module;
import com.google.inject.Provides;
import io.github.wysohn.realeconomy.inject.annotation.MaxCapital;
import io.github.wysohn.realeconomy.inject.annotation.MinCapital;
import io.github.wysohn.realeconomy.interfaces.IGovernment;
import io.github.wysohn.realeconomy.interfaces.banking.IAccount;
import io.github.wysohn.realeconomy.interfaces.banking.IBankUser;
import io.github.wysohn.realeconomy.manager.banking.BankingTypeRegistry;
import io.github.wysohn.realeconomy.manager.banking.CentralBankingManager;
import io.github.wysohn.realeconomy.manager.banking.bank.CentralBank;
import io.github.wysohn.realeconomy.manager.currency.Currency;
import io.github.wysohn.realeconomy.manager.currency.CurrencyManager;
import org.junit.Before;
import org.junit.Test;

import java.lang.ref.WeakReference;
import java.math.BigDecimal;
import java.util.*;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.*;

public class BankingMediatorTest {

    List<Module> moduleList = new LinkedList<>();
    private CentralBank serverBank;
    private CurrencyManager currencyManager;
    private CentralBankingManager centralBankingManager;

    @Before
    public void init() {
        serverBank = mock(CentralBank.class);
        currencyManager = mock(CurrencyManager.class);
        when(currencyManager.newCurrency(anyString(), anyString(), any())).thenReturn(CurrencyManager.Result.OK);
        centralBankingManager = mock(CentralBankingManager.class);

        moduleList.add(new AbstractModule() {
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
        });
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
                mediator.createCurrency(government, "dollar", "USD"));
        verify(currencyManager).newCurrency(eq("default"), eq("DFT"), eq(BankingMediator.getServerBank()));
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
        verify(currencyManager).newCurrency(eq("default"), eq("DFT"), eq(BankingMediator.getServerBank()));
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
                mediator.renameCurrency("dollar", "other"));
        verify(currencyManager).newCurrency(eq("default"), eq("DFT"), eq(BankingMediator.getServerBank()));
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
        when(currencyManager.get(eq("dollar")))
                .thenReturn(Optional.empty());

        assertEquals(BankingMediator.Result.NOT_FOUND,
                mediator.renameCurrency("dollar", "other"));
        verify(currencyManager).newCurrency(eq("default"), eq("DFT"), eq(BankingMediator.getServerBank()));
        verify(currencyManager).get(eq("other"));
        verify(currencyManager).get(eq("dollar"));
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
        when(currencyManager.get(eq("dollar")))
                .thenReturn(Optional.of(new WeakReference<>(currency)));

        assertEquals(BankingMediator.Result.OK,
                mediator.renameCurrency("dollar", "other"));

        verify(currencyManager).newCurrency(eq("default"), eq("DFT"), eq(BankingMediator.getServerBank()));
        verify(currencyManager).get(eq("other"));
        verify(currencyManager, times(2)).get(eq("dollar"));
        verify(currency).setStringKey("other");
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
        when(serverBank.getBaseCurrency()).thenReturn(currency);
        when(account.getCurrencyMap()).thenReturn(balances);

        assertEquals(BigDecimal.ZERO, mediator.balance(user, BankingTypeRegistry.CHECKING));
    }

    @Test
    public void deposit() throws Exception {
        BankingMediator mediator = Guice.createInjector(moduleList).getInstance(BankingMediator.class);
        when(centralBankingManager.getOrNew(any(UUID.class)))
                .thenReturn(Optional.of(new WeakReference<>(serverBank)));
        mediator.enable();

        IBankUser user = mock(IBankUser.class);

        assertEquals(BankingMediator.Result.NO_CURRENCY_SET, mediator.deposit(user,
                BankingTypeRegistry.CHECKING,
                BigDecimal.TEN));
    }

    @Test
    public void deposit2() throws Exception {
        BankingMediator mediator = Guice.createInjector(moduleList).getInstance(BankingMediator.class);
        when(centralBankingManager.getOrNew(any(UUID.class)))
                .thenReturn(Optional.of(new WeakReference<>(serverBank)));
        mediator.enable();

        IBankUser user = mock(IBankUser.class);
        UUID currencyUuid = UUID.randomUUID();
        Currency currency = mock(Currency.class);

        when(currency.getKey()).thenReturn(currencyUuid);
        when(serverBank.getBaseCurrency()).thenReturn(currency);

        assertEquals(BankingMediator.Result.NO_ACCOUNT, mediator.deposit(user,
                BankingTypeRegistry.CHECKING,
                BigDecimal.TEN));
    }

    @Test
    public void deposit3() throws Exception {
        BankingMediator mediator = Guice.createInjector(moduleList).getInstance(BankingMediator.class);
        when(centralBankingManager.getOrNew(any(UUID.class)))
                .thenReturn(Optional.of(new WeakReference<>(serverBank)));
        mediator.enable();

        IBankUser user = mock(IBankUser.class);
        UUID currencyUuid = UUID.randomUUID();
        Currency currency = mock(Currency.class);
        IAccount account = mock(IAccount.class);
        Map<UUID, BigDecimal> balances = new HashMap<>();

        when(currency.getKey()).thenReturn(currencyUuid);
        when(serverBank.getBaseCurrency()).thenReturn(currency);
        when(serverBank.hasAccount(eq(user), eq(BankingTypeRegistry.CHECKING))).thenReturn(true);
        when(account.getCurrencyMap()).thenReturn(balances);

        mediator.deposit(user,
                BankingTypeRegistry.CHECKING,
                BigDecimal.TEN);
        verify(serverBank).depositAccount(eq(user),
                eq(BankingTypeRegistry.CHECKING),
                eq(BigDecimal.TEN),
                eq(currency));
    }

    @Test
    public void withdraw() throws Exception {
        BankingMediator mediator = Guice.createInjector(moduleList).getInstance(BankingMediator.class);
        when(centralBankingManager.getOrNew(any(UUID.class)))
                .thenReturn(Optional.of(new WeakReference<>(serverBank)));
        mediator.enable();

        IBankUser user = mock(IBankUser.class);

        assertEquals(BankingMediator.Result.NO_CURRENCY_SET, mediator.withdraw(user,
                BankingTypeRegistry.CHECKING,
                BigDecimal.TEN));
    }

    @Test
    public void withdraw2() throws Exception {
        BankingMediator mediator = Guice.createInjector(moduleList).getInstance(BankingMediator.class);
        when(centralBankingManager.getOrNew(any(UUID.class)))
                .thenReturn(Optional.of(new WeakReference<>(serverBank)));
        mediator.enable();

        IBankUser user = mock(IBankUser.class);
        UUID currencyUuid = UUID.randomUUID();
        Currency currency = mock(Currency.class);

        when(currency.getKey()).thenReturn(currencyUuid);
        when(serverBank.getBaseCurrency()).thenReturn(currency);

        assertEquals(BankingMediator.Result.NO_ACCOUNT, mediator.withdraw(user,
                BankingTypeRegistry.CHECKING,
                BigDecimal.TEN));
    }

    @Test
    public void withdraw3() throws Exception {
        BankingMediator mediator = Guice.createInjector(moduleList).getInstance(BankingMediator.class);
        when(centralBankingManager.getOrNew(any(UUID.class)))
                .thenReturn(Optional.of(new WeakReference<>(serverBank)));
        mediator.enable();

        IBankUser user = mock(IBankUser.class);
        UUID currencyUuid = UUID.randomUUID();
        Currency currency = mock(Currency.class);
        IAccount account = mock(IAccount.class);
        Map<UUID, BigDecimal> balances = new HashMap<>();

        when(currency.getKey()).thenReturn(currencyUuid);
        when(serverBank.getBaseCurrency()).thenReturn(currency);
        when(serverBank.hasAccount(eq(user), eq(BankingTypeRegistry.CHECKING))).thenReturn(true);
        when(account.getCurrencyMap()).thenReturn(balances);

        mediator.withdraw(user,
                BankingTypeRegistry.CHECKING,
                BigDecimal.TEN);
        verify(serverBank).withdrawAccount(eq(user),
                eq(BankingTypeRegistry.CHECKING),
                eq(BigDecimal.TEN),
                eq(currency));
    }
}