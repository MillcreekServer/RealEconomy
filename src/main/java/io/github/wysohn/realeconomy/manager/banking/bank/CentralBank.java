package io.github.wysohn.realeconomy.manager.banking.bank;

import java.util.UUID;

public class CentralBank extends AbstractBank {
    private CentralBank() {
        super(null);
    }

    public CentralBank(UUID key) {
        super(key);
    }
}
