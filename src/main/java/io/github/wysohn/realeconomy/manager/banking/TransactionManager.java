package io.github.wysohn.realeconomy.manager.banking;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.github.wysohn.rapidframework3.core.main.Manager;
import io.github.wysohn.rapidframework3.interfaces.IMemento;
import io.github.wysohn.rapidframework3.interfaces.paging.DataProvider;
import io.github.wysohn.rapidframework3.utils.FailSensitiveTaskGeneric;
import io.github.wysohn.rapidframework3.utils.Validation;
import io.github.wysohn.realeconomy.interfaces.IFinancialEntity;
import io.github.wysohn.realeconomy.interfaces.banking.IAssetHolder;
import io.github.wysohn.realeconomy.interfaces.banking.IBankUser;
import io.github.wysohn.realeconomy.interfaces.banking.IBankingType;
import io.github.wysohn.realeconomy.manager.asset.Asset;
import io.github.wysohn.realeconomy.manager.asset.signature.AssetSignature;
import io.github.wysohn.realeconomy.manager.banking.bank.AbstractBank;
import io.github.wysohn.realeconomy.manager.banking.bank.CentralBank;
import io.github.wysohn.realeconomy.manager.currency.Currency;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Optional;
import java.util.function.Supplier;

@Singleton
public class TransactionManager extends Manager {
    private final VisitingBankManager visitingBankManager;

    @Inject
    public TransactionManager(VisitingBankManager visitingBankManager) {
        this.visitingBankManager = visitingBankManager;

        dependsOn(VisitingBankManager.class);
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
     * @param from
     * @param to
     * @param amount
     * @param currency
     * @return
     */
    public synchronized Result send(
            IFinancialEntity from,
            IFinancialEntity to,
            BigDecimal amount,
            Currency currency) {

        Validation.assertNotNull(amount);
        Validation.assertNotNull(currency);
        Validation.validate(amount, val -> val.signum() >= 0, "Cannot use negative value.");

        CentralBank currencyOwner = currency.ownerBank();
        if (currencyOwner == null)
            return Result.NO_OWNER;

        if (from == null)
            from = currencyOwner;
        if (to == null)
            to = currencyOwner;

        IFinancialEntity finalFrom = from;
        IFinancialEntity finalTo = to;
        return FailSensitiveTaskResult.of(() -> {
            if (!finalFrom.withdraw(amount, currency))
                return Result.FROM_WITHDRAW_REFUSED;

            if (!finalTo.deposit(amount, currency))
                return Result.TO_DEPOSIT_REFUSED;

            return Result.OK;
        }, Result.OK).handleException(Throwable::printStackTrace)
                .addStateSupplier("from", finalFrom::saveState)
                .addStateConsumer("from", finalFrom::restoreState)
                .addStateSupplier("to", finalTo::saveState)
                .addStateConsumer("to", finalTo::restoreState)
                .run();
    }

    public synchronized Result send(
            IBankUser from,
            IBankingType type,
            IFinancialEntity to,
            BigDecimal amount,
            Currency currency) {
        return send(new BankAccountWrapper(visitingBankManager.getUsingBank(from), from, type), to, amount, currency);
    }

    public synchronized Result send(
            IFinancialEntity from,
            IBankUser to,
            IBankingType type,
            BigDecimal amount,
            Currency currency) {
        return send(from, new BankAccountWrapper(visitingBankManager.getUsingBank(to), to, type), amount, currency);
    }

    public synchronized Result send(
            IBankUser from,
            IBankingType from_type,
            IBankUser to,
            IBankingType to_type,
            BigDecimal amount,
            Currency currency) {
        return send(new BankAccountWrapper(visitingBankManager.getUsingBank(from), from, from_type),
                new BankAccountWrapper(visitingBankManager.getUsingBank(to), to, to_type),
                amount,
                currency);
    }

    public BankAccountWrapper wrapAccount(AbstractBank bank, IBankUser user, IBankingType type) {
        return new BankAccountWrapper(bank, user, type);
    }

    public AssetStorageWrapper wrapStorage(AbstractBank bank, IBankUser user) {
        return new AssetStorageWrapper(bank, user);
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
                    .map(visitingBankManager::getUsingBank)
                    .map(AbstractBank::getBaseCurrency);

            if (!optBaseCurrency.map(baseCurrency -> baseCurrency.equals(currency))
                    .orElse(false)) {
                throw new RuntimeException("Currently using bank of " + user + " and the requested currency" +
                        " type does not match. " + currency + " vs " + optBaseCurrency.orElse(null));
            }
        }

        @Override
        public BigDecimal balance(Currency currency) {
            verifyCurrency(currency);

            return bank.balanceOfAccount(user, type);
        }

        @Override
        public boolean deposit(BigDecimal value, Currency currency) {
            verifyCurrency(currency);

            return bank.depositAccount(user, type, value, currency);
        }

        @Override
        public boolean withdraw(BigDecimal value, Currency currency) {
            verifyCurrency(currency);

            return bank.withdrawAccount(user, type, value, currency);
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
        public Asset removeAsset(int index) {
            return bank.removeAccountAsset(user, index);
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

        protected FailSensitiveTaskResult(Supplier<Result> task, Result expected) {
            super(task, expected);
        }

        public static FailSensitiveTaskResult of(Supplier<Result> task, Result expected) {
            return new FailSensitiveTaskResult(task, expected);
        }
    }

    public enum Result {
        NO_OWNER,
        FROM_WITHDRAW_REFUSED,
        TO_DEPOSIT_REFUSED,
        OK,
    }
}
