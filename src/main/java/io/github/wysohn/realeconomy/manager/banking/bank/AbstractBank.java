package io.github.wysohn.realeconomy.manager.banking.bank;

import io.github.wysohn.rapidframework3.core.caching.CachedElement;
import io.github.wysohn.realeconomy.interfaces.banking.IBankOwner;
import io.github.wysohn.realeconomy.interfaces.banking.IBankOwnerProvider;
import io.github.wysohn.realeconomy.manager.asset.Loan;
import io.github.wysohn.realeconomy.manager.currency.Currency;
import io.github.wysohn.realeconomy.manager.currency.CurrencyManager;

import javax.inject.Inject;
import java.lang.ref.Reference;
import java.util.*;

public abstract class AbstractBank extends CachedElement<UUID> {
    @Inject
    private Set<IBankOwnerProvider> ownerProviders;
    @Inject
    private CurrencyManager currencyManager;

    private final Map<UUID, Double> capitals = new HashMap<>();
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
}
