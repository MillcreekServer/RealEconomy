package io.github.wysohn.realeconomy.manager.banking.bank;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Module;
import com.google.inject.Provides;
import com.google.inject.multibindings.ProvidesIntoSet;
import io.github.wysohn.rapidframework3.bukkit.testutils.manager.AbstractBukkitManagerTest;
import io.github.wysohn.rapidframework3.interfaces.IMemento;
import io.github.wysohn.rapidframework3.interfaces.paging.DataProvider;
import io.github.wysohn.realeconomy.inject.annotation.MaxCapital;
import io.github.wysohn.realeconomy.inject.annotation.MinCapital;
import io.github.wysohn.realeconomy.interfaces.banking.IBankOwner;
import io.github.wysohn.realeconomy.interfaces.banking.IBankOwnerProvider;
import io.github.wysohn.realeconomy.interfaces.banking.IBankUser;
import io.github.wysohn.realeconomy.interfaces.currency.ICurrencyOwnerProvider;
import io.github.wysohn.realeconomy.manager.asset.Asset;
import io.github.wysohn.realeconomy.manager.asset.Item;
import io.github.wysohn.realeconomy.manager.asset.signature.AssetSignature;
import io.github.wysohn.realeconomy.manager.asset.signature.ItemStackSignature;
import io.github.wysohn.realeconomy.manager.banking.BankingTypeRegistry;
import io.github.wysohn.realeconomy.manager.currency.Currency;
import io.github.wysohn.realeconomy.manager.currency.CurrencyManager;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
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
    private ICurrencyOwnerProvider currencyOwnerProvider;

    @Before
    public void init() {
        currencyManager = mock(CurrencyManager.class);
        when(currencyManager.get(any(UUID.class))).thenReturn(Optional.empty());

        provider = mock(IBankOwnerProvider.class);
        when(provider.get(any())).thenReturn(mock(IBankOwner.class));

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

            @ProvidesIntoSet
            IBankOwnerProvider bankOwnerProvider(){
                return provider;
            }
        });
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

        assertFalse(bank.hasAccount(user, BankingTypeRegistry.CHECKING));
        assertTrue(bank.putAccount(user, BankingTypeRegistry.CHECKING));
        assertFalse(bank.putAccount(user, BankingTypeRegistry.CHECKING));
        assertTrue(bank.hasAccount(user, BankingTypeRegistry.CHECKING));
    }

    @Test
    public void removeAccount() {

        AbstractBank bank = new TempBank();
        addFakeObserver(bank);
        Guice.createInjector(moduleList).injectMembers(bank);

        IBankUser user = mock(IBankUser.class);
        UUID uuid = UUID.randomUUID();
        when(user.getUuid()).thenReturn(uuid);

        assertFalse(bank.hasAccount(user, BankingTypeRegistry.CHECKING));
        assertTrue(bank.putAccount(user, BankingTypeRegistry.CHECKING));
        assertTrue(bank.hasAccount(user, BankingTypeRegistry.CHECKING));
        assertTrue(bank.removeAccount(user, BankingTypeRegistry.CHECKING));
        assertFalse(bank.removeAccount(user, BankingTypeRegistry.CHECKING));
        assertFalse(bank.hasAccount(user, BankingTypeRegistry.CHECKING));
    }

    @Test
    public void accountTransaction() {
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
        bank.depositAccount(user, BankingTypeRegistry.CHECKING, 39800.55, currency);
        bank.withdrawAccount(user, BankingTypeRegistry.CHECKING, 2552.34, currency);
        assertEquals(BigDecimal.valueOf(39800.55).subtract(BigDecimal.valueOf(2552.34)),
                bank.balanceOfAccount(user, BankingTypeRegistry.CHECKING));
    }

    @Test
    public void accountTransactionFailure() {
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
        bank.depositAccount(user, BankingTypeRegistry.CHECKING, 10.0, currency);
        bank.depositAccount(user, BankingTypeRegistry.CHECKING, BigDecimal.valueOf(Double.MAX_VALUE), currency);// max limit
        // transaction failure should revert account back to original state
        assertEquals(BigDecimal.valueOf(10.0),
                bank.balanceOfAccount(user, BankingTypeRegistry.CHECKING));
    }

    @Test
    public void testAddAccountAsset() {
        AbstractBank bank = new TempBank();
        addFakeObserver(bank);
        Guice.createInjector(moduleList).injectMembers(bank);

        IBankUser user = mock(IBankUser.class);
        UUID uuid = UUID.randomUUID();
        AssetSignature assetSignature = new ItemStackSignature(new ItemStack(Material.DIAMOND));
        Asset asset = new Item(UUID.randomUUID(), assetSignature);

        when(user.getUuid()).thenReturn(uuid);

        bank.putAccount(user, BankingTypeRegistry.TRADING);
        bank.addAccountAsset(user, asset);

        DataProvider<Asset> assetDataProvider = bank.accountAssetProvider(user);
        assertEquals(asset, assetDataProvider.get(0, 45).get(0));
    }

    @Test
    public void testRemoveAccountAsset() {
        AbstractBank bank = new TempBank();
        addFakeObserver(bank);
        Guice.createInjector(moduleList).injectMembers(bank);

        IBankUser user = mock(IBankUser.class);
        UUID uuid = UUID.randomUUID();
        AssetSignature assetSignature = new ItemStackSignature(new ItemStack(Material.DIAMOND));
        Asset asset = new Item(UUID.randomUUID(), assetSignature);

        when(user.getUuid()).thenReturn(uuid);

        bank.putAccount(user, BankingTypeRegistry.TRADING);
        bank.addAccountAsset(user, asset);
        assertEquals(1, bank.removeAccountAsset(user, assetSignature, 1));
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