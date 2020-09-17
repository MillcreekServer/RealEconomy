package io.github.wysohn.realeconomy.manager.currency;

import com.google.inject.Guice;
import com.google.inject.Module;
import io.github.wysohn.rapidframework3.bukkit.testutils.manager.AbstractBukkitManagerTest;
import io.github.wysohn.rapidframework3.interfaces.serialize.ISerializer;
import io.github.wysohn.rapidframework3.testmodules.MockConfigModule;
import io.github.wysohn.rapidframework3.testmodules.MockMainModule;
import io.github.wysohn.rapidframework3.testmodules.MockPluginDirectoryModule;
import io.github.wysohn.rapidframework3.testmodules.MockSerializerModule;
import io.github.wysohn.rapidframework3.utils.Pair;
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

    @Before
    public void init() {
        mockSerializer = mock(ISerializer.class);

        moduleList.add(new MockMainModule());
        moduleList.add(new MockSerializerModule(mockSerializer));
        moduleList.add(new MockPluginDirectoryModule());
        moduleList.add(new MockConfigModule(Pair.of(CurrencyManager.KEY_MAX_LEN, 3)));
    }

    @Test
    public void newCurrency() throws Exception {
        CurrencyManager manager = Guice.createInjector(moduleList).getInstance(CurrencyManager.class);
        manager.enable();

        assertEquals(CurrencyManager.Result.CODE_LENGTH, manager.newCurrency("blahblah", "ABCDE"));
        manager.disable();
    }

    @Test
    public void newCurrency2() throws Exception {
        CurrencyManager manager = Guice.createInjector(moduleList).getInstance(CurrencyManager.class);
        manager.enable();

        assertEquals(CurrencyManager.Result.OK, manager.newCurrency("dollar", "USD"));
        assertEquals(CurrencyManager.Result.DUP_CODE, manager.newCurrency("hoho", "USD"));
        manager.disable();
    }

    @Test
    public void newCurrency3() throws Exception {
        CurrencyManager manager = Guice.createInjector(moduleList).getInstance(CurrencyManager.class);
        manager.enable();

        assertEquals(CurrencyManager.Result.OK, manager.newCurrency("dollar", "USD"));
        assertEquals(CurrencyManager.Result.DUP_NAME, manager.newCurrency("dollar", "ABC"));
        manager.disable();
    }

    @Test
    public void newCurrency4() throws Exception {
        CurrencyManager manager = Guice.createInjector(moduleList).getInstance(CurrencyManager.class);
        manager.enable();

        assertEquals(CurrencyManager.Result.OK, manager.newCurrency("berk", "BRK"));
        assertEquals(CurrencyManager.Result.DUP_NAME, manager.newCurrency("dollar", "USD"));
        manager.disable();
    }
}