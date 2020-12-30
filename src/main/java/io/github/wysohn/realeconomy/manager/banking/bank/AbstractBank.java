package io.github.wysohn.realeconomy.manager.banking.bank;

import io.github.wysohn.rapidframework3.core.caching.CachedElement;
import io.github.wysohn.rapidframework3.interfaces.IMemento;
import io.github.wysohn.rapidframework3.interfaces.IPluginObject;
import io.github.wysohn.rapidframework3.interfaces.language.ILang;
import io.github.wysohn.rapidframework3.interfaces.paging.DataProvider;
import io.github.wysohn.rapidframework3.utils.Validation;
import io.github.wysohn.realeconomy.inject.annotation.MaxCapital;
import io.github.wysohn.realeconomy.inject.annotation.MinCapital;
import io.github.wysohn.realeconomy.interfaces.IFinancialEntity;
import io.github.wysohn.realeconomy.interfaces.banking.*;
import io.github.wysohn.realeconomy.main.RealEconomyLangs;
import io.github.wysohn.realeconomy.manager.asset.Asset;
import io.github.wysohn.realeconomy.manager.asset.signature.AssetSignature;
import io.github.wysohn.realeconomy.manager.banking.AssetUtil;
import io.github.wysohn.realeconomy.manager.banking.BankingTypeRegistry;
import io.github.wysohn.realeconomy.manager.banking.TransactionUtil;
import io.github.wysohn.realeconomy.manager.banking.account.TradingAccount;
import io.github.wysohn.realeconomy.manager.currency.Currency;
import io.github.wysohn.realeconomy.manager.currency.CurrencyManager;

import javax.inject.Inject;
import java.lang.ref.Reference;
import java.math.BigDecimal;
import java.util.*;

public abstract class AbstractBank extends CachedElement<UUID> implements IPluginObject, IFinancialEntity, IAssetHolder {
    public static final String BANK_MARK = "\u2608";

    private transient final Object transactionLock = new Object();

    @Inject
    private Set<IBankOwnerProvider> ownerProviders;
    @Inject
    private CurrencyManager currencyManager;
    @Inject
    @MinCapital
    protected BigDecimal minimum;
    @Inject
    @MaxCapital
    protected BigDecimal maximum;

    // Currency uuid -> value
    private final Map<UUID, BigDecimal> capitals = new HashMap<>();
    private final Map<UUID, Map<IBankingType, IAccount>> accounts = new HashMap<>();
    private final List<Asset> ownedAssets = new ArrayList<>();

    private UUID bankOwnerUuid;
    private UUID baseCurrencyUuid;

    private boolean operating = true;

    public AbstractBank(UUID key) {
        super(key);
    }

    public IBankOwner getBankOwner() {
        return Optional.ofNullable(bankOwnerUuid)
                .flatMap(uuid -> ownerProviders.stream()
                        .map(provider -> provider.get(uuid))
                        .findAny())
                .orElse(null);
    }

    public void setBankOwner(IBankOwner owner) {
        this.bankOwnerUuid = owner.getUuid();

        notifyObservers();
    }

    public Currency getBaseCurrency() {
        return Optional.ofNullable(baseCurrencyUuid)
                .flatMap(currencyManager::get)
                .map(Reference::get)
                .orElse(null);
    }

    protected UUID getBaseCurrencyUuid() {
        return baseCurrencyUuid;
    }

    public void setBaseCurrency(Currency currency) {
        synchronized (transactionLock) {
            if (this.baseCurrencyUuid != null)
                throw new RuntimeException("BaseCurrency can be set only once.");

            this.baseCurrencyUuid = currency.getKey();

            notifyObservers();
        }
    }

    public boolean isOperating() {
        return operating;
    }

    public void setOperating(boolean operating) {
        this.operating = operating;
        notifyObservers();
    }

    @Override
    public UUID getUuid() {
        return getKey();
    }

    @Override
    public BigDecimal balance(Currency currency) {
        return TransactionUtil.balance(capitals, currency);
    }

    @Override
    public boolean deposit(BigDecimal value, Currency currency) {
        if (!operating)
            throw new RuntimeException("Cannot use the bank that is closed. Bank: " + getStringKey());

        synchronized (transactionLock) {
            final boolean aBoolean = TransactionUtil.deposit(maximum, capitals, value, currency);
            notifyObservers();
            return aBoolean;
        }
    }

    @Override
    public boolean withdraw(BigDecimal value, Currency currency) {
        if (!operating)
            throw new RuntimeException("Cannot use the bank that is closed. Bank: " + getStringKey());

        synchronized (transactionLock) {
            final boolean aBoolean = TransactionUtil.withdraw(minimum, capitals, value, currency, true);
            notifyObservers();
            return aBoolean;
        }
    }

    public boolean hasAccount(IBankUser user, IBankingType type) {
        Validation.assertNotNull(user);
        Validation.assertNotNull(type);

        Validation.assertNotNull(user);
        if (!accounts.containsKey(user.getUuid()))
            return false;

        Map<IBankingType, IAccount> accountMap = accounts.get(user.getUuid());
        return accountMap.containsKey(type);
    }

    /**
     * @param user the owner of account.
     * @param type the account type to be added to this bank.
     * @return true if newly created; false if the account type already exist
     */
    public boolean putAccount(IBankUser user, IBankingType type) {
        if (!operating)
            throw new RuntimeException("Cannot use the bank that is closed. Bank: " + getStringKey());

        Validation.assertNotNull(user);
        Validation.assertNotNull(type);

        Map<IBankingType, IAccount> accountMap = accounts.computeIfAbsent(user.getUuid(),
                key -> new HashMap<>());

        if (accountMap.containsKey(type))
            return false;

        Validation.validate(accountMap.put(type, type.createAccount()),
                Objects::isNull,
                "Inconsistent Map behavior.");

        notifyObservers();
        return true;
    }

    /**
     * @param user the owner of account.
     * @param type the account type to be added to this bank.
     * @return true if deleted; false if account didn't exist in the first place.
     */
    public boolean removeAccount(IBankUser user, IBankingType type) {
        if (!operating)
            throw new RuntimeException("Cannot use the bank that is closed. Bank: " + getStringKey());

        Validation.assertNotNull(user);

        Map<IBankingType, IAccount> accountMap = accounts.get(user.getUuid());
        if (accountMap == null)
            return false;

        final boolean deleted = accountMap.remove(type) != null;
        if (deleted) notifyObservers();
        return deleted;
    }

    public void addAccountAsset(IBankUser user, Asset asset) {
        if (!operating)
            throw new RuntimeException("Cannot use the bank that is closed. Bank: " + getStringKey());

        Validation.assertNotNull(user);
        if (!accounts.containsKey(user.getUuid()))
            throw new RuntimeException("Account of " + user + " does not exist.");

        Map<IBankingType, IAccount> accountMap = accounts.get(user.getUuid());
        if (!accountMap.containsKey(BankingTypeRegistry.TRADING))
            throw new RuntimeException("Account of " + user + " does not exist.");

        TradingAccount account = (TradingAccount) accountMap.get(BankingTypeRegistry.TRADING);
        account.addAsset(asset);
        notifyObservers();
    }

    public int removeAccountAsset(IBankUser user, AssetSignature signature, int amount) {
        if (!operating)
            throw new RuntimeException("Cannot use the bank that is closed. Bank: " + getStringKey());

        Validation.assertNotNull(user);
        if (!accounts.containsKey(user.getUuid()))
            throw new RuntimeException("Account of " + user + " does not exist.");

        Map<IBankingType, IAccount> accountMap = accounts.get(user.getUuid());
        if (!accountMap.containsKey(BankingTypeRegistry.TRADING))
            throw new RuntimeException("Account of " + user + " does not exist.");

        TradingAccount account = (TradingAccount) accountMap.get(BankingTypeRegistry.TRADING);
        int i = account.removeAsset(signature, amount).size();
        if (i > 0)
            notifyObservers();
        return i;
    }

    public DataProvider<Asset> accountAssetProvider(IBankUser user) {
        if (!operating)
            throw new RuntimeException("Cannot use the bank that is closed. Bank: " + getStringKey());

        Validation.assertNotNull(user);
        if (!accounts.containsKey(user.getUuid()))
            throw new RuntimeException("Account of " + user + " does not exist.");

        Map<IBankingType, IAccount> accountMap = accounts.get(user.getUuid());
        if (!accountMap.containsKey(BankingTypeRegistry.TRADING))
            throw new RuntimeException("Account of " + user + " does not exist.");

        TradingAccount account = (TradingAccount) accountMap.get(BankingTypeRegistry.TRADING);
        return account.assetDataProvider();
    }

    public boolean depositAccount(IBankUser user, IBankingType type, BigDecimal amount, Currency currency) {
        if (!operating)
            throw new RuntimeException("Cannot use the bank that is closed. Bank: " + getStringKey());

        Validation.assertNotNull(user);
        if (!accounts.containsKey(user.getUuid()))
            throw new RuntimeException("Account of " + user + " does not exist.");

        Map<IBankingType, IAccount> accountMap = accounts.get(user.getUuid());
        if (!accountMap.containsKey(type))
            throw new RuntimeException("Account of " + user + " does not exist.");

        IAccount account = accountMap.get(type);
        boolean deposit = TransactionUtil.deposit(maximum, account.getCurrencyMap(), amount, currency);
        if (deposit)
            notifyObservers();
        return deposit;
    }

    public boolean depositAccount(IBankUser user, IBankingType type, double amount, Currency currency) {
        return depositAccount(user, type, BigDecimal.valueOf(amount), currency);
    }

    public boolean withdrawAccount(IBankUser user, IBankingType type, BigDecimal amount, Currency currency) {
        if (!operating)
            throw new RuntimeException("Cannot use the bank that is closed. Bank: " + getStringKey());

        Validation.assertNotNull(user);
        if (!accounts.containsKey(user.getUuid()))
            throw new RuntimeException("Account of " + user + " does not exist.");

        Map<IBankingType, IAccount> accountMap = accounts.get(user.getUuid());
        if (!accountMap.containsKey(type))
            throw new RuntimeException("Account of " + user + " does not exist.");

        IAccount account = accountMap.get(type);
        notifyObservers();

        BigDecimal accountMinimum = minimum.compareTo(account.minimumBalance()) > 0 ? minimum : account.minimumBalance();
        return TransactionUtil.withdraw(accountMinimum, account.getCurrencyMap(), amount, currency, true);
    }

    public boolean withdrawAccount(IBankUser user, IBankingType type, double amount, Currency currency) {
        return withdrawAccount(user, type, BigDecimal.valueOf(amount), currency);
    }

    public BigDecimal balanceOfAccount(IBankUser user, IBankingType type) {
        return balanceOfAccount(user, type, getBaseCurrency());
    }

    public BigDecimal balanceOfAccount(IBankUser user, IBankingType type, Currency currency) {
        Validation.assertNotNull(user);
        if (!accounts.containsKey(user.getUuid()))
            throw new RuntimeException("Account of " + user + " does not exist.");

        Map<IBankingType, IAccount> accountMap = accounts.get(user.getUuid());
        if (!accountMap.containsKey(type))
            throw new RuntimeException("Account of " + user + " does not exist.");

        IAccount account = accountMap.get(type);
        return TransactionUtil.balance(account.getCurrencyMap(), currency);
    }

    @Override
    public void addAsset(Asset asset) {
        if (!operating)
            throw new RuntimeException("Cannot use the bank that is closed. Bank: " + getStringKey());

        AssetUtil.addAsset(ownedAssets, asset);
    }

    @Override
    public Collection<Asset> removeAsset(AssetSignature signature, int amount) {
        if (!operating)
            throw new RuntimeException("Cannot use the bank that is closed. Bank: " + getStringKey());

        return AssetUtil.removeAsset(ownedAssets, signature, amount);
    }

    @Override
    public DataProvider<Asset> assetDataProvider() {
        return AssetUtil.assetDataProvider(ownedAssets);
    }

    @Override
    public Map<ILang, Object> properties() {
        Map<ILang, Object> properties = new LinkedHashMap<>();
        Optional.ofNullable(getBankOwner())
                .ifPresent(owner -> properties.put(RealEconomyLangs.Bank_Owner, owner));
        Optional.ofNullable(getBaseCurrency())
                .ifPresent(currency -> properties.put(RealEconomyLangs.Bank_BaseCurrency, currency));
        properties.put(RealEconomyLangs.Bank_NumAccounts, accounts.size());
        return properties;
    }

    @Override
    public IMemento saveState() {
        return new AbstractMemento(this);
    }

    @Override
    public void restoreState(IMemento memento) {
        AbstractMemento mem = (AbstractMemento) memento;

        synchronized (transactionLock) {
            this.capitals.clear();
            this.capitals.putAll(mem.capitals);

            this.accounts.clear();
            this.accounts.putAll(mem.accounts);
        }
    }

    protected static class AbstractMemento implements IMemento {
        private final Map<UUID, BigDecimal> capitals = new HashMap<>();
        private final Map<UUID, Map<IBankingType, IAccount>> accounts = new HashMap<>();

        public AbstractMemento(AbstractBank bank) {
            synchronized (bank.transactionLock) {
                capitals.putAll(bank.capitals);
                // make a deep copy
                accounts.putAll(createDeepCopy(bank.accounts));
            }
        }
    }

    private static Map<UUID, Map<IBankingType, IAccount>> createDeepCopy(Map<UUID, Map<IBankingType, IAccount>> from) {
        Map<UUID, Map<IBankingType, IAccount>> mapCopyParent = new HashMap<>();
        from.forEach((uuid, accountMap) -> {
            Map<IBankingType, IAccount> mapCopy = new HashMap<>();
            accountMap.forEach((type, account) -> mapCopy.put(type, account.clone()));
            mapCopyParent.put(uuid, mapCopy);
        });
        return mapCopyParent;
    }

    @Override
    public String toString() {
        return BANK_MARK + getStringKey();
    }
}
