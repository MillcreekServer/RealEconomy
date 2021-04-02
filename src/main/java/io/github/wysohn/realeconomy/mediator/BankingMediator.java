package io.github.wysohn.realeconomy.mediator;

import io.github.wysohn.rapidframework3.core.main.ManagerConfig;
import io.github.wysohn.rapidframework3.core.main.Mediator;
import io.github.wysohn.rapidframework3.utils.FailSensitiveTaskGeneric;
import io.github.wysohn.realeconomy.interfaces.IGovernment;
import io.github.wysohn.realeconomy.interfaces.banking.IBankOwner;
import io.github.wysohn.realeconomy.manager.banking.CentralBankingManager;
import io.github.wysohn.realeconomy.manager.banking.bank.CentralBank;
import io.github.wysohn.realeconomy.manager.currency.CurrencyManager;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.lang.ref.Reference;
import java.util.UUID;
import java.util.function.Supplier;

@Singleton
public class BankingMediator extends Mediator {
    private final ManagerConfig config;
    private final CurrencyManager currencyManager;
    private final CentralBankingManager centralBankingManager;

    @Inject
    public BankingMediator(
            ManagerConfig config,
            CurrencyManager currencyManager,
            CentralBankingManager centralBankingManager) {
        this.config = config;
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
        // bank already exist
        if (centralBankingManager.get(government.getUuid()).isPresent())
            return Result.ALREADY_SET;

        CentralBank centralBank = centralBankingManager.getOrNew(government.getUuid())
                .map(Reference::get)
                .orElseThrow(RuntimeException::new);

        return FailSensitiveTaskResult.of(() -> {
            centralBank.setBankOwner(government);
            government.setBaseBank(centralBank);

            CurrencyManager.Result result = currencyManager.newCurrency(name, code, centralBank);
            if (result != CurrencyManager.Result.OK) {
                switch (result) {
                    case DUP_NAME:
                        return Result.DUP_NAME;
                    case DUP_CODE:
                        return Result.DUP_CODE;
                    case CODE_LENGTH:
                        return Result.CODE_LENGTH;
                    default:
                        throw new RuntimeException();
                }
            } else {
                return Result.OK;
            }
        }, Result.OK).handleException(Throwable::printStackTrace).onFail(() -> {
            // delete the bank as currency creation failed
            centralBankingManager.delete(centralBank.getKey());
            government.setBaseBank(null);
        }).run();
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

    public void createCommercialBank(IBankOwner owner, UUID baseCurrency, double base, String name) {
        //TODO
    }

    public static class FailSensitiveTaskResult extends FailSensitiveTaskGeneric<FailSensitiveTaskResult, Result> {

        protected FailSensitiveTaskResult(Supplier<Result> task,
                                          Result expected) {
            super(task, expected);
        }

        public static FailSensitiveTaskResult of(Supplier<Result> task,
                                                 Result expected) {
            return new FailSensitiveTaskResult(task, expected);
        }
    }

    public enum Result {
        OK,
        UNKNOWN,
        DUP_NAME,
        DUP_CODE,
        CODE_LENGTH,
        ALREADY_SET,
        NOT_FOUND,
    }
}
