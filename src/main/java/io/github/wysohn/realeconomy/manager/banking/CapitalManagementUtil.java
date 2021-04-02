package io.github.wysohn.realeconomy.manager.banking;

import io.github.wysohn.rapidframework3.core.caching.CachedElement;
import io.github.wysohn.realeconomy.manager.currency.Currency;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class CapitalManagementUtil {

    public static BigDecimal balance(Map<UUID, BigDecimal> capitals, Currency currency) {
        return Optional.of(currency)
                .map(CachedElement::getKey)
                .map(capitals::get)
                .orElse(BigDecimal.valueOf(0.0));
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
                    BigDecimal current = capitals.getOrDefault(uuid, BigDecimal.valueOf(0.0));
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
                    BigDecimal current = capitals.getOrDefault(uuid, BigDecimal.valueOf(0.0));
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
}
