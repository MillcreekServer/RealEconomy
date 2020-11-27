package io.github.wysohn.realeconomy.manager.asset.signature;

import io.github.wysohn.rapidframework3.core.language.ManagerLanguage;
import io.github.wysohn.rapidframework3.core.message.Message;
import io.github.wysohn.rapidframework3.interfaces.ICommandSender;
import io.github.wysohn.realeconomy.manager.asset.Asset;

import java.util.Map;

/**
 * A class that represent the detailed information about an Asset.
 * Children class must be immutable in order to provide concurrent access from multiple threads.
 */
public abstract class AssetSignature {
    /**
     * Check whether this asset is something that can be physically owned.
     * For example, when selling an item, item is the asset, and it is a physical entity.
     * Therefore, it has to be removed from the owner's container before added to the buyer's container.
     * However, some assets may not be physically owned. Bond, for example, the owner
     * does not necessarily have to physically 'own' the Bond in order to sell it.
     * However, upon sold, Bond will be virtually held by the buyer.
     *
     * @return
     */
    public abstract boolean isPhysical();

    public abstract String category();

    public abstract AssetSignature clone();

    public abstract Asset create(Map<String, Object> metaData);

    public abstract Message[] toMessage(ManagerLanguage lang, ICommandSender sender);
}
