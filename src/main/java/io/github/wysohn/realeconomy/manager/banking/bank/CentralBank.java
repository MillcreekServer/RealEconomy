package io.github.wysohn.realeconomy.manager.banking.bank;

import io.github.wysohn.realeconomy.interfaces.IMemento;

import java.util.UUID;

public class CentralBank extends AbstractBank {
    private CentralBank() {
        super(null);
    }

    public CentralBank(UUID key) {
        super(key);
    }

    @Override
    public IMemento saveState() {
        return new Memento(this);
    }

    @Override
    public void restoreState(IMemento memento) {
        Memento mem = (Memento) memento;
        super.restoreState(mem);
    }

    private class Memento extends AbstractMemento {
        public Memento(CentralBank bank) {
            super(bank);
        }
    }
}
