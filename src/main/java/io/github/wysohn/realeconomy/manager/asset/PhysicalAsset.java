package io.github.wysohn.realeconomy.manager.asset;

import io.github.wysohn.rapidframework3.core.language.DynamicLang;
import io.github.wysohn.realeconomy.main.RealEconomyLangs;
import io.github.wysohn.realeconomy.manager.asset.signature.AssetSignature;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public abstract class PhysicalAsset extends Asset {
    private int amount;

    public PhysicalAsset(UUID key,
                         AssetSignature signature) {
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
    public List<DynamicLang> lore() {
        return new ArrayList<DynamicLang>() {{
            add(new DynamicLang(RealEconomyLangs.GUI_Assets_Physical_Amount, (s, m) -> m.addInteger(amount)));
        }};
    }
}
