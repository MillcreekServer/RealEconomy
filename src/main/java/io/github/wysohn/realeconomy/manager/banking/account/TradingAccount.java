package io.github.wysohn.realeconomy.manager.banking.account;

import io.github.wysohn.rapidframework3.interfaces.paging.DataProvider;
import io.github.wysohn.realeconomy.interfaces.banking.IAccount;
import io.github.wysohn.realeconomy.interfaces.banking.IAssetHolder;
import io.github.wysohn.realeconomy.interfaces.banking.IBankingType;
import io.github.wysohn.realeconomy.manager.asset.Asset;
import io.github.wysohn.realeconomy.manager.asset.signature.AssetSignature;
import io.github.wysohn.realeconomy.manager.banking.AssetUtil;
import io.github.wysohn.realeconomy.manager.banking.BankingTypeRegistry;

import java.math.BigDecimal;
import java.util.*;

public class TradingAccount implements IAccount, IAssetHolder {
    public final Map<UUID, BigDecimal> balances = new HashMap<>();
    private final List<Asset> ownedAssets = new ArrayList<>();

    @Override
    public void addAsset(Asset asset) {
        AssetUtil.addAsset(ownedAssets, asset);
    }

    @Override
    public int removeAsset(AssetSignature signature, int amount) {
        return AssetUtil.removeAsset(ownedAssets, signature, amount);
    }

    @Override
    public DataProvider<Asset> assetDataProvider() {
        return AssetUtil.assetDataProvider(ownedAssets);
    }

    @Override
    public Map<UUID, BigDecimal> getCurrencyMap() {
        return balances;
    }

    @Override
    public IBankingType getType() {
        return BankingTypeRegistry.TRADING;
    }

    @Override
    public IAccount clone() {
        CheckingAccount checkingAccount = new CheckingAccount();
        // both UUID and BigDecimal are immutable
        checkingAccount.balances.putAll(balances);
        return checkingAccount;
    }
}
