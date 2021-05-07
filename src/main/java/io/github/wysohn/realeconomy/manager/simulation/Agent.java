package io.github.wysohn.realeconomy.manager.simulation;

import io.github.wysohn.rapidframework3.interfaces.IMemento;
import io.github.wysohn.rapidframework3.interfaces.store.IKeyValueStorage;
import io.github.wysohn.rapidframework3.utils.Pair;
import io.github.wysohn.realeconomy.interfaces.banking.IBankUser;
import io.github.wysohn.realeconomy.interfaces.listing.IListingInfoProvider;
import io.github.wysohn.realeconomy.manager.asset.Asset;
import io.github.wysohn.realeconomy.manager.asset.Item;
import io.github.wysohn.realeconomy.manager.asset.signature.AssetSignature;
import io.github.wysohn.realeconomy.manager.currency.Currency;
import io.github.wysohn.realeconomy.manager.listing.OrderType;
import io.github.wysohn.realeconomy.manager.listing.TradeInfo;
import io.github.wysohn.realeconomy.mediator.SimulationMediator;
import io.github.wysohn.realeconomy.mediator.TradeMediator;
import org.bukkit.configuration.ConfigurationSection;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class Agent implements IBankUser {
    public static final String AGENT_UUID = "uuid";
    public static final String RESOURCES_NEEDED = "resourcesNeeded";
    public static final String PRODUCTION = "production";
    public static final BigDecimal MINIMUM_UNIT_COST = BigDecimal.valueOf(0.000001);

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
                synchronized (buyOrderIdSet) {
                    bool = buyOrderIdSet.add(orderId);
                }
                break;
            case SELL:
                synchronized (sellOrderIdSet) {
                    bool = sellOrderIdSet.add(orderId);
                }
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
                synchronized (buyOrderIdSet) {
                    return buyOrderIdSet.contains(orderId);
                }
            case SELL:
                synchronized (sellOrderIdSet) {
                    return sellOrderIdSet.contains(orderId);
                }
            default:
                throw new RuntimeException("Unknown order type " + type);
        }
    }

    @Override
    public boolean removeOrderId(OrderType type, int orderId) {
        boolean bool = false;
        switch (type) {
            case BUY:
                synchronized (buyOrderIdSet) {
                    bool = buyOrderIdSet.remove(orderId);
                }
                break;
            case SELL:
                synchronized (sellOrderIdSet) {
                    bool = sellOrderIdSet.remove(orderId);
                }
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
                synchronized (buyOrderIdSet) {
                    return new HashSet<>(buyOrderIdSet);
                }
            case SELL:
                synchronized (sellOrderIdSet) {
                    return new HashSet<>(sellOrderIdSet);
                }
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

        synchronized (tradeDemands) {
            if (type == OrderType.BUY) {
                // if we bought successfully, could we buy it at cheaper price?
                tradeDemands.put(info.getListingUuid(), tradeDemands.getOrDefault(info.getListingUuid(), 0)
                        + SimulationMediator.DEMAND_SENSITIVITY_BID * info.getAmount());
            } else if (type == OrderType.SELL) {
                // if we sold successfully, could we sell it at higher price?
                tradeDemands.put(info.getListingUuid(), tradeDemands.getOrDefault(info.getListingUuid(), 0)
                        - SimulationMediator.DEMAND_SENSITIVITY_ASK * info.getAmount());
            } else {
                throw new RuntimeException();
            }
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
            synchronized (assets) {
                double current = assets.getOrDefault(asset.getSignature(), 0.0);
                assets.put(asset.getSignature(), current + asset.getNumericalMeasure());
            }
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
            synchronized (assets) {
                double current = assets.getOrDefault(sign, 0.0);
                double need = resourcesNeeded.getOrDefault(sign, 0.0);

                if (current < need)
                    return false;
            }
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
            synchronized (assets) {
                double current = assets.getOrDefault(sign, 0.0);
                double need = resourcesNeeded.getOrDefault(sign, 0.0);
                assets.put(sign, current - need);
            }
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
        BigDecimal cost = BigDecimal.valueOf(0.0);
        int totalProduced = 0;

        for (Map.Entry<AssetSignature, Double> entry : production.entrySet()) {
            double amountProduced = entry.getValue();
            totalProduced += amountProduced;
        }

        for(Map.Entry<AssetSignature, Double> entry : resourcesNeeded.entrySet()) {
            synchronized (currentPricing) {
                BigDecimal currentPrice = currentPricing.getOrDefault(entry.getKey(), BigDecimal.valueOf(0.0));
                double amountNeeded = entry.getValue();

                cost = cost.add(currentPrice.multiply(BigDecimal.valueOf(amountNeeded)));
            }
        }

        BigDecimal result = totalProduced < 1 ?
                BigDecimal.ONE : cost.divide(BigDecimal.valueOf(totalProduced), RoundingMode.HALF_UP);
        if (result.compareTo(MINIMUM_UNIT_COST) < 0)
            result = MINIMUM_UNIT_COST;

        return result;
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
        synchronized (tradeDemands) {
            return tradeDemands.getOrDefault(listingUuid, 0);
        }
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
        synchronized (tradeDemands) {
            tradeDemands.put(listingUuid, num);
        }
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
    public BigDecimal getCurrentPricing(AssetSignature sign) {
        synchronized (currentPricing) {
            return currentPricing.get(sign);
        }
    }

    public void updateCurrentPricing(AssetSignature sign, BigDecimal price) {
        synchronized (currentPricing) {
            currentPricing.put(sign, price);
        }
    }

    @Override
    public String toString() {
        return "Agent{" +
                "uuid=" + uuid +
                ", name='" + name + '\'' +
                ", resourcesNeeded=" + resourcesNeeded +
                ", production=" + production +
                '}';
    }

    @Override
    public IMemento saveState() {
        return new SavedState(this);
    }

    @Override
    public void restoreState(IMemento iMemento) {
        SavedState savedState = (SavedState) iMemento;

        synchronized (buyOrderIdSet) {
            this.buyOrderIdSet.clear();
            this.buyOrderIdSet.addAll(savedState.buyOrderIdSet);
        }

        synchronized (sellOrderIdSet) {
            this.sellOrderIdSet.clear();
            this.sellOrderIdSet.addAll(savedState.sellOrderIdSet);
        }

        synchronized (assets) {
            this.assets.clear();
            this.assets.putAll(savedState.assets);
        }

        synchronized (currentPricing) {
            this.currentPricing.clear();
            this.currentPricing.putAll(savedState.currentPricing);
        }

        synchronized (tradeDemands) {
            this.tradeDemands.clear();
            this.tradeDemands.putAll(savedState.tradeDemands);
        }
    }

    public void write(ConfigurationSection section, IListingInfoProvider assetInfoProvider) {
        section.set(name + "." + AGENT_UUID, uuid.toString());

        resourcesNeeded.forEach((sign, amount) -> {
            assetInfoProvider.newListing(sign);
            UUID uuid = assetInfoProvider.signatureToUuid(sign);
            section.set(name + "." + RESOURCES_NEEDED + "." + uuid, amount);
        });

        production.forEach((sign, amount) -> {
            assetInfoProvider.newListing(sign);
            UUID uuid = assetInfoProvider.signatureToUuid(sign);
            section.set(name + "." + PRODUCTION + "." + uuid, amount);
        });
    }

    public static Agent read(IKeyValueStorage config,
                             Logger logger,
                             IListingInfoProvider assetInfoProvider,
                             String agentName,
                             Object agentSection) {
        List<Pair<AssetSignature, Double>> resourcedNeeded = new LinkedList<>();
        List<Pair<AssetSignature, Double>> production = new LinkedList<>();

        UUID uuid = config.get(agentSection, AGENT_UUID)
                .map(String.class::cast)
                .map(UUID::fromString)
                .orElseThrow(RuntimeException::new);

        config.get(agentSection, RESOURCES_NEEDED).ifPresent(resourcesSection -> {
            config.getKeys(resourcesSection, false).forEach(uuidKey -> {
                UUID listingUuid = UUID.fromString(uuidKey);
                double amount = config.get(resourcesSection, uuidKey)
                        .map(Number.class::cast)
                        .map(Number::doubleValue)
                        .orElse(0.0);

                if (amount <= 0.0)
                    return;

                Optional.of(listingUuid)
                        .map(assetInfoProvider::uuidToSignature)
                        .ifPresent(signature -> resourcedNeeded.add(Pair.of(signature, amount)));
            });
        });

        config.get(agentSection, PRODUCTION).ifPresent(productionSection -> {
            config.getKeys(productionSection, false).forEach(uuidKey -> {
                UUID listingUuid = UUID.fromString(uuidKey);
                double amount = config.get(productionSection, uuidKey)
                        .map(Number.class::cast)
                        .map(Number::doubleValue)
                        .orElse(0.0);

                if (amount <= 0.0)
                    return;

                Optional.of(listingUuid)
                        .map(assetInfoProvider::uuidToSignature)
                        .ifPresent(signature -> production.add(Pair.of(signature, amount)));
            });
        });

        return new Agent(logger,
                uuid,
                agentName,
                resourcedNeeded,
                production);
    }

    public static Collection<Agent> readAll(IKeyValueStorage config,
                                            Logger logger,
                                            IListingInfoProvider assetInfoProvider,
                                            Object section) {
        return Optional.of(section)
                .map(obj -> config.getKeys(obj, false))
                .map(agentNames -> agentNames.stream().map(agentName ->
                        config.get(section, agentName).map(agentSection -> read(config,
                                logger,
                                assetInfoProvider,
                                agentName,
                                agentSection)))
                        .filter(Optional::isPresent)
                        .map(Optional::get))
                .map(stream -> stream.collect(Collectors.toList()))
                .orElseGet(LinkedList::new);
    }

    private static class SavedState implements IMemento {
        private final Set<Integer> buyOrderIdSet = new HashSet<>();
        private final Set<Integer> sellOrderIdSet = new HashSet<>();
        private final Map<AssetSignature, Double> assets = new HashMap<>();
        private final Map<AssetSignature, BigDecimal> currentPricing = new HashMap<>();
        private final Map<UUID, Integer> tradeDemands = new HashMap<>();

        private SavedState(Agent agent) {
            synchronized (agent.buyOrderIdSet) {
                this.buyOrderIdSet.addAll(agent.buyOrderIdSet);
            }
            synchronized (agent.sellOrderIdSet) {
                this.sellOrderIdSet.addAll(agent.sellOrderIdSet);
            }
            synchronized (agent.assets) {
                this.assets.putAll(agent.assets);
            }
            synchronized (agent.currentPricing) {
                this.currentPricing.putAll(agent.currentPricing);
            }
            synchronized (agent.tradeDemands) {
                this.tradeDemands.putAll(agent.tradeDemands);
            }
        }
    }
}
