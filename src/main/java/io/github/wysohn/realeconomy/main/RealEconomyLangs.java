package io.github.wysohn.realeconomy.main;

import io.github.wysohn.rapidframework3.interfaces.language.ILang;

public enum RealEconomyLangs implements ILang {
    BankingType_Checking("Checking Account"),

    Command_Common_UserNotFound("&cNo player found with name &6${string}&c."),
    Command_Common_CurrencyNotFound("&cNo currency found with name &6${string}&c."),
    Command_Common_NoCurrencyOwner("&7The currency &6${string} &7has no owner."),
    Command_Common_WithdrawRefused("&cWithdraw failed. Make sure you can afford the amount entered."),
    Command_Common_DepositRefused("&cDeposit refused. Contact administrator if you believe it's a bug."),
    Command_Common_SendSuccess_Sender("&6${string} ${string} &ais sent to &6${string}&a."),
    Command_Common_SendSuccess_Receiver("&aYou have received &6${string} ${string}&a from ${string}&a."),

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
