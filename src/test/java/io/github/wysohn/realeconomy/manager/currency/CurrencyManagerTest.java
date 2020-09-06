package io.github.wysohn.realeconomy.manager.currency;

import io.github.wysohn.rapidframework2.bukkit.testutils.manager.AbstractBukkitManagerTest;
import io.github.wysohn.rapidframework2.bukkit.testutils.manager.ManagerTestBuilder2;
import io.github.wysohn.rapidframework2.core.database.Database;
import io.github.wysohn.rapidframework2.core.main.PluginMain;
import org.junit.Test;
import org.powermock.core.classloader.annotations.PowerMockIgnore;

import java.lang.reflect.InvocationTargetException;
import java.util.function.Consumer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

@PowerMockIgnore("jdk.internal.reflect.*")
public class CurrencyManagerTest extends AbstractBukkitManagerTest {
    @Test
    public void newCurrency() throws InvocationTargetException {
        CurrencyManager manager = new CurrencyManager(PluginMain.Manager.NORM_PRIORITY);
        assertTrue(ManagerTestBuilder2.of(manager)
                .config(CurrencyManager.KEY_MAX_LEN, 3)
                .mock((Consumer<CurrencyManager>) currencyManager -> assertEquals(CurrencyManager.Result.CODE_LENGTH,
                        currencyManager.newCurrency("blahblah", "ABCDE")))
                .test());
    }

    @Test
    public void newCurrency2() throws Exception {
        CurrencyManager manager = new CurrencyManager(PluginMain.Manager.NORM_PRIORITY);

        assertTrue(ManagerTestBuilder2.of(manager)
                .config(CurrencyManager.KEY_MAX_LEN, 3)
                .config("dbType", "file")
                .enable()
                .mock((Consumer<CurrencyManager>) currencyManager -> assertEquals(CurrencyManager.Result.OK,
                        currencyManager.newCurrency("dollar", "USD")))
                .mock((Consumer<CurrencyManager>) currencyManager -> assertEquals(CurrencyManager.Result.DUP_CODE,
                        currencyManager.newCurrency("hoho", "USD")))
                .disable()
                .test());
    }

    @Test
    public void newCurrency3() throws Exception {
        CurrencyManager manager = new CurrencyManager(PluginMain.Manager.NORM_PRIORITY);

        assertTrue(ManagerTestBuilder2.of(manager)
                .config(CurrencyManager.KEY_MAX_LEN, 3)
                .config("dbType", "file")
                .enable()
                .mock((Consumer<CurrencyManager>) currencyManager -> assertEquals(CurrencyManager.Result.OK,
                        currencyManager.newCurrency("dollar", "USD")))
                .mock((Consumer<CurrencyManager>) currencyManager -> assertEquals(CurrencyManager.Result.DUP_NAME,
                        currencyManager.newCurrency("dollar", "ABC")))
                .test());
    }

    @Test
    public void newCurrency4() throws InvocationTargetException {
        CurrencyManager manager = new CurrencyManager(PluginMain.Manager.NORM_PRIORITY);
        Database<Currency> mockDb = mock(Database.class);

        assertTrue(ManagerTestBuilder2.of(manager)
                .mock((Consumer<CurrencyManager>) currencyManager -> assertEquals(CurrencyManager.Result.OK,
                        currencyManager.newCurrency("berk", "BRK")))
                .config(CurrencyManager.KEY_MAX_LEN, 3)
                .mock((Consumer<CurrencyManager>) currencyManager -> assertEquals(CurrencyManager.Result.OK,
                        currencyManager.newCurrency("dollar", "USD")))
                .test());
    }
}