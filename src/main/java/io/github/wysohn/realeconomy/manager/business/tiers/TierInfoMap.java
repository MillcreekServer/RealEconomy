package io.github.wysohn.realeconomy.manager.business.tiers;

import io.github.wysohn.realeconomy.manager.asset.signature.AssetSignature;
import io.github.wysohn.realeconomy.manager.listing.AssetListing;
import io.github.wysohn.realeconomy.manager.listing.AssetListingManager;

import java.lang.ref.Reference;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class TierInfoMap {
    private final Map<String, Object> listingToAmountMap;

    public TierInfoMap(Map<String, Object> listingToAmountMap) {
        this.listingToAmountMap = listingToAmountMap;
    }

    public Map<AssetSignature, Double> getAll(AssetListingManager assetListingManager) {
        Map<AssetSignature, Double> out = new HashMap<>();
        listingToAmountMap.forEach((uuidStr, amount) -> {
            UUID uuid = UUID.fromString(uuidStr);
            double value = Optional.ofNullable(amount)
                    .map(Double.class::cast)
                    .orElseThrow(() -> new RuntimeException("Invalid amount " + amount + " for UUID " + uuidStr));

            AssetListing listing = assetListingManager.get(uuid)
                    .map(Reference::get)
                    .orElse(null);
            if (listing == null)
                return;

            if (value <= 0.0)
                return;

            out.put(listing.getSignature(), value);
        });
        return out;
    }
}
