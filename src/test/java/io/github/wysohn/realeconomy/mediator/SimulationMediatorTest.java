package io.github.wysohn.realeconomy.mediator;

import com.google.inject.*;
import com.google.inject.multibindings.ProvidesIntoSet;
import io.github.wysohn.rapidframework3.core.main.ManagerConfig;
import io.github.wysohn.rapidframework3.testmodules.MockLoggerModule;
import io.github.wysohn.rapidframework3.utils.Pair;
import io.github.wysohn.realeconomy.interfaces.banking.IBankUserProvider;
import io.github.wysohn.realeconomy.manager.asset.signature.AssetSignature;
import io.github.wysohn.realeconomy.manager.asset.signature.ItemStackSignature;
import io.github.wysohn.realeconomy.manager.banking.CentralBankingManager;
import io.github.wysohn.realeconomy.manager.banking.bank.CentralBank;
import io.github.wysohn.realeconomy.manager.currency.Currency;
import io.github.wysohn.realeconomy.manager.currency.CurrencyManager;
import io.github.wysohn.realeconomy.manager.listing.AssetListingManager;
import io.github.wysohn.realeconomy.manager.simulation.Agent;
import io.github.wysohn.realeconomy.manager.simulation.MarketSimulationManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Server;
import org.bukkit.inventory.ItemFactory;
import org.junit.Before;
import org.junit.Test;
import org.powermock.reflect.Whitebox;

import java.lang.reflect.Field;
import java.util.*;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class SimulationMediatorTest {
    List<Module> moduleList = new LinkedList<>();
    private ManagerConfig config;

    @Before
    public void init() throws Exception {
        config = mock(ManagerConfig.class);

        Server server = mock(Server.class);
        Field field = Bukkit.class.getDeclaredField("server");
        field.setAccessible(true);
        field.set(null, server);

        ItemFactory itemFactory = mock(ItemFactory.class);
        when(server.getItemFactory()).thenReturn(itemFactory);

        moduleList.add(new MockLoggerModule());
        moduleList.add(new AbstractModule() {
            @Provides
            ManagerConfig managerConfig(){
                return config;
            }
        });
    }

    @Test
    public void testSimulator() throws Exception{
        CurrencyManager currencyManager = mock(CurrencyManager.class);
        AssetListingManager assetListingManager = mock(AssetListingManager.class);
        IBankUserProvider bankUserProvider = mock(IBankUserProvider.class);
        CentralBankingManager centralBankingManager = mock(CentralBankingManager.class);
        moduleList.add(new AbstractModule() {
            @Provides
            CurrencyManager currencyManager() {
                return currencyManager;
            }

            @Provides
            AssetListingManager assetListingManager() {
                return assetListingManager;
            }

            @ProvidesIntoSet
            IBankUserProvider bankUserProvider() {
                return bankUserProvider;
            }

            @Provides
            CentralBankingManager centralBankingManager(){
                return centralBankingManager;
            }
        });

        CentralBank centralBank = mock(CentralBank.class);
        Currency currency = mock(Currency.class);

        when(centralBank.getBaseCurrency()).thenReturn(currency);
        when(currency.ownerBank()).thenReturn(centralBank);

        Field field = BankingMediator.class.getDeclaredField("serverBank");
        field.setAccessible(true);
        field.set(null, centralBank);

        Injector injector = Guice.createInjector(moduleList);
        SimulationMediator mediator = injector.getInstance(SimulationMediator.class);
        MarketSimulationManager simulationManager = injector.getInstance(MarketSimulationManager.class);

        Map<UUID, Agent> agentMap = new HashMap<>();
        Whitebox.setInternalState(simulationManager, "agentList", agentMap);
        agentMap.put(UUID.randomUUID(), new Agent(UUID.randomUUID(),
                "Pastry_1",
                new LinkedList<Pair<AssetSignature, Double>>(){{
                    add(Pair.of(new ItemStackSignature(Material.WHEAT), 200.0));
                    add(Pair.of(new ItemStackSignature(Material.COCOA_BEANS), 100.0));
                }},
                new LinkedList<Pair<AssetSignature, Double>>(){{
                    add(Pair.of(new ItemStackSignature(Material.COOKIE), 800.0));
                }}));
        agentMap.put(UUID.randomUUID(), new Agent(UUID.randomUUID(),
                "Pastry_2",
                new LinkedList<Pair<AssetSignature, Double>>(){{
                    add(Pair.of(new ItemStackSignature(Material.WHEAT), 300.0));
                }},
                new LinkedList<Pair<AssetSignature, Double>>(){{
                    add(Pair.of(new ItemStackSignature(Material.BREAD), 100.0));
                }}));
        mediator.enable();
        mediator.load();

        Thread.sleep(10L);
        mediator.shutdown();

        mediator.disable();
    }
}