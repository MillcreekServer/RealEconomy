package io.github.wysohn.realeconomy.manager.banking.bank;

import io.github.wysohn.rapidframework3.core.caching.CachedElement;
import io.github.wysohn.realeconomy.inject.annotation.MaxCapital;
import io.github.wysohn.realeconomy.inject.annotation.MinCapital;
import io.github.wysohn.realeconomy.interfaces.IFinancialEntity;
import io.github.wysohn.realeconomy.interfaces.IMemento;
import io.github.wysohn.realeconomy.interfaces.banking.IBankOwner;
import io.github.wysohn.realeconomy.interfaces.banking.IBankOwnerProvider;
import io.github.wysohn.realeconomy.manager.account.AccountManager;
import io.github.wysohn.realeconomy.manager.asset.Loan;
import io.github.wysohn.realeconomy.manager.currency.Currency;
import io.github.wysohn.realeconomy.manager.currency.CurrencyManager;

import javax.inject.Inject;
import java.lang.ref.Reference;
import java.math.BigDecimal;
import java.util.*;

public abstract class AbstractBank extends CachedElement<UUID> implements IFinancialEntity {
    @Inject
    private Set<IBankOwnerProvider> ownerProviders;
    @Inject
    private CurrencyManager currencyManager;
    @Inject
    private AccountManager accountManager;
    @Inject
    @MinCapital
    private BigDecimal minimum;
    @Inject
    @MaxCapital
    private BigDecimal maximum;

    private final Map<UUID, BigDecimal> capitals = new HashMap<>();
    private final Map<UUID, Loan> loans = new HashMap<>();

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

    public void setBaseCurrency(Currency currency) {
        this.baseCurrencyUuid = currency.getKey();

        notifyObservers();
    }

    @Override
    public UUID getUuid() {
        return getKey();
    }

    @Override
    public BigDecimal balance(Currency currency) {
        return Optional.of(currency)
                .map(CachedElement::getKey)
                .map(capitals::get)
                .orElse(BigDecimal.ZERO);
    }

    @Override
    public boolean deposit(BigDecimal value, Currency currency) {
        if (value.signum() < 0)
            throw new RuntimeException("Cannot use negative value.");

        final boolean aBoolean = Optional.of(currency)
                .map(CachedElement::getKey)
                .map(uuid -> {
                    BigDecimal current = capitals.getOrDefault(uuid, BigDecimal.ZERO);
                    BigDecimal added = current.add(value);

                    if (added.compareTo(maximum) > 0)
                        return false;

                    capitals.put(uuid, added);
                    return true;
                })
                .orElse(false);
        notifyObservers();
        return aBoolean;
    }

    @Override
    public boolean withdraw(BigDecimal value, Currency currency) {
        if (value.signum() < 0)
            throw new RuntimeException("Cannot use negative value.");

        final boolean aBoolean = Optional.of(currency)
                .map(CachedElement::getKey)
                .map(uuid -> {
                    BigDecimal current = capitals.getOrDefault(uuid, BigDecimal.ZERO);
                    BigDecimal subtracted = current.subtract(value);

                    if (subtracted.compareTo(minimum) < 0)
                        return false;

                    capitals.put(uuid, subtracted);
                    return true;
                })
                .orElse(false);
        notifyObservers();
        return aBoolean;
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

        this.loans.clear();
        this.loans.putAll(mem.loans);
    }

    protected static class AbstractMemento implements IMemento {
        private final Map<UUID, BigDecimal> capitals = new HashMap<>();
        private final Map<UUID, Loan> loans = new HashMap<>();

        public AbstractMemento(AbstractBank bank) {
            capitals.putAll(bank.capitals);
            loans.putAll(bank.loans);
        }
    }
}
