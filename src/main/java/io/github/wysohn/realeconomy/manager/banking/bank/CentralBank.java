package io.github.wysohn.realeconomy.manager.banking.bank;

import io.github.wysohn.rapidframework3.core.caching.CachedElement;
import io.github.wysohn.rapidframework3.interfaces.IMemento;
import io.github.wysohn.rapidframework3.interfaces.language.ILang;
import io.github.wysohn.rapidframework3.utils.Validation;
import io.github.wysohn.realeconomy.main.Metrics;
import io.github.wysohn.realeconomy.main.RealEconomyLangs;
import io.github.wysohn.realeconomy.manager.currency.Currency;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class CentralBank extends AbstractBank {
    private transient final Object transactionLock = new Object();

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
        Validation.assertNotNull(getBaseCurrencyUuid());

        return Optional.ofNullable(currency)
                .map(CachedElement::getKey)
                .filter(uuid -> uuid.equals(getBaseCurrencyUuid()))
                .map(uuid -> {
                    synchronized (transactionLock) {
                        // collecting currencies
                        liquidity = liquidity.subtract(value);
                        notifyObservers();
                    }
                    return true;
                })
                .orElseGet(() -> super.deposit(value, currency));
    }

    @Override
    public boolean withdraw(BigDecimal value, Currency currency) {
        Validation.assertNotNull(getBaseCurrencyUuid());

        return Optional.ofNullable(currency)
                .map(CachedElement::getKey)
                .filter(uuid -> uuid.equals(getBaseCurrencyUuid()))
                .map(uuid -> {
                    synchronized (transactionLock) {
                        // printing currencies
                        liquidity = liquidity.add(value);
                        notifyObservers();
                    }
                    return true;
                })
                .orElseGet(() -> super.withdraw(value, currency));
    }

    @Override
    public Map<ILang, Object> properties() {
        Map<ILang, Object> properties = super.properties();
        properties.put(RealEconomyLangs.Bank_Liquidity, Metrics.df.format(liquidity) + " " + getBaseCurrency());
        return properties;
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
        notifyObservers();
    }

    private static class Memento extends AbstractMemento {
        private final BigDecimal liquidity;

        public Memento(CentralBank bank) {
            super(bank);
            this.liquidity = bank.liquidity;
        }
    }
}
