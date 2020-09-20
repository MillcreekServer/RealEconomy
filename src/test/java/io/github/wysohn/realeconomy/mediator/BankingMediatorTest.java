package io.github.wysohn.realeconomy.mediator;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Module;
import com.google.inject.Provides;
import io.github.wysohn.realeconomy.inject.annotation.ServerBank;
import io.github.wysohn.realeconomy.inject.module.MaxCapitalModule;
import io.github.wysohn.realeconomy.interfaces.IGovernment;
import io.github.wysohn.realeconomy.manager.banking.CentralBankingManager;
import io.github.wysohn.realeconomy.manager.banking.bank.CentralBank;
import io.github.wysohn.realeconomy.manager.currency.Currency;
import io.github.wysohn.realeconomy.manager.currency.CurrencyManager;
import org.junit.Before;
import org.junit.Test;

import java.lang.ref.WeakReference;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

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
        when(currencyManager.newCurrency(anyString(), anyString())).thenReturn(CurrencyManager.Result.OK);
        centralBankingManager = mock(CentralBankingManager.class);

        moduleList.add(new AbstractModule() {
            @Provides
            @ServerBank
            CentralBank centralBank() {
                return serverBank;
            }
        });
        moduleList.add(new MaxCapitalModule());
        moduleList.add(new AbstractModule() {
            @Provides
            CurrencyManager currencyManager() {
                return currencyManager;
            }

            @Provides
            CentralBankingManager centralBankingManager() {
                return centralBankingManager;
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
    }

    @Test
    public void deposit() {
    }

    @Test
    public void withdraw() {
    }
}