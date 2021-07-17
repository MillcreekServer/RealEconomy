package io.github.wysohn.realeconomy.manager.currency;

import io.github.wysohn.rapidframework3.core.caching.CachedElement;
import io.github.wysohn.rapidframework3.utils.Validation;
import io.github.wysohn.realeconomy.manager.banking.CentralBankingManager;
import io.github.wysohn.realeconomy.manager.banking.bank.CentralBank;

import javax.inject.Inject;
import javax.persistence.Column;
import javax.persistence.Transient;
import java.util.Optional;
import java.util.UUID;

public class Currency extends CachedElement<UUID> {
    @Inject
    @Transient
    private CentralBankingManager centralBankingManager;

    @Column
    private String code;
    @Column
    private UUID centralBankUuid;
    @Column
    private int useCount;

    private Currency() {
        super((UUID) null);
    }

    private Currency(Currency copy){
        super(copy.getKey());
        centralBankingManager = copy.centralBankingManager;
        code = copy.code;
        centralBankUuid = copy.centralBankUuid;
        useCount = copy.useCount;
    }

    public Currency(UUID key) {
        super(key);
    }

    public String getCode() {
        return code;
    }

    void setCode(String code) {
        mutate(() -> this.code = code);
    }

    public CentralBank ownerBank() {
        return Optional.ofNullable(read(() -> centralBankUuid))
                .flatMap(centralBankingManager::get)
                .orElse(null);
    }

    public void setCentralBank(CentralBank centralBank) {
        Validation.assertNotNull(centralBank);
        if (this.centralBankUuid != null)
            throw new RuntimeException("Currency " + this + " already has owner but is attempted to update.");

        mutate(() -> this.centralBankUuid = centralBank.getKey());
    }

    public int getUseCount() {
        return read(() -> useCount);
    }

    public void setUseCount(int useCount) {
        mutate(() -> this.useCount = useCount);
    }

    @Override
    public String toString() {
        return getStringKey();
    }
}
