package io.github.wysohn.realeconomy.inject.module;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import io.github.wysohn.realeconomy.inject.annotation.NamespaceKeyAssetSerialized;
import io.github.wysohn.realeconomy.inject.annotation.NamespaceKeyCheckBalance;
import io.github.wysohn.realeconomy.inject.annotation.NamespaceKeyCheckCurrency;
import io.github.wysohn.realeconomy.main.RealEconomy;
import org.bukkit.NamespacedKey;

public class NamespacedKeyModule extends AbstractModule {
    public static final String KEY_CHECK_CURRENCY = "RealEconomy_CheckCurrency";
    public static final String KEY_CHECK_BALANCE = "RealEconomy_CheckBalance";
    public static final String KEY_ASSET_SERIALIZE = "RealEconomy_AssetSerialize";

    @Provides
    @NamespaceKeyCheckCurrency
    NamespacedKey namespaceKey(RealEconomy realEconomy) {
        return new NamespacedKey(realEconomy, KEY_CHECK_CURRENCY);
    }

    @Provides
    @NamespaceKeyCheckBalance
    NamespacedKey namespacedKeyBal(RealEconomy realEconomy) {
        return new NamespacedKey(realEconomy, KEY_CHECK_BALANCE);
    }

    @Provides
    @NamespaceKeyAssetSerialized
    NamespacedKey namedspacedKeyAssetSer(RealEconomy realEconomy){
        return new NamespacedKey(realEconomy, KEY_ASSET_SERIALIZE);
    }
}
