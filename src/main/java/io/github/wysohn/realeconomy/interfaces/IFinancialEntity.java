package io.github.wysohn.realeconomy.interfaces;

import io.github.wysohn.rapidframework3.interfaces.IPluginObject;
import io.github.wysohn.realeconomy.manager.currency.Currency;

public interface IFinancialEntity extends ITransactionSnapshot, IPluginObject {
    double balance(Currency currency);

    boolean deposit(double value, Currency currency);

    boolean withdraw(double value, Currency currency);
}
