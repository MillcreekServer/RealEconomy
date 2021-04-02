package io.github.wysohn.realeconomy.manager.banking;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.github.wysohn.rapidframework3.core.main.Manager;
import io.github.wysohn.rapidframework3.core.main.ManagerConfig;
import io.github.wysohn.rapidframework3.interfaces.IPluginObject;
import io.github.wysohn.rapidframework3.utils.Validation;
import io.github.wysohn.realeconomy.interfaces.banking.IBankUser;
import io.github.wysohn.realeconomy.manager.banking.bank.AbstractBank;
import io.github.wysohn.realeconomy.manager.banking.bank.CentralBank;
import io.github.wysohn.realeconomy.manager.currency.Currency;
import io.github.wysohn.realeconomy.manager.currency.CurrencyManager;

import java.lang.ref.Reference;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Singleton
public class VisitingBankManager extends Manager {
    private static final UUID SERVER_BANK_UUID = UUID.fromString("567738eb-15f8-4550-bed2-49be1e57ebb7");
    public static final String KEY_SERVER_BANK_ENABLE = "serverBank.enable";
    public static final String SERVER_CURRENCY = "server";
    public static final String SERVER_CURRENCY_CODE = "SRV";
    private static CentralBank serverBank;

    public static CentralBank getServerBank() {
        return serverBank;
    }

    private final Map<UUID, AbstractBank> usingBanks = new HashMap<>();

    private final ManagerConfig config;
    private final CurrencyManager currencyManager;
    private final CentralBankingManager centralBankingManager;

    @Inject
    public VisitingBankManager(ManagerConfig config,
                               CurrencyManager currencyManager,
                               CentralBankingManager centralBankingManager) {
        this.config = config;
        this.currencyManager = currencyManager;
        this.centralBankingManager = centralBankingManager;

        dependsOn(CurrencyManager.class);
        dependsOn(CentralBankingManager.class);
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
        if (serverBank.getBaseCurrency() == null)
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
}
