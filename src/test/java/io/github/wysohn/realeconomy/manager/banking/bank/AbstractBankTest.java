package io.github.wysohn.realeconomy.manager.banking.bank;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Module;
import com.google.inject.Provides;
import io.github.wysohn.rapidframework3.bukkit.testutils.manager.AbstractBukkitManagerTest;
import io.github.wysohn.realeconomy.inject.module.BankOwnerProviderModule;
import io.github.wysohn.realeconomy.interfaces.banking.IBankOwner;
import io.github.wysohn.realeconomy.interfaces.banking.IBankOwnerProvider;
import io.github.wysohn.realeconomy.manager.currency.Currency;
import io.github.wysohn.realeconomy.manager.currency.CurrencyManager;
import org.junit.Before;
import org.junit.Test;

import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

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
        });
        moduleList.add(new BankOwnerProviderModule(provider));
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

    public static class TempBank extends AbstractBank {
        private TempBank() {
            super(null);
        }

        public TempBank(UUID key) {
            super(key);
        }
    }
}