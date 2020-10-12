package io.github.wysohn.realeconomy.manager.user;

import io.github.wysohn.rapidframework3.bukkit.data.BukkitPlayer;
import io.github.wysohn.rapidframework3.core.paging.DataProviderProxy;
import io.github.wysohn.rapidframework3.interfaces.IMemento;
import io.github.wysohn.rapidframework3.interfaces.paging.DataProvider;
import io.github.wysohn.rapidframework3.interfaces.plugin.ITaskSupervisor;
import io.github.wysohn.rapidframework3.utils.Pair;
import io.github.wysohn.realeconomy.interfaces.banking.IBankUser;
import io.github.wysohn.realeconomy.interfaces.banking.ITransactionHandler;
import io.github.wysohn.realeconomy.manager.currency.Currency;

import javax.inject.Inject;
import java.math.BigDecimal;
import java.util.*;

public class User extends BukkitPlayer implements IBankUser {
    @Inject
    private ITransactionHandler transactionHandler;
    @Inject
    private ITaskSupervisor task;

    private final Map<UUID, BigDecimal> wallet = new HashMap<>();

    private transient DataProvider<Pair<UUID, BigDecimal>> balanceProvider;

    private User() {
        super(null);
    }

    public User(UUID key) {
        super(key);
    }

    @Override
    public UUID getUuid() {
        return getKey();
    }

    @Override
    public BigDecimal balance(Currency currency) {
        synchronized (wallet) {
            return transactionHandler.balance(wallet, currency);
        }
    }

    @Override
    public boolean deposit(BigDecimal value, Currency currency) {
        synchronized (wallet) {
            final boolean deposit = transactionHandler.deposit(wallet, value, currency);
            notifyObservers();
            return deposit;
        }
    }

    @Override
    public boolean withdraw(BigDecimal value, Currency currency) {
        synchronized (wallet) {
            final boolean withdraw = transactionHandler.withdraw(wallet, value, currency);
            notifyObservers();
            return withdraw;
        }
    }

    /**
     * Clear all wallet and return all of the content of the wallet.
     *
     * @return
     */
    public List<Pair<UUID, BigDecimal>> clearWallet() {
        List<Pair<UUID, BigDecimal>> copy = new ArrayList<>();
        synchronized (wallet) {
            wallet.forEach((uuid, bigDecimal) -> copy.add(Pair.of(uuid, bigDecimal)));
            wallet.clear();
            notifyObservers();
        }
        return copy;
    }

    public DataProvider<Pair<UUID, BigDecimal>> balancesPagination() {
        if (balanceProvider == null) {
            balanceProvider = new DataProviderProxy<>(range -> {
                List<Pair<UUID, BigDecimal>> copy = new ArrayList<>();
                synchronized (wallet) {
                    wallet.forEach((uuid, bigDecimal) -> copy.add(Pair.of(uuid, bigDecimal)));
                }
                copy.sort((a, b) -> b.value.compareTo(a.value));
                return copy.subList(range.index, Math.min(copy.size(), range.index + range.size));
            }, () -> {
                synchronized (wallet) {
                    return wallet.size();
                }
            });
        }
        return balanceProvider;
    }

    @Override
    public IMemento saveState() {
        return new Memento(super.saveState(), this);
    }

    @Override
    public void restoreState(IMemento memento) {
        Memento mem = (Memento) memento;
        super.restoreState(mem.parentState);

        synchronized (wallet) {
            wallet.clear();
            wallet.putAll(mem.wallet);
            notifyObservers();
        }
    }

    private static class Memento implements IMemento {
        private final IMemento parentState;
        private final Map<UUID, BigDecimal> wallet = new HashMap<>();

        public Memento(IMemento parentState, User user) {
            this.parentState = parentState;
            synchronized (wallet) {
                //UUID and BigDecimal are both immutable
                wallet.putAll(user.wallet);
            }
        }
    }

    @Override
    public String toString() {
        return Optional.ofNullable(getStringKey())
                .orElse("[?]");
    }
}
