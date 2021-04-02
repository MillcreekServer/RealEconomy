package io.github.wysohn.realeconomy.mediator;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Module;
import com.google.inject.Provides;
import com.google.inject.multibindings.ProvidesIntoSet;
import io.github.wysohn.rapidframework3.bukkit.testutils.manager.AbstractBukkitManagerTest;
import io.github.wysohn.rapidframework3.core.main.ManagerConfig;
import io.github.wysohn.realeconomy.inject.annotation.MaxCapital;
import io.github.wysohn.realeconomy.inject.annotation.MinCapital;
import io.github.wysohn.realeconomy.interfaces.IGovernment;
import io.github.wysohn.realeconomy.interfaces.banking.IAccount;
import io.github.wysohn.realeconomy.interfaces.banking.IBankOwnerProvider;
import io.github.wysohn.realeconomy.interfaces.banking.IBankUser;
import io.github.wysohn.realeconomy.manager.banking.BankingTypeRegistry;
import io.github.wysohn.realeconomy.manager.banking.CentralBankingManager;
import io.github.wysohn.realeconomy.manager.banking.VisitingBankManager;
import io.github.wysohn.realeconomy.manager.banking.bank.CentralBank;
import io.github.wysohn.realeconomy.manager.currency.Currency;
import io.github.wysohn.realeconomy.manager.currency.CurrencyManager;
import org.junit.Before;
import org.junit.Test;

import java.lang.ref.WeakReference;
import java.math.BigDecimal;
import java.util.*;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.*;

public class BankingMediatorTest extends AbstractBukkitManagerTest {

    List<Module> moduleList = new LinkedList<>();
    private CentralBank serverBank;
    private Currency currency;
    private UUID serverCurrencyUuid;
    private CurrencyManager currencyManager;
    private CentralBankingManager centralBankingManager;
    private ManagerConfig config;

    @Before
    public void init() {
        UUID bankUuid = UUID.randomUUID();
        serverCurrencyUuid = UUID.randomUUID();
        serverBank = new CentralBank(bankUuid);
        currency = mock(Currency.class);
        currencyManager = mock(CurrencyManager.class);
        config = mock(ManagerConfig.class);

        when(currency.getKey()).thenReturn(serverCurrencyUuid);
        when(currency.ownerBank()).thenReturn(serverBank);
        addFakeObserver(serverBank);
        serverBank.setBaseCurrency(currency);
        serverBank.setLimitlessPapers(true);

        when(currencyManager.newCurrency(anyString(), anyString(), any())).thenReturn(CurrencyManager.Result.OK);
        centralBankingManager = mock(CentralBankingManager.class);
        when(config.get(eq(VisitingBankManager.KEY_SERVER_BANK_ENABLE))).thenReturn(Optional.of(true));
        when(currencyManager.get(eq(VisitingBankManager.SERVER_CURRENCY)))
                .thenReturn(Optional.of(new WeakReference<>(currency)));
        when(currencyManager.get(eq(serverCurrencyUuid)))
                .thenReturn(Optional.of(new WeakReference<>(currency)));

        moduleList.add(new AbstractModule() {
            @Provides
            ManagerConfig config() {
                return config;
            }

            @Provides
            CurrencyManager currencyManager() {
                return currencyManager;
            }

            @Provides
            CentralBankingManager centralBankingManager() {
                return centralBankingManager;
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

            @ProvidesIntoSet
            IBankOwnerProvider bankOwnerProvider() {
                return mock(IBankOwnerProvider.class);
            }
        });

        Guice.createInjector(moduleList).injectMembers(serverBank);
    }

    @Test
    public void createCurrency() throws Exception {
        BankingMediator mediator = Guice.createInjector(moduleList).getInstance(BankingMediator.class);
        when(centralBankingManager.getOrNew(any(UUID.class)))
                .thenReturn(Optional.of(new WeakReference<>(serverBank)));
        mediator.enable();

        UUID uuid = UUID.randomUUID();
        IGovernment government = mock(IGovernment.class);
        Currency currency = mock(Currency.class);
        when(government.getUuid()).thenReturn(uuid);
        when(currencyManager.get(anyString())).thenReturn(Optional.of(new WeakReference<>(currency)));
        when(centralBankingManager.get(any(UUID.class)))
                .thenReturn(Optional.of(new WeakReference<>(mock(CentralBank.class))));

        assertEquals(BankingMediator.Result.ALREADY_SET,
                mediator.createCurrency(government, VisitingBankManager.SERVER_CURRENCY, "USD"));
    }

    @Test
    public void createCurrency2() throws Exception {
        BankingMediator mediator = Guice.createInjector(moduleList).getInstance(BankingMediator.class);
        when(centralBankingManager.getOrNew(any(UUID.class)))
                .thenReturn(Optional.of(new WeakReference<>(serverBank)));
        mediator.enable();

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
        verify(currencyManager).newCurrency(eq("dollar"), eq("USD"), any(CentralBank.class));
        verify(centralBankingManager).getOrNew(eq(uuid));
    }

    @Test
    public void renameCurrency() throws Exception {
        BankingMediator mediator = Guice.createInjector(moduleList).getInstance(BankingMediator.class);
        when(centralBankingManager.getOrNew(any(UUID.class)))
                .thenReturn(Optional.of(new WeakReference<>(serverBank)));
        mediator.enable();

        when(currencyManager.get(anyString()))
                .thenReturn(Optional.of(new WeakReference<>(mock(Currency.class))));

        assertEquals(BankingMediator.Result.DUP_NAME,
                mediator.renameCurrency(VisitingBankManager.SERVER_CURRENCY, "other"));
        verify(currencyManager).get(eq("other"));
    }

    @Test
    public void renameCurrency2() throws Exception {
        BankingMediator mediator = Guice.createInjector(moduleList).getInstance(BankingMediator.class);
        when(centralBankingManager.getOrNew(any(UUID.class)))
                .thenReturn(Optional.of(new WeakReference<>(serverBank)));
        mediator.enable();

        when(currencyManager.get(eq("other")))
                .thenReturn(Optional.empty());
        when(currencyManager.get(eq(VisitingBankManager.SERVER_CURRENCY)))
                .thenReturn(Optional.empty());

        assertEquals(BankingMediator.Result.NOT_FOUND,
                mediator.renameCurrency(VisitingBankManager.SERVER_CURRENCY, "other"));
    }

    @Test
    public void renameCurrency3() throws Exception {
        BankingMediator mediator = Guice.createInjector(moduleList).getInstance(BankingMediator.class);
        when(centralBankingManager.getOrNew(any(UUID.class)))
                .thenReturn(Optional.of(new WeakReference<>(serverBank)));
        mediator.enable();

        Currency currency = mock(Currency.class);
        when(currencyManager.get(eq("other")))
                .thenReturn(Optional.empty());
        when(currencyManager.get(eq(VisitingBankManager.SERVER_CURRENCY)))
                .thenReturn(Optional.of(new WeakReference<>(currency)));

        assertEquals(BankingMediator.Result.OK,
                mediator.renameCurrency(VisitingBankManager.SERVER_CURRENCY, "other"));
    }

    @Test
    public void balance() throws Exception {
        BankingMediator mediator = Guice.createInjector(moduleList).getInstance(BankingMediator.class);
        when(centralBankingManager.getOrNew(any(UUID.class)))
                .thenReturn(Optional.of(new WeakReference<>(serverBank)));
        mediator.enable();

        UUID uuid = UUID.randomUUID();
        UUID currencyUuid = UUID.randomUUID();
        IBankUser user = mock(IBankUser.class);
        IAccount account = mock(IAccount.class);
        Currency currency = mock(Currency.class);
        Map<UUID, BigDecimal> balances = mock(Map.class);

        when(user.getUuid()).thenReturn(uuid);
        when(currency.getKey()).thenReturn(currencyUuid);
        when(account.getCurrencyMap()).thenReturn(balances);

        assertEquals(BigDecimal.valueOf(0.0), serverBank.balanceOfAccount(user, BankingTypeRegistry.CHECKING));
    }
}