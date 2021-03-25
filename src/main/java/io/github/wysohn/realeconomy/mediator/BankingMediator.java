package io.github.wysohn.realeconomy.mediator;

import io.github.wysohn.rapidframework3.core.caching.CachedElement;
import io.github.wysohn.rapidframework3.core.main.ManagerConfig;
import io.github.wysohn.rapidframework3.core.main.Mediator;
import io.github.wysohn.rapidframework3.interfaces.IMemento;
import io.github.wysohn.rapidframework3.interfaces.IPluginObject;
import io.github.wysohn.rapidframework3.interfaces.paging.DataProvider;
import io.github.wysohn.rapidframework3.utils.FailSensitiveTaskGeneric;
import io.github.wysohn.rapidframework3.utils.Validation;
import io.github.wysohn.realeconomy.interfaces.IFinancialEntity;
import io.github.wysohn.realeconomy.interfaces.IGovernment;
import io.github.wysohn.realeconomy.interfaces.banking.IAssetHolder;
import io.github.wysohn.realeconomy.interfaces.banking.IBankOwner;
import io.github.wysohn.realeconomy.interfaces.banking.IBankUser;
import io.github.wysohn.realeconomy.interfaces.banking.IBankingType;
import io.github.wysohn.realeconomy.manager.asset.Asset;
import io.github.wysohn.realeconomy.manager.asset.signature.AssetSignature;
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
import java.util.*;
import java.util.function.Supplier;

@Singleton
public class BankingMediator extends Mediator {
    private static final UUID SERVER_BANK_UUID = UUID.fromString("567738eb-15f8-4550-bed2-49be1e57ebb7");
    public static final String KEY_SERVER_BANK_ENABLE = "serverBank.enable";
    public static final String SERVER_CURRENCY = "server";
    public static final String SERVER_CURRENCY_CODE = "SRV";
    private static CentralBank serverBank;

    public static CentralBank getServerBank() {
        return serverBank;
    }

    private final ManagerConfig config;
    private final CurrencyManager currencyManager;
    private final CentralBankingManager centralBankingManager;

    private final Map<UUID, AbstractBank> usingBanks = new HashMap<>();

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
        if (!config.get(KEY_SERVER_BANK_ENABLE).isPresent())
            config.put(KEY_SERVER_BANK_ENABLE, false);

        serverBank = centralBankingManager.getOrNew(SERVER_BANK_UUID)
                .map(Reference::get)
                .orElseThrow(RuntimeException::new);

        // make sure the server currency exist no matter what
        currencyManager.newCurrency(SERVER_CURRENCY, SERVER_CURRENCY_CODE, serverBank);
        Currency currency = currencyManager.get(SERVER_CURRENCY)
                .map(Reference::get)
                .orElseThrow(RuntimeException::new);

        serverBank.setStringKey("*");
        if(serverBank.getBaseCurrency() == null)
            serverBank.setBaseCurrency(currency);
        serverBank.setLimitlessPapers(true);
    }

    @Override
    public void load() throws Exception {
        serverBank.setOperating(config.get(KEY_SERVER_BANK_ENABLE)
                .map(Boolean.class::cast)
                .orElse(false));
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

    public boolean openAccount(IBankUser user, IBankingType type) {
        return this.openAccount(getUsingBank(user), user, type);
    }

    public boolean openAccount(AbstractBank bank, IBankUser user, IBankingType type) {
        return bank.putAccount(user, type);
    }

    public BigDecimal balance(IBankUser user, IBankingType type) {
        return this.balance(getUsingBank(user), user, type);
    }

    public BigDecimal balance(AbstractBank bank, IBankUser user, IBankingType type) {
        Validation.assertNotNull(bank);
        Validation.assertNotNull(user);
        Validation.assertNotNull(type);

        return Optional.of(bank)
                .map(AbstractBank::getBaseCurrency)
                .map(CachedElement::getKey)
                .map(currencyUuid -> bank.balanceOfAccount(user, type))
                .orElse(BigDecimal.ZERO);
    }

    public Result deposit(IBankUser user, IBankingType type, BigDecimal amount) {
        return deposit(getUsingBank(user), user, type, amount);
    }

    public Result deposit(AbstractBank bank, IBankUser user, IBankingType type, BigDecimal amount) {
        Validation.assertNotNull(bank);
        Validation.assertNotNull(user);
        Validation.assertNotNull(type);
        Validation.assertNotNull(amount);

        if (bank.getBaseCurrency() == null)
            return Result.NO_CURRENCY_SET;

        if (!bank.hasAccount(user, type))
            return Result.NO_ACCOUNT;

        if (bank.depositAccount(user, type, amount, bank.getBaseCurrency())) {
            return Result.OK;
        } else {
            return Result.FAIL_DEPOSIT;
        }
    }

    public Result withdraw(IBankUser user, IBankingType type, BigDecimal amount) {
        return this.withdraw(getUsingBank(user), user, type, amount);
    }

    public Result withdraw(AbstractBank bank, IBankUser user, IBankingType type, BigDecimal amount) {
        Validation.assertNotNull(bank);
        Validation.assertNotNull(user);
        Validation.assertNotNull(type);
        Validation.assertNotNull(amount);

        if (bank.getBaseCurrency() == null)
            return Result.NO_CURRENCY_SET;

        if (!bank.hasAccount(user, type))
            return Result.NO_ACCOUNT;

        if (bank.withdrawAccount(user, type, amount, bank.getBaseCurrency())) {
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

    public TransactionUtil.Result send(
            IBankUser from,
            IBankingType type,
            IFinancialEntity to,
            BigDecimal amount,
            Currency currency) {
        return send(new BankAccountWrapper(getUsingBank(from), from, type), to, amount, currency);
    }

    public TransactionUtil.Result send(
            IFinancialEntity from,
            IBankUser to,
            IBankingType type,
            BigDecimal amount,
            Currency currency) {
        return send(from, new BankAccountWrapper(getUsingBank(to), to, type), amount, currency);
    }

    public TransactionUtil.Result send(
            IBankUser from,
            IBankingType from_type,
            IBankUser to,
            IBankingType to_type,
            BigDecimal amount,
            Currency currency) {
        return send(new BankAccountWrapper(getUsingBank(from), from, from_type),
                new BankAccountWrapper(getUsingBank(to), to, to_type),
                amount,
                currency);
    }

    public BankAccountWrapper wrapAccount(AbstractBank bank, IBankUser user, IBankingType type) {
        return new BankAccountWrapper(bank, user, type);
    }

    public AssetStorageWrapper wrapStorage(AbstractBank bank, IBankUser user) {
        return new AssetStorageWrapper(bank, user);
    }

    public boolean isInBank(IBankUser user) {
        return Optional.ofNullable(user)
                .map(IPluginObject::getUuid)
                .map(usingBanks::containsKey)
                .orElse(false);
    }

    public AbstractBank getUsingBank(IBankUser user) {
        return Optional.ofNullable(user)
                .map(IPluginObject::getUuid)
                .map(usingBanks::get)
                .orElse(serverBank.isOperating() ? serverBank : null);
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

    public class BankAccountWrapper implements IFinancialEntity {
        private final AbstractBank bank;
        private final IBankUser user;
        private final IBankingType type;

        public BankAccountWrapper(AbstractBank bank,
                                  IBankUser user,
                                  IBankingType type) {
            Validation.assertNotNull(bank);
            Validation.assertNotNull(user);
            Validation.assertNotNull(type);

            this.bank = bank;
            this.user = user;
            this.type = type;
        }

        private void verifyCurrency(Currency currency) {
            Optional<Currency> optBaseCurrency = Optional.of(user)
                    .map(BankingMediator.this::getUsingBank)
                    .map(AbstractBank::getBaseCurrency);

            if (!optBaseCurrency.map(baseCurrency -> baseCurrency.equals(currency))
                    .orElse(false)) {
                throw new RuntimeException("Currently using bank of "+user+" and the requested currency" +
                        " type does not match. "+ currency +" vs "+optBaseCurrency.orElse(null));
            }
        }

        @Override
        public BigDecimal balance(Currency currency) {
            verifyCurrency(currency);

            return BankingMediator.this.balance(bank, user, type);
        }

        @Override
        public boolean deposit(BigDecimal value, Currency currency) {
            verifyCurrency(currency);

            return BankingMediator.this.deposit(bank, user, type, value) == Result.OK;
        }

        @Override
        public boolean withdraw(BigDecimal value, Currency currency) {
            verifyCurrency(currency);

            return BankingMediator.this.withdraw(bank, user, type, value) == Result.OK;
        }

        @Override
        public int realizeAsset(Asset asset) {
            return user.realizeAsset(asset);
        }

        @Override
        public IMemento saveState() {
            return user.saveState();
        }

        @Override
        public void restoreState(IMemento iMemento) {
            user.restoreState(iMemento);
        }
    }

    public class AssetStorageWrapper implements IAssetHolder {
        private final AbstractBank bank;
        private final IBankUser user;

        public AssetStorageWrapper(AbstractBank bank, IBankUser user) {
            this.bank = bank;
            this.user = user;
        }

        @Override
        public void addAsset(Asset asset) {
            bank.addAccountAsset(user, asset);
        }

        @Override
        public double countAsset(AssetSignature signature) {
            return bank.countAccountAsset(user, signature);
        }

        @Override
        public Collection<Asset> removeAsset(AssetSignature signature, double amount) {
            return bank.removeAccountAsset(user, signature, amount);
        }

        @Override
        public DataProvider<Asset> assetDataProvider() {
            return bank.accountAssetProvider(user);
        }

        @Override
        public IMemento saveState() {
            return bank.saveState();
        }

        @Override
        public void restoreState(IMemento savedState) {
            bank.restoreState(savedState);
        }
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
        NO_CURRENCY_SET,
        NO_ACCOUNT,
        FAIL_DEPOSIT,
        FAIL_WITHDRAW,
    }
}
