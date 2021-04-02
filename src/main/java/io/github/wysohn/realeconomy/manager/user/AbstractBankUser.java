package io.github.wysohn.realeconomy.manager.user;

import io.github.wysohn.rapidframework3.bukkit.data.BukkitPlayer;
import io.github.wysohn.rapidframework3.core.paging.DataProviderProxy;
import io.github.wysohn.rapidframework3.interfaces.IMemento;
import io.github.wysohn.rapidframework3.interfaces.paging.DataProvider;
import io.github.wysohn.rapidframework3.utils.Pair;
import io.github.wysohn.realeconomy.inject.annotation.MaxCapital;
import io.github.wysohn.realeconomy.inject.annotation.MinCapital;
import io.github.wysohn.realeconomy.interfaces.banking.IBankUser;
import io.github.wysohn.realeconomy.manager.asset.Asset;
import io.github.wysohn.realeconomy.manager.banking.CapitalManagementUtil;
import io.github.wysohn.realeconomy.manager.currency.Currency;
import io.github.wysohn.realeconomy.manager.listing.OrderType;
import io.github.wysohn.realeconomy.manager.listing.TradeInfo;
import io.github.wysohn.realeconomy.mediator.TradeMediator;

import javax.inject.Inject;
import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public abstract class AbstractBankUser extends BukkitPlayer implements IBankUser {
    private final Map<UUID, BigDecimal> wallet = new ConcurrentHashMap<>();
    private final Set<Integer> buyOrderIdSet = Collections.synchronizedSet(new HashSet<>());
    private final Set<Integer> sellOrderIdSet = Collections.synchronizedSet(new HashSet<>());

    @Inject
    @MinCapital
    protected BigDecimal minimum;
    @Inject
    @MaxCapital
    protected BigDecimal maximum;

    private transient DataProvider<Pair<UUID, BigDecimal>> balanceProvider;

    public AbstractBankUser(UUID key) {
        super(key);
    }

    @Override
    public UUID getUuid() {
        return getKey();
    }

    @Override
    public BigDecimal balance(Currency currency) {
        return CapitalManagementUtil.balance(wallet, currency);
    }

    @Override
    public boolean deposit(BigDecimal value, Currency currency) {
        final boolean deposit = CapitalManagementUtil.deposit(maximum, wallet, value, currency);
        notifyObservers();
        return deposit;
    }

    @Override
    public boolean withdraw(BigDecimal value, Currency currency) {
        final boolean withdraw = CapitalManagementUtil.withdraw(minimum, wallet, value, currency);
        notifyObservers();
        return withdraw;
    }

    /**
     * Clear all wallet and return all of the content of the wallet.
     *
     * @return
     */
    public List<Pair<UUID, BigDecimal>> clearWallet() {
        List<Pair<UUID, BigDecimal>> copy = new ArrayList<>();
        wallet.forEach((uuid, bigDecimal) -> copy.add(Pair.of(uuid, bigDecimal)));
        wallet.clear();
        notifyObservers();
        return copy;
    }

    /**
     * DataProvider for the waller.
     * <p>
     * Note that DataProvider is asynchronous, so the contents is not necessarily
     * synchronized with the actual wallet.
     *
     * @return
     */
    public DataProvider<Pair<UUID, BigDecimal>> balancesPagination() {
        if (balanceProvider == null) {
            balanceProvider = new DataProviderProxy<>(range -> {
                List<Pair<UUID, BigDecimal>> copy = new ArrayList<>();
                wallet.forEach((uuid, bigDecimal) -> copy.add(Pair.of(uuid, bigDecimal)));
                copy.sort((a, b) -> b.value.compareTo(a.value));
                return copy.subList(range.index, Math.min(copy.size(), range.index + range.size));
            }, wallet::size);
        }
        return balanceProvider;
    }

    @Override
    public boolean addOrderId(OrderType type, int orderId) {
        boolean bool = false;
        switch (type) {
            case BUY:
                bool = buyOrderIdSet.add(orderId);
                break;
            case SELL:
                bool = sellOrderIdSet.add(orderId);
                break;
            default:
                throw new RuntimeException("Unknown order type " + type);
        }
        notifyObservers();
        return bool;
    }

    @Override
    public boolean hasOrderId(OrderType type, int orderId) {
        switch (type) {
            case BUY:
                return buyOrderIdSet.contains(orderId);
            case SELL:
                return sellOrderIdSet.contains(orderId);
            default:
                throw new RuntimeException("Unknown order type " + type);
        }
    }

    @Override
    public boolean removeOrderId(OrderType type, int orderId) {
        boolean bool = false;
        switch (type) {
            case BUY:
                bool = buyOrderIdSet.remove(orderId);
                break;
            case SELL:
                bool = sellOrderIdSet.remove(orderId);
                break;
            default:
                throw new RuntimeException("Unknown order type " + type);
        }
        notifyObservers();
        return bool;
    }

    @Override
    public Collection<Integer> getOrderIds(OrderType type) {
        switch (type) {
            case BUY:
                return new HashSet<>(buyOrderIdSet);
            case SELL:
                return new HashSet<>(sellOrderIdSet);
            default:
                throw new RuntimeException("Unknown order type " + type);
        }
    }

    @Override
    public abstract void handleTransactionResult(TradeInfo info,
                                                 OrderType type,
                                                 TradeMediator.TradeResult result);

    @Override
    public abstract int realizeAsset(Asset asset);

    /**
     * Warning) This does not save the parent state, which contains
     * the snapshot of the player's inventory content.
     * <p>
     * This is because this class can be used even if the player is offline,
     * yet the parent class's state require player to be online.
     * <p>
     * And since any trade will be held through TRADING account, bukkit
     * inventory is not necessary.
     *
     * @return
     */
    @Override
    public IMemento saveState() {
        return new Memento(this);
    }

    @Override
    public void restoreState(IMemento memento) {
        Memento mem = (Memento) memento;

        wallet.clear();
        wallet.putAll(mem.wallet);

        buyOrderIdSet.clear();
        buyOrderIdSet.addAll(mem.buyOrderIdSet);

        sellOrderIdSet.clear();
        sellOrderIdSet.addAll(mem.sellOrderIdSet);

        notifyObservers();
    }

    private static class Memento implements IMemento {
        private final Map<UUID, BigDecimal> wallet = new HashMap<>();
        private final Set<Integer> buyOrderIdSet = new HashSet<>();
        private final Set<Integer> sellOrderIdSet = new HashSet<>();

        public Memento(AbstractBankUser user) {
            //UUID and BigDecimal are both immutable
            wallet.putAll(user.wallet);

            buyOrderIdSet.addAll(user.buyOrderIdSet);

            sellOrderIdSet.addAll(user.sellOrderIdSet);
        }
    }
}
