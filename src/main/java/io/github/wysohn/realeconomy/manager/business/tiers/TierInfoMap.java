package io.github.wysohn.realeconomy.manager.business.tiers;

import io.github.wysohn.rapidframework3.interfaces.store.IKeyValueStorage;
import io.github.wysohn.rapidframework3.utils.Validation;
import io.github.wysohn.realeconomy.manager.asset.signature.AssetSignature;
import io.github.wysohn.realeconomy.manager.listing.AssetListing;
import io.github.wysohn.realeconomy.manager.listing.AssetListingManager;

import java.lang.ref.Reference;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class TierInfoMap {
    private final IKeyValueStorage storage;
    private final Object section;

    public TierInfoMap(IKeyValueStorage storage, Object section) {
        Validation.validate(section, storage::isSection, "not a section.");
        this.storage = storage;
        this.section = section;
    }

    public Map<AssetSignature, Double> getAll(AssetListingManager assetListingManager) {
        Map<AssetSignature, Double> out = new HashMap<>();
        storage.getKeys(section, false).stream()
                .map(String.class::cast)
                .map(UUID::fromString)
                .forEach(uuid -> {
                    AssetListing listing = assetListingManager.get(uuid)
                            .map(Reference::get)
                            .orElse(null);
                    if (listing == null)
                        return;

                    double value = storage.get(section, uuid.toString())
                            .map(Double.class::cast)
                            .orElse(0.0);
                    if (value <= 0.0)
                        return;

                    out.put(listing.getSignature(), value);
                });
        return out;
    }

    public void put(AssetListingManager assetListingManager, AssetSignature signature, double value) {
        AssetListing listing = assetListingManager.fromSignature(signature);
        if (listing == null)
            return;

        storage.put(section, listing.getKey().toString(), value);
    }


}
