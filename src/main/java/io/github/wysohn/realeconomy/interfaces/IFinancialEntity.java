package io.github.wysohn.realeconomy.interfaces;

import io.github.wysohn.rapidframework3.interfaces.IPluginObject;
import io.github.wysohn.rapidframework3.interfaces.entity.IEntitySnapshot;
import io.github.wysohn.realeconomy.manager.currency.Currency;

import java.math.BigDecimal;

public interface IFinancialEntity extends IEntitySnapshot, IPluginObject {
    BigDecimal balance(Currency currency);

    boolean deposit(BigDecimal value, Currency currency);

    default boolean deposit(double value, Currency currency) {
        return deposit(BigDecimal.valueOf(value), currency);
    }

    boolean withdraw(BigDecimal value, Currency currency);

    default boolean withdraw(double value, Currency currency) {
        return withdraw(BigDecimal.valueOf(value), currency);
    }
}
