package io.github.wysohn.realeconomy.manager.asset;

import io.github.wysohn.rapidframework3.interfaces.IMemento;
import io.github.wysohn.realeconomy.manager.asset.signature.AssetSignature;

import java.util.UUID;

public class Item extends Asset {
    private int amount;

    private Item() {
        super(null, null);
    }

    public Item(UUID key, AssetSignature signature) {
        super(key, signature);
    }

    public int getAmount() {
        return amount;
    }

    public void setAmount(int amount) {
        this.amount = amount;

        setLastUpdate(System.currentTimeMillis());
    }

    @Override
    public IMemento saveState() {
        return new Memento(this);
    }

    @Override
    public void restoreState(IMemento iMemento) {
        super.restoreState(iMemento);

        Memento mem = (Memento) iMemento;

        this.amount = mem.amount;
    }

    private static class Memento extends AbstractMemento {
        private final int amount;

        public Memento(Item item) {
            super(item);
            this.amount = item.amount;
        }
    }
}
