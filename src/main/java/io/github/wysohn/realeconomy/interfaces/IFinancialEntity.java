package io.github.wysohn.realeconomy.interfaces;

import io.github.wysohn.rapidframework3.interfaces.IPluginObject;
import io.github.wysohn.realeconomy.manager.currency.Currency;

import java.math.BigDecimal;

public interface IFinancialEntity extends ITransactionSnapshot, IPluginObject {
    BigDecimal balance(Currency currency);

    boolean deposit(BigDecimal value, Currency currency);

    boolean withdraw(BigDecimal value, Currency currency);
}
