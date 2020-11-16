package io.github.wysohn.realeconomy.interfaces.banking;

import io.github.wysohn.realeconomy.interfaces.IFinancialEntity;
import io.github.wysohn.realeconomy.manager.asset.Asset;

public interface IBankUser extends IFinancialEntity, IOrderIssuer {
    /**
     * The bank user 'realizes' the asset. Realize means that unlike the asset
     * is some sort of virtual idea, realizing is converting this virtual idea
     * into the real entity. For example, if the given asset is Item, realizing
     * Item will yield actual ItemStack to this bank user so that the ItemStack
     * can be used in the real world. On the other hand, if currency asset is
     * realized, it will increase the amount of currency they have in their
     * wallet. The behavior of this method is entirely depending on the type of Asset.
     *
     * @param asset
     * @return amount left. 0 if everything fit. Item for example, if ItemStack didn't fit
     * in player's inventory, number of leftovers will be returned.
     */
    int realizeAsset(Asset asset);
}
