package io.github.wysohn.realeconomy.mediator;

import io.github.wysohn.rapidframework3.core.main.Mediator;
import io.github.wysohn.realeconomy.inject.annotation.MaxCapital;
import io.github.wysohn.realeconomy.inject.annotation.ServerBank;
import io.github.wysohn.realeconomy.interfaces.IGovernment;
import io.github.wysohn.realeconomy.interfaces.banking.IBankOwner;
import io.github.wysohn.realeconomy.interfaces.banking.IBankUser;
import io.github.wysohn.realeconomy.interfaces.banking.IBankingType;
import io.github.wysohn.realeconomy.manager.banking.CentralBankingManager;
import io.github.wysohn.realeconomy.manager.banking.bank.AbstractBank;
import io.github.wysohn.realeconomy.manager.banking.bank.CentralBank;
import io.github.wysohn.realeconomy.manager.currency.Currency;
import io.github.wysohn.realeconomy.manager.currency.CurrencyManager;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.lang.ref.Reference;
import java.math.BigDecimal;
import java.util.UUID;

@Singleton
public class BankingMediator extends Mediator {
    private final CentralBank serverBank;
    private final BigDecimal maxCapital;
    private final CurrencyManager currencyManager;
    private final CentralBankingManager centralBankingManager;

    @Inject
    public BankingMediator(
            @ServerBank CentralBank serverBank,
            @MaxCapital BigDecimal maxCapital,
            CurrencyManager currencyManager,
            CentralBankingManager centralBankingManager) {
        this.serverBank = serverBank;
        this.maxCapital = maxCapital;
        this.currencyManager = currencyManager;
        this.centralBankingManager = centralBankingManager;
    }

    @Override
    public void enable() throws Exception {

    }

    @Override
    public void load() throws Exception {

    }

    @Override
    public void disable() throws Exception {

    }

    /**
     * @param government
     * @param name
     * @param code
     * @return
     */
    public Result createCurrency(IGovernment government, String name, String code) {
        switch (currencyManager.newCurrency(name, code)) {
            case DUP_NAME:
                return Result.DUP_NAME;
            case DUP_CODE:
                return Result.DUP_CODE;
            case CODE_LENGTH:
                return Result.CODE_LENGTH;
        }

        Currency currency = currencyManager.get(name)
                .map(Reference::get)
                .orElseThrow(RuntimeException::new); // how?

        // bank already exist
        if (centralBankingManager.get(government.getUuid()).isPresent())
            return Result.ALREADY_SET;

        centralBankingManager.getOrNew(government.getUuid())
                .map(Reference::get)
                .ifPresent(centralBank -> {
                    centralBank.setBankOwner(government);
                    centralBank.setBaseCurrency(currency);
                });

        return Result.OK;
    }

    public Result renameCurrency(String name, String newName) {
        if (currencyManager.get(newName).isPresent())
            return Result.DUP_NAME;

        if (!currencyManager.get(name).isPresent())
            return Result.NOT_FOUND;

        currencyManager.get(name)
                .map(Reference::get)
                .ifPresent(currency -> currency.setStringKey(newName));

        return Result.OK;
    }

    public Result renameCode(String name, String newCode) {
        switch (currencyManager.changeCode(name, newCode)) {
            case NOT_EXIST:
                return Result.NOT_FOUND;
            case OK:
                return Result.OK;
        }

        return Result.UNKNOWN;
    }

    public void openAccount(AbstractBank bank, IBankUser user, IBankingType type) {

    }

    public void createCommercialBank(IBankOwner owner, UUID baseCurrency, double base, String name) {
        //TODO
    }

    public enum Result {
        UNKNOWN, DUP_NAME, DUP_CODE, CODE_LENGTH, ALREADY_SET, NOT_FOUND, OK
    }
}
