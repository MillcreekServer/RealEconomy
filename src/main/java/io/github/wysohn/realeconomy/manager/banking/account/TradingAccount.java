package io.github.wysohn.realeconomy.manager.banking.account;

import io.github.wysohn.rapidframework3.interfaces.IMemento;
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
    public double countAsset(AssetSignature signature) {
        return AssetUtil.countAsset(ownedAssets, signature);
    }

    @Override
    public Collection<Asset> removeAsset(AssetSignature signature, double amount) {
        return AssetUtil.removeAsset(ownedAssets, signature, amount);
    }

    @Override
    public Asset removeAsset(int index) {
        return AssetUtil.removeAsset(ownedAssets, index);
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
        TradingAccount checkingAccount = new TradingAccount();
        // both UUID and BigDecimal are immutable
        checkingAccount.balances.putAll(balances);
        ownedAssets.forEach(asset -> checkingAccount.ownedAssets.add(asset.clone()));
        return checkingAccount;
    }

    @Override
    public IMemento saveState() {
        return new Memento(this);
    }

    @Override
    public void restoreState(IMemento savedState) {
        Memento memento = (Memento) savedState;

        balances.clear();
        balances.putAll(memento.balances);

        ownedAssets.clear();
        memento.ownedAssets.stream()
                .map(Asset::clone)
                .forEach(ownedAssets::add);
    }

    private class Memento implements IMemento {
        private final Map<UUID, BigDecimal> balances = new HashMap<>();
        private final List<Asset> ownedAssets = new ArrayList<>();

        public Memento(TradingAccount tradingAccount) {
            balances.putAll(tradingAccount.balances);
            deepCopy(tradingAccount.ownedAssets, ownedAssets);
        }

        private void deepCopy(List<Asset> from, List<Asset> to) {
            for (Asset asset : from) {
                to.add(asset.clone());
            }
        }
    }
}
