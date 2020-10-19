package io.github.wysohn.realeconomy.manager.banking;

import io.github.wysohn.realeconomy.interfaces.banking.IBankingType;
import io.github.wysohn.realeconomy.main.RealEconomyLangs;
import io.github.wysohn.realeconomy.manager.banking.account.CheckingAccount;
import io.github.wysohn.realeconomy.manager.banking.account.TradingAccount;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class BankingTypeRegistry {
    private static final Map<UUID, IBankingType> BANKING_TYPES = new HashMap<>();
    private static final Map<String, IBankingType> BANKING_TYPES_STR = new HashMap<>();

    public static void registerType(UUID uuid, String name, IBankingType type) {
        if (BANKING_TYPES.containsKey(uuid))
            throw new RuntimeException("Duplicated type: [" + uuid + ", " + type + "]");
        if (BANKING_TYPES_STR.containsKey(name))
            throw new RuntimeException("Duplicated name: [" + type.name() + ", " + type + "]");

        BANKING_TYPES.put(uuid, type);
        BANKING_TYPES_STR.put(name, type);
    }

    public static final IBankingType CHECKING = new BankingTypeAdapter<>(
            UUID.fromString("3404aa73-a419-413f-8bc5-3d2c4b404f29"),
            "CHECKING",
            RealEconomyLangs.BankingType_Checking,
            CheckingAccount::new);

    public static final IBankingType TRADING = new BankingTypeAdapter<>(
            UUID.fromString("393d21ee-6225-4afa-9db5-1d386ee2530b"),
            "TRADING",
            RealEconomyLangs.BankingType_Trading,
            TradingAccount::new
    );

    public static IBankingType fromUuid(UUID uuid) {
        return BANKING_TYPES.get(uuid);
    }

    public static IBankingType fromString(String s) {
        return BANKING_TYPES_STR.get(s);
    }

    public static IBankingType[] values() {
        return BANKING_TYPES.values().toArray(new IBankingType[0]);
    }
}
