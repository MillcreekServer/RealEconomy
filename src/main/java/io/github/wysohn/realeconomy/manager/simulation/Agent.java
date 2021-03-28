package io.github.wysohn.realeconomy.manager.simulation;

import io.github.wysohn.rapidframework3.interfaces.IMemento;
import io.github.wysohn.rapidframework3.utils.Pair;
import io.github.wysohn.realeconomy.interfaces.banking.IBankUser;
import io.github.wysohn.realeconomy.manager.asset.Asset;
import io.github.wysohn.realeconomy.manager.asset.Item;
import io.github.wysohn.realeconomy.manager.asset.signature.AssetSignature;
import io.github.wysohn.realeconomy.manager.currency.Currency;
import io.github.wysohn.realeconomy.manager.listing.OrderType;
import io.github.wysohn.realeconomy.manager.listing.TradeInfo;
import io.github.wysohn.realeconomy.mediator.TradeMediator;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.logging.Logger;

public class Agent implements IBankUser {
    private final Logger logger;
    private final UUID uuid;
    private final String name;
    private final Map<AssetSignature, Double> resourcesNeeded = new HashMap<>();
    private final Map<AssetSignature, Double> production = new HashMap<>();

    private final Set<Integer> buyOrderIdSet = new HashSet<>();
    private final Set<Integer> sellOrderIdSet = new HashSet<>();
    private final Map<AssetSignature, Double> assets = new HashMap<>();
    private final Map<AssetSignature, BigDecimal> currentPricing = new HashMap<>();
    private final Map<UUID, Integer> tradeDemands = new HashMap<>(); // assetListingUuid -> # of trades

    public Agent(Logger logger,
                 UUID uuid,
                 String name,
                 List<Pair<AssetSignature, Double>> resourcesNeeded,
                 List<Pair<AssetSignature, Double>> production) {
        this.logger = logger;
        this.uuid = uuid;
        this.name = name;
        resourcesNeeded.forEach(pair -> this.resourcesNeeded.put(pair.key, pair.value));
        production.forEach(pair -> this.production.put(pair.key, pair.value));
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

    private String simplifyTradeInfo(TradeInfo info){
        return String.format("{ask=%d@%f [%s], bid=%d@%f [%s]}",
                info.getStock(), info.getAsk(), info.getSeller().toString(),
                info.getAmount(), info.getBid(), info.getBuyer().toString());
    }

    @Override
    public void handleTransactionResult(TradeInfo info, OrderType type, TradeMediator.TradeResult result) {
        logger.fine("Agent: " + toString());
        logger.fine("Info: " + simplifyTradeInfo(info));
        logger.fine("Type: " + type);
        logger.fine("Result: " + result);

        if (type == OrderType.BUY) {
            // if we bought successfully, could we buy it at cheaper price?
            tradeDemands.put(info.getListingUuid(), tradeDemands.getOrDefault(info.getListingUuid(), 0) + info.getAmount());
        } else if (type == OrderType.SELL) {
            // if we sold successfully, could we sell it at higher price?
            tradeDemands.put(info.getListingUuid(), tradeDemands.getOrDefault(info.getListingUuid(), 0) - info.getAmount());
        } else {
            throw new RuntimeException();
        }
    }

    @Override
    public UUID getUuid() {
        return uuid;
    }

    @Override
    public BigDecimal balance(Currency currency) {
        throw new RuntimeException();
    }

    @Override
    public boolean deposit(BigDecimal value, Currency currency) {
        throw new RuntimeException();
    }

    @Override
    public boolean withdraw(BigDecimal value, Currency currency) {
        throw new RuntimeException();
    }

    @Override
    public int realizeAsset(Asset asset) {
        if (asset instanceof Item) {
            double current = assets.getOrDefault(asset.getSignature(), 0.0);
            assets.put(asset.getSignature(), current + asset.getNumericalMeasure());
            return 0;
        } else {
            throw new RuntimeException("Not yet implemented: " + asset);
        }
    }

    /**
     * Get collection of resources needed to produce outcome
     * @return the resources; empty if already has enough resources
     */
    public Collection<Pair<AssetSignature, Double>> neededResources(){
        Collection<Pair<AssetSignature, Double>> col = new LinkedList<>();
        resourcesNeeded.forEach((sign, value) -> {
            double current = assets.getOrDefault(sign, 0.0);
            double need = resourcesNeeded.getOrDefault(sign, 0.0);

            if(need > current)
                col.add(Pair.of(sign, need - current));
        });
        return col;
    }

    public boolean canProduce(){
        for (Map.Entry<AssetSignature, Double> entry : resourcesNeeded.entrySet()) {
            AssetSignature sign = entry.getKey();
            double current = assets.getOrDefault(sign, 0.0);
            double need = resourcesNeeded.getOrDefault(sign, 0.0);

            if(current < need)
                return false;
        }

        return true;
    }

    public Collection<Pair<AssetSignature, Double>> produce(){
        Collection<Pair<AssetSignature, Double>> produced = new LinkedList<>();
        if(!canProduce())
            return produced;

        // consume
        for (Map.Entry<AssetSignature, Double> entry : resourcesNeeded.entrySet()) {
            AssetSignature sign = entry.getKey();
            double current = assets.getOrDefault(sign, 0.0);
            double need = resourcesNeeded.getOrDefault(sign, 0.0);
            assets.put(sign, current - need);
        }

        // produce
        for (Map.Entry<AssetSignature, Double> entry : production.entrySet()) {
            AssetSignature sign = entry.getKey();
            double amount = entry.getValue();
            produced.add(Pair.of(sign, amount));
        }

        return produced;
    }

    /**
     * Get unit cost of producing one unit of outcome. This is simple calculation, so
     * even if there are more than one type of outcome, they will still be weighted the same.
     * For example, if outcome is 5 asset A + 2 asset B, the number of production is still 7, thus
     * the unit cost will be the (total cost / 7)
     * @return
     */
    public BigDecimal getFixedUnitCost() {
        BigDecimal cost = BigDecimal.ZERO;
        int totalProduced = 0;

        for (Map.Entry<AssetSignature, Double> entry : production.entrySet()) {
            double amountProduced = entry.getValue();
            totalProduced += amountProduced;
        }

        for(Map.Entry<AssetSignature, Double> entry : resourcesNeeded.entrySet()) {
            BigDecimal currentPrice = currentPricing.getOrDefault(entry.getKey(), BigDecimal.ZERO);
            double amountNeeded = entry.getValue();

            cost = cost.add(currentPrice.multiply(BigDecimal.valueOf(amountNeeded)));
        }

        return totalProduced < 1 ?
                BigDecimal.ONE : cost.divide(BigDecimal.valueOf(totalProduced), RoundingMode.HALF_UP);
    }

    /**
     * Get number of trades successfully made.
     * <p>
     * This can be thought of the 'stock' piling up in the
     * warehouse. So, if the agent is buying something and stock is piling up, the demand for the asset
     * will decrease, thus decreasing the price, and vice versa. On the other hand, if the agent is selling
     * something and stock is piling up, this means the seller should decrease the price to meet the
     * price point where buyers will be willing to buy it.
     *
     * @param listingUuid target asset signature UUID
     * @return current number of stock; 0 if not found
     */
    public int getTradeDemand(UUID listingUuid) {
        return tradeDemands.getOrDefault(listingUuid, 0);
    }

    /**
     * Set number of trades successfully made.
     * <p>
     * This can be thought of the 'stock' piling up in the
     * warehouse. So, if the agent is buying something and stock is piling up, the demand for the asset
     * will decrease, thus decreasing the price, and vice versa. On the other hand, if the agent is selling
     * something and stock is piling up, this means the seller should decrease the price to meet the
     * price point where buyers will be willing to buy it.
     *
     * @param listingUuid target asset signature UUID
     * @param num         new stock value to set
     */
    public void setTradeDemand(UUID listingUuid, int num) {
        tradeDemands.put(listingUuid, num);
    }

    /**
     * Get collection of read-only AssetSignatures that are produced.
     *
     * @return
     */
    public Collection<AssetSignature> getProductionTypes() {
        return Collections.unmodifiableCollection(production.keySet());
    }

    /**
     * Get current price target of the given signature. This is specific to the agent,
     * not shared by other agents.
     * @param sign signature
     * @return the current price target; null if not set
     */
    public BigDecimal getCurrentPricing(AssetSignature sign){
        return currentPricing.get(sign);
    }

    public void updateCurrentPricing(AssetSignature sign, BigDecimal price){
        currentPricing.put(sign, price);
    }

    @Override
    public String toString() {
        return "Agent{" +
                "uuid=" + uuid +
                ", name='" + name + '\'' +
                ", resourcesNeeded=" + resourcesNeeded +
                ", production=" + production +
                ", buyOrderIdSet=" + buyOrderIdSet +
                ", sellOrderIdSet=" + sellOrderIdSet +
                ", assets=" + assets +
                ", currentPricing=" + currentPricing +
                '}';
    }

    @Override
    public IMemento saveState() {
        return new SavedState(this);
    }

    @Override
    public void restoreState(IMemento iMemento) {
        SavedState savedState = (SavedState) iMemento;

        this.buyOrderIdSet.clear();
        this.buyOrderIdSet.addAll(savedState.buyOrderIdSet);

        this.sellOrderIdSet.clear();
        this.sellOrderIdSet.addAll(savedState.sellOrderIdSet);

        this.assets.clear();
        this.assets.putAll(savedState.assets);

        this.currentPricing.clear();
        this.currentPricing.putAll(savedState.currentPricing);
    }

    private static class SavedState implements IMemento{
        private final Set<Integer> buyOrderIdSet = new HashSet<>();
        private final Set<Integer> sellOrderIdSet = new HashSet<>();
        private final Map<AssetSignature, Double> assets = new HashMap<>();
        private final Map<AssetSignature, BigDecimal> currentPricing = new HashMap<>();

        private SavedState(Agent agent){
            this.buyOrderIdSet.addAll(agent.buyOrderIdSet);
            this.sellOrderIdSet.addAll(agent.sellOrderIdSet);
            this.assets.putAll(agent.assets);
            this.currentPricing.putAll(agent.currentPricing);
        }
    }
}
