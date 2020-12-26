package io.github.wysohn.realeconomy.interfaces;

import io.github.wysohn.rapidframework3.interfaces.IPluginObject;
import io.github.wysohn.realeconomy.interfaces.banking.IBankOwner;
import io.github.wysohn.realeconomy.manager.banking.bank.CentralBank;

public interface IGovernment extends IBankOwner, IPluginObject {
    /**
     * Called once when a bank is established by a government.
     * Use this method to keep track of the owned bank.
     *
     * @param bank the UUID of the central bank to be owning. Can be null to reset
     */
    void setBaseBank(CentralBank bank);
}
