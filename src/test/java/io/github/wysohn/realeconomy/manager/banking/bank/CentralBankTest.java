package io.github.wysohn.realeconomy.manager.banking.bank;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Module;
import com.google.inject.Provides;
import io.github.wysohn.rapidframework3.bukkit.testutils.manager.AbstractBukkitManagerTest;
import io.github.wysohn.rapidframework3.interfaces.language.ILang;
import io.github.wysohn.realeconomy.inject.annotation.MaxCapital;
import io.github.wysohn.realeconomy.inject.annotation.MinCapital;
import io.github.wysohn.realeconomy.inject.module.BankOwnerProviderModule;
import io.github.wysohn.realeconomy.interfaces.banking.IBankOwner;
import io.github.wysohn.realeconomy.interfaces.banking.IBankOwnerProvider;
import io.github.wysohn.realeconomy.main.RealEconomyLangs;
import io.github.wysohn.realeconomy.manager.currency.Currency;
import io.github.wysohn.realeconomy.manager.currency.CurrencyManager;
import org.junit.Before;
import org.junit.Test;

import java.lang.ref.WeakReference;
import java.math.BigDecimal;
import java.util.*;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class CentralBankTest extends AbstractBukkitManagerTest {
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
    }

    @Test
    public void deposit() {
        UUID uuid = UUID.randomUUID();
        CentralBank bank = new CentralBank(uuid);
        addFakeObserver(bank);
        Guice.createInjector(moduleList).injectMembers(bank);

        UUID currencyUuid = UUID.randomUUID();
        Currency currency = mock(Currency.class);
        when(currency.getKey()).thenReturn(currencyUuid);
        when(currencyManager.get(eq(currencyUuid))).thenReturn(Optional.of(new WeakReference<>(currency)));
        bank.setBaseCurrency(currency);

        bank.deposit(BigDecimal.valueOf(20304.33), currency);
        // liquidity decreases as currency is collected
        assertEquals(BigDecimal.valueOf(-20304.33), bank.getLiquidity());
        // always max for base currency
        assertEquals(BigDecimal.valueOf(Double.MAX_VALUE), bank.balance(currency));
    }

    @Test
    public void depositNonBase() {
        UUID uuid = UUID.randomUUID();
        CentralBank bank = new CentralBank(uuid);
        addFakeObserver(bank);
        Guice.createInjector(moduleList).injectMembers(bank);

        UUID baseCurrencyUuid = UUID.randomUUID();
        Currency baseCurrency = mock(Currency.class);
        when(baseCurrency.getKey()).thenReturn(baseCurrencyUuid);
        bank.setBaseCurrency(baseCurrency);

        UUID currencyUuid = UUID.randomUUID();
        Currency currency = mock(Currency.class);
        when(currency.getKey()).thenReturn(currencyUuid);
        when(currencyManager.get(eq(currencyUuid))).thenReturn(Optional.of(new WeakReference<>(currency)));

        bank.deposit(BigDecimal.valueOf(20304.33), currency);
        assertEquals(BigDecimal.ZERO, bank.getLiquidity());
    }

    @Test
    public void withdraw() {
        UUID uuid = UUID.randomUUID();
        CentralBank bank = new CentralBank(uuid);
        addFakeObserver(bank);
        Guice.createInjector(moduleList).injectMembers(bank);

        UUID currencyUuid = UUID.randomUUID();
        Currency currency = mock(Currency.class);
        when(currency.getKey()).thenReturn(currencyUuid);
        when(currencyManager.get(eq(currencyUuid))).thenReturn(Optional.of(new WeakReference<>(currency)));
        bank.setBaseCurrency(currency);
        bank.setNumPapers(BigDecimal.valueOf(10000000));

        bank.withdraw(BigDecimal.valueOf(30567.22), currency);
        // liquidity increases as currency is created
        assertEquals(BigDecimal.valueOf(30567.22), bank.getLiquidity());
        // printing new currency cost paper per currency
        assertEquals(BigDecimal.valueOf(10000000 - 30567.22), bank.getNumPapers());
        // always max for base currency
        assertEquals(BigDecimal.valueOf(Double.MAX_VALUE), bank.balance(currency));
    }

    @Test
    public void withdrawNonBase() {
        UUID uuid = UUID.randomUUID();
        CentralBank bank = new CentralBank(uuid);
        addFakeObserver(bank);
        Guice.createInjector(moduleList).injectMembers(bank);

        UUID baseCurrencyUuid = UUID.randomUUID();
        Currency baseCurrency = mock(Currency.class);
        when(baseCurrency.getKey()).thenReturn(baseCurrencyUuid);
        bank.setBaseCurrency(baseCurrency);

        UUID currencyUuid = UUID.randomUUID();
        Currency currency = mock(Currency.class);
        when(currency.getKey()).thenReturn(currencyUuid);
        when(currencyManager.get(eq(currencyUuid))).thenReturn(Optional.of(new WeakReference<>(currency)));

        bank.withdraw(BigDecimal.valueOf(30567.22), currency);
        assertEquals(BigDecimal.valueOf(0), bank.getLiquidity());
    }

    @Test
    public void properties() {
        UUID uuid = UUID.randomUUID();
        CentralBank bank = new CentralBank(uuid);
        addFakeObserver(bank);
        Guice.createInjector(moduleList).injectMembers(bank);

        IBankOwner owner = mock(IBankOwner.class);
        UUID ownerUuid = UUID.randomUUID();
        when(owner.getUuid()).thenReturn(ownerUuid);

        UUID currencyUuid = UUID.randomUUID();
        Currency currency = mock(Currency.class);
        when(currency.getKey()).thenReturn(currencyUuid);
        when(currencyManager.get(eq(currencyUuid))).thenReturn(Optional.of(new WeakReference<>(currency)));

        bank.setBankOwner(owner);
        bank.setBaseCurrency(currency);

        Map<ILang, Object> properties = bank.properties();
        assertTrue(properties.containsKey(RealEconomyLangs.Bank_Owner));
        assertTrue(properties.containsKey(RealEconomyLangs.Bank_BaseCurrency));
        assertTrue(properties.containsKey(RealEconomyLangs.Bank_NumAccounts));
        assertTrue(properties.containsKey(RealEconomyLangs.Bank_Liquidity));
    }
}