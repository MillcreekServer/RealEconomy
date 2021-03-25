package io.github.wysohn.realeconomy.api.smartinv;

import fr.minuskube.inv.SmartInventory;
import io.github.wysohn.rapidframework3.bukkit.data.BukkitPlayer;
import io.github.wysohn.rapidframework3.bukkit.data.BukkitWrapper;
import io.github.wysohn.rapidframework3.core.api.ExternalAPI;
import io.github.wysohn.rapidframework3.core.language.ManagerLanguage;
import io.github.wysohn.rapidframework3.core.main.PluginMain;
import io.github.wysohn.realeconomy.api.smartinv.gui.AssetTransferGUI;
import io.github.wysohn.realeconomy.inject.annotation.NamespaceKeyAssetSerialized;
import io.github.wysohn.realeconomy.manager.banking.BankingTypeRegistry;
import io.github.wysohn.realeconomy.manager.banking.bank.AbstractBank;
import io.github.wysohn.realeconomy.manager.user.User;
import io.github.wysohn.realeconomy.manager.user.UserManager;
import io.github.wysohn.realeconomy.mediator.BankingMediator;
import io.github.wysohn.realeconomy.mediator.TradeMediator;
import org.bukkit.NamespacedKey;

import javax.inject.Inject;
import java.lang.ref.Reference;
import java.util.Optional;

public class SmartInvAPI extends ExternalAPI {
    @Inject
    private ManagerLanguage lang;
    @Inject
    private BankingMediator bankingMediator;
    @Inject
    private TradeMediator tradeMediator;
    @Inject
    @NamespaceKeyAssetSerialized
    private NamespacedKey serializedKey;
    @Inject
    private UserManager userManager;

    public SmartInvAPI(PluginMain main, String pluginName) {
        super(main, pluginName);
    }

    @Override
    public void enable() throws Exception {

    }

    @Override
    public void load() throws Exception {

    }

    @Override
    public void disable() throws Exception {

    }

    public void openTradeAccountGUI(AbstractBank bank, BukkitPlayer bukkitPlayer) {
        User user = userManager.get(bukkitPlayer.getKey())
                .map(Reference::get)
                .orElseThrow(RuntimeException::new);
        BankingMediator.AssetStorageWrapper storageWrapper = bankingMediator.wrapStorage(bank, user);
        BankingMediator.BankAccountWrapper accountWrapper = bankingMediator.wrapAccount(bank,
                user,
                BankingTypeRegistry.TRADING);

        SmartInventory.builder()
                .id("Assets")
                .provider(new AssetTransferGUI(lang, tradeMediator, player -> Optional.of(player)
                        .map(BukkitWrapper::sender)
                        .orElse(null),
                        storageWrapper,
                        accountWrapper,
                        player -> true)
                )
                .size(6, 9)
                .closeable(true)
                .build()
                .open(user.getSender());
    }
}
