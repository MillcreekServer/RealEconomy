package io.github.wysohn.realeconomy.manager.banking;

import io.github.wysohn.realeconomy.interfaces.banking.IBankingType;
import io.github.wysohn.realeconomy.manager.banking.account.CheckingAccount;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class BankingTypeRegistry {
    private static final Map<UUID, IBankingType> BANKING_TYPES = new HashMap<>();

    public static void registerType(UUID uuid, IBankingType type) {
        if (BANKING_TYPES.containsKey(uuid))
            throw new RuntimeException("Duplicated type: [" + uuid + ", " + type + "]");

        BANKING_TYPES.put(uuid, type);
    }

    public static final IBankingType CHECKING = new BankingTypeAdapter<>(
            UUID.fromString("3404aa73-a419-413f-8bc5-3d2c4b404f29"),
            CheckingAccount::new);

    public static IBankingType fromUuid(UUID uuid) {
        return BANKING_TYPES.get(uuid);
    }
}
