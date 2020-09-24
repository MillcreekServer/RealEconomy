package io.github.wysohn.realeconomy.manager.currency;

import io.github.wysohn.rapidframework3.core.caching.CachedElement;
import io.github.wysohn.rapidframework3.utils.Validation;
import io.github.wysohn.realeconomy.manager.banking.CentralBankingManager;
import io.github.wysohn.realeconomy.manager.banking.bank.CentralBank;

import javax.inject.Inject;
import java.lang.ref.Reference;
import java.util.Optional;
import java.util.UUID;

public class Currency extends CachedElement<UUID> {
    @Inject
    private CentralBankingManager centralBankingManager;

    private String code;
    private UUID centralBankUuid;

    private Currency() {
        super(null);
    }

    public Currency(UUID key) {
        super(key);
    }

    public String getCode() {
        return code;
    }

    void setCode(String code) {
        this.code = code;

        notifyObservers();
    }

    public CentralBank ownerBank() {
        return Optional.ofNullable(centralBankUuid)
                .flatMap(centralBankingManager::get)
                .map(Reference::get)
                .orElse(null);
    }

    public void setCentralBank(CentralBank centralBank) {
        Validation.assertNotNull(centralBank);
        if (this.centralBankUuid != null)
            throw new RuntimeException("Currency " + this + " already has owner but is attempted to update.");

        this.centralBankUuid = centralBank.getKey();

        notifyObservers();
    }

    @Override
    public String toString() {
        return getStringKey();
    }
}
