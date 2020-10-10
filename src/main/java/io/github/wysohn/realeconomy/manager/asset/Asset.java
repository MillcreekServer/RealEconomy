package io.github.wysohn.realeconomy.manager.asset;

import io.github.wysohn.rapidframework3.interfaces.IMemento;
import io.github.wysohn.rapidframework3.interfaces.entity.IEntitySnapshot;
import io.github.wysohn.realeconomy.manager.asset.signature.AssetSignature;

import java.util.Objects;
import java.util.UUID;

public abstract class Asset implements IEntitySnapshot {
    private final UUID uuid;
    private final AssetSignature signature;
    private final long issuedDate;

    private long lastUpdate;

    public Asset(UUID key, AssetSignature signature, long issuedDate) {
        this.uuid = key;
        this.signature = signature;
        this.issuedDate = issuedDate;
    }

    public Asset(UUID key, AssetSignature signature) {
        this(key, signature, System.currentTimeMillis());
    }

    public Asset(AssetSignature signature) {
        this(UUID.randomUUID(), signature, System.currentTimeMillis());
    }

    public UUID getUuid() {
        return uuid;
    }

    public AssetSignature getSignature() {
        return signature;
    }

    public long getIssuedDate() {
        return issuedDate;
    }

    public long getLastUpdate() {
        return lastUpdate;
    }

    void setLastUpdate(long lastUpdate) {
        this.lastUpdate = lastUpdate;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Asset asset = (Asset) o;
        return uuid.equals(asset.uuid);
    }

    @Override
    public int hashCode() {
        return Objects.hash(uuid);
    }

    @Override
    public void restoreState(IMemento iMemento) {
        AbstractMemento mem = (AbstractMemento) iMemento;

        this.lastUpdate = mem.lastUpdate;
    }

    static abstract class AbstractMemento implements IMemento {
        private final long lastUpdate;

        public AbstractMemento(Asset asset) {
            this.lastUpdate = asset.lastUpdate;
        }
    }
}
