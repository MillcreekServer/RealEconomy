package io.github.wysohn.realeconomy.inject.module;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import io.github.wysohn.realeconomy.inject.annotation.ServerBank;
import io.github.wysohn.realeconomy.manager.banking.CentralBankingManager;
import io.github.wysohn.realeconomy.manager.banking.bank.CentralBank;
import io.github.wysohn.realeconomy.manager.currency.CurrencyManager;

import javax.inject.Singleton;
import java.lang.ref.Reference;
import java.util.UUID;

public class ServerBankModule extends AbstractModule {
    private static final UUID SERVER_BANK_UUID = UUID.fromString("567738eb-15f8-4550-bed2-49be1e57ebb7");

    @Provides
    @ServerBank
    @Singleton
    CentralBank serverBank(CentralBankingManager centralBankingManager, CurrencyManager currencyManager) {
        CentralBank serverBank = centralBankingManager.getOrNew(SERVER_BANK_UUID)
                .map(Reference::get)
                .orElseThrow(RuntimeException::new);
        if (serverBank.getBaseCurrency() == null) {
            currencyManager.newCurrency("default", "DFT", serverBank);
        }
        return serverBank;
    }
}
