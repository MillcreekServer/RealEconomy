package io.github.wysohn.realeconomy.manager.asset;

import io.github.wysohn.rapidframework3.interfaces.IMemento;

import java.util.UUID;

public class PhysicalAsset<T> extends Asset<PhysicalAsset<T>> {
    public PhysicalAsset(UUID key) {
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
