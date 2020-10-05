package io.github.wysohn.realeconomy.manager.banking;

import io.github.wysohn.rapidframework3.core.caching.CachedElement;
import io.github.wysohn.rapidframework3.interfaces.IMemento;
import io.github.wysohn.rapidframework3.utils.FailSensitiveTaskGeneric;
import io.github.wysohn.rapidframework3.utils.Validation;
import io.github.wysohn.realeconomy.inject.annotation.MaxCapital;
import io.github.wysohn.realeconomy.inject.annotation.MinCapital;
import io.github.wysohn.realeconomy.interfaces.IFinancialEntity;
import io.github.wysohn.realeconomy.interfaces.banking.ITransactionHandler;
import io.github.wysohn.realeconomy.manager.banking.bank.CentralBank;
import io.github.wysohn.realeconomy.manager.currency.Currency;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

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
    public boolean withdraw(Map<UUID, BigDecimal> capitals,
                            BigDecimal value,
                            Currency currency,
                            boolean allowNegative) {
        if (value.signum() < 0)
            throw new RuntimeException("Cannot use negative value.");

        final boolean aBoolean = Optional.of(currency)
                .map(CachedElement::getKey)
                .map(uuid -> {
                    BigDecimal current = capitals.getOrDefault(uuid, BigDecimal.ZERO);
                    BigDecimal subtracted = current.subtract(value);

                    if (!allowNegative && subtracted.signum() < 0)
                        return false;

                    if (subtracted.compareTo(minimum) < 0)
                        return false;

                    capitals.put(uuid, subtracted);
                    return true;
                })
                .orElse(false);
        return aBoolean;
    }

    @Override
    public Result send(IFinancialEntity from, IFinancialEntity to, BigDecimal amount, Currency currency) {
        Validation.assertNotNull(amount);
        Validation.assertNotNull(currency);
        Validation.validate(amount, val -> val.signum() >= 0, "Cannot use negative value.");

        CentralBank currencyOwner = currency.ownerBank();
        if (currencyOwner == null)
            return Result.NO_OWNER;

        if (from == null)
            from = currencyOwner;
        if (to == null)
            to = currencyOwner;

        IMemento savedFromState = from.saveState();
        IMemento savedToState = to.saveState();

        IFinancialEntity finalFrom = from;
        IFinancialEntity finalTo = to;
        return FailSensitiveTaskResult.of(() -> {
            if (!finalFrom.withdraw(amount, currency))
                return Result.FROM_WITHDRAW_REFUSED;

            if (!finalTo.deposit(amount, currency))
                return Result.TO_DEPOSIT_REFUSED;

            return Result.OK;
        }, Result.OK).onFail(() -> {
            finalFrom.restoreState(savedFromState);
            finalTo.restoreState(savedToState);
        }).handleException(Throwable::printStackTrace).run();
    }

    public static class FailSensitiveTaskResult extends FailSensitiveTaskGeneric<FailSensitiveTaskResult, Result> {
        private FailSensitiveTaskResult(
                Supplier<Result> task,
                Result expected) {
            super(task, expected);
        }

        public static FailSensitiveTaskResult of(Supplier<Result> task, Result expected) {
            return new FailSensitiveTaskResult(task, expected);
        }
    }
}
