package io.github.wysohn.realeconomy.manager.banking.bank;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Module;
import com.google.inject.Provides;
import io.github.wysohn.rapidframework3.bukkit.testutils.manager.AbstractBukkitManagerTest;
import io.github.wysohn.rapidframework3.interfaces.IMemento;
import io.github.wysohn.realeconomy.inject.annotation.MaxCapital;
import io.github.wysohn.realeconomy.inject.annotation.MinCapital;
import io.github.wysohn.realeconomy.inject.module.BankOwnerProviderModule;
import io.github.wysohn.realeconomy.inject.module.TransactionHandlerModule;
import io.github.wysohn.realeconomy.interfaces.banking.IBankOwner;
import io.github.wysohn.realeconomy.interfaces.banking.IBankOwnerProvider;
import io.github.wysohn.realeconomy.interfaces.banking.IBankUser;
import io.github.wysohn.realeconomy.interfaces.banking.ITransactionHandler;
import io.github.wysohn.realeconomy.interfaces.currency.ICurrencyOwnerProvider;
import io.github.wysohn.realeconomy.manager.banking.BankingTypeRegistry;
import io.github.wysohn.realeconomy.manager.currency.Currency;
import io.github.wysohn.realeconomy.manager.currency.CurrencyManager;
import modules.MockTransactionHandlerModule;
import org.junit.Before;
import org.junit.Test;

import java.lang.ref.WeakReference;
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
    private ITransactionHandler transactionHandler;
    private ICurrencyOwnerProvider currencyOwnerProvider;

    @Before
    public void init() {
        currencyManager = mock(CurrencyManager.class);
        when(currencyManager.get(any(UUID.class))).thenReturn(Optional.empty());

        provider = mock(IBankOwnerProvider.class);
        when(provider.get(any())).thenReturn(mock(IBankOwner.class));

        transactionHandler = mock(ITransactionHandler.class);
        currencyOwnerProvider = mock(ICurrencyOwnerProvider.class);

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

            @Provides
            ICurrencyOwnerProvider currencyOwnerProvider() {
                return currencyOwnerProvider;
            }
        });
        moduleList.add(new BankOwnerProviderModule(provider));
    }

    @Test
    public void getBankOwner() {
        moduleList.add(new MockTransactionHandlerModule());

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
        moduleList.add(new MockTransactionHandlerModule());

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
    public void saveState() {
        moduleList.add(new TransactionHandlerModule());

        AbstractBank bank = new TempBank();
        addFakeObserver(bank);
        Guice.createInjector(moduleList).injectMembers(bank);

        UUID uuid = UUID.randomUUID();
        Currency currency = mock(Currency.class);
        when(currency.getKey()).thenReturn(uuid);
        when(transactionHandler.deposit(anyMap(), any(), any())).thenReturn(true);
        when(transactionHandler.withdraw(anyMap(), any(), any())).thenReturn(true);

        assertTrue(bank.deposit(123246873.11212154, currency));
        IMemento savedState = bank.saveState();
        assertTrue(bank.deposit(13453.43, currency));
        assertTrue(bank.withdraw(3354.75867, currency));
        bank.restoreState(savedState);
        assertEquals(BigDecimal.valueOf(123246873.11212154), bank.balance(currency));
    }

    @Test
    public void putAccount() {
        moduleList.add(new MockTransactionHandlerModule());

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
        moduleList.add(new MockTransactionHandlerModule());

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

    @Test
    public void accountTransaction() {
        moduleList.add(new TransactionHandlerModule());

        AbstractBank bank = new TempBank();
        addFakeObserver(bank);
        Guice.createInjector(moduleList).injectMembers(bank);

        IBankUser user = mock(IBankUser.class);
        UUID uuid = UUID.randomUUID();
        when(user.getUuid()).thenReturn(uuid);
        UUID currencyUuid = UUID.randomUUID();
        Currency currency = mock(Currency.class);
        when(currency.getKey()).thenReturn(currencyUuid);
        when(currencyManager.get(eq(currencyUuid))).thenReturn(Optional.of(new WeakReference<>(currency)));

        bank.setBaseCurrency(currency);
        assertTrue(bank.putAccount(user, BankingTypeRegistry.CHECKING));
        bank.accountTransaction(user, BankingTypeRegistry.CHECKING)
                .deposit(39800.55)
                .withdraw(2552.34)
                .commit();
        assertEquals(BigDecimal.valueOf(39800.55).subtract(BigDecimal.valueOf(2552.34)),
                bank.getAccount(user, BankingTypeRegistry.CHECKING).getCurrencyMap().get(currencyUuid));
    }

    @Test
    public void accountTransactionFailure() {
        moduleList.add(new TransactionHandlerModule());

        AbstractBank bank = new TempBank();
        addFakeObserver(bank);
        Guice.createInjector(moduleList).injectMembers(bank);

        IBankUser user = mock(IBankUser.class);
        UUID uuid = UUID.randomUUID();
        when(user.getUuid()).thenReturn(uuid);
        UUID currencyUuid = UUID.randomUUID();
        Currency currency = mock(Currency.class);
        when(currency.getKey()).thenReturn(currencyUuid);
        when(currencyManager.get(eq(currencyUuid))).thenReturn(Optional.of(new WeakReference<>(currency)));

        bank.setBaseCurrency(currency);
        assertTrue(bank.putAccount(user, BankingTypeRegistry.CHECKING));
        bank.accountTransaction(user, BankingTypeRegistry.CHECKING)
                .deposit(10.0)
                .commit();
        bank.accountTransaction(user, BankingTypeRegistry.CHECKING)
                .deposit(39800.55)
                .withdraw(2552.34)
                .deposit(BigDecimal.valueOf(Double.MAX_VALUE)) // max limit
                .commit();
        // transaction failure should revert account back to original state
        assertEquals(BigDecimal.valueOf(10.0),
                bank.getAccount(user, BankingTypeRegistry.CHECKING)
                        .getCurrencyMap()
                        .get(currencyUuid));
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