package io.github.wysohn.realeconomy.mediator;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Module;
import com.google.inject.Provides;
import io.github.wysohn.realeconomy.inject.annotation.MaxCapital;
import io.github.wysohn.realeconomy.inject.annotation.MinCapital;
import io.github.wysohn.realeconomy.inject.annotation.ServerBank;
import io.github.wysohn.realeconomy.interfaces.IGovernment;
import io.github.wysohn.realeconomy.interfaces.banking.IAccount;
import io.github.wysohn.realeconomy.interfaces.banking.IBankUser;
import io.github.wysohn.realeconomy.interfaces.banking.ITransactionHandler;
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
    private ITransactionHandler transactionHandler;

    @Before
    public void init() {
        serverBank = mock(CentralBank.class);
        currencyManager = mock(CurrencyManager.class);
        when(currencyManager.newCurrency(anyString(), anyString())).thenReturn(CurrencyManager.Result.OK);
        centralBankingManager = mock(CentralBankingManager.class);
        transactionHandler = mock(ITransactionHandler.class);

        moduleList.add(new AbstractModule() {
            @Provides
            @ServerBank
            CentralBank centralBank() {
                return serverBank;
            }
        });
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

            @Provides
            ITransactionHandler transactionHandler() {
                return transactionHandler;
            }
        });
    }

    @Test
    public void createCurrency() {
        BankingMediator mediator = Guice.createInjector(moduleList).getInstance(BankingMediator.class);

        UUID uuid = UUID.randomUUID();
        IGovernment government = mock(IGovernment.class);
        Currency currency = mock(Currency.class);
        when(government.getUuid()).thenReturn(uuid);
        when(currencyManager.get(anyString())).thenReturn(Optional.of(new WeakReference<>(currency)));

        when(centralBankingManager.get(any(UUID.class)))
                .thenReturn(Optional.of(new WeakReference<>(mock(CentralBank.class))));
        assertEquals(BankingMediator.Result.ALREADY_SET,
                mediator.createCurrency(government, "dollar", "USD"));
        verify(currencyManager).newCurrency(eq("dollar"), eq("USD"));
    }

    @Test
    public void createCurrency2() {
        BankingMediator mediator = Guice.createInjector(moduleList).getInstance(BankingMediator.class);

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
        verify(currencyManager).newCurrency(eq("dollar"), eq("USD"));
        verify(centralBankingManager).getOrNew(eq(uuid));
    }

    @Test
    public void renameCurrency() {
        BankingMediator mediator = Guice.createInjector(moduleList).getInstance(BankingMediator.class);

        when(currencyManager.get(anyString()))
                .thenReturn(Optional.of(new WeakReference<>(mock(Currency.class))));
        assertEquals(BankingMediator.Result.DUP_NAME,
                mediator.renameCurrency("dollar", "other"));
        verify(currencyManager).get(eq("other"));
    }

    @Test
    public void renameCurrency2() {
        BankingMediator mediator = Guice.createInjector(moduleList).getInstance(BankingMediator.class);

        when(currencyManager.get(eq("other")))
                .thenReturn(Optional.empty());
        when(currencyManager.get(eq("dollar")))
                .thenReturn(Optional.empty());
        assertEquals(BankingMediator.Result.NOT_FOUND,
                mediator.renameCurrency("dollar", "other"));
        verify(currencyManager).get(eq("other"));
        verify(currencyManager).get(eq("dollar"));
    }

    @Test
    public void renameCurrency3() {
        BankingMediator mediator = Guice.createInjector(moduleList).getInstance(BankingMediator.class);

        Currency currency = mock(Currency.class);
        when(currencyManager.get(eq("other")))
                .thenReturn(Optional.empty());
        when(currencyManager.get(eq("dollar")))
                .thenReturn(Optional.of(new WeakReference<>(currency)));
        assertEquals(BankingMediator.Result.OK,
                mediator.renameCurrency("dollar", "other"));
        verify(currencyManager).get(eq("other"));
        verify(currencyManager, times(2)).get(eq("dollar"));
        verify(currency).setStringKey("other");
    }

    @Test
    public void balance() {
        BankingMediator mediator = Guice.createInjector(moduleList).getInstance(BankingMediator.class);

        UUID uuid = UUID.randomUUID();
        UUID currencyUuid = UUID.randomUUID();
        IBankUser user = mock(IBankUser.class);
        IAccount account = mock(IAccount.class);
        Currency currency = mock(Currency.class);
        Map<UUID, BigDecimal> balances = mock(Map.class);

        when(user.getUuid()).thenReturn(uuid);
        when(currency.getKey()).thenReturn(currencyUuid);
        when(serverBank.getBaseCurrency()).thenReturn(currency);
        when(serverBank.getAccount(user, BankingTypeRegistry.CHECKING)).thenReturn(account);
        when(account.getBalanceMap()).thenReturn(balances);

        mediator.balance(user, BankingTypeRegistry.CHECKING);

        verify(balances).get(currencyUuid);
    }

    @Test
    public void deposit() {
        BankingMediator mediator = Guice.createInjector(moduleList).getInstance(BankingMediator.class);

        IBankUser user = mock(IBankUser.class);

        assertEquals(BankingMediator.Result.NO_CURRENCY_SET, mediator.deposit(user,
                BankingTypeRegistry.CHECKING,
                BigDecimal.TEN));
    }

    @Test
    public void deposit2() {
        BankingMediator mediator = Guice.createInjector(moduleList).getInstance(BankingMediator.class);

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
    public void deposit3() {
        BankingMediator mediator = Guice.createInjector(moduleList).getInstance(BankingMediator.class);

        IBankUser user = mock(IBankUser.class);
        UUID currencyUuid = UUID.randomUUID();
        Currency currency = mock(Currency.class);
        IAccount account = mock(IAccount.class);
        Map<UUID, BigDecimal> balances = mock(Map.class);

        when(currency.getKey()).thenReturn(currencyUuid);
        when(serverBank.getBaseCurrency()).thenReturn(currency);
        when(serverBank.getAccount(user, BankingTypeRegistry.CHECKING)).thenReturn(account);
        when(account.getBalanceMap()).thenReturn(balances);

        when(transactionHandler.deposit(anyMap(), any(), any())).thenReturn(true);
        assertEquals(BankingMediator.Result.OK, mediator.deposit(user,
                BankingTypeRegistry.CHECKING,
                BigDecimal.TEN));

        when(transactionHandler.deposit(anyMap(), any(), any())).thenReturn(false);
        assertEquals(BankingMediator.Result.FAIL_DEPOSIT, mediator.deposit(user,
                BankingTypeRegistry.CHECKING,
                BigDecimal.TEN));
    }

    @Test
    public void withdraw() {
        BankingMediator mediator = Guice.createInjector(moduleList).getInstance(BankingMediator.class);

        IBankUser user = mock(IBankUser.class);

        assertEquals(BankingMediator.Result.NO_CURRENCY_SET, mediator.withdraw(user,
                BankingTypeRegistry.CHECKING,
                BigDecimal.TEN));
    }

    @Test
    public void withdraw2() {
        BankingMediator mediator = Guice.createInjector(moduleList).getInstance(BankingMediator.class);

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
    public void withdraw3() {
        BankingMediator mediator = Guice.createInjector(moduleList).getInstance(BankingMediator.class);

        IBankUser user = mock(IBankUser.class);
        UUID currencyUuid = UUID.randomUUID();
        Currency currency = mock(Currency.class);
        IAccount account = mock(IAccount.class);
        Map<UUID, BigDecimal> balances = mock(Map.class);

        when(currency.getKey()).thenReturn(currencyUuid);
        when(serverBank.getBaseCurrency()).thenReturn(currency);
        when(serverBank.getAccount(user, BankingTypeRegistry.CHECKING)).thenReturn(account);
        when(account.getBalanceMap()).thenReturn(balances);

        when(transactionHandler.withdraw(anyMap(), any(), any())).thenReturn(true);
        assertEquals(BankingMediator.Result.OK, mediator.withdraw(user,
                BankingTypeRegistry.CHECKING,
                BigDecimal.TEN));

        when(transactionHandler.withdraw(anyMap(), any(), any())).thenReturn(false);
        assertEquals(BankingMediator.Result.FAIL_WITHDRAW, mediator.withdraw(user,
                BankingTypeRegistry.CHECKING,
                BigDecimal.TEN));
    }
}