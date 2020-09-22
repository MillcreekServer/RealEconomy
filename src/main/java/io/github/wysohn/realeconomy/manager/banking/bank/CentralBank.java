package io.github.wysohn.realeconomy.manager.banking.bank;

import io.github.wysohn.rapidframework3.core.caching.CachedElement;
import io.github.wysohn.rapidframework3.interfaces.IMemento;
import io.github.wysohn.realeconomy.inject.annotation.MaxCapital;
import io.github.wysohn.realeconomy.manager.currency.Currency;

import javax.inject.Inject;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

public class CentralBank extends AbstractBank {
    @Inject
    @MaxCapital
    private BigDecimal maximum;

    private BigDecimal liquidity = BigDecimal.ZERO;

    private CentralBank() {
        super(null);
    }

    public CentralBank(UUID key) {
        super(key);
    }

    public BigDecimal getLiquidity() {
        return Optional.ofNullable(liquidity)
                .orElse(BigDecimal.ZERO);
    }

    @Override
    public BigDecimal balance(Currency currency) {
        // CentralBank can print the currency, so it never runs out its own currency.
        return Optional.ofNullable(currency)
                .map(CachedElement::getKey)
                .filter(uuid -> uuid.equals(getBaseCurrencyUuid()))
                .map(uuid -> maximum) // always maximum
                .orElseGet(() -> super.balance(currency));
    }

    @Override
    public boolean deposit(BigDecimal value, Currency currency) {
        return Optional.ofNullable(currency)
                .map(CachedElement::getKey)
                .filter(uuid -> uuid.equals(getBaseCurrencyUuid()))
                .map(uuid -> {
                    // collecting currencies
                    liquidity = liquidity.subtract(value);
                    return true;
                })
                .orElseGet(() -> super.deposit(value, currency));
    }

    @Override
    public boolean withdraw(BigDecimal value, Currency currency) {
        return Optional.ofNullable(currency)
                .map(CachedElement::getKey)
                .filter(uuid -> uuid.equals(getBaseCurrencyUuid()))
                .map(uuid -> {
                    // printing currencies
                    liquidity = liquidity.add(value);
                    return true;
                })
                .orElseGet(() -> super.withdraw(value, currency));
    }

    @Override
    public IMemento saveState() {
        return new Memento(this);
    }

    @Override
    public void restoreState(IMemento memento) {
        Memento mem = (Memento) memento;
        super.restoreState(mem);

        this.liquidity = mem.liquidity;
    }

    private static class Memento extends AbstractMemento {
        private final BigDecimal liquidity;

        public Memento(CentralBank bank) {
            super(bank);
            this.liquidity = bank.liquidity;
        }
    }
}
