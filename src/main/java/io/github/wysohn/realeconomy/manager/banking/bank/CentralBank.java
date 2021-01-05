package io.github.wysohn.realeconomy.manager.banking.bank;

import io.github.wysohn.rapidframework3.core.caching.CachedElement;
import io.github.wysohn.rapidframework3.core.language.ManagerLanguage;
import io.github.wysohn.rapidframework3.interfaces.ICommandSender;
import io.github.wysohn.rapidframework3.interfaces.IMemento;
import io.github.wysohn.rapidframework3.utils.Validation;
import io.github.wysohn.realeconomy.main.Metrics;
import io.github.wysohn.realeconomy.main.RealEconomyLangs;
import io.github.wysohn.realeconomy.manager.asset.Asset;
import io.github.wysohn.realeconomy.manager.currency.Currency;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class CentralBank extends AbstractBank {
    private transient final Object transactionLock = new Object();

    private BigDecimal numPapers = BigDecimal.ZERO;
    private BigDecimal liquidity = BigDecimal.ZERO;

    private boolean limitlessPapers = false;

    private CentralBank() {
        super(null);
    }

    public CentralBank(UUID key) {
        super(key);
    }

    public BigDecimal getNumPapers() {
        return numPapers;
    }

    public void setNumPapers(BigDecimal numPapers) {
        this.numPapers = numPapers;
        notifyObservers();
    }

    public boolean isLimitlessPapers() {
        return limitlessPapers;
    }

    public void setLimitlessPapers(boolean limitlessPapers) {
        this.limitlessPapers = limitlessPapers;
        notifyObservers();
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
                        numPapers = numPapers.add(value);
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

        // not enough papers
        if (!limitlessPapers && numPapers.compareTo(value) < 0) {
            return false;
        }

        return Optional.ofNullable(currency)
                .map(CachedElement::getKey)
                .filter(uuid -> uuid.equals(getBaseCurrencyUuid()))
                .map(uuid -> {
                    synchronized (transactionLock) {
                        numPapers = numPapers.subtract(value);
                        // printing currencies
                        liquidity = liquidity.add(value);
                        notifyObservers();
                    }
                    return true;
                })
                .orElseGet(() -> super.withdraw(value, currency));
    }

    @Override
    public int realizeAsset(Asset asset) {
        addAsset(asset);
        return 0;
    }

    @Override
    public Map<Object, Object> properties(ManagerLanguage lang, ICommandSender sender) {
        Map<Object, Object> properties = super.properties(lang, sender);
        properties.put(RealEconomyLangs.Bank_Liquidity, Metrics.df.format(liquidity) + " " + getBaseCurrency());
        properties.put(RealEconomyLangs.Bank_Papers, numPapers.toBigInteger().toString());
        properties.put(RealEconomyLangs.Bank_PaperUnlimited, limitlessPapers ? "&aO" : "&cX");
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
