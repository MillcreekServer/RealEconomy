package io.github.wysohn.realeconomy.api.vault;

import io.github.wysohn.rapidframework3.core.api.ExternalAPI;
import io.github.wysohn.rapidframework3.core.main.PluginMain;
import io.github.wysohn.realeconomy.main.Metrics;
import io.github.wysohn.realeconomy.manager.banking.BankingTypeRegistry;
import io.github.wysohn.realeconomy.manager.banking.bank.AbstractBank;
import io.github.wysohn.realeconomy.manager.currency.Currency;
import io.github.wysohn.realeconomy.manager.user.UserManager;
import io.github.wysohn.realeconomy.mediator.BankingMediator;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.ServicePriority;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.lang.ref.Reference;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Singleton
public class VaultHook extends ExternalAPI {
    @Inject
    private BankingMediator bankingMediator;
    @Inject
    private UserManager userManager;

    private boolean enabled = true;

    public VaultHook(PluginMain main, String pluginName) {
        super(main, pluginName);
    }

    @Override
    public void enable() throws Exception {
        Bukkit.getServicesManager().register(Economy.class,
                new EconomyProvider(),
                main.getPlatform(),
                ServicePriority.Highest);

        enabled = true;
    }

    @Override
    public void load() throws Exception {

    }

    @Override
    public void disable() throws Exception {
        enabled = false;
    }

    private class EconomyProvider implements Economy {

        @Override
        public boolean isEnabled() {
            return enabled;
        }

        @Override
        public String getName() {
            return "RealEconomy";
        }

        @Override
        public boolean hasBankSupport() {
            return true;
        }

        @Override
        public int fractionalDigits() {
            return 2;
        }

        @Override
        public String format(double amount) {
            return Metrics.df.format(amount);
        }

        @Override
        public String currencyNamePlural() {
            return Optional.of(BankingMediator.getServerBank())
                    .filter(AbstractBank::isOperating)
                    .map(AbstractBank::getBaseCurrency)
                    .map(Currency::toString)
                    .orElseThrow(RuntimeException::new);
        }

        @Override
        public String currencyNameSingular() {
            return Optional.of(BankingMediator.getServerBank())
                    .filter(AbstractBank::isOperating)
                    .map(AbstractBank::getBaseCurrency)
                    .map(Currency::getCode)
                    .orElseThrow(RuntimeException::new);
        }

        @Override
        public boolean hasAccount(String playerName) {
            return userManager.get(playerName)
                    .map(Reference::get)
                    .flatMap(user -> Optional.ofNullable(bankingMediator.getUsingBank(user))
                            .map(bank -> bank.hasAccount(user, BankingTypeRegistry.CHECKING)))
                    .orElse(false);
        }

        @Override
        public boolean hasAccount(OfflinePlayer player) {
            return userManager.get(player.getUniqueId())
                    .map(Reference::get)
                    .flatMap(user -> Optional.ofNullable(bankingMediator.getUsingBank(user))
                            .map(bank -> bank.hasAccount(user, BankingTypeRegistry.CHECKING)))
                    .orElse(false);
        }

        @Override
        public boolean hasAccount(String playerName, String worldName) {
            return hasAccount(playerName);
        }

        @Override
        public boolean hasAccount(OfflinePlayer player, String worldName) {
            return hasAccount(player);
        }

        @Override
        public double getBalance(String playerName) {
            return userManager.get(playerName)
                    .map(Reference::get)
                    .map(user -> user.balance(BankingMediator.getServerBank().getBaseCurrency()).doubleValue())
                    .orElse(0.0);
        }

        @Override
        public double getBalance(OfflinePlayer player) {
            return userManager.get(player.getUniqueId())
                    .map(Reference::get)
                    .map(user -> user.balance(BankingMediator.getServerBank().getBaseCurrency()).doubleValue())
                    .orElse(0.0);
        }

        @Override
        public double getBalance(String playerName, String world) {
            return getBalance(playerName);
        }

        @Override
        public double getBalance(OfflinePlayer player, String world) {
            return getBalance(player);
        }

        @Override
        public boolean has(String playerName, double amount) {
            return getBalance(playerName) >= amount;
        }

        @Override
        public boolean has(OfflinePlayer player, double amount) {
            return getBalance(player) >= amount;
        }

        @Override
        public boolean has(String playerName, String worldName, double amount) {
            return getBalance(playerName) >= amount;
        }

        @Override
        public boolean has(OfflinePlayer player, String worldName, double amount) {
            return getBalance(player) >= amount;
        }

        @Override
        public EconomyResponse withdrawPlayer(String playerName, double amount) {
            return userManager.get(playerName)
                    .map(Reference::get)
                    .filter(user -> user.withdraw(amount, BankingMediator.getServerBank().getBaseCurrency()))
                    .map(user -> new EconomyResponse(amount, getBalance(playerName), EconomyResponse.ResponseType.SUCCESS, null))
                    .orElseGet(() -> new EconomyResponse(0, getBalance(playerName), EconomyResponse.ResponseType.FAILURE, null));
        }

        @Override
        public EconomyResponse withdrawPlayer(OfflinePlayer player, double amount) {
            return userManager.get(player.getUniqueId())
                    .map(Reference::get)
                    .filter(user -> user.withdraw(amount, BankingMediator.getServerBank().getBaseCurrency()))
                    .map(user -> new EconomyResponse(amount, getBalance(player), EconomyResponse.ResponseType.SUCCESS, null))
                    .orElseGet(() -> new EconomyResponse(0, getBalance(player), EconomyResponse.ResponseType.FAILURE, null));
        }

        @Override
        public EconomyResponse withdrawPlayer(String playerName, String worldName, double amount) {
            return new EconomyResponse(0, getBalance(playerName), EconomyResponse.ResponseType.NOT_IMPLEMENTED, null);
        }

        @Override
        public EconomyResponse withdrawPlayer(OfflinePlayer player, String worldName, double amount) {
            return new EconomyResponse(0, getBalance(player), EconomyResponse.ResponseType.NOT_IMPLEMENTED, null);
        }

        @Override
        public EconomyResponse depositPlayer(String playerName, double amount) {
            return userManager.get(playerName)
                    .map(Reference::get)
                    .filter(user -> user.deposit(amount, BankingMediator.getServerBank().getBaseCurrency()))
                    .map(user -> new EconomyResponse(amount, getBalance(playerName), EconomyResponse.ResponseType.SUCCESS, null))
                    .orElseGet(() -> new EconomyResponse(0, getBalance(playerName), EconomyResponse.ResponseType.FAILURE, null));
        }

        @Override
        public EconomyResponse depositPlayer(OfflinePlayer player, double amount) {
            return userManager.get(player.getUniqueId())
                    .map(Reference::get)
                    .filter(user -> user.deposit(amount, BankingMediator.getServerBank().getBaseCurrency()))
                    .map(user -> new EconomyResponse(amount, getBalance(player), EconomyResponse.ResponseType.SUCCESS, null))
                    .orElseGet(() -> new EconomyResponse(0, getBalance(player), EconomyResponse.ResponseType.FAILURE, null));
        }

        @Override
        public EconomyResponse depositPlayer(String playerName, String worldName, double amount) {
            return new EconomyResponse(0, getBalance(playerName), EconomyResponse.ResponseType.NOT_IMPLEMENTED, null);
        }

        @Override
        public EconomyResponse depositPlayer(OfflinePlayer player, String worldName, double amount) {
            return new EconomyResponse(0, getBalance(player), EconomyResponse.ResponseType.NOT_IMPLEMENTED, null);
        }

        @Override
        public EconomyResponse createBank(String name, String player) {
            return new EconomyResponse(0, getBalance(player), EconomyResponse.ResponseType.NOT_IMPLEMENTED, null);
        }

        @Override
        public EconomyResponse createBank(String name, OfflinePlayer player) {
            return new EconomyResponse(0, getBalance(player), EconomyResponse.ResponseType.NOT_IMPLEMENTED, null);
        }

        @Override
        public EconomyResponse deleteBank(String name) {
            return new EconomyResponse(0, getBalance(name), EconomyResponse.ResponseType.NOT_IMPLEMENTED, null);
        }

        @Override
        public EconomyResponse bankBalance(String name) {
            return new EconomyResponse(0, getBalance(name), EconomyResponse.ResponseType.NOT_IMPLEMENTED, null);
        }

        @Override
        public EconomyResponse bankHas(String name, double amount) {
            return new EconomyResponse(0, getBalance(name), EconomyResponse.ResponseType.NOT_IMPLEMENTED, null);
        }

        @Override
        public EconomyResponse bankWithdraw(String name, double amount) {
            return new EconomyResponse(0, getBalance(name), EconomyResponse.ResponseType.NOT_IMPLEMENTED, null);
        }

        @Override
        public EconomyResponse bankDeposit(String name, double amount) {
            return new EconomyResponse(0, getBalance(name), EconomyResponse.ResponseType.NOT_IMPLEMENTED, null);
        }

        @Override
        public EconomyResponse isBankOwner(String name, String playerName) {
            return new EconomyResponse(0, getBalance(name), EconomyResponse.ResponseType.NOT_IMPLEMENTED, null);
        }

        @Override
        public EconomyResponse isBankOwner(String name, OfflinePlayer player) {
            return new EconomyResponse(0, getBalance(player), EconomyResponse.ResponseType.NOT_IMPLEMENTED, null);
        }

        @Override
        public EconomyResponse isBankMember(String name, String playerName) {
            return new EconomyResponse(0, getBalance(name), EconomyResponse.ResponseType.NOT_IMPLEMENTED, null);
        }

        @Override
        public EconomyResponse isBankMember(String name, OfflinePlayer player) {
            return new EconomyResponse(0, getBalance(player), EconomyResponse.ResponseType.NOT_IMPLEMENTED, null);
        }

        @Override
        public List<String> getBanks() {
            return Collections.singletonList(BankingMediator.getServerBank().getStringKey());
        }

        @Override
        public boolean createPlayerAccount(String playerName) {
            return userManager.get(playerName)
                    .map(Reference::get)
                    .flatMap(user -> Optional.of(bankingMediator)
                            .map(mediator -> mediator.getUsingBank(user))
                            .map(bank -> bank.putAccount(user, BankingTypeRegistry.TRADING)))
                    .orElse(false);
        }

        @Override
        public boolean createPlayerAccount(OfflinePlayer player) {
            return userManager.get(player.getUniqueId())
                    .map(Reference::get)
                    .flatMap(user -> Optional.of(bankingMediator)
                            .map(mediator -> mediator.getUsingBank(user))
                            .map(bank -> bank.putAccount(user, BankingTypeRegistry.TRADING)))
                    .orElse(false);
        }

        @Override
        public boolean createPlayerAccount(String playerName, String worldName) {
            return createPlayerAccount(playerName);
        }

        @Override
        public boolean createPlayerAccount(OfflinePlayer player, String worldName) {
            return createPlayerAccount(player);
        }
    }
}
