package io.github.wysohn.realeconomy.manager.account;

import io.github.wysohn.rapidframework3.core.caching.CachedElement;
import io.github.wysohn.realeconomy.inject.annotation.MaxCapital;
import io.github.wysohn.realeconomy.inject.annotation.MinCapital;
import io.github.wysohn.realeconomy.interfaces.IFinancialEntity;
import io.github.wysohn.realeconomy.interfaces.IMemento;
import io.github.wysohn.realeconomy.manager.currency.Currency;

import javax.inject.Inject;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class Account extends CachedElement<UUID> implements IFinancialEntity {
    @Inject
    @MinCapital
    private BigDecimal minimum;
    @Inject
    @MaxCapital
    private BigDecimal maximum;

    private final Map<UUID, BigDecimal> balances = new HashMap<>();

    private Account() {
        super(null);
    }

    public Account(UUID key) {
        super(key);
    }

    @Override
    public BigDecimal balance(Currency currency) {
        return Optional.of(currency)
                .map(CachedElement::getKey)
                .map(balances::get)
                .orElse(BigDecimal.ZERO);
    }

    @Override
    public boolean deposit(BigDecimal value, Currency currency) {
        final boolean aBoolean = Optional.of(currency)
                .map(CachedElement::getKey)
                .map(uuid -> {
                    BigDecimal current = balances.getOrDefault(uuid, BigDecimal.ZERO);
                    BigDecimal added = current.add(value);

                    if (added.compareTo(maximum) > 0)
                        return false;

                    balances.put(uuid, added);
                    return true;
                })
                .orElse(false);
        notifyObservers();
        return aBoolean;
    }

    @Override
    public boolean withdraw(BigDecimal value, Currency currency) {
        final boolean aBoolean = Optional.of(currency)
                .map(CachedElement::getKey)
                .map(uuid -> {
                    BigDecimal current = balances.getOrDefault(uuid, BigDecimal.ZERO);
                    BigDecimal subtracted = current.subtract(value);

                    if (subtracted.compareTo(minimum) < 0)
                        return false;

                    balances.put(uuid, subtracted);
                    return true;
                })
                .orElse(false);
        notifyObservers();
        return aBoolean;
    }

    @Override
    public UUID getUuid() {
        return getKey();
    }

    @Override
    public IMemento saveState() {
        return new Memento(this);
    }

    @Override
    public void restoreState(IMemento memento) {
        Memento mem = (Memento) memento;

        this.balances.clear();
        this.balances.putAll(mem.balances);
    }

    private class Memento implements IMemento {
        private final Map<UUID, BigDecimal> balances = new HashMap<>();

        public Memento(Account account) {
            balances.putAll(account.balances);
        }
    }
}
