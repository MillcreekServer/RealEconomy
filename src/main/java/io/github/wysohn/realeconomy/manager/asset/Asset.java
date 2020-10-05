package io.github.wysohn.realeconomy.manager.asset;

import io.github.wysohn.rapidframework3.interfaces.entity.IEntitySnapshot;
import io.github.wysohn.realeconomy.manager.asset.signature.AssetSignature;

import java.util.Objects;
import java.util.UUID;

public abstract class Asset implements IEntitySnapshot {
    private final UUID uuid;
    private final AssetSignature signature;

    private long issuedDate;
    private long lastUpdate;

    public Asset(UUID key, AssetSignature signature) {
        this.uuid = key;
        this.signature = signature;
    }

    public Asset(AssetSignature signature) {
        this(UUID.randomUUID(), signature);
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

    void setIssuedDate(long issuedDate) {
        this.issuedDate = issuedDate;
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
}
