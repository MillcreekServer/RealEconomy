package io.github.wysohn.realeconomy.manager.asset;

import io.github.wysohn.rapidframework3.core.language.DynamicLang;
import io.github.wysohn.realeconomy.api.smartinv.gui.GUISlotIcon;
import io.github.wysohn.realeconomy.main.RealEconomyLangs;
import io.github.wysohn.realeconomy.manager.asset.signature.AssetSignature;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.*;

public abstract class Asset implements GUISlotIcon {
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

    public List<DynamicLang> lore() {
        return new ArrayList<DynamicLang>() {{
            add(new DynamicLang(RealEconomyLangs.GUI_Assets_IssuedDate, (s, m) ->
                    m.addDate(new Date(issuedDate))));
        }};
    }

    @Override
    public ItemStack getIcon(){
        return new ItemStack(Material.PAPER);
    }

    public abstract Asset clone();

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Asset asset = (Asset) o;
        return uuid.equals(asset.uuid) &&
                signature.equals(asset.signature);
    }

    @Override
    public int hashCode() {
        return Objects.hash(uuid, signature);
    }
}
