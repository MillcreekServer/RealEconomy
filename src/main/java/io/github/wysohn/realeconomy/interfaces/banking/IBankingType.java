package io.github.wysohn.realeconomy.interfaces.banking;

import io.github.wysohn.rapidframework3.interfaces.IPluginObject;
import io.github.wysohn.rapidframework3.interfaces.language.ILang;

public interface IBankingType extends IPluginObject {
    IAccount createAccount();

    String name();

    ILang lang();
}
