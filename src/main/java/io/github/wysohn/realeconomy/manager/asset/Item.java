package io.github.wysohn.realeconomy.manager.asset;

import io.github.wysohn.realeconomy.manager.asset.signature.AssetSignature;

import java.util.UUID;

public class Item extends PhysicalAsset {
    private Item() {
        super(null, null);
    }

    public Item(UUID key, AssetSignature signature) {
        super(key, signature);
    }

    @Override
    public Asset clone() {
        Item item = new Item(getUuid(), getSignature());
        item.setAmount(this.getAmount());
        return item;
    }
}
