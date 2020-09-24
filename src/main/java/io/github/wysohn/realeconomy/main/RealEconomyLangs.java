package io.github.wysohn.realeconomy.main;

import io.github.wysohn.rapidframework3.interfaces.language.ILang;

public enum RealEconomyLangs implements ILang {
    BankingType_Checking("Checking Account"),

    Command_Common_UserNotFound("&cNo player found with name &6${string}&c."),
    Command_Common_CurrencyNotFound("&cNo currency found with name &6${string}&c."),

    ;

    private final String[] def;

    RealEconomyLangs(String... def) {
        this.def = def;
    }

    @Override
    public String[] getEngDefault() {
        return def;
    }
}
