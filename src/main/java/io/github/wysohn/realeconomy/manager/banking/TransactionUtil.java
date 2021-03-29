package io.github.wysohn.realeconomy.manager.banking;

import io.github.wysohn.rapidframework3.core.caching.CachedElement;
import io.github.wysohn.rapidframework3.utils.FailSensitiveTaskGeneric;
import io.github.wysohn.rapidframework3.utils.Validation;
import io.github.wysohn.realeconomy.interfaces.IFinancialEntity;
import io.github.wysohn.realeconomy.manager.banking.bank.CentralBank;
import io.github.wysohn.realeconomy.manager.currency.Currency;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

public class TransactionUtil {

    public static BigDecimal balance(Map<UUID, BigDecimal> capitals, Currency currency) {
        return Optional.of(currency)
                .map(CachedElement::getKey)
                .map(capitals::get)
                .orElse(BigDecimal.ZERO);
    }

    public static boolean deposit(BigDecimal maximum,
                                  Map<UUID, BigDecimal> capitals,
                                  BigDecimal value,
                                  Currency currency) {
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

    public static boolean withdraw(BigDecimal minimum,
                                   Map<UUID, BigDecimal> capitals,
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

    public static boolean withdraw(BigDecimal minimum,
                                   Map<UUID, BigDecimal> capitals,
                                   BigDecimal value,
                                   Currency currency) {
        return withdraw(minimum, capitals, value, currency, false);
    }

    public static Result send(IFinancialEntity from, IFinancialEntity to, BigDecimal amount, Currency currency) {
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

        IFinancialEntity finalFrom = from;
        IFinancialEntity finalTo = to;
        return FailSensitiveTaskResult.of(() -> {
            if (!finalFrom.withdraw(amount, currency))
                return Result.FROM_WITHDRAW_REFUSED;

            if (!finalTo.deposit(amount, currency))
                return Result.TO_DEPOSIT_REFUSED;

            return Result.OK;
        }, Result.OK).handleException(Throwable::printStackTrace)
                .addStateSupplier("from", finalFrom::saveState)
                .addStateConsumer("from", finalFrom::restoreState)
                .addStateSupplier("to", finalTo::saveState)
                .addStateConsumer("to", finalTo::restoreState)
                .run();
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

    public enum Result {
        NO_OWNER, FROM_WITHDRAW_REFUSED, TO_DEPOSIT_REFUSED, OK
    }
}
