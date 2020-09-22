package io.github.wysohn.realeconomy.manager.asset;

import io.github.wysohn.rapidframework3.interfaces.IMemento;

import java.util.UUID;

public class CurrencyAsset extends Asset<CurrencyAsset> {
    public CurrencyAsset(UUID key) {
        super(key);
    }

    @Override
    public IMemento saveState() {
        return null;
    }

    @Override
    public void restoreState(IMemento memento) {

    }
}
