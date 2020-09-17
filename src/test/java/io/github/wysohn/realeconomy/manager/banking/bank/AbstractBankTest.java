package io.github.wysohn.realeconomy.manager.banking.bank;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Module;
import com.google.inject.Provides;
import io.github.wysohn.realeconomy.inject.module.BankOwnerProviderModule;
import io.github.wysohn.realeconomy.manager.currency.CurrencyManager;
import org.junit.Before;
import org.junit.Test;

import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.mock;

public class AbstractBankTest {
    CurrencyManager currencyManager;

    List<Module> moduleList = new LinkedList<>();

    @Before
    public void init() {
        currencyManager = mock(CurrencyManager.class);

        moduleList.add(new AbstractModule() {
            @Provides
            CurrencyManager currencyManager() {
                return currencyManager;
            }
        });
        moduleList.add(new BankOwnerProviderModule());
    }

    @Test
    public void getBankOwner() {
        AbstractBank bank = new TempBank();
        Guice.createInjector(moduleList).injectMembers(bank);
    }

    @Test
    public void getBaseCurrency() {
        AbstractBank bank = new TempBank();
        Guice.createInjector(moduleList).injectMembers(bank);
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