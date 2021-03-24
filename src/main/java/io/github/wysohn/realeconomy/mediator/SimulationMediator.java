package io.github.wysohn.realeconomy.mediator;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.github.wysohn.rapidframework3.core.inject.annotations.PluginLogger;
import io.github.wysohn.rapidframework3.core.main.Mediator;
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
import java.util.Optional;
import java.util.logging.Logger;

@Singleton
public class SimulationMediator extends Mediator {
    private static final BigDecimal DEFAULT_PRICING_START = BigDecimal.ONE;

    private final Logger logger;
    private final MarketSimulationManager marketSimulationManager;
    private final AssetListingManager assetListingManager;

    private final TradeMediator tradeMediator;
    private final BankingMediator bankingMediator;

    MarketSimulator marketSimulator;

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
        if(marketSimulator != null)
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
        marketSimulator.interrupt();
    }

    /**
     * @deprecated test only. Shutdown simulator thread and wait for join
     */
    public void shutdown() throws InterruptedException {
        marketSimulator.interrupt();
        marketSimulator.join();
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
                    Thread.sleep(60 * 60 * 1000L); // hourly
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
                agent.getOrderIds(OrderType.BUY).forEach(orderId ->
                        tradeMediator.cancelOrder(agent, orderId, OrderType.BUY));

                // also, return the currency to the bank.
                BigDecimal currentBalance = centralBank.balanceOfAccount(agent,
                        BankingTypeRegistry.TRADING);
                TransactionUtil.Result returnResult = bankingMediator.send(agent,
                        BankingTypeRegistry.TRADING,
                        centralBank,
                        currentBalance,
                        centralBank.getBaseCurrency());
                if(returnResult != TransactionUtil.Result.OK){
                    logger.fine("agent "+agent+" is unable to return currency to the bank.");
                    logger.fine("amount: "+currentBalance);
                    logger.fine("reason: "+returnResult);
                    return;
                }

                agent.neededResources().forEach(pair -> {
                    AssetSignature sign = pair.key;
                    int amount = (int) Math.ceil(pair.value);

                    // get pricing of this agent is using
                    BigDecimal currentPricing = agent.getCurrentPricing(sign);
                    if (currentPricing == null) {
                        currentPricing = DEFAULT_PRICING_START;
                    }

                    // get current lowest market price
                    BigDecimal lowestPricing = Optional.of(assetListingManager)
                            .map(manager -> manager.getLowestPrice(sign, currency))
                            .map(PricePoint::getPrice)
                            .orElse(BigDecimal.ONE);

                    // get (agent price + lowest price) / 2
                    BigDecimal midPoint = currentPricing.add(lowestPricing)
                            .divide(BigDecimal.valueOf(2.0), RoundingMode.HALF_UP);

                    // we are probably making a bid again, that means the market is not willing
                    // to take the price we suggested an hour ago
                    // increase bid a bit to make it more attractable
                    midPoint = midPoint.multiply(BigDecimal.valueOf(1.0 + TradeMediator.PRICE_CHANGE_PCT));
                    agent.updateCurrentPricing(sign, midPoint);

                    // before making bids, make sure we have enough balance in the bank
                    BigDecimal totalPrice = midPoint.multiply(BigDecimal.valueOf(amount));
                    TransactionUtil.Result sendResult = bankingMediator.send(centralBank,
                            agent,
                            BankingTypeRegistry.TRADING,
                            totalPrice,
                            centralBank.getBaseCurrency());
                    if(sendResult != TransactionUtil.Result.OK){
                        logger.fine("agent "+agent+" is unable to borrow currency from the bank.");
                        logger.fine("amount: "+totalPrice);
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
                                (int) Math.ceil(amount));
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
                agent.getOrderIds(OrderType.SELL).forEach(orderId ->
                        tradeMediator.cancelOrder(agent, orderId, OrderType.SELL));

                BigDecimal unitCost = agent.getFixedUnitCost();
                agent.getProductionTypes().forEach(sign -> {
                    int currentStock = (int) Math.ceil(centralBank.countAccountAsset(agent, sign));

                    // we don't have enough stock
                    if(currentStock < 1)
                        return;

                    // get current highest market price
                    BigDecimal highestPricing = Optional.of(assetListingManager)
                            .map(manager -> manager.getHighestPrice(sign, currency))
                            .map(PricePoint::getPrice)
                            .orElse(BigDecimal.TEN);

                    // get (agent price + lowest price) / 2
                    BigDecimal sellingPrice = unitCost.add(highestPricing)
                            .divide(BigDecimal.valueOf(2.0), RoundingMode.HALF_UP);

                    // we are probably making an ask again, that means the market is not willing
                    // to take the price we suggested an hour ago
                    // decrease the price a bit to make it more attractable
                    sellingPrice = sellingPrice.multiply(BigDecimal.valueOf(1.0 - TradeMediator.PRICE_CHANGE_PCT));

                    // we cannot sell items at the price cheaper than the unit cost!
                    // that would be a dumb thing to do
                    if (sellingPrice.compareTo(unitCost) < 0) {
                        sellingPrice = unitCost;
                    }

                    agent.updateCurrentPricing(sign, sellingPrice);

                    try {
                        assetListingManager.addOrder(sign,
                                OrderType.SELL,
                                agent,
                                unitCost.doubleValue(),
                                currency,
                                currentStock);
                        assetListingManager.commitOrders();
                    } catch (SQLException ex) {
                        ex.printStackTrace();
                    }
                });
            }
        }
    }
}
