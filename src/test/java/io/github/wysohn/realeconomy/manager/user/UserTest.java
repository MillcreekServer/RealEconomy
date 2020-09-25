package io.github.wysohn.realeconomy.manager.user;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Module;
import com.google.inject.Provides;
import io.github.wysohn.rapidframework3.bukkit.testutils.manager.AbstractBukkitManagerTest;
import io.github.wysohn.rapidframework3.core.inject.module.TaskSupervisorModule;
import io.github.wysohn.rapidframework3.core.language.ManagerLanguage;
import io.github.wysohn.rapidframework3.core.language.Pagination;
import io.github.wysohn.rapidframework3.interfaces.ICommandSender;
import io.github.wysohn.rapidframework3.interfaces.plugin.ITaskSupervisor;
import io.github.wysohn.rapidframework3.utils.Pair;
import io.github.wysohn.realeconomy.inject.annotation.MaxCapital;
import io.github.wysohn.realeconomy.inject.annotation.MinCapital;
import io.github.wysohn.realeconomy.inject.module.TransactionHandlerModule;
import io.github.wysohn.realeconomy.interfaces.currency.ICurrencyOwnerProvider;
import io.github.wysohn.realeconomy.manager.currency.Currency;
import org.junit.Before;
import org.junit.Test;

import java.math.BigDecimal;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.*;

public class UserTest extends AbstractBukkitManagerTest {
    private final List<Module> moduleList = new LinkedList<>();

    private ITaskSupervisor taskSupervisor;
    private ICurrencyOwnerProvider currencyOwnerProvider;

    @Before
    public void init() {
        taskSupervisor = mock(ITaskSupervisor.class);
        currencyOwnerProvider = mock(ICurrencyOwnerProvider.class);

        moduleList.add(new TransactionHandlerModule());
        moduleList.add(new TaskSupervisorModule(taskSupervisor));
        moduleList.add(new AbstractModule() {
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
    }

    @Test
    public void clearWallet() {
        UUID uuid = UUID.randomUUID();
        User user = new User(uuid);
        addFakeObserver(user);
        Guice.createInjector(moduleList).injectMembers(user);

        Currency currency1 = mock(Currency.class);
        UUID currency1Uuid = UUID.randomUUID();
        when(currency1.getKey()).thenReturn(currency1Uuid);

        Currency currency2 = mock(Currency.class);
        UUID currency2Uuid = UUID.randomUUID();
        when(currency2.getKey()).thenReturn(currency2Uuid);

        Currency currency3 = mock(Currency.class);
        UUID currency3Uuid = UUID.randomUUID();
        when(currency3.getKey()).thenReturn(currency3Uuid);

        user.deposit(10405.33, currency1);
        user.deposit(22304.67, currency2);
        user.deposit(40305.12, currency3);

        List<Pair<UUID, BigDecimal>> currencies = user.clearWallet();
        assertTrue(currencies.contains(Pair.of(currency1Uuid, BigDecimal.valueOf(10405.33))));
        assertTrue(currencies.contains(Pair.of(currency2Uuid, BigDecimal.valueOf(22304.67))));
        assertTrue(currencies.contains(Pair.of(currency3Uuid, BigDecimal.valueOf(40305.12))));

        assertEquals(BigDecimal.ZERO, user.balance(currency1));
        assertEquals(BigDecimal.ZERO, user.balance(currency2));
        assertEquals(BigDecimal.ZERO, user.balance(currency3));
        assertEquals(0, user.clearWallet().size());
    }

    @Test
    public void forEachBalance() throws InterruptedException {
        UUID uuid = UUID.randomUUID();
        User user = new User(uuid);
        addFakeObserver(user);
        Guice.createInjector(moduleList).injectMembers(user);

        Currency currency1 = mock(Currency.class);
        UUID currency1Uuid = UUID.randomUUID();
        when(currency1.getKey()).thenReturn(currency1Uuid);

        Currency currency2 = mock(Currency.class);
        UUID currency2Uuid = UUID.randomUUID();
        when(currency2.getKey()).thenReturn(currency2Uuid);

        Currency currency3 = mock(Currency.class);
        UUID currency3Uuid = UUID.randomUUID();
        when(currency3.getKey()).thenReturn(currency3Uuid);

        user.deposit(45605.33, currency1);
        user.deposit(451.53, currency3);
        user.deposit(41122.54, currency2);

        ManagerLanguage lang = mock(ManagerLanguage.class);
        ICommandSender sender = mock(ICommandSender.class);
        Pagination.MessageConverter<Pair<UUID, BigDecimal>> converter = mock(Pagination.MessageConverter.class);

        Pagination<Pair<UUID, BigDecimal>> pairPagination = user.balancesPagination(lang,
                7, "title", "somecommand");
        pairPagination.show(sender, 0, converter);
        pairPagination.shutdown();

        verify(converter).convert(eq(sender),
                eq(Pair.of(currency1Uuid, BigDecimal.valueOf(45605.33))),
                eq(0));
        verify(converter).convert(eq(sender),
                eq(Pair.of(currency2Uuid, BigDecimal.valueOf(41122.54))),
                eq(1));
        verify(converter).convert(eq(sender),
                eq(Pair.of(currency3Uuid, BigDecimal.valueOf(451.53))),
                eq(2));
    }
}