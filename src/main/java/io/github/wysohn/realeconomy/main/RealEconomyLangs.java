package io.github.wysohn.realeconomy.main;

import io.github.wysohn.rapidframework3.interfaces.language.ILang;

public enum RealEconomyLangs implements ILang {
    Wallet("Wallet"),
    Currencies("Currencies"),
    Electricity("Electricity"),
    Duration("Duration"),

    BankingType_Checking("Checking Account"),
    BankingType_Trading("Trading Account"),

    Business_Tier("Tier"),
    Business_Progress("Progress"),
    Business_Input("Inputs"),
    Business_Output("Outputs"),

    MiningBusiness_LabourSources("LabourSources"),

    GUI_PreviousPage("&dPrevious Page"),
    GUI_Home_Title("&dFirst Page"),
    GUI_Home_Lore("", "&7Current Page&8: &6${integer}"),
    GUI_NextPage("&dNext Page"),
    GUI_Assets_IssuedDate("&dIssued on &a${date}"),
    GUI_Assets_Physical_Amount("&dAmount&8: &6${integer}"),

    TradeResult_Buy("&dBUY"),
    TradeResult_Sell("&6SELL"),
    TradeResult_InvalidInfo("&cSomething went"),
    TradeResult_Format("&8[${string}&8] &7Asset&8: &6${string}&8, &7Id&8: &6${integer}&8, &7Amount&8: &6${integer}"),
    TradeResult_Format2("  &8- &f${string}"),

    DelayedMessage_Format("&d${date full GMT-08:00}",
            "- &6${string} ${string} \u26c1${integer}",
            "- &7Result&8: ${string}"),
    DelayedMessage_InvalidInfo("&cAsset info doesn't match with the listed items."),
    DelayedMessage_WithdrawFailAsBuyer("&cYour account did not have enough currency in it to pay for the price." +
            " &7Check your balance with &d/bank info TRADING"),
    DelayedMessage_WithdrawFailAsSeller("&7The buyer did not have enough currency to pay for the price."),
    DelayedMessage_DepositFailAsBuyer("&7Failed to deposit money to the seller's account."),
    DelayedMessage_DepositFailAsSeller("&cCurrency couldn't be deposited to your account. Maximum reached maybe?" +
            " &7Check your balance with &d/bank info TRADING"),
    DelayedMessage_InsufficientAssetsBuyer("&7Matching was successful, yet the seller did not have the asset in the account."),
    DelayedMessage_InsufficientAssetsSeller("&cYou did not have enough assets as you promised in your account."),
    DelayedMessage_Ok("&aOK &7Check out &d/bank assets TRADING &7for the changes."),

    Command_Common_UserNotFound("&cNo player found with name &6${string}&c."),
    Command_Common_BankNotFound("&cNo bank found with name &6${string}&c."),
    Command_Common_CurrencyNotFound("&cNo currency found with name &6${string}&c."),
    Command_Common_SenderReceiverSame("&7The sender and the receiver cannot be the same."),
    Command_Common_NoCurrencyOwner("&7The currency &6${string} &7has no owner."),
    Command_Common_WithdrawRefused("&cWithdraw failed. &7Possible reasons:",
            "&71. Most likely, payer doesn't have enough money in the account.",
            "&72. If payer is a bank, it may ran out of papers to print more currencies."),
    Command_Common_DepositRefused("&cDeposit failed. &7Possible reasons:",
            "&71. Account no longer exist. Maybe the recipient account is closed?",
            "&72. Account reached maximum allowed limit."),
    Command_Common_SendSuccess("&d${string} &f=> &6${string} ${string} &f=> &d${string}&a."),
    Command_Common_InvalidOrderId("&cOrder id must be greater than 0."),
    Command_Common_PriceRange("&cPrice must be greater than 0.0 and less than ${double}"),
    Command_Common_NotInABank("&cThis command can be used only when you are in a bank."),
    Command_Common_NoAccount("&cYou must open &6${string} &caccount to do this." +
            " &7Use &d/bank open ${string} &7to open an account."),
    Command_Common_InvalidAmount("&cAmount must be larger than 0."),
    Command_Common_InvalidAccountType("&6${string} &cis not a valid account type!"),
    Command_Common_DeniedTradeType("&cThis item cannot be traded."),
    Command_Common_Adding("&7Adding the order..."),

    Command_Wallet_Desc("Check your wallet to see how much currency you have. &cThis doesn't" +
            " show your currency in your bank accounts."),
    Command_Wallet_Usage("&d/eco bal 1",
            " &8- &7First page of your wallet."),

    Command_Pay_Desc("Pay your currency to someone else."),
    Command_Pay_Usage("&d/eco pay wysohn 1304.22 dollar",
            " &8- &7Give wysohn 1304.22 dollar."),

    Command_Currencies_Desc("Show list of all currencies available in the server."),
    Command_Currencies_Usage("&d/eco currencies"),

    Command_Give_Desc("'Print' new currency and give it to target. Note that printing" +
            " new currency cause inflation."),
    Command_Give_Usage("&d/eco give wysohn 1003.67 dollar",
            " &8- &7wysohn will get 1003.67 dollar out of nowhere!"),

    Command_Take_Desc("'Destroy' currency by taking it from target"),
    Command_Take_Usage("&d/eco take wysohn 1003.67 dollar",
            " &8- &7take 1003.67 dollar from wysohn. Whoosh!"),

    Command_Bank_Desc("Various operations related to bank"),
    Command_Bank_Usage("&d/eco bank info [type]",
            " &8- &7check summary of the bank. If you enter the account type, it will show the balance of your account.",
            "&d/eco bank open <type>",
            " &8- &7Open a new account of given type at current bank.",
            "&d/eco bank deposit <type> <amount>",
            " &8- &7Deposit currency to the account of given type at current bank.",
            "&d/eco bank withdraw <type> <amount>",
            " &8- &7Withdraw currency from the account of given type to current bank.",
            "&d/eco bank assets <type>",
            " &8- &7List assets stored in the acccount."),
    Command_Bank_Balance("&dYour Account Balance &8: &6${string} ${string}"),
    Command_Bank_Assets_InvalidType("&cThat account cannot store assets."),
    Command_Bank_Info_Guide("&6To check your own account, specify the account type.",
            "&d/bank info <type>"),

    Command_Bank_Open_Success("&aNew account has opened!"),
    Command_Bank_Open_AlreadyExist("&7You already have the account in this bank."),
    Command_Bank_NoCurrencySet("&7The bank you are in does not have currency set."),
    Command_Bank_NoAccount("&cYou need appropriate account to proceed."),
    Command_Bank_NoCurrencyOwner("&cCurrency does not have owner."),
    Command_Bank_Success("&aTransaction successfully finished!"),

    Command_Items_Desc("List all selling items. You will have to use the &borderId &7to" +
            " purchase assets, so keep it in your mind."),
    Command_Items_Usage("&d/eco items [page] [category] &8- &7list all items currently selling"),
    Command_Items_InvalidCategory("&cInvalid category &6${string}"),
    //signature, price, currency, order_id
    Command_Items_Format("&8: &6${double} ${string} &8[&bOrderId &d${integer}&8]"),

    Command_Buy_Desc("Buy a listed item. You need &borderId &7by using &d/items &7command."),
    Command_Buy_Usage("&d/eco buy <order id> <price> <amount> &8- &7Bid to purchase the given asset.",
            "&7Bidding on asset does not guarantee the successful trade.",
            "&7Trade is made whenever &6there is a listed order with the price below the price you provided&7."),
    Command_Buy_NotEnoughCurrency("&cNot enough currency in your account.",
            "&7Remember that you are paying &6after &7the deals are matched by the system, so your &6TRADING &7account" +
                    " must be filled with enough currency in it beforehand. Otherwise, order will be canceled automatically."),
    Command_Buy_FailNotice("&7Your order is submitted. If your order is not visible in the order list," +
            " it may means that you have typed the wrong orderId, or the target item is no longer valid."),

    Command_Sell_Desc("Sell an item using the item in your hand."),
    Command_Sell_Usage("&d/eco sell <price> &8- &7sell the item in hand for specified price.",
            "&d/eco sell <price> <amount> &8- &7sell item amount of <amount> of the item type same as the one in your hand for specified price.",
            "&d/eco sell <price> * &8- &7sell &6all &7the of item type same as the one in your hand for specified price."),

    Command_Price_Desc("Check the price trend of the item in your hand."),
    Command_Price_Usage("&d/eco price &8- &7Price trend of the item (in your hand) in last &6${integer} days."),

    Command_Price_Format_Header_Top("&7Current open orders&8:"),
    Command_Price_Format_Seller("  &dSeller &7wants&8: &d${integer} &8@ &6${double} ${string}"), // Seller want: 119 @ 232 dollar
    Command_Price_Format_Buyer("  &dBuyer &7wants&8: &d${integer} &8@ &6${double} ${string}"), // Buyer want: 22 @ 199 dollar
    Command_Price_Format_Header_Trends("&6${integer} days &7trends&8:"),
    Command_Price_Format_LastPrice("  &dLast Trade Price&8: &6${double} ${string}"),
    Command_Price_Format_Average("  &dAverage Trade Price&8: &6${double} ${string}"),

    Command_Orders_Desc("List all orders issued by you."),
    Command_Orders_Usage("&d/eco orders &8- &7list all orders."),
    Command_Orders_Buys("&8[&aBUY &7id&8:&d${integer}&8] &7at &6${double} ${string} &e\u26c1${integer}"),
    Command_Orders_Sells("&8[&6SELL &7id&8:&d${integer}&8] &7at &6${double} ${string} &e\u26c1${integer}"),

    Command_Cancel_Desc("Cancel buy/sell order that has not processed yet."),
    Command_Cancel_Usage("&d/eco cancel <type> <order id> &8- &7cancel the order with given id.",
            "&dTypes&8: ${string}"),
    Command_Cancel_InvalidOrderType("&6${string} &cis not a valid order type."),
    Command_Cancel_Ownership("&cThat order is not issued by you."),

    // /eco business open <tier> <subtype>
    // /eco business disband
    // /eco business info
    // /eco business invite <name>
    // /eco business kick <offline name>
    // /eco business tiers [tier]
    Command_Business_Desc("Operate a business at this location."),
    Command_Business_Usage("&d/eco bus[iness] open <tier> [subtype]",
            " &8- &7Start a new business at this location. <tier> is main type of the business, and <subtype> is specialization" +
                    " if exist. Check out &d/eco business tiers &7command to see the list of possible tiers.",
            "&d/eco bus[iness] disband",
            " &8 - &7Delete the business at this location completely. &cThere will be no refund!",
            "&d/eco bus[iness] info",
            " &8 - &7Check information of business at this location",
            "&d/eco bus[iness] add[member] <name>",
            " &8 - &7Add <name> to the business at this location. &dThis may or may not work depending on the server setting.",
            "&d/eco bus[iness] remove[member] <name>",
            " &8 - &7Kick the <name> from the business at this location. &dThis may or may not work depending on the server setting.",
            "&d/eco bus[iness] tiers [tier]",
            " &8 - &7List all available tiers. You may specify [tier] to see the list of sub-types of the [tier]."),
    Command_Business_TierNotFound("&7No tier named &6${string}&7."),
    Command_Business_InvalidSubType("&7Sub-type &6${string} 7is not valid for tier &6${string}&7."),
    Command_Business_Open_NoProvider("&cNo provider found. This is a bug! Report to admins."),
    Command_Business_Open_DuplicatedLocation("&cThis business cannot be opened at this location."),
    Command_Business_Open_Ok("&aBusiness is open and now operating."),
    Command_Business_Disband_Ok("&aDisbanded the business."),
    Command_Business_Disband_Failure("&cSomething went wrong. Non of the providers handled the deletion." +
            " Report this to the admin"),
    Command_Business_Join_AlreadyRequested("&7You already have requested to join this business."),
    Command_Business_Join_AlreadyMember("&7You are already a member of this business."),
    Command_Business_Join_Requested("&aDone. &7Wait for the owner to accept your request!"),
    Command_Business_Add_AlreadyMember("&7This user is already a member."),
    Command_Business_Add_Ok("&aThe user has joined the business."),
    Command_Business_Kick_NotMember("&cThis user is not member."),
    Command_Business_Kick_Ok("&aThe user is no longer a member."),

    Command_Business_Common_NoBusinessUsing("&7No business found. You must be in a business you want to edit."),
    Command_Business_Common_MultipleBusinessUsing("&7Found multiple businesses. One of them will be" +
            " chosen randomly."),
    Command_Business_Common_NotOwner("&7You are not the owner of the business."),

    Bank_Owner("&9Owner"),
    Bank_BaseCurrency("&9BaseCurrency"),
    Bank_NumAccounts("&9Number of accounts"),
    Bank_Liquidity("&9Currency in circulation"),
    Bank_Papers("&9Number of papers"),
    Bank_PaperUnlimited("&9Paper unlimited"),
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
