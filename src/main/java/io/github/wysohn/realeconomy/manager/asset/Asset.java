package io.github.wysohn.realeconomy.manager.asset;

import io.github.wysohn.rapidframework3.core.caching.CachedElement;
import io.github.wysohn.rapidframework3.interfaces.entity.IEntitySnapshot;
import io.github.wysohn.realeconomy.manager.asset.signautre.AssetSignature;

import java.util.UUID;

public abstract class Asset<A extends Asset> extends CachedElement<UUID> implements IEntitySnapshot {
    private AssetSignature<A> signature;

    private long issuedDate;
    private long lastUpdate;

    private Asset() {
        super(null);
    }

    public Asset(UUID key) {
        super(key);
    }

    public AssetSignature<A> getSignature() {
        return signature;
    }

    void setSignature(AssetSignature<A> signature) {
        this.signature = signature;
    }

    public long getIssuedDate() {
        return issuedDate;
    }

    void setIssuedDate(long issuedDate) {
        this.issuedDate = issuedDate;

        notifyObservers();
    }

    public long getLastUpdate() {
        return lastUpdate;
    }

    void setLastUpdate(long lastUpdate) {
        this.lastUpdate = lastUpdate;

        notifyObservers();
    }

    protected static abstract class AbstractMemento {
        private final long issuedDate;
        private final long lastUpdate;

        public AbstractMemento(Asset asset) {
            issuedDate = asset.issuedDate;
            lastUpdate = asset.lastUpdate;
        }
    }
}
