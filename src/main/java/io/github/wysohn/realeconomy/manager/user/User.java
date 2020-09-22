package io.github.wysohn.realeconomy.manager.user;

import io.github.wysohn.rapidframework3.bukkit.data.BukkitPlayer;
import io.github.wysohn.rapidframework3.interfaces.IMemento;
import io.github.wysohn.realeconomy.interfaces.banking.IBankUser;
import io.github.wysohn.realeconomy.interfaces.banking.ITransactionHandler;
import io.github.wysohn.realeconomy.manager.currency.Currency;

import javax.inject.Inject;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class User extends BukkitPlayer implements IBankUser {
    @Inject
    private ITransactionHandler transactionHandler;

    private final Map<UUID, BigDecimal> wallet = new HashMap<>();

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
        return transactionHandler.balance(wallet, currency);
    }

    @Override
    public boolean deposit(BigDecimal value, Currency currency) {
        return transactionHandler.deposit(wallet, value, currency);
    }

    @Override
    public boolean withdraw(BigDecimal value, Currency currency) {
        return transactionHandler.withdraw(wallet, value, currency);
    }

    @Override
    public IMemento saveState() {
        return new Memento(super.saveState(), this);
    }

    @Override
    public void restoreState(IMemento memento) {
        Memento mem = (Memento) memento;
        super.restoreState(mem.parentState);

        wallet.clear();
        wallet.putAll(mem.wallet);
    }

    private static class Memento implements IMemento {
        private final IMemento parentState;
        private final Map<UUID, BigDecimal> wallet = new HashMap<>();

        public Memento(IMemento parentState, User user) {
            this.parentState = parentState;
            //UUID and BigDecimal are both immutable
            wallet.putAll(user.wallet);
        }
    }
}
