package io.github.wysohn.realeconomy.manager.banking.bank;

import io.github.wysohn.rapidframework3.core.caching.CachedElement;
import io.github.wysohn.rapidframework3.interfaces.IMemento;
import io.github.wysohn.rapidframework3.interfaces.IPluginObject;
import io.github.wysohn.rapidframework3.utils.FailSensitiveTask;
import io.github.wysohn.rapidframework3.utils.Validation;
import io.github.wysohn.realeconomy.inject.annotation.MaxCapital;
import io.github.wysohn.realeconomy.inject.annotation.MinCapital;
import io.github.wysohn.realeconomy.interfaces.IFinancialEntity;
import io.github.wysohn.realeconomy.interfaces.banking.*;
import io.github.wysohn.realeconomy.manager.currency.Currency;
import io.github.wysohn.realeconomy.manager.currency.CurrencyManager;

import javax.inject.Inject;
import java.lang.ref.Reference;
import java.math.BigDecimal;
import java.util.*;

public abstract class AbstractBank extends CachedElement<UUID> implements IFinancialEntity {
    private transient final Object transactionLock = new Object();

    @Inject
    private ITransactionHandler transactionHandler;
    @Inject
    private Set<IBankOwnerProvider> ownerProviders;
    @Inject
    private CurrencyManager currencyManager;
    @Inject
    @MinCapital
    private BigDecimal minimum;
    @Inject
    @MaxCapital
    private BigDecimal maximum;

    // Currency uuid -> value
    private final Map<UUID, BigDecimal> capitals = new HashMap<>();
    private final Map<UUID, Map<IBankingType, IAccount>> accounts = new HashMap<>();

    private UUID bankOwnerUuid;
    private UUID baseCurrencyUuid;

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

    @Override
    public UUID getUuid() {
        return getKey();
    }

    @Override
    public BigDecimal balance(Currency currency) {
        return transactionHandler.balance(capitals, currency);
    }

    @Override
    public boolean deposit(BigDecimal value, Currency currency) {
        synchronized (transactionLock) {
            final boolean aBoolean = transactionHandler.deposit(capitals, value, currency);
            notifyObservers();
            return aBoolean;
        }
    }

    @Override
    public boolean withdraw(BigDecimal value, Currency currency) {
        synchronized (transactionLock) {
            final boolean aBoolean = transactionHandler.withdraw(capitals, value, currency);
            notifyObservers();
            return aBoolean;
        }
    }

    /**
     * Get a snapshot of the Account. Changes made to the IAccount instance provided does
     * not have any effect. To make modification, use {@link #accountTransaction(IBankUser, IBankingType)}.
     *
     * @param user the owner of account.
     * @param type the account type to get from this bank.
     * @return
     */
    public IAccount getAccount(IBankUser user, IBankingType type) {
        Validation.assertNotNull(type);

        return Optional.ofNullable(user)
                .map(IPluginObject::getUuid)
                .map(accounts::get)
                .map(accountMap -> accountMap.get(type))
                .map(IAccount::clone)
                .orElse(null);
    }

    /**
     * @param user the owner of account.
     * @param type the account type to be added to this bank.
     * @return true if newly created; false if the account type already exist
     */
    public boolean putAccount(IBankUser user, IBankingType type) {
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
        Validation.assertNotNull(user);

        Map<IBankingType, IAccount> accountMap = accounts.get(user.getUuid());
        if (accountMap == null)
            return false;

        final boolean deleted = accountMap.remove(type) != null;
        if (deleted) notifyObservers();
        return deleted;
    }

    /**
     * Begin a transaction for the specified user. You may perform any number of
     * transaction using the Transaction instance, yet none of the changes will have
     * effect until you invoke {@link Transaction#commit()}. If something goes wrong
     * while processing the transaction, account's state will be reverted.
     *
     * @param user the user
     * @param type type of account
     * @return Transaction instance
     * @throws RuntimeException This does not check if the user has an account already.
     *                          So if no account is found, exception will be thrown.
     */
    public Transaction accountTransaction(IBankUser user, IBankingType type) {
        Validation.assertNotNull(user);
        if (!accounts.containsKey(user.getUuid()))
            throw new RuntimeException("Account of " + user + " does not exist.");

        Map<IBankingType, IAccount> accountMap = accounts.get(user.getUuid());
        if (!accountMap.containsKey(type))
            throw new RuntimeException("Account of " + user + " does not exist.");

        return new Transaction(accountMap.get(type));
    }

    public class Transaction {
        private final IAccount account;
        private final List<Node> transactions = new ArrayList<>();

        private Transaction(IAccount account) {
            this.account = account;
        }

        public Transaction deposit(BigDecimal value) {
            transactions.add(new DepositNode(value));
            return this;
        }

        public Transaction deposit(double value) {
            return this.deposit(BigDecimal.valueOf(value));
        }

        public Transaction withdraw(BigDecimal value) {
            transactions.add(new WithdrawNode(value));
            return this;
        }

        public Transaction withdraw(double value) {
            return this.withdraw(BigDecimal.valueOf(value));
        }

        public void commit() {
            synchronized (transactionLock) {
                IMemento savedState = saveState();
                FailSensitiveTask.of(() -> {
                    for (Runnable transaction : transactions) {
                        try {
                            transaction.run();
                        } catch (Exception ex) {
                            throw new RuntimeException("Transaction failure", ex);
                        }
                    }
                    notifyObservers();
                    return true;
                }).handleException(Throwable::printStackTrace)
                        .onFail(() -> restoreState(savedState))
                        .run();
            }
        }

        private abstract class Node implements Runnable {
            protected final BigDecimal value;

            public Node(BigDecimal value) {
                this.value = value;
            }

            @Override
            public String toString() {
                return "[" + getKey() + ":" + getStringKey() + "] "
                        + account + ", "
                        + getClass().getSimpleName() + ", "
                        + value;
            }
        }

        private class DepositNode extends Node {
            public DepositNode(BigDecimal value) {
                super(value);
            }

            @Override
            public void run() {
                if (transactionHandler.deposit(account.getBalanceMap(),
                        value,
                        Objects.requireNonNull(getBaseCurrency()))) {
                    if (!AbstractBank.this.deposit(value, Objects.requireNonNull(getBaseCurrency()))) {
                        throw new RuntimeException("Bank deposit refused.");
                    }
                } else {
                    throw new RuntimeException("Account deposit refused.");
                }
            }
        }

        private class WithdrawNode extends Node {
            public WithdrawNode(BigDecimal value) {
                super(value);
            }

            @Override
            public void run() {
                if (AbstractBank.this.withdraw(value, Objects.requireNonNull(getBaseCurrency()))) {
                    if (!transactionHandler.withdraw(account.getBalanceMap(),
                            value,
                            Objects.requireNonNull(getBaseCurrency()))) {
                        throw new RuntimeException("Account withdraw refused.");
                    }
                } else {
                    throw new RuntimeException("Bank withdraw refused.");
                }
            }
        }
    }

    @Override
    public IMemento saveState() {
        return new AbstractMemento(this);
    }

    @Override
    public void restoreState(IMemento memento) {
        AbstractMemento mem = (AbstractMemento) memento;

        this.capitals.clear();
        this.capitals.putAll(mem.capitals);

        this.accounts.clear();
        this.accounts.putAll(mem.accounts);
    }

    protected static class AbstractMemento implements IMemento {
        private final Map<UUID, BigDecimal> capitals = new HashMap<>();
        private final Map<UUID, Map<IBankingType, IAccount>> accounts = new HashMap<>();

        public AbstractMemento(AbstractBank bank) {
            capitals.putAll(bank.capitals);
            // make a deep copy
            accounts.putAll(createDeepCopy(bank.accounts));
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
}
