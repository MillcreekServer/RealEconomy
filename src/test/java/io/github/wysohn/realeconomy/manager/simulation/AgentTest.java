package io.github.wysohn.realeconomy.manager.simulation;

import io.github.wysohn.rapidframework3.utils.Pair;
import io.github.wysohn.realeconomy.manager.asset.signature.AssetSignature;
import io.github.wysohn.realeconomy.manager.asset.signature.ItemStackSignature;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Server;
import org.bukkit.inventory.ItemFactory;
import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.LinkedList;
import java.util.UUID;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class AgentTest {

    Agent agent;

    @Before
    public void init() throws Exception{
        Server server = mock(Server.class);
        Field field = Bukkit.class.getDeclaredField("server");
        field.setAccessible(true);
        field.set(null, server);

        ItemFactory itemFactory = mock(ItemFactory.class);
        when(server.getItemFactory()).thenReturn(itemFactory);

        agent = new Agent(UUID.randomUUID(),
                "some agent",
                new LinkedList<Pair<AssetSignature, Double>>(){{
                    add(Pair.of(new ItemStackSignature(Material.WHEAT), 200.0));
                    add(Pair.of(new ItemStackSignature(Material.COCOA_BEANS), 100.0));
                }},
                new LinkedList<Pair<AssetSignature, Double>>(){{
                    add(Pair.of(new ItemStackSignature(Material.COOKIE), 800.0));
                }});
    }

    @Test
    public void neededResources() {
        Collection<Pair<AssetSignature, Double>> expected = new LinkedList<>();
        expected.add(Pair.of(new ItemStackSignature(Material.WHEAT), 200.0));
        expected.add(Pair.of(new ItemStackSignature(Material.COCOA_BEANS), 100.0));

        assertTrue(agent.neededResources().containsAll(expected));
    }

    @Test
    public void canProduce() {
        assertFalse(agent.canProduce());

        ItemStackSignature wheat = new ItemStackSignature(Material.WHEAT);
        ItemStackSignature cocoa = new ItemStackSignature(Material.COCOA_BEANS);

        agent.realizeAsset(wheat.asset(150.0));
        assertFalse(agent.canProduce());

        agent.realizeAsset(cocoa.asset(50.0));
        assertFalse(agent.canProduce());

        agent.realizeAsset(wheat.asset(49.99999998));
        agent.realizeAsset(cocoa.asset(49.99999998));
        assertFalse(agent.canProduce());

        agent.realizeAsset(wheat.asset(1.0));
        agent.realizeAsset(cocoa.asset(1.0));
        assertTrue(agent.canProduce());
    }

    @Test
    public void produce() {
        ItemStackSignature wheat = new ItemStackSignature(Material.WHEAT);
        ItemStackSignature cocoa = new ItemStackSignature(Material.COCOA_BEANS);
        ItemStackSignature cookie = new ItemStackSignature(Material.COOKIE);

        agent.realizeAsset(wheat.asset(400.0));
        agent.realizeAsset(cocoa.asset(200.0));

        Collection<Pair<AssetSignature, Double>> expected = new LinkedList<>();
        expected.add(Pair.of(cookie, 800.0));

        assertTrue(agent.produce().containsAll(expected));
        assertTrue(agent.produce().containsAll(expected));
        assertFalse(agent.produce().containsAll(expected));
    }

    @Test
    public void getFixedUnitCost() {
        agent.updateCurrentPricing(new ItemStackSignature(Material.WHEAT), BigDecimal.valueOf(0.01));
        agent.updateCurrentPricing(new ItemStackSignature(Material.COCOA_BEANS), BigDecimal.valueOf(6.5));

        // cost per one cookie
        assertEquals(BigDecimal.valueOf((200*0.01 + 100*6.5) / 800), agent.getFixedUnitCost());
    }

    @Test
    public void getProductionTypes() {
        Collection<AssetSignature> expected = new LinkedList<>();
        expected.add(new ItemStackSignature(Material.COOKIE));

        assertTrue(agent.getProductionTypes().containsAll(expected));
    }

    @Test
    public void updateCurrentPricing() {
        ItemStackSignature wheat = new ItemStackSignature(Material.WHEAT);
        ItemStackSignature cocoa = new ItemStackSignature(Material.COCOA_BEANS);
        ItemStackSignature cookie = new ItemStackSignature(Material.COOKIE);

        assertNull(agent.getCurrentPricing(wheat));
        assertNull(agent.getCurrentPricing(cocoa));
        assertNull(agent.getCurrentPricing(cookie));

        agent.updateCurrentPricing(wheat, BigDecimal.valueOf(0.12));
        agent.updateCurrentPricing(cocoa, BigDecimal.valueOf(3.4));
        agent.updateCurrentPricing(cookie, BigDecimal.valueOf(5.0));

        assertEquals(BigDecimal.valueOf(0.12), agent.getCurrentPricing(wheat));
        assertEquals(BigDecimal.valueOf(3.4), agent.getCurrentPricing(cocoa));
        assertEquals(BigDecimal.valueOf(5.0), agent.getCurrentPricing(cookie));
    }
}