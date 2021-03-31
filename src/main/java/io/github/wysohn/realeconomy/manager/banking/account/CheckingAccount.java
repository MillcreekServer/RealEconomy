package io.github.wysohn.realeconomy.manager.banking.account;

import io.github.wysohn.realeconomy.interfaces.banking.IAccount;
import io.github.wysohn.realeconomy.interfaces.banking.IBankingType;
import io.github.wysohn.realeconomy.manager.banking.BankingTypeRegistry;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class CheckingAccount implements IAccount {
    public final Map<UUID, BigDecimal> balances = new ConcurrentHashMap<>();

    @Override
    public Map<UUID, BigDecimal> getCurrencyMap() {
        return balances;
    }

    @Override
    public IBankingType getType() {
        return BankingTypeRegistry.CHECKING;
    }

    @Override
    public IAccount clone() {
        CheckingAccount checkingAccount = new CheckingAccount();
        // both UUID and BigDecimal are immutable
        checkingAccount.balances.putAll(balances);
        return checkingAccount;
    }
}
