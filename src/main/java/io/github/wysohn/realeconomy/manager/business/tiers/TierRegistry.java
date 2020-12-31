package io.github.wysohn.realeconomy.manager.business.tiers;

import io.github.wysohn.realeconomy.interfaces.business.tiers.ITier;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class TierRegistry {
    private static final Map<UUID, ITier> REGISTERED_TIERS = new HashMap<>();
    private static final Map<String, ITier> REGISTERED_TIER_NAMES = new HashMap<>();

    public static void register(ITier tier) {
        if (REGISTERED_TIERS.containsKey(tier.getUuid()))
            throw new RuntimeException("Duplicated ITier: " + tier.getUuid());
        if (REGISTERED_TIER_NAMES.containsKey(tier.name()))
            throw new RuntimeException("Duplicated ITier name: " + tier.name());

        REGISTERED_TIERS.put(tier.getUuid(), tier);
        REGISTERED_TIER_NAMES.put(tier.name(), tier);
    }

    public static ITier fromUuid(UUID uuid) {
        return REGISTERED_TIERS.get(uuid);
    }

    public static ITier fromString(String name) {
        return REGISTERED_TIER_NAMES.get(name);
    }

    public static ITier[] values() {
        return REGISTERED_TIERS.values().toArray(new ITier[0]);
    }
}
