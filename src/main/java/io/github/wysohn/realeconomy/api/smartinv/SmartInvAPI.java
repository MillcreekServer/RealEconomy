package io.github.wysohn.realeconomy.api.smartinv;

import fr.minuskube.inv.SmartInventory;
import io.github.wysohn.rapidframework3.bukkit.data.BukkitPlayer;
import io.github.wysohn.rapidframework3.core.api.ExternalAPI;
import io.github.wysohn.rapidframework3.core.language.ManagerLanguage;
import io.github.wysohn.rapidframework3.core.main.PluginMain;
import io.github.wysohn.realeconomy.api.smartinv.gui.TradeAccountGUI;
import io.github.wysohn.realeconomy.inject.annotation.NamespaceKeyAssetSerialized;
import io.github.wysohn.realeconomy.manager.user.UserManager;
import io.github.wysohn.realeconomy.mediator.BankingMediator;
import io.github.wysohn.realeconomy.mediator.TradeMediator;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Entity;

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

    public void openTradeAccountGUI(BukkitPlayer user) {
        SmartInventory.builder()
                .id("Assets")
                .provider(new TradeAccountGUI(lang, bankingMediator, tradeMediator, serializedKey,
                        player -> Optional.of(player)
                                .map(Entity::getUniqueId)
                                .flatMap(userManager::get)
                                .map(Reference::get)
                                .orElse(null)))
                .size(6, 9)
                .closeable(true)
                .build()
                .open(user.getSender());
    }
}
