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
import io.github.wysohn.realeconomy.manager.banking.CapitalManagementUtil;
import io.github.wysohn.realeconomy.manager.banking.account.TradingAccount;
import io.github.wysohn.realeconomy.manager.currency.Currency;
import io.github.wysohn.realeconomy.manager.currency.CurrencyManager;

import javax.inject.Inject;
import javax.persistence.Column;
import javax.persistence.Transient;
import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Represents a bank where transactions occur. This instance is used
 * for synchronization, so if multiple transactions must happen at the same time,
 * the caller may acquire monitor, which is the instance itself.
 */
public abstract class AbstractBank extends CachedElement<UUID> implements IPluginObject, IFinancialEntity, IAssetHolder {
    @Transient
    public static final String BANK_MARK = "\u2608";

    @Inject
    @Transient
    private Set<IBankOwnerProvider> ownerProviders;
    @Inject
    @Transient
    private CurrencyManager currencyManager;
    @Inject
    @Transient
    @MinCapital
    protected BigDecimal minimum;
    @Inject
    @Transient
    @MaxCapital
    protected BigDecimal maximum;

    // Currency uuid -> value
    @Column
    private final Map<UUID, BigDecimal> capitals = new ConcurrentHashMap<>();
    @Column
    private final Map<UUID, Map<IBankingType, IAccount>> accounts = new ConcurrentHashMap<>();
    @Column
    private final List<Asset> ownedAssets = Collections.synchronizedList(new ArrayList<>());

    @Column
    private UUID bankOwnerUuid;
    @Column
    private UUID baseCurrencyUuid;

    @Column
    private boolean operating = true;

    public AbstractBank(UUID key) {
        super(key);
    }

    private AbstractBank(AbstractBank copy){
        super(copy.getKey());
        ownerProviders = copy.ownerProviders;
        currencyManager = copy.currencyManager;
        minimum = copy.minimum;
        maximum = copy.maximum;
        capitals.putAll(copy.capitals);
        accounts.putAll(deepCopy(copy.accounts));
        ownedAssets.addAll(copy.ownedAssets.stream()
                .map(Asset::clone)
                .collect(Collectors.toList()));
        bankOwnerUuid = copy.bankOwnerUuid;
        baseCurrencyUuid = copy.baseCurrencyUuid;
        operating = copy.operating;
    }

    private Map<UUID, Map<IBankingType, IAccount>> deepCopy(Map<UUID, Map<IBankingType, IAccount>> accounts) {
        Map<UUID, Map<IBankingType, IAccount>> copy = new HashMap<>();
        accounts.forEach((uuid, iBankingTypeIAccountMap) -> {
            Map<IBankingType, IAccount> accountMap = copy.computeIfAbsent(uuid, u -> new HashMap<>());
            iBankingTypeIAccountMap.forEach((type, acc) -> {
                accountMap.put(type, acc.clone());
            });
        });
        return copy;
    }

    public IBankOwner getBankOwner() {
        return Optional.ofNullable(read(() -> bankOwnerUuid))
                .flatMap(uuid -> ownerProviders.stream()
                        .map(provider -> provider.get(uuid))
                        .findAny())
                .orElse(null);
    }

    public void setBankOwner(IBankOwner owner) {
        mutate(() -> this.bankOwnerUuid = owner.getUuid());
    }

    public Currency getBaseCurrency() {
        return Optional.ofNullable(read(() -> baseCurrencyUuid))
                .flatMap(currencyManager::get)
                .orElse(null);
    }

    protected UUID getBaseCurrencyUuid() {
        return read(() -> baseCurrencyUuid);
    }

    public void setBaseCurrency(Currency currency) {
        if (this.baseCurrencyUuid != null)
            throw new RuntimeException("BaseCurrency can be set only once.");

        mutate(() -> this.baseCurrencyUuid = currency.getKey());
    }

    public boolean isOperating() {
        return read(() -> operating);
    }

    public void setOperating(boolean operating) {
        mutate(() -> this.operating = operating);
    }

    @Override
    public UUID getUuid() {
        return getKey();
    }

    @Override
    public BigDecimal balance(Currency currency) {
        return read(() -> CapitalManagementUtil.balance(capitals, currency));
    }

    @Override
    public boolean deposit(BigDecimal value, Currency currency) {
        if (!operating)
            throw new RuntimeException("Cannot use the bank that is closed. Bank: " + getStringKey());

        return mutate(() -> CapitalManagementUtil.deposit(maximum, capitals, value, currency));
    }

    @Override
    public boolean withdraw(BigDecimal value, Currency currency) {
        if (!operating)
            throw new RuntimeException("Cannot use the bank that is closed. Bank: " + getStringKey());

        return mutate(() -> CapitalManagementUtil.withdraw(minimum, capitals, value, currency, true));
    }

    public boolean hasAccount(IBankUser user, IBankingType type) {
        return read(() -> {
            Map<IBankingType, IAccount> accountMap = accounts.get(user.getUuid());
            return accountMap.containsKey(type);
        });
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

        return mutate(() -> {
            Map<IBankingType, IAccount> accountMap = accounts.computeIfAbsent(user.getUuid(),
                    key -> new HashMap<>());

            if (accountMap.containsKey(type))
                return false;

            Validation.validate(accountMap.put(type, type.createAccount()),
                    Objects::isNull,
                    "Inconsistent Map behavior.");
            
            return true;
        });
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

        return mutate(() -> {
            Map<IBankingType, IAccount> accountMap = accounts.get(user.getUuid());
            if (accountMap == null)
                return false;

            return accountMap.remove(type) != null;
        });
    }

    private IAccount getOrThrowAccount(IBankUser user, IBankingType type) {
        Map<IBankingType, IAccount> accountMap = accounts.get(user.getUuid());
        if (accountMap == null)
            throw new RuntimeException("Account of " + user + " does not exist.");

        IAccount account = accountMap.get(type);
        if (account == null)
            throw new RuntimeException("Account of " + user + " does not exist.");
        return account;
    }

    public void addAccountAsset(IBankUser user, Asset asset) {
        if (!operating)
            throw new RuntimeException("Cannot use the bank that is closed. Bank: " + getStringKey());

        Validation.assertNotNull(user);
        Validation.assertNotNull(asset);

        mutate(() -> {
            IAccount account = getOrThrowAccount(user, BankingTypeRegistry.TRADING);
            TradingAccount tradingAccount = (TradingAccount) account;
            tradingAccount.addAsset(asset);
        });
    }

    public double countAccountAsset(IBankUser user, AssetSignature signature) {
        if (!operating)
            throw new RuntimeException("Cannot use the bank that is closed. Bank: " + getStringKey());

        Validation.assertNotNull(user);
        Validation.assertNotNull(signature);

        return read(() -> {
            IAccount account = getOrThrowAccount(user, BankingTypeRegistry.TRADING);
            TradingAccount tradingAccount = (TradingAccount) account;
            return tradingAccount.countAsset(signature);
        });
    }

    public Collection<Asset> removeAccountAsset(IBankUser user, AssetSignature signature, double amount) {
        if (!operating)
            throw new RuntimeException("Cannot use the bank that is closed. Bank: " + getStringKey());

        Validation.assertNotNull(user);
        Validation.assertNotNull(signature);
        Validation.validate(amount, val -> val >= 0.0, "negative amount not allowed.");

        return mutate(() -> {
            IAccount account = getOrThrowAccount(user, BankingTypeRegistry.TRADING);
            TradingAccount tradingAccount = (TradingAccount) account;
            return tradingAccount.removeAsset(signature, amount);
        });
    }

    public Asset removeAccountAsset(IBankUser user, int index) {
        if (!operating)
            throw new RuntimeException("Cannot use the bank that is closed. Bank: " + getStringKey());

        Validation.assertNotNull(user);
        Validation.validate(index, val -> val >= 0, "negative index not allowed.");

        return mutate(() -> {
            IAccount account = getOrThrowAccount(user, BankingTypeRegistry.TRADING);
            TradingAccount tradingAccount = (TradingAccount) account;
            return tradingAccount.removeAsset(index);
        });
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

        return re
    }

    public boolean depositAccount(IBankUser user,
                                               IBankingType type,
                                               BigDecimal amount,
                                               Currency currency) {
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

            return CapitalManagementUtil.deposit(maximum, account.getCurrencyMap(), amount, currency);
        }, false);
        
        mutate(() -> synchronousAccountTask(user, type, visitor));

        return visitor.result;
    }

    public boolean depositAccount(IBankUser user, IBankingType type, double amount, Currency currency) {
        return depositAccount(user, type, BigDecimal.valueOf(amount), currency);
    }

    public boolean withdrawAccount(IBankUser user,
                                                IBankingType type,
                                                BigDecimal amount,
                                                Currency currency) {
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
            return CapitalManagementUtil.withdraw(accountMinimum, account.getCurrencyMap(), amount, currency, true);
        }, false);
        
        mutate(()->synchronousAccountTask(user, type, visitor));

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

            return CapitalManagementUtil.balance(account.getCurrencyMap(), currency);
        }, BigDecimal.valueOf(0.0));
        
        read(() -> synchronousAccountTask(user, type, visitor));

        return visitor.result;
    }

    @Override
    public void addAsset(Asset asset) {
        if (!operating)
            throw new RuntimeException("Cannot use the bank that is closed. Bank: " + getStringKey());

        mutate(() -> AssetUtil.addAsset(ownedAssets, asset));
    }

    @Override
    public double countAsset(AssetSignature signature) {
        return read(() -> AssetUtil.countAsset(ownedAssets, signature));
    }

    @Override
    public Collection<Asset> removeAsset(AssetSignature signature, double amount) {
        if (!read(() -> operating))
            throw new RuntimeException("Cannot use the bank that is closed. Bank: " + getStringKey());

        return mutate(() -> AssetUtil.removeAsset(ownedAssets, signature, amount));
    }

    @Override
    public Asset removeAsset(int index) {
        if (!operating)
            throw new RuntimeException("Cannot use the bank that is closed. Bank: " + getStringKey());

        return mutate(() -> AssetUtil.removeAsset(ownedAssets, index));
    }

    @Override
    public DataProvider<Asset> assetDataProvider() {
        return AssetUtil.assetDataProvider(Collections.unmodifiableList(ownedAssets));
    }

    @Override
    public Map<Object, Object> properties(ManagerLanguage lang, ICommandSender sender) {
        return read(() -> {
            Map<Object, Object> properties = new LinkedHashMap<>();
            Optional.ofNullable(getBankOwner())
                    .ifPresent(owner -> properties.put(RealEconomyLangs.Bank_Owner, owner));
            Optional.ofNullable(getBaseCurrency())
                    .ifPresent(currency -> properties.put(RealEconomyLangs.Bank_BaseCurrency, currency));
            properties.put(RealEconomyLangs.Bank_NumAccounts, accounts.size());
            return properties;
        });
    }

    @Override
    public String toString() {
        return BANK_MARK + getStringKey();
    }

    @Override
    public IMemento saveState() {
        return read(() -> new AbstractMemento(this));
    }

    @Override
    public void restoreState(IMemento memento) {
        AbstractMemento mem = (AbstractMemento) memento;

        mutate(() -> {
            this.capitals.clear();
            this.capitals.putAll(mem.capitals);

            this.accounts.forEach((uuid, accountMap) -> {
                Map<IBankingType, IMemento> statesMap = mem.accountStates.get(uuid);
                if (statesMap == null)
                    return;

                accountMap.forEach((type, account) -> account.restoreState(statesMap.get(type)));
            });
        });
    }

    protected static class AbstractMemento implements IMemento {
        private final Map<UUID, BigDecimal> capitals = new HashMap<>();
        private final Map<UUID, Map<IBankingType, IMemento>> accountStates = new HashMap<>();

        public AbstractMemento(AbstractBank bank) {
            capitals.putAll(bank.capitals);
            accountStates.putAll(createAccountStates(bank.accounts));
        }
    }

    private static Map<UUID, Map<IBankingType, IMemento>> createAccountStates(Map<UUID, Map<IBankingType, IAccount>> from) {
        Map<UUID, Map<IBankingType, IMemento>> mapParent = new HashMap<>();
        from.forEach((uuid, accountMap) -> {
            Map<IBankingType, IMemento> stateMap = new HashMap<>();
            (accountMap) {
                accountMap.forEach((type, account) -> stateMap.put(type, account.saveState()));
            }
            mapParent.put(uuid, stateMap);
        });
        return mapParent;
    }
}
