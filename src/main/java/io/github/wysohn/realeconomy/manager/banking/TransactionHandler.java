package io.github.wysohn.realeconomy.manager.banking;

import io.github.wysohn.rapidframework3.core.caching.CachedElement;
import io.github.wysohn.realeconomy.inject.annotation.MaxCapital;
import io.github.wysohn.realeconomy.inject.annotation.MinCapital;
import io.github.wysohn.realeconomy.interfaces.banking.ITransactionHandler;
import io.github.wysohn.realeconomy.manager.currency.Currency;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Singleton
public class TransactionHandler implements ITransactionHandler {
    private final BigDecimal maximum;
    private final BigDecimal minimum;

    @Inject
    public TransactionHandler(
            @MaxCapital BigDecimal maximum,
            @MinCapital BigDecimal minimum) {
        this.maximum = maximum;
        this.minimum = minimum;
    }

    @Override
    public BigDecimal balance(Map<UUID, BigDecimal> capitals, Currency currency) {
        return Optional.of(currency)
                .map(CachedElement::getKey)
                .map(capitals::get)
                .orElse(BigDecimal.ZERO);
    }

    @Override
    public boolean deposit(Map<UUID, BigDecimal> capitals, BigDecimal value, Currency currency) {
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
        return aBoolean;
    }

    @Override
    public boolean withdraw(Map<UUID, BigDecimal> capitals, BigDecimal value, Currency currency) {
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
        return aBoolean;
    }
}
