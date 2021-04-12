package io.github.wysohn.realeconomy.mediator;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.github.wysohn.rapidframework3.bukkit.manager.api.PlaceholderAPI;
import io.github.wysohn.rapidframework3.core.api.ManagerExternalAPI;
import io.github.wysohn.rapidframework3.core.main.Mediator;
import io.github.wysohn.realeconomy.interfaces.banking.IBankingType;
import io.github.wysohn.realeconomy.manager.banking.BankingTypeRegistry;
import io.github.wysohn.realeconomy.manager.banking.VisitingBankManager;
import io.github.wysohn.realeconomy.manager.banking.bank.AbstractBank;
import io.github.wysohn.realeconomy.manager.currency.Currency;
import io.github.wysohn.realeconomy.manager.currency.CurrencyManager;
import io.github.wysohn.realeconomy.manager.user.User;
import io.github.wysohn.realeconomy.manager.user.UserManager;

import java.lang.ref.Reference;
import java.math.RoundingMode;

@Singleton
public class PlaceholderMediator extends Mediator {
    private final ManagerExternalAPI api;
    private final CurrencyManager currencyManager;
    private final UserManager userManager;
    private final VisitingBankManager visitingBankManager;

    @Inject
    public PlaceholderMediator(ManagerExternalAPI api,
                               CurrencyManager currencyManager,
                               UserManager userManager,
                               VisitingBankManager visitingBankManager) {
        this.api = api;
        this.currencyManager = currencyManager;
        this.userManager = userManager;
        this.visitingBankManager = visitingBankManager;
    }

    @Override
    public void enable() throws Exception {
        api.getAPI(PlaceholderAPI.class).ifPresent(placeholderAPI -> {
            placeholderAPI.register("recon", (p, params) -> {
                if (p == null)
                    return null;

                User user = userManager.get(p.getUuid())
                        .map(Reference::get)
                        .orElseThrow(RuntimeException::new);

                String[] splits = params.split("_");

                if (splits.length == 1 && "currentbank".equals(splits[0])) {
                    return visitingBankManager.getUsingBank(user).toString();
                } else if (splits.length == 2 && "wallet".equalsIgnoreCase(splits[0])) {
                    String currencyName = splits[1];
                    Currency currency = currencyManager.get(currencyName)
                            .map(Reference::get)
                            .orElse(null);
                    if (currency == null)
                        return currencyName + "[?]";

                    return user.balance(currency) + " " + currency;
                } else if (splits.length == 3 && "account".equals(splits[0])) {
                    String contentType = splits[1];
                    String accountType = splits[2];

                    AbstractBank bank = visitingBankManager.getUsingBank(user);
                    if (bank == null)
                        return "No Bank";

                    IBankingType type = BankingTypeRegistry.fromString(accountType.toUpperCase());
                    if (type == null)
                        return accountType + "[?]";

                    switch (contentType) {
                        case "balance":
                            if (!bank.hasAccount(user, type))
                                return "X";

                            return String.valueOf(bank.balanceOfAccount(user, type)
                                    .setScale(4, RoundingMode.FLOOR));
                        case "items":
                            if (type != BankingTypeRegistry.TRADING)
                                return "0";

                            if (!bank.hasAccount(user, type))
                                return "X";

                            return String.valueOf(bank.accountAssetProvider(user).size());
                        default:
                            return contentType + "[?]";
                    }
                }

                return "";
            });
        });
    }

    @Override
    public void load() throws Exception {

    }

    @Override
    public void disable() throws Exception {

    }
}
