package io.github.wysohn.realeconomy.main;

import io.github.wysohn.rapidframework3.interfaces.language.ILang;

public enum RealEconomyLangs implements ILang {
    Wallet("Wallet"),
    Currencies("Currencies"),

    BankingType_Checking("Checking Account"),
    BankingType_Trading("Trading Account"),

    Command_Common_UserNotFound("&cNo player found with name &6${string}&c."),
    Command_Common_BankNotFound("&cNo bank found with name &6${string}&c."),
    Command_Common_CurrencyNotFound("&cNo currency found with name &6${string}&c."),
    Command_Common_SenderReceiverSame("&7The sender and the receiver cannot be the same."),
    Command_Common_NoCurrencyOwner("&7The currency &6${string} &7has no owner."),
    Command_Common_WithdrawRefused("&cWithdraw failed. Contact administrator if you believe it's a bug."),
    Command_Common_DepositRefused("&cDeposit refused. Contact administrator if you believe it's a bug."),
    Command_Common_SendSuccess("&d${string} &f=> &6${string} ${string} &f=> &d${string}&a."),

    Command_Balance_Desc("Check your wallet."),
    Command_Balance_Usage("&d/eco bal 1",
            " &8- &7First page of your wallet."),

    Command_Pay_Desc("Pay your currency to someone else."),
    Command_Pay_Usage("&d/eco pay wysohn 1304.22 dollar",
            " &8- &7Give wysohn 1304.22 dollar."),

    Command_Currencies_Desc("Show list of all currencies available in the server."),
    Command_Currencies_Usage("&d/eco give wysohn 1003.67 dollar",
            " &8- &7wysohn will get 1003.67 dollar out of thin air."),

    Command_Give_Desc("'Print' new currency and give it to target"),
    Command_Give_Usage("&d/eco give wysohn 1003.67 dollar",
            " &8- &7wysohn will get 1003.67 dollar out of thin air."),

    Command_Take_Desc("'Destroy' currency by taking it from target"),
    Command_Take_Usage("&d/eco take wysohn 1003.67 dollar",
            " &8- &7take 1003.67 dollar from wysohn. Whoosh!"),

    Command_Bank_Desc("Various operations related to bank"),
    Command_Bank_Usage("&d/eco bank * info",
            " &8- &7check summary of the bank owned by server",
            "&d/eco bank MyBank info",
            " &8- &7check summary of MyBank"),

    Bank_Owner("&9Owner"),
    Bank_BaseCurrency("&9BaseCurrency"),
    Bank_NumAccounts("&9Number of accounts"),
    Bank_Liquidity("&9Currency in circulation"),
    Bank_Finance("&9Finance"),

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
