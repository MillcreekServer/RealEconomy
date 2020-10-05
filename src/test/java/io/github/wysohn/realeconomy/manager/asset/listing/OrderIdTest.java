package io.github.wysohn.realeconomy.manager.asset.listing;

import org.junit.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class OrderIdTest {

    @Test
    public void testEquals() {
        OrderId id = OrderId.valueOf("103943-492953");
        OrderId id2 = OrderId.valueOf("103943-492953");
        assertEquals(id.hashCode(), id2.hashCode());
        assertEquals(id, id2);
    }

    @Test
    public void testToString() {
        OrderId id = OrderId.valueOf("346632-124236");
        assertEquals("346632-124236", id.toString());
    }

    @Test
    public void randomId() {
        for (int t = 0; t < 100; t++) {
            Set<OrderId> idSet = new HashSet<>();
            for (int i = 0; i < 10000; i++) {
                assertTrue("t:" + t + ", i:" + i, idSet.add(OrderId.randomId()));
            }
        }
    }
}