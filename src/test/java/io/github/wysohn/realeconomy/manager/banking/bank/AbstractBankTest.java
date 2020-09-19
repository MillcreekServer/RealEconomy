package io.github.wysohn.realeconomy.manager.banking.bank;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Module;
import com.google.inject.Provides;
import io.github.wysohn.rapidframework3.bukkit.testutils.manager.AbstractBukkitManagerTest;
import io.github.wysohn.realeconomy.inject.annotation.MaxCapital;
import io.github.wysohn.realeconomy.inject.annotation.MinCapital;
import io.github.wysohn.realeconomy.inject.module.BankOwnerProviderModule;
import io.github.wysohn.realeconomy.inject.module.TransactionHandlerModule;
import io.github.wysohn.realeconomy.interfaces.IMemento;
import io.github.wysohn.realeconomy.interfaces.banking.IBankOwner;
import io.github.wysohn.realeconomy.interfaces.banking.IBankOwnerProvider;
import io.github.wysohn.realeconomy.interfaces.banking.IBankUser;
import io.github.wysohn.realeconomy.manager.banking.BankingTypeRegistry;
import io.github.wysohn.realeconomy.manager.currency.Currency;
import io.github.wysohn.realeconomy.manager.currency.CurrencyManager;
import org.junit.Before;
import org.junit.Test;

import java.math.BigDecimal;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class AbstractBankTest extends AbstractBukkitManagerTest {
    CurrencyManager currencyManager;

    List<Module> moduleList = new LinkedList<>();
    private IBankOwnerProvider provider;

    @Before
    public void init() {
        currencyManager = mock(CurrencyManager.class);
        when(currencyManager.get(any(UUID.class))).thenReturn(Optional.empty());

        provider = mock(IBankOwnerProvider.class);
        when(provider.get(any())).thenReturn(mock(IBankOwner.class));

        moduleList.add(new AbstractModule() {
            @Provides
            CurrencyManager currencyManager() {
                return currencyManager;
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
        moduleList.add(new BankOwnerProviderModule(provider));
        moduleList.add(new TransactionHandlerModule());
    }

    @Test
    public void getBankOwner() {
        AbstractBank bank = new TempBank();
        addFakeObserver(bank);
        Guice.createInjector(moduleList).injectMembers(bank);

        UUID uuid = UUID.randomUUID();
        IBankOwner owner = mock(IBankOwner.class);
        when(owner.getUuid()).thenReturn(uuid);

        bank.setBankOwner(owner);
        bank.getBankOwner();

        verify(provider).get(eq(uuid));
    }

    @Test
    public void getBaseCurrency() {
        AbstractBank bank = new TempBank();
        addFakeObserver(bank);
        Guice.createInjector(moduleList).injectMembers(bank);

        UUID uuid = UUID.randomUUID();
        Currency currency = mock(Currency.class);
        when(currency.getKey()).thenReturn(uuid);
        bank.setBaseCurrency(currency);
        bank.getBaseCurrency();

        verify(currencyManager).get(uuid);
    }

    @Test
    public void deposit() {
        AbstractBank bank = new TempBank();
        addFakeObserver(bank);
        Guice.createInjector(moduleList).injectMembers(bank);

        UUID uuid = UUID.randomUUID();
        Currency currency = mock(Currency.class);
        when(currency.getKey()).thenReturn(uuid);

        assertTrue(bank.deposit(1552.34, currency));
        assertEquals(0, bank.balance(currency).compareTo(BigDecimal.valueOf(1552.34)));
        assertTrue(bank.deposit(5000.2243, currency));
        assertEquals(0, bank.balance(currency).compareTo(BigDecimal.valueOf(1552.34 + 5000.2243)));
        assertFalse(bank.deposit(Double.MAX_VALUE, currency));
        assertEquals(0, bank.balance(currency).compareTo(BigDecimal.valueOf(1552.34 + 5000.2243)));
    }

    @Test
    public void withdraw() {
        AbstractBank bank = new TempBank();
        addFakeObserver(bank);
        Guice.createInjector(moduleList).injectMembers(bank);

        UUID uuid = UUID.randomUUID();
        Currency currency = mock(Currency.class);
        when(currency.getKey()).thenReturn(uuid);

        assertTrue(bank.withdraw(3306.34, currency));
        assertEquals(0, bank.balance(currency).compareTo(BigDecimal.valueOf(-3306.34)));
        assertTrue(bank.withdraw(1254.673, currency));
        assertEquals(0, bank.balance(currency).compareTo(BigDecimal.valueOf(-3306.34 - 1254.673)));
        assertFalse(bank.withdraw(Double.MAX_VALUE, currency));
        assertEquals(0, bank.balance(currency).compareTo(BigDecimal.valueOf(-3306.34 - 1254.673)));
    }

    @Test
    public void saveState() {
        AbstractBank bank = new TempBank();
        addFakeObserver(bank);
        Guice.createInjector(moduleList).injectMembers(bank);

        UUID uuid = UUID.randomUUID();
        Currency currency = mock(Currency.class);
        when(currency.getKey()).thenReturn(uuid);

        assertTrue(bank.deposit(123246873.11212154, currency));
        IMemento savedState = bank.saveState();
        assertTrue(bank.deposit(13453.43, currency));
        assertTrue(bank.withdraw(3354.75867, currency));
        bank.restoreState(savedState);
        assertEquals(BigDecimal.valueOf(123246873.11212154), bank.balance(currency));
    }

    @Test
    public void putAccount() {
        AbstractBank bank = new TempBank();
        addFakeObserver(bank);
        Guice.createInjector(moduleList).injectMembers(bank);

        IBankUser user = mock(IBankUser.class);
        UUID uuid = UUID.randomUUID();
        when(user.getUuid()).thenReturn(uuid);

        assertNull(bank.getAccount(user, BankingTypeRegistry.CHECKING));
        assertTrue(bank.putAccount(user, BankingTypeRegistry.CHECKING));
        assertFalse(bank.putAccount(user, BankingTypeRegistry.CHECKING));
        assertNotNull(bank.getAccount(user, BankingTypeRegistry.CHECKING));
    }

    @Test
    public void removeAccount() {
        AbstractBank bank = new TempBank();
        addFakeObserver(bank);
        Guice.createInjector(moduleList).injectMembers(bank);

        IBankUser user = mock(IBankUser.class);
        UUID uuid = UUID.randomUUID();
        when(user.getUuid()).thenReturn(uuid);

        assertNull(bank.getAccount(user, BankingTypeRegistry.CHECKING));
        assertTrue(bank.putAccount(user, BankingTypeRegistry.CHECKING));
        assertNotNull(bank.getAccount(user, BankingTypeRegistry.CHECKING));
        assertTrue(bank.removeAccount(user, BankingTypeRegistry.CHECKING));
        assertFalse(bank.removeAccount(user, BankingTypeRegistry.CHECKING));
        assertNull(bank.getAccount(user, BankingTypeRegistry.CHECKING));
    }


    public static class TempBank extends AbstractBank {
        private TempBank() {
            super(null);
        }

        public TempBank(UUID key) {
            super(key);
        }
    }
}