package io.github.wysohn.realeconomy.manager.asset.listing;

import io.github.wysohn.rapidframework3.bukkit.testutils.manager.AbstractBukkitManagerTest;
import io.github.wysohn.realeconomy.manager.asset.signature.AssetSignature;
import org.junit.Before;
import org.junit.Test;

import java.util.UUID;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;

public class AssetListingTest extends AbstractBukkitManagerTest {

    private AssetSignature signature;
    private AssetListing listing;

    @Before
    public void init() {
        signature = mock(AssetSignature.class);
        listing = new AssetListing(signature);
        addFakeObserver(listing);
    }

    @Test
    public void addBuy() {
        UUID issuerUuid = UUID.randomUUID();
        UUID currencyUuid = UUID.randomUUID();

        Order order = new Order(issuerUuid,
                2043.44,
                currencyUuid,
                30);
        listing.addBuy(order);

        assertTrue(listing.buyAvailable());
        assertEquals(order.getOrderId(), listing.peekBuy());
        assertEquals(order.getOrderId(), listing.pollBuy());
        assertFalse(listing.buyAvailable());
    }

    @Test
    public void addBuyOrder() {
        UUID issuerUuid = UUID.randomUUID();
        UUID currencyUuid = UUID.randomUUID();

        listing.addBuy(new Order(issuerUuid,
                1030.4,
                currencyUuid,
                30));
        listing.addBuy(new Order(issuerUuid,
                1244.5,
                currencyUuid,
                15));
        listing.addBuy(new Order(issuerUuid,
                2000.6,
                currencyUuid,
                10));

        assertTrue(listing.buyAvailable());
        assertEquals(2000.6, listing.pollBuy().getPrice(), 0.00001);
        assertEquals(1244.5, listing.pollBuy().getPrice(), 0.00001);
        assertEquals(1030.4, listing.pollBuy().getPrice(), 0.00001);
        assertFalse(listing.buyAvailable());
    }

    @Test
    public void cancelBuy() {
    }

    @Test
    public void addSell() {
    }

    @Test
    public void pollSell() {
    }

    @Test
    public void peekSell() {
    }

    @Test
    public void cancelSell() {
    }

    @Test
    public void sellAvailable() {
    }

    @Test
    public void pollIfMatch() {
    }
}