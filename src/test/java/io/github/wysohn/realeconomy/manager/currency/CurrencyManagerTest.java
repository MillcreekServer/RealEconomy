package io.github.wysohn.realeconomy.manager.currency;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Module;
import com.google.inject.Provides;
import io.github.wysohn.rapidframework3.bukkit.testutils.manager.AbstractBukkitManagerTest;
import io.github.wysohn.rapidframework3.core.inject.module.PluginInfoModule;
import io.github.wysohn.rapidframework3.interfaces.serialize.ISerializer;
import io.github.wysohn.rapidframework3.testmodules.*;
import io.github.wysohn.rapidframework3.utils.Pair;
import io.github.wysohn.realeconomy.manager.banking.CentralBankingManager;
import io.github.wysohn.realeconomy.manager.banking.bank.CentralBank;
import org.junit.Before;
import org.junit.Test;
import org.powermock.core.classloader.annotations.PowerMockIgnore;

import java.util.LinkedList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;

@PowerMockIgnore("jdk.internal.reflect.*")
public class CurrencyManagerTest extends AbstractBukkitManagerTest {

    List<Module> moduleList = new LinkedList<>();
    private MockMainModule mainModule;
    private ISerializer mockSerializer;
    private CentralBankingManager centralBankingManager;

    @Before
    public void init() {
        mockSerializer = mock(ISerializer.class);
        centralBankingManager = mock(CentralBankingManager.class);

        moduleList.add(new PluginInfoModule("test", "test", "test"));
        moduleList.add(new MockLoggerModule());
        moduleList.add(new MockConfigModule(Pair.of(CurrencyManager.KEY_MAX_LEN, 3)));
        moduleList.add(new MockPluginDirectoryModule());
        moduleList.add(new MockSerializerModule(mockSerializer));
        moduleList.add(new MockShutdownModule(() -> {

        }));
        moduleList.add(new AbstractModule() {
            @Provides
            CentralBankingManager centralBankingManager() {
                return centralBankingManager;
            }
        });
    }

    @Test
    public void newCurrency() throws Exception {
        CurrencyManager manager = Guice.createInjector(moduleList).getInstance(CurrencyManager.class);
        manager.enable();

        CentralBank centralBank = mock(CentralBank.class);

        assertEquals(CurrencyManager.Result.CODE_LENGTH, manager.newCurrency("blahblah", "ABCDE", centralBank));
        manager.disable();
    }

    @Test
    public void newCurrency2() throws Exception {
        CurrencyManager manager = Guice.createInjector(moduleList).getInstance(CurrencyManager.class);
        manager.enable();

        CentralBank centralBank = mock(CentralBank.class);

        assertEquals(CurrencyManager.Result.OK, manager.newCurrency("dollar", "USD", centralBank));
        assertEquals(CurrencyManager.Result.DUP_CODE, manager.newCurrency("hoho", "USD", centralBank));
        manager.disable();
    }

    @Test
    public void newCurrency3() throws Exception {
        CurrencyManager manager = Guice.createInjector(moduleList).getInstance(CurrencyManager.class);
        manager.enable();

        CentralBank centralBank = mock(CentralBank.class);

        assertEquals(CurrencyManager.Result.OK, manager.newCurrency("dollar", "USD", centralBank));
        assertEquals(CurrencyManager.Result.DUP_NAME, manager.newCurrency("dollar", "ABC", centralBank));
        manager.disable();
    }

    @Test
    public void newCurrency4() throws Exception {
        CurrencyManager manager = Guice.createInjector(moduleList).getInstance(CurrencyManager.class);
        manager.enable();

        CentralBank centralBank = mock(CentralBank.class);

        assertEquals(CurrencyManager.Result.OK, manager.newCurrency("berk", "BRK", centralBank));
        assertEquals(CurrencyManager.Result.OK, manager.newCurrency("dollar", "USD", centralBank));
        manager.disable();
    }
}