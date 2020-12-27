package io.github.wysohn.realeconomy.interfaces;

import io.github.wysohn.rapidframework3.interfaces.entity.IEntitySnapshot;
import io.github.wysohn.realeconomy.manager.asset.Asset;
import io.github.wysohn.realeconomy.manager.currency.Currency;

import java.math.BigDecimal;

/**
 * Represent any entity that can hold assets in 'real' form.
 * Using {@link #realizeAsset(Asset)}, the asset, which is virtual, will be
 * converted to the real things. For example, a player can be a IFinancialEntity,
 * and when it realizes {@link io.github.wysohn.realeconomy.manager.asset.Item} asset,
 * the player will physically own the item in its inventory.
 * <p>
 * If {@link io.github.wysohn.realeconomy.interfaces.banking.IAssetHolder} is the 'virtual' asset storage,
 * this class is the 'real' asset storage.
 */
public interface IFinancialEntity extends IEntitySnapshot {
    BigDecimal balance(Currency currency);

    boolean deposit(BigDecimal value, Currency currency);

    default boolean deposit(double value, Currency currency) {
        return deposit(BigDecimal.valueOf(value), currency);
    }

    boolean withdraw(BigDecimal value, Currency currency);

    default boolean withdraw(double value, Currency currency) {
        return withdraw(BigDecimal.valueOf(value), currency);
    }

    /**
     * The financial entity 'realizes' the asset. Realize means that unlike the asset
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
