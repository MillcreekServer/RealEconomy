package io.github.wysohn.realeconomy.mediator;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.github.wysohn.rapidframework3.core.inject.annotations.PluginLogger;
import io.github.wysohn.rapidframework3.core.main.Mediator;
import io.github.wysohn.realeconomy.interfaces.simulation.IAgentReloadObserver;
import io.github.wysohn.realeconomy.manager.asset.signature.AssetSignature;
import io.github.wysohn.realeconomy.manager.banking.BankingTypeRegistry;
import io.github.wysohn.realeconomy.manager.banking.TransactionUtil;
import io.github.wysohn.realeconomy.manager.banking.bank.CentralBank;
import io.github.wysohn.realeconomy.manager.currency.Currency;
import io.github.wysohn.realeconomy.manager.listing.AssetListingManager;
import io.github.wysohn.realeconomy.manager.listing.OrderType;
import io.github.wysohn.realeconomy.manager.listing.PricePoint;
import io.github.wysohn.realeconomy.manager.simulation.Agent;
import io.github.wysohn.realeconomy.manager.simulation.MarketSimulationManager;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Logger;

@Singleton
public class SimulationMediator extends Mediator {
    private static final BigDecimal DEFAULT_PRICING_START = BigDecimal.ONE;
    private static final double PURCHASE_THRESHOLD = 100000.0;
    private static final double PRICE_ADJUSTMENT_FACTOR = -4.0 / PURCHASE_THRESHOLD;

    private final Logger logger;
    private final MarketSimulationManager marketSimulationManager;
    private final AssetListingManager assetListingManager;

    private final TradeMediator tradeMediator;
    private final BankingMediator bankingMediator;

    MarketSimulator marketSimulator;
    ReloadObserver reloadObserver = new ReloadObserver();

    @Inject
    public SimulationMediator(@PluginLogger Logger logger,
                              MarketSimulationManager marketSimulationManager,
                              AssetListingManager assetListingManager,
                              TradeMediator tradeMediator,
                              BankingMediator bankingMediator) {
        this.logger = logger;
        this.marketSimulationManager = marketSimulationManager;
        this.assetListingManager = assetListingManager;
        this.tradeMediator = tradeMediator;
        this.bankingMediator = bankingMediator;
    }

    @Override
    public void enable() throws Exception {

    }

    @Override
    public void load() throws Exception {
        // make sure that all agents have their accounts open for trade
        marketSimulationManager.getAgents().forEach(agent ->
                bankingMediator.openAccount(agent, BankingTypeRegistry.TRADING));

        if (marketSimulator != null)
            marketSimulator.interrupt();
        marketSimulator = new MarketSimulator(assetListingManager,
                logger, marketSimulationManager,
                tradeMediator,
                BankingMediator.getServerBank(),
                bankingMediator);
        marketSimulator.start();
    }

    @Override
    public void disable() throws Exception {
        shutdown();

        // cancel all agent orders
        cancelAgentOrders(marketSimulationManager.getAgents());
    }

    private void cancelAgentOrders(Collection<Agent> agents) {
        agents.forEach(agent -> {
            agent.getOrderIds(OrderType.BUY).forEach(id -> tradeMediator.cancelOrder(agent, id, OrderType.BUY));
            agent.getOrderIds(OrderType.SELL).forEach(id -> tradeMediator.cancelOrder(agent, id, OrderType.SELL));
        });
    }

    /**
     * @deprecated test only. Shutdown simulator thread and wait for join
     */
    public void shutdown() throws InterruptedException {
        marketSimulator.interrupt();
        marketSimulator.join(30 * 1000L);
    }

    public ReloadObserver getReloadObserver() {
        return reloadObserver;
    }

    class ReloadObserver implements IAgentReloadObserver {
        @Override
        public void beforeAgentReload(Collection<Agent> agents) {
            cancelAgentOrders(agents);
        }
    }

    static class MarketSimulator extends Thread {
        private final CentralBank centralBank;
        private final AssetListingManager assetListingManager;
        private final Logger logger;
        private final MarketSimulationManager marketSimulationManager;
        private final TradeMediator tradeMediator;
        private final BankingMediator bankingMediator;

        public MarketSimulator(AssetListingManager assetListingManager,
                               Logger logger,
                               MarketSimulationManager marketSimulationManager,
                               TradeMediator tradeMediator,
                               CentralBank centralBank,
                               BankingMediator bankingMediator) {
            this.centralBank = centralBank;
            this.assetListingManager = assetListingManager;
            this.logger = logger;
            this.marketSimulationManager = marketSimulationManager;
            this.tradeMediator = tradeMediator;
            this.bankingMediator = bankingMediator;

            setPriority(NORM_PRIORITY - 1);
            setName("RealEconomy - MarketSimulator");
        }

        @Override
        public void run() {
            while (centralBank != null && !interrupted()) {
                iterate();

                try {
                    Thread.sleep(1000L); // TODO for test
                    //Thread.sleep(60 * 60 * 1000L); // hourly
                } catch (InterruptedException e) {
                    logger.info(getName() + " is interrupted.");
                    break;
                }
            }
        }

        public void iterate(){
            agentBid();
            agentWithdraw();
            agentProduce();
            agentAsk();
        }

        /**
         * Crate bids to purchase assets required to produce outcome
         */
        private void agentBid() {
            Currency currency = centralBank.getBaseCurrency();

            for (Agent agent : marketSimulationManager.getAgents()) {
                // cancel previous bids first to not make duplicated orders
                agent.getOrderIds(OrderType.BUY).forEach(orderId -> {
                    // this is a failed buy order, so subtract the amount from the number of trades
                    // the number being negative (agent not able to buy at the current price)
                    //   will lead to decline in price and vice versa
                    tradeMediator.getInfo(orderId, OrderType.BUY, orderInfo -> {
                        UUID listingUuid = orderInfo.getListingUuid();
                        agent.setTradeDemand(listingUuid,
                                agent.getTradeDemand(listingUuid) - orderInfo.getAmount());
                    });
                    tradeMediator.cancelOrder(agent, orderId, OrderType.BUY);
                });

                // also, return the currency to the bank.
                BigDecimal currentBalance = centralBank.balanceOfAccount(agent,
                        BankingTypeRegistry.TRADING);
                TransactionUtil.Result returnResult = bankingMediator.send(agent,
                        BankingTypeRegistry.TRADING,
                        centralBank,
                        currentBalance,
                        centralBank.getBaseCurrency());
                if (returnResult != TransactionUtil.Result.OK) {
                    logger.fine("agent "+agent+" is unable to return currency to the bank.");
                    logger.fine("amount: "+currentBalance);
                    logger.fine("reason: "+returnResult);
                    return;
                }

                agent.neededResources().forEach(pair -> {
                    AssetSignature sign = pair.key;
                    int amount = (int) Math.ceil(pair.value);

                    // how much we have right now?
                    // don't bid on resources if we have enough
                    int currentStock = (int) Math.ceil(centralBank.countAccountAsset(agent, sign));
                    if (currentStock > amount)
                        return;

                    // if threshold passes the lower bound, we probably need to stop it
                    // this will prevent the case where the price will go sky-high indefinitely
                    UUID listingUuid = assetListingManager.signatureToUuid(sign);
                    int currentDemand = agent.getTradeDemand(listingUuid);
                    agent.setTradeDemand(listingUuid, (int) Math.max(-PURCHASE_THRESHOLD, currentDemand));

                    // get pricing of this agent is using
                    BigDecimal currentPricing = agent.getCurrentPricing(sign);
                    if (currentPricing == null)
                        currentPricing = DEFAULT_PRICING_START;

                    // get current lowest market price
                    BigDecimal lowestPricing = Optional.of(assetListingManager)
                            .map(manager -> manager.getLowestPrice(sign, currency))
                            .map(PricePoint::getPrice)
                            .orElse(BigDecimal.ONE);

                    // get (agent price + lowest price) / 2
                    BigDecimal midPoint = currentPricing.add(lowestPricing)
                            .divide(BigDecimal.valueOf(2.0), RoundingMode.HALF_UP);

                    // change the price according to the number of trades
                    midPoint = midPoint.multiply(BigDecimal.valueOf(1.0
                            + Math.tanh(PRICE_ADJUSTMENT_FACTOR * agent.getTradeDemand(assetListingManager.signatureToUuid(sign)))));

                    agent.updateCurrentPricing(sign, midPoint);
                    logger.fine("agent " + agent + " updating price (bid).");
                    logger.fine(sign + " : " + midPoint);

                    // before making bids, make sure we have enough balance in the bank
                    BigDecimal totalPrice = midPoint.multiply(BigDecimal.valueOf(amount));
                    TransactionUtil.Result sendResult = bankingMediator.send(centralBank,
                            agent,
                            BankingTypeRegistry.TRADING,
                            totalPrice,
                            centralBank.getBaseCurrency());
                    if (sendResult != TransactionUtil.Result.OK) {
                        logger.fine("agent " + agent + " is unable to borrow currency from the bank.");
                        logger.fine("amount: " + totalPrice);
                        logger.fine("reason: "+sendResult);
                        return;
                    }

                    // make a new bid
                    try {
                        assetListingManager.addOrder(sign,
                                OrderType.BUY,
                                agent,
                                midPoint.doubleValue(),
                                currency,
                                (int) Math.ceil(amount),
                                true);
                        assetListingManager.commitOrders();
                    } catch (SQLException ex) {
                        ex.printStackTrace();
                    }
                });
            }
        }

        /**
         * Take needed resources from bank account
         */
        private void agentWithdraw() {
            Currency currency = centralBank.getBaseCurrency();

            for (Agent agent : marketSimulationManager.getAgents()) {
                agent.neededResources().forEach(pair -> {
                    AssetSignature sign = pair.key;
                    double value = pair.value;

                    currency.ownerBank().removeAccountAsset(agent,
                            sign,
                            (int) Math.ceil(value)).forEach(agent::realizeAsset);
                });
            }
        }

        /**
         * Assuming the trade was successful, we can now produce outcomes.
         * Produce them and add it to the bank account of the agent.
         */
        private void agentProduce() {
            Currency currency = centralBank.getBaseCurrency();

            for (Agent agent : marketSimulationManager.getAgents()) {
                agent.produce().forEach(pair -> {
                    AssetSignature sign = pair.key;
                    double amount = pair.value;

                    centralBank.addAccountAsset(agent, sign.asset(amount));
                });
            }
        }

        /**
         * Since agent now have the outcomes, try selling it to the market
         */
        private void agentAsk() {
            Currency currency = centralBank.getBaseCurrency();

            for (Agent agent : marketSimulationManager.getAgents()) {
                // cancel previous asks first to not make duplicated orders
                agent.getOrderIds(OrderType.SELL).forEach(orderId -> {
                    // this is failed sell order so add amount to the number of trades
                    // the number being positive (agent is unable to sell at the current price)
                    //   will lead to the decline in price and vice versa
                    tradeMediator.getInfo(orderId, OrderType.SELL, orderInfo -> {
                        UUID listingUuid = orderInfo.getListingUuid();
                        agent.setTradeDemand(listingUuid,
                                agent.getTradeDemand(listingUuid) + orderInfo.getAmount());
                    });
                    tradeMediator.cancelOrder(agent, orderId, OrderType.SELL);
                });

                BigDecimal unitCost = agent.getFixedUnitCost();
                agent.getProductionTypes().forEach(sign -> {
                    int currentStock = (int) Math.ceil(centralBank.countAccountAsset(agent, sign));

                    // we don't have enough stock
                    if (currentStock < 1)
                        return;

                    // get current highest market price
                    BigDecimal highestPricing = Optional.of(assetListingManager)
                            .map(manager -> manager.getHighestPrice(sign, currency))
                            .map(PricePoint::getPrice)
                            .orElse(BigDecimal.TEN);

                    // get (agent price + lowest price) / 2
                    BigDecimal sellingPrice = unitCost.add(highestPricing)
                            .divide(BigDecimal.valueOf(2.0), RoundingMode.HALF_UP);

                    // change the price according to the number of trades
                    sellingPrice = sellingPrice.multiply(BigDecimal.valueOf(1.0
                            + Math.tanh(PRICE_ADJUSTMENT_FACTOR * agent.getTradeDemand(assetListingManager.signatureToUuid(sign)))));

                    // we cannot sell items at the price cheaper than the unit cost!
                    // that would be a dumb thing to do
                    if (sellingPrice.compareTo(unitCost) < 0) {
                        sellingPrice = unitCost;
                    }

                    agent.updateCurrentPricing(sign, sellingPrice);
                    logger.fine("agent " + agent + " updating price (ask).");
                    logger.fine(sign + " : " + sellingPrice);

                    try {
                        assetListingManager.addOrder(sign,
                                OrderType.SELL,
                                agent,
                                sellingPrice.doubleValue(),
                                currency,
                                currentStock,
                                true);
                        assetListingManager.commitOrders();
                    } catch (SQLException ex) {
                        ex.printStackTrace();
                    }
                });
            }
        }
    }
}
