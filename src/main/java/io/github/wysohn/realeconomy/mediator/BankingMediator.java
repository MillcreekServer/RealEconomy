package io.github.wysohn.realeconomy.mediator;

import io.github.wysohn.rapidframework3.core.caching.CachedElement;
import io.github.wysohn.rapidframework3.core.main.Mediator;
import io.github.wysohn.rapidframework3.interfaces.IPluginObject;
import io.github.wysohn.rapidframework3.utils.Validation;
import io.github.wysohn.realeconomy.inject.annotation.MaxCapital;
import io.github.wysohn.realeconomy.inject.annotation.MinCapital;
import io.github.wysohn.realeconomy.interfaces.IFinancialEntity;
import io.github.wysohn.realeconomy.interfaces.IGovernment;
import io.github.wysohn.realeconomy.interfaces.banking.IAccount;
import io.github.wysohn.realeconomy.interfaces.banking.IBankOwner;
import io.github.wysohn.realeconomy.interfaces.banking.IBankUser;
import io.github.wysohn.realeconomy.interfaces.banking.IBankingType;
import io.github.wysohn.realeconomy.manager.banking.CentralBankingManager;
import io.github.wysohn.realeconomy.manager.banking.TransactionUtil;
import io.github.wysohn.realeconomy.manager.banking.bank.AbstractBank;
import io.github.wysohn.realeconomy.manager.banking.bank.CentralBank;
import io.github.wysohn.realeconomy.manager.currency.Currency;
import io.github.wysohn.realeconomy.manager.currency.CurrencyManager;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.lang.ref.Reference;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Singleton
public class BankingMediator extends Mediator {
    private static final UUID SERVER_BANK_UUID = UUID.fromString("567738eb-15f8-4550-bed2-49be1e57ebb7");
    private static CentralBank serverBank;

    public static CentralBank getServerBank() {
        return serverBank;
    }

    private final BigDecimal maxCapital;
    private final BigDecimal minCapital;
    private final CurrencyManager currencyManager;
    private final CentralBankingManager centralBankingManager;

    private final Map<UUID, AbstractBank> usingBanks = new HashMap<>();

    @Inject
    public BankingMediator(
            @MaxCapital BigDecimal maxCapital,
            @MinCapital BigDecimal minCapital,
            CurrencyManager currencyManager,
            CentralBankingManager centralBankingManager) {
        this.maxCapital = maxCapital;
        this.minCapital = minCapital;
        this.currencyManager = currencyManager;
        this.centralBankingManager = centralBankingManager;
    }

    @Override
    public void enable() throws Exception {
        serverBank = centralBankingManager.getOrNew(SERVER_BANK_UUID)
                .map(Reference::get)
                .orElseThrow(RuntimeException::new);
        if (serverBank.getBaseCurrency() == null) {
            serverBank.setStringKey("*");
            currencyManager.newCurrency("default", "DFT", serverBank);
        }
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
        centralBank.setBankOwner(government);

        CurrencyManager.Result result = currencyManager.newCurrency(name, code, centralBank);
        if (result != CurrencyManager.Result.OK) {
            // delete the bank as currency creation failed
            centralBankingManager.delete(centralBank.getKey());

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

    public boolean openAccount(IBankUser user, IBankingType type) {
        return this.openAccount(serverBank, user, type);
    }

    public boolean openAccount(AbstractBank bank, IBankUser user, IBankingType type) {
        return bank.putAccount(user, type);
    }

    public BigDecimal balance(IBankUser user, IBankingType type) {
        return this.balance(serverBank, user, type);
    }

    public BigDecimal balance(AbstractBank bank, IBankUser user, IBankingType type) {
        Validation.assertNotNull(bank);
        Validation.assertNotNull(user);
        Validation.assertNotNull(type);

        return Optional.of(bank)
                .map(AbstractBank::getBaseCurrency)
                .map(CachedElement::getKey)
                .map(currencyUuid -> {
                    IAccount account = bank.getAccount(user, type);
                    return account.getCurrencyMap().get(currencyUuid);
                })
                .orElse(BigDecimal.ZERO);
    }

    public Result deposit(IBankUser user, IBankingType type, BigDecimal amount) {
        return deposit(serverBank, user, type, amount);
    }

    public Result deposit(AbstractBank bank, IBankUser user, IBankingType type, BigDecimal amount) {
        Validation.assertNotNull(bank);
        Validation.assertNotNull(user);
        Validation.assertNotNull(type);
        Validation.assertNotNull(amount);

        if (bank.getBaseCurrency() == null)
            return Result.NO_CURRENCY_SET;

        IAccount account = bank.getAccount(user, type);
        if (account == null)
            return Result.NO_ACCOUNT;

        //TODO wrong implementation. must use bank#accountTransaction()
        if (TransactionUtil.deposit(maxCapital, account.getCurrencyMap(), amount, bank.getBaseCurrency())) {
            return Result.OK;
        } else {
            return Result.FAIL_DEPOSIT;
        }
    }

    public Result withdraw(IBankUser user, IBankingType type, BigDecimal amount) {
        return this.withdraw(serverBank, user, type, amount);
    }

    public Result withdraw(AbstractBank bank, IBankUser user, IBankingType type, BigDecimal amount) {
        Validation.assertNotNull(bank);
        Validation.assertNotNull(user);
        Validation.assertNotNull(type);
        Validation.assertNotNull(amount);

        if (bank.getBaseCurrency() == null)
            return Result.NO_CURRENCY_SET;

        IAccount account = bank.getAccount(user, type);
        if (account == null)
            return Result.NO_ACCOUNT;

        //TODO wrong implementation. must use bank#accountTransaction()
        if (TransactionUtil.withdraw(minCapital, account.getCurrencyMap(), amount, bank.getBaseCurrency())) {
            return Result.OK;
        } else {
            return Result.FAIL_WITHDRAW;
        }
    }

    /**
     * {@link TransactionUtil#send(IFinancialEntity, IFinancialEntity, BigDecimal, Currency)}
     *
     * @param from
     * @param to
     * @param amount
     * @param currency
     * @return
     */
    public TransactionUtil.Result send(
            IFinancialEntity from,
            IFinancialEntity to,
            BigDecimal amount,
            Currency currency) {

        return TransactionUtil.send(from, to, amount, currency);
    }

    public AbstractBank getUsingBank(IBankUser user) {
        return Optional.ofNullable(user)
                .map(IPluginObject::getUuid)
                .map(usingBanks::get)
                .orElse(serverBank);
    }

    public boolean enterBank(IBankUser user, AbstractBank bank) {
        Validation.assertNotNull(user);
        Validation.assertNotNull(bank);

        if (usingBanks.containsKey(user.getUuid()))
            return false;

        usingBanks.put(user.getUuid(), bank);
        return true;
    }

    public boolean exitBank(IBankUser user, AbstractBank bank) {
        Validation.assertNotNull(user);
        Validation.assertNotNull(bank);

        return usingBanks.remove(user.getUuid()) != null;
    }

    public void createCommercialBank(IBankOwner owner, UUID baseCurrency, double base, String name) {
        //TODO
    }

    public enum Result {
        OK,
        UNKNOWN,
        DUP_NAME,
        DUP_CODE,
        CODE_LENGTH,
        ALREADY_SET,
        NOT_FOUND,
        NO_CURRENCY_SET,
        NO_ACCOUNT,
        FAIL_DEPOSIT,
        FAIL_WITHDRAW,
    }
}
