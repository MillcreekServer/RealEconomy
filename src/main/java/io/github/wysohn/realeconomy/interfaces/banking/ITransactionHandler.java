package io.github.wysohn.realeconomy.interfaces.banking;

import io.github.wysohn.realeconomy.interfaces.IFinancialEntity;
import io.github.wysohn.realeconomy.manager.currency.Currency;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

public interface ITransactionHandler {
    BigDecimal balance(Map<UUID, BigDecimal> capitals, Currency currency);

    boolean deposit(Map<UUID, BigDecimal> capitals, BigDecimal value, Currency currency);

    boolean withdraw(Map<UUID, BigDecimal> capitals, BigDecimal value, Currency currency);

    /**
     * Send currency between two financial entities. 'from' or 'to' can be null, yet in that case, the owner of the
     * 'currency,' which is a CentralBank, will be used in the place. While having null for both 'from' and 'to' is okay
     * , it has no useful effect since 'from' and 'to' will be same.
     *
     * @param from     financial entity to take currency from
     * @param to       financial entity to give currency to
     * @param amount   amount of currency
     * @param currency currency type
     * @return the result
     */
    Result send(IFinancialEntity from, IFinancialEntity to, BigDecimal amount, Currency currency);

    enum Result {
        NO_OWNER, FROM_INSUFFICIENT, TO_DEPOSIT_REFUSED, OK
    }
}
