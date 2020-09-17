package io.github.wysohn.realeconomy.interfaces;

import io.github.wysohn.rapidframework3.interfaces.IPluginObject;
import io.github.wysohn.realeconomy.manager.banking.State;

public interface IFinancialStatus extends IPluginObject {
    State state();
}
