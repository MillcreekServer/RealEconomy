package io.github.wysohn.realeconomy.manager.business.tiers;

import io.github.wysohn.realeconomy.interfaces.business.tiers.ITier;

import java.util.HashMap;
import java.util.Map;

public class TierRegistry {
    private static final Map<String, ITier> REGISTERED_TIERS = new HashMap<>();

    public static void register(ITier tier) {
        if (REGISTERED_TIERS.containsKey(tier.name()))
            throw new RuntimeException("Duplicated ITier name: " + tier.name());

        REGISTERED_TIERS.put(tier.name(), tier);
    }

    public static ITier fromString(String name) {
        return REGISTERED_TIERS.get(name);
    }

    public static ITier[] values() {
        return REGISTERED_TIERS.values().toArray(new ITier[0]);
    }
}
