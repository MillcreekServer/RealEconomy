package io.github.wysohn.realeconomy.manager.banking.bank;

import io.github.wysohn.rapidframework3.core.caching.CachedElement;
import io.github.wysohn.rapidframework3.core.language.ManagerLanguage;
import io.github.wysohn.rapidframework3.interfaces.ICommandSender;
import io.github.wysohn.rapidframework3.interfaces.IMemento;
import io.github.wysohn.rapidframework3.interfaces.IPluginObject;
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
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

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
    private final Map<UUID, BigDecimal> capitals = new ConcurrentHashMap<>();
    private final Map<UUID, Map<IBankingType, IAccount>> accounts = new ConcurrentHashMap<>();
    private final List<Asset> ownedAssets = Collections.synchronizedList(new ArrayList<>());

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
        synchronized (transactionLock) {
            return TransactionUtil.balance(capitals, currency);
        }
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

    /**
     * execute 'accountConsumer' while acquiring the lock for the specific user
     * node and its bank account node.
     *
     * @param user
     * @param type
     * @param visitor
     */
    private <U> void synchronousAccountTask(IBankUser user, IBankingType type, AccountVisitor<U> visitor) {
        Validation.assertNotNull(user);
        Validation.assertNotNull(type);
        Validation.assertNotNull(visitor);
        Validation.assertNotNull(visitor.function);

        accounts.computeIfPresent(user.getUuid(), (key, accountMap) -> {
            synchronized (accountMap) {
                accountMap.computeIfPresent(type, (t, account) -> {
                    visitor.result = visitor.function.apply(account);
                    return account;
                });
            }
            return accountMap;
        });
    }

    public boolean hasAccount(IBankUser user, IBankingType type) {
        AccountVisitor<Boolean> visitor = new AccountVisitor<>((account) -> true, false);
        synchronousAccountTask(user, type, visitor);
        return visitor.result;
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

        synchronized (accountMap) {
            if (accountMap.containsKey(type))
                return false;

            Validation.validate(accountMap.put(type, type.createAccount()),
                    Objects::isNull,
                    "Inconsistent Map behavior.");
        }

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

        synchronized (accountMap) {
            final boolean deleted = accountMap.remove(type) != null;
            if (deleted) notifyObservers();
            return deleted;
        }
    }

    public void addAccountAsset(IBankUser user, Asset asset) {
        if (!operating)
            throw new RuntimeException("Cannot use the bank that is closed. Bank: " + getStringKey());

        Validation.assertNotNull(user);
        Validation.assertNotNull(asset);

        AccountVisitor<Void> visitor = new AccountVisitor<>(account -> {
            if (account == null)
                throw new RuntimeException("Account of " + user + " does not exist.");

            TradingAccount tradingAccount = (TradingAccount) account;
            tradingAccount.addAsset(asset);
            return null;
        });
        synchronousAccountTask(user, BankingTypeRegistry.TRADING, visitor);
        notifyObservers();
    }

    public double countAccountAsset(IBankUser user, AssetSignature signature) {
        if (!operating)
            throw new RuntimeException("Cannot use the bank that is closed. Bank: " + getStringKey());

        Validation.assertNotNull(user);
        Validation.assertNotNull(signature);

        AccountVisitor<Double> visitor = new AccountVisitor<>(account -> {
            if (account == null)
                throw new RuntimeException("Account of " + user + " does not exist.");

            TradingAccount tradingAccount = (TradingAccount) account;
            return tradingAccount.countAsset(signature);
        }, 0.0);
        synchronousAccountTask(user, BankingTypeRegistry.TRADING, visitor);

        return visitor.result == null ? 0.0 : visitor.result;
    }

    public Collection<Asset> removeAccountAsset(IBankUser user, AssetSignature signature, double amount) {
        if (!operating)
            throw new RuntimeException("Cannot use the bank that is closed. Bank: " + getStringKey());

        Validation.assertNotNull(user);
        Validation.assertNotNull(signature);
        Validation.validate(amount, val -> val >= 0.0, "negative amount not allowed.");

        AccountVisitor<Collection<Asset>> visitor = new AccountVisitor<>(account -> {
            if (account == null)
                throw new RuntimeException("Account of " + user + " does not exist.");

            TradingAccount tradingAccount = (TradingAccount) account;
            return tradingAccount.removeAsset(signature, amount);
        }, new LinkedList<>());
        synchronousAccountTask(user, BankingTypeRegistry.TRADING, visitor);

        Collection<Asset> removed = visitor.result;
        if (removed.size() > 0)
            notifyObservers();
        return removed;
    }

    public Asset removeAccountAsset(IBankUser user, int index) {
        if (!operating)
            throw new RuntimeException("Cannot use the bank that is closed. Bank: " + getStringKey());

        Validation.assertNotNull(user);
        Validation.validate(index, val -> val >= 0, "negative index not allowed.");

        AccountVisitor<Asset> visitor = new AccountVisitor<>(account -> {
            if (account == null)
                throw new RuntimeException("Account of " + user + " does not exist.");

            TradingAccount tradingAccount = (TradingAccount) account;
            return tradingAccount.removeAsset(index);
        });
        synchronousAccountTask(user, BankingTypeRegistry.TRADING, visitor);

        Asset removed = visitor.result;
        if (removed != null)
            notifyObservers();
        return removed;
    }

    public DataProvider<Asset> accountAssetProvider(IBankUser user) {
        if (!operating)
            throw new RuntimeException("Cannot use the bank that is closed. Bank: " + getStringKey());

        Validation.assertNotNull(user);

        AccountVisitor<DataProvider<Asset>> visitor = new AccountVisitor<>(account -> {
            if (account == null)
                throw new RuntimeException("Account of " + user + " does not exist.");

            TradingAccount tradingAccount = (TradingAccount) account;
            return tradingAccount.assetDataProvider();
        });
        synchronousAccountTask(user, BankingTypeRegistry.TRADING, visitor);

        return visitor.result;
    }

    public boolean depositAccount(IBankUser user, IBankingType type, BigDecimal amount, Currency currency) {
        if (!operating)
            throw new RuntimeException("Cannot use the bank that is closed. Bank: " + getStringKey());

        Validation.assertNotNull(user);
        Validation.assertNotNull(type);
        Validation.assertNotNull(amount);
        Validation.validate(amount, val -> val.signum() >= 0, "negative amount not allowed");
        Validation.assertNotNull(currency);

        AccountVisitor<Boolean> visitor = new AccountVisitor<>(account -> {
            if (account == null)
                throw new RuntimeException("Account of " + user + " does not exist.");

            return TransactionUtil.deposit(maximum, account.getCurrencyMap(), amount, currency);
        }, false);
        synchronousAccountTask(user, type, visitor);

        boolean deposit = visitor.result;
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
        Validation.assertNotNull(type);
        Validation.assertNotNull(amount);
        Validation.validate(amount, val -> val.signum() >= 0, "negative amount not allowed");
        Validation.assertNotNull(currency);

        AccountVisitor<Boolean> visitor = new AccountVisitor<>(account -> {
            if (account == null)
                throw new RuntimeException("Account of " + user + " does not exist.");

            BigDecimal accountMinimum = minimum.compareTo(account.minimumBalance()) > 0 ? minimum : account.minimumBalance();
            return TransactionUtil.withdraw(accountMinimum, account.getCurrencyMap(), amount, currency, true);
        }, false);
        synchronousAccountTask(user, type, visitor);

        notifyObservers();
        return visitor.result;
    }

    public boolean withdrawAccount(IBankUser user, IBankingType type, double amount, Currency currency) {
        return withdrawAccount(user, type, BigDecimal.valueOf(amount), currency);
    }

    public BigDecimal balanceOfAccount(IBankUser user, IBankingType type) {
        return balanceOfAccount(user, type, getBaseCurrency());
    }

    public BigDecimal balanceOfAccount(IBankUser user, IBankingType type, Currency currency) {
        Validation.assertNotNull(user);
        Validation.assertNotNull(type);
        Validation.assertNotNull(currency);

        AccountVisitor<BigDecimal> visitor = new AccountVisitor<>(account -> {
            if (account == null)
                throw new RuntimeException("Account of " + user + " does not exist.");

            return TransactionUtil.balance(account.getCurrencyMap(), currency);
        }, BigDecimal.valueOf(0.0));
        synchronousAccountTask(user, type, visitor);

        return visitor.result;
    }

    @Override
    public void addAsset(Asset asset) {
        if (!operating)
            throw new RuntimeException("Cannot use the bank that is closed. Bank: " + getStringKey());

        AssetUtil.addAsset(ownedAssets, asset);
    }

    @Override
    public double countAsset(AssetSignature signature) {
        return AssetUtil.countAsset(ownedAssets, signature);
    }

    @Override
    public Collection<Asset> removeAsset(AssetSignature signature, double amount) {
        if (!operating)
            throw new RuntimeException("Cannot use the bank that is closed. Bank: " + getStringKey());

        // TODO temporary solution
        synchronized (ownedAssets) {
            Collection<Asset> assets = AssetUtil.removeAsset(ownedAssets, signature, amount);
            if (assets.size() > 0)
                notifyObservers();
            return assets;
        }
    }

    @Override
    public Asset removeAsset(int index) {
        if (!operating)
            throw new RuntimeException("Cannot use the bank that is closed. Bank: " + getStringKey());

        Asset removeAsset = AssetUtil.removeAsset(ownedAssets, index);
        if (removeAsset != null)
            notifyObservers();
        return removeAsset;
    }

    @Override
    public DataProvider<Asset> assetDataProvider() {
        return AssetUtil.assetDataProvider(ownedAssets);
    }

    @Override
    public Map<Object, Object> properties(ManagerLanguage lang, ICommandSender sender) {
        Map<Object, Object> properties = new LinkedHashMap<>();
        Optional.ofNullable(getBankOwner())
                .ifPresent(owner -> properties.put(RealEconomyLangs.Bank_Owner, owner));
        Optional.ofNullable(getBaseCurrency())
                .ifPresent(currency -> properties.put(RealEconomyLangs.Bank_BaseCurrency, currency));
        properties.put(RealEconomyLangs.Bank_NumAccounts, accounts.size());
        return properties;
    }

    @Override
    public String toString() {
        return BANK_MARK + getStringKey();
    }

    public static class AccountVisitor<U> {
        private final Function<IAccount, U> function;
        private U result;

        public AccountVisitor(Function<IAccount, U> function, U defVal) {
            this.function = function;
            this.result = defVal;
        }

        public AccountVisitor(Function<IAccount, U> function) {
            this(function, null);
        }
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

            this.accounts.forEach((uuid, accountMap) -> {
                Map<IBankingType, IMemento> statesMap = mem.accountStates.get(uuid);
                if (statesMap == null)
                    return;

                synchronized (accountMap) {
                    accountMap.forEach((type, account) -> account.restoreState(statesMap.get(type)));
                }
            });
        }
    }

    protected static class AbstractMemento implements IMemento {
        private final Map<UUID, BigDecimal> capitals = new HashMap<>();
        private final Map<UUID, Map<IBankingType, IMemento>> accountStates = new HashMap<>();

        public AbstractMemento(AbstractBank bank) {
            synchronized (bank.transactionLock) {
                capitals.putAll(bank.capitals);
                accountStates.putAll(createAccountStates(bank.accounts));
            }
        }
    }

    private static Map<UUID, Map<IBankingType, IMemento>> createAccountStates(Map<UUID, Map<IBankingType, IAccount>> from) {
        Map<UUID, Map<IBankingType, IMemento>> mapParent = new HashMap<>();
        from.forEach((uuid, accountMap) -> {
            Map<IBankingType, IMemento> stateMap = new HashMap<>();
            synchronized (accountMap) {
                accountMap.forEach((type, account) -> stateMap.put(type, account.saveState()));
            }
            mapParent.put(uuid, stateMap);
        });
        return mapParent;
    }
}
