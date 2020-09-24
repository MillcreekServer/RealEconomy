package io.github.wysohn.realeconomy.interfaces.currency;

import io.github.wysohn.realeconomy.manager.banking.bank.CentralBank;

import java.util.UUID;

public interface ICurrencyOwnerProvider {
    CentralBank get(UUID currencyOwnerUuid);
}
