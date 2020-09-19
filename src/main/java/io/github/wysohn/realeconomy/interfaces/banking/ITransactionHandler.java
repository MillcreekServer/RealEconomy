package io.github.wysohn.realeconomy.interfaces.banking;

import io.github.wysohn.realeconomy.manager.currency.Currency;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

public interface ITransactionHandler {
    BigDecimal balance(Map<UUID, BigDecimal> capitals, Currency currency);

    boolean deposit(Map<UUID, BigDecimal> capitals, BigDecimal value, Currency currency);

    boolean withdraw(Map<UUID, BigDecimal> capitals, BigDecimal value, Currency currency);
}
