package io.github.wysohn.realeconomy.manager.asset.listing;

import io.github.wysohn.realeconomy.manager.asset.Asset;
import io.github.wysohn.realeconomy.manager.asset.signautre.AssetSignature;

public abstract class AssetListing<A extends Asset> {
    private AssetSignature<A> signature;

    protected int maxStock;
    protected int currentStock;

    public int getMaxStock() {
        return maxStock;
    }

    public int getCurrentStock() {
        return currentStock;
    }

    public abstract A actualize(Object... args);
}
