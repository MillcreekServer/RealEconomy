package io.github.wysohn.realeconomy.manager.asset;

import io.github.wysohn.rapidframework3.interfaces.IMemento;
import io.github.wysohn.realeconomy.manager.asset.signature.AssetSignature;

import java.util.UUID;

public class Check extends Asset {
    private Check() {
        super(null, null);
    }

    public Check(UUID key, AssetSignature signature) {
        super(key, signature);
    }

    public Check(AssetSignature signature) {
        super(signature);
    }

    @Override
    public IMemento saveState() {
        throw new RuntimeException();
    }

    @Override
    public void restoreState(IMemento savedState) {
        throw new RuntimeException();
    }
}
