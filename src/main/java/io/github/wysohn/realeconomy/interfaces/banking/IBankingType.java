package io.github.wysohn.realeconomy.interfaces.banking;

import io.github.wysohn.rapidframework3.interfaces.IPluginObject;

public interface IBankingType extends IPluginObject {
    IAccount createAccount();
}
