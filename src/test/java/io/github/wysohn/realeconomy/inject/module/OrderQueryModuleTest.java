package io.github.wysohn.realeconomy.inject.module;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Module;
import com.google.inject.Provides;
import io.github.wysohn.rapidframework3.core.inject.annotations.PluginDirectory;
import io.github.wysohn.rapidframework3.interfaces.IMemento;
import io.github.wysohn.rapidframework3.interfaces.io.IPluginResourceProvider;
import io.github.wysohn.rapidframework3.interfaces.paging.DataProvider;
import io.github.wysohn.rapidframework3.testmodules.MockConfigModule;
import io.github.wysohn.rapidframework3.testmodules.MockShutdownModule;
import io.github.wysohn.rapidframework3.utils.Pair;
import io.github.wysohn.realeconomy.interfaces.banking.IOrderIssuer;
import io.github.wysohn.realeconomy.interfaces.trade.IOrderQueryModule;
import io.github.wysohn.realeconomy.manager.asset.Asset;
import io.github.wysohn.realeconomy.manager.currency.Currency;
import io.github.wysohn.realeconomy.manager.listing.OrderInfo;
import io.github.wysohn.realeconomy.manager.listing.OrderType;
import io.github.wysohn.realeconomy.manager.listing.TradeInfo;
import io.github.wysohn.realeconomy.mediator.TradeMediator;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.math.BigDecimal;
import java.util.*;
import java.util.function.Consumer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isNull;
import static org.mockito.Mockito.*;

public class OrderQueryModuleTest {
    List<Module> moduleList = new LinkedList<>();

    @Before
    public void init() {
        moduleList.add(new OrderQueryModule());
        moduleList.add(new OrderSQLModule());
        moduleList.add(new MockConfigModule(Pair.of("database.type", "sqlite")));
        moduleList.add(new MockShutdownModule(() -> {

        }));
        moduleList.add(new AbstractModule() {
            @Provides
            IPluginResourceProvider resourceProvider() {
                File folder = new File("src/main/resources/");
                return name -> {
                    try {
                        return new FileInputStream(new File(folder, name));
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    }
                    return null;
                };
            }
        });
    }

    @Test
    public void selectBuy() throws Exception {
        File folder = new File("build/tmp/selbuy/");
        folder.mkdir();
        moduleList.add(new AbstractModule() {
            @Provides
            @PluginDirectory
            File directory() {
                return folder;
            }
        });
        new File(folder, "orders.db").delete();
        IOrderQueryModule orderPlacementHandler = Guice.createInjector(moduleList)
                .getInstance(IOrderQueryModule.class);

        UUID issuerUuid = UUID.randomUUID();
        IOrderIssuer orderIssuer = new OrderIssuer(issuerUuid);
        Currency currency = mock(Currency.class);
        UUID currencyUuid = UUID.randomUUID();

        UUID uuid1 = UUID.randomUUID();
        UUID uuid2 = UUID.randomUUID();

        when(currency.getKey()).thenReturn(currencyUuid);

        orderPlacementHandler.addOrder(uuid1,
                "item1",
                OrderType.BUY,
                orderIssuer,
                1023.22,
                currency,
                10,
                false);

        orderPlacementHandler.addOrder(uuid2,
                "item1",
                OrderType.BUY,
                orderIssuer,
                20304.55,
                currency,
                20,
                false);

        assertEquals(OrderInfo.create(1,
                uuid1,
                1,
                issuerUuid,
                1023.22,
                currencyUuid,
                10,
                10), orderPlacementHandler.getInfo(1, OrderType.BUY));

        assertEquals(OrderInfo.create(2,
                uuid2,
                1,
                issuerUuid,
                20304.55,
                currencyUuid,
                20,
                20), orderPlacementHandler.getInfo(2, OrderType.BUY));

        assertNull(orderPlacementHandler.getInfo(22, OrderType.SELL));
    }

    @Test
    public void selectSell() throws Exception {
        File folder = new File("build/tmp/selsell/");
        folder.mkdir();
        moduleList.add(new AbstractModule() {
            @Provides
            @PluginDirectory
            File directory() {
                return folder;
            }
        });
        new File(folder, "orders.db").delete();
        IOrderQueryModule orderPlacementHandler = Guice.createInjector(moduleList)
                .getInstance(IOrderQueryModule.class);

        UUID issuerUuid = UUID.randomUUID();
        IOrderIssuer orderIssuer = new OrderIssuer(issuerUuid);
        Currency currency = mock(Currency.class);
        UUID currencyUuid = UUID.randomUUID();

        UUID uuid1 = UUID.randomUUID();
        UUID uuid2 = UUID.randomUUID();

        when(currency.getKey()).thenReturn(currencyUuid);

        orderPlacementHandler.addOrder(uuid1,
                "item1",
                OrderType.SELL,
                orderIssuer,
                1023.22,
                currency,
                10,
                false);

        orderPlacementHandler.addOrder(uuid2,
                "item1",
                OrderType.SELL,
                orderIssuer,
                20304.55,
                currency,
                20,
                false);

        assertEquals(OrderInfo.create(1,
                uuid1,
                1,
                issuerUuid,
                1023.22,
                currencyUuid,
                10,
                10), orderPlacementHandler.getInfo(1, OrderType.SELL));

        assertEquals(OrderInfo.create(2,
                uuid2,
                1,
                issuerUuid,
                20304.55,
                currencyUuid,
                20,
                20), orderPlacementHandler.getInfo(2, OrderType.SELL));

        assertNull(orderPlacementHandler.getInfo(22, OrderType.BUY));
    }

    @Test
    public void editOrder() throws Exception {
        File folder = new File("build/tmp/editorder/");
        folder.mkdir();
        moduleList.add(new AbstractModule() {
            @Provides
            @PluginDirectory
            File directory() {
                return folder;
            }
        });
        new File(folder, "orders.db").delete();
        IOrderQueryModule orderPlacementHandler = Guice.createInjector(moduleList)
                .getInstance(IOrderQueryModule.class);

        UUID issuerUuid = UUID.randomUUID();
        IOrderIssuer orderIssuer = new OrderIssuer(issuerUuid);
        Currency currency = mock(Currency.class);
        UUID currencyUuid = UUID.randomUUID();

        UUID uuid1 = UUID.randomUUID();

        when(currency.getKey()).thenReturn(currencyUuid);

        orderPlacementHandler.addOrder(uuid1,
                "item1",
                OrderType.BUY,
                orderIssuer,
                1023.22,
                currency,
                10,
                false);

        orderPlacementHandler.editOrder(1,
                OrderType.BUY,
                2);

        orderPlacementHandler.commitOrders();

        assertEquals(OrderInfo.create(1,
                uuid1,
                1,
                issuerUuid,
                1023.22,
                currencyUuid,
                2,
                10), orderPlacementHandler.getInfo(1, OrderType.BUY));
    }

    @Test
    public void cancelOrder() throws Exception {
        File folder = new File("build/tmp/cancelorder/");
        folder.mkdir();
        moduleList.add(new AbstractModule() {
            @Provides
            @PluginDirectory
            File directory() {
                return folder;
            }
        });
        new File(folder, "orders.db").delete();
        IOrderQueryModule orderPlacementHandler = Guice.createInjector(moduleList)
                .getInstance(IOrderQueryModule.class);

        UUID issuerUuid = UUID.randomUUID();
        IOrderIssuer orderIssuer = new OrderIssuer(issuerUuid);
        Currency currency = mock(Currency.class);
        UUID currencyUuid = UUID.randomUUID();

        UUID uuid1 = UUID.randomUUID();

        when(currency.getKey()).thenReturn(currencyUuid);

        orderPlacementHandler.addOrder(uuid1,
                "item1",
                OrderType.BUY,
                orderIssuer,
                1023.22,
                currency,
                10,
                false);
        orderPlacementHandler.addOrder(uuid1,
                "item2",
                OrderType.BUY,
                orderIssuer,
                3464.25,
                currency,
                20,
                false);
        orderPlacementHandler.addOrder(uuid1,
                "item3",
                OrderType.SELL,
                orderIssuer,
                5525.33,
                currency,
                30,
                false);

        orderPlacementHandler.commitOrders();

        assertEquals(OrderInfo.create(1,
                uuid1,
                1,
                issuerUuid,
                1023.22,
                currencyUuid,
                10,
                10), orderPlacementHandler.getInfo(1, OrderType.BUY));

        Consumer<Integer> consumer = mock(Consumer.class);
        orderPlacementHandler.cancelOrder(1, OrderType.BUY, consumer);

        verify(consumer).accept(1);
        assertNull(orderPlacementHandler.getInfo(1, OrderType.BUY));
    }

    @Test
    public void peekMatchingOrder() throws Exception {
        File folder = new File("build/tmp/matchingorder/");
        folder.mkdir();
        moduleList.add(new AbstractModule() {
            @Provides
            @PluginDirectory
            File directory() {
                return folder;
            }
        });
        new File(folder, "orders.db").delete();
        IOrderQueryModule orderPlacementHandler = Guice.createInjector(moduleList)
                .getInstance(IOrderQueryModule.class);

        UUID listingUuid = UUID.randomUUID();
        IOrderIssuer orderIssuer = new OrderIssuer(UUID.randomUUID());
        Currency currency = mock(Currency.class);
        UUID currencyUuid = UUID.randomUUID();

        when(currency.getKey()).thenReturn(currencyUuid);

        orderPlacementHandler.addOrder(listingUuid,
                TradeMediator.MATERIAL_CATEGORY_DEFAULT,
                OrderType.SELL,
                orderIssuer,
                2000.55,
                currency,
                20,
                false);
        orderPlacementHandler.addOrder(listingUuid,
                TradeMediator.MATERIAL_CATEGORY_DEFAULT,
                OrderType.SELL,
                orderIssuer,
                1034.55,
                currency,
                5,
                false);
        orderPlacementHandler.addOrder(listingUuid,
                TradeMediator.MATERIAL_CATEGORY_DEFAULT,
                OrderType.SELL,
                orderIssuer,
                2050.55,
                currency,
                30,
                false);

        Consumer<TradeInfo> consumer = mock(Consumer.class);

        orderPlacementHandler.peekMatchingOrders(consumer);

        verify(consumer).accept(isNull(TradeInfo.class));

        orderPlacementHandler.addOrder(listingUuid,
                TradeMediator.MATERIAL_CATEGORY_DEFAULT,
                OrderType.BUY,
                orderIssuer,
                1000.0,
                currency,
                30,
                false);

        orderPlacementHandler.addOrder(listingUuid,
                TradeMediator.MATERIAL_CATEGORY_DEFAULT,
                OrderType.BUY,
                orderIssuer,
                3000.0,
                currency,
                30,
                false);

        orderPlacementHandler.addOrder(listingUuid,
                TradeMediator.MATERIAL_CATEGORY_DEFAULT,
                OrderType.BUY,
                orderIssuer,
                5000.0,
                currency,
                30,
                false);

        orderPlacementHandler.peekMatchingOrders(consumer);

        // out of all 3 sell offers, always the cheapest deal will be matched.
        // out of all 3 bids, even though the second buyer offers higher price
        //   than the first buyer, the match is always prioritized by time order.
        // and of course, if buyer's bid doesn't cover any of the deal, it will
        //   be simply ignored until the best match is found.
        verify(consumer).accept(eq(TradeInfo.create(2, // cheapest offer
                orderIssuer.getUuid(),
                1034.55,
                5,
                2, //first bid is ignored since bid is too low
                orderIssuer.getUuid(),
                3000.0,
                30,
                currencyUuid,
                listingUuid,
                1)));
    }

    @Test
    public void getListedOrderProvider() throws Exception {
        File folder = new File("build/tmp/orderprovider/");
        folder.mkdir();
        moduleList.add(new AbstractModule() {
            @Provides
            @PluginDirectory
            File directory() {
                return folder;
            }
        });
        new File(folder, "orders.db").delete();
        IOrderQueryModule orderPlacementHandler = Guice.createInjector(moduleList)
                .getInstance(IOrderQueryModule.class);

        UUID listingUuid1 = UUID.randomUUID();
        UUID listingUuid2 = UUID.randomUUID();
        UUID listingUuid3 = UUID.randomUUID();

        IOrderIssuer orderIssuer = new OrderIssuer(UUID.randomUUID());
        Currency currency = mock(Currency.class);
        UUID currencyUuid = UUID.randomUUID();

        when(currency.getKey()).thenReturn(currencyUuid);

        orderPlacementHandler.addOrder(listingUuid1,
                "item1",
                OrderType.SELL,
                orderIssuer,
                2000.55,
                currency,
                20,
                false);
        orderPlacementHandler.addOrder(listingUuid1,
                "item1",
                OrderType.SELL,
                orderIssuer,
                1045.33,
                currency,
                20,
                false);
        orderPlacementHandler.addOrder(listingUuid1,
                "item1",
                OrderType.SELL,
                orderIssuer,
                1001.32,
                currency,
                20,
                false);
        orderPlacementHandler.addOrder(listingUuid2,
                "item2",
                OrderType.SELL,
                orderIssuer,
                3463.55,
                currency,
                5,
                false);
        orderPlacementHandler.addOrder(listingUuid2,
                "item2",
                OrderType.SELL,
                orderIssuer,
                5534.55,
                currency,
                5,
                false);
        orderPlacementHandler.addOrder(listingUuid2,
                "item2",
                OrderType.SELL,
                orderIssuer,
                3940.23,
                currency,
                5,
                false);
        orderPlacementHandler.addOrder(listingUuid3,
                "item3",
                OrderType.SELL,
                orderIssuer,
                980.42,
                currency,
                30,
                false);
        orderPlacementHandler.addOrder(listingUuid3,
                "item3",
                OrderType.SELL,
                orderIssuer,
                1212.34,
                currency,
                30,
                false);
        orderPlacementHandler.addOrder(listingUuid3,
                "item3",
                OrderType.SELL,
                orderIssuer,
                1050.53,
                currency,
                30,
                false);
        orderPlacementHandler.commitOrders();

        DataProvider<OrderInfo> provider =
                orderPlacementHandler.getListedOrderProvider();

        List<OrderInfo> expected = new ArrayList<>();
        expected.add(OrderInfo.create(3,
                listingUuid1,
                2,
                orderIssuer.getUuid(),
                1001.32,
                currencyUuid,
                20,
                20));
        expected.add(OrderInfo.create(4,
                listingUuid2,
                2,
                orderIssuer.getUuid(),
                3463.55,
                currencyUuid,
                5,
                5));
        expected.add(OrderInfo.create(7,
                listingUuid3,
                3,
                orderIssuer.getUuid(),
                980.42,
                currencyUuid,
                30,
                30));
        List<OrderInfo> actual = new ArrayList<>();
        actual.add(provider.get(0, 1).stream()
                .findFirst()
                .orElseThrow(RuntimeException::new));
        actual.add(provider.get(1, 1).stream()
                .findFirst()
                .orElseThrow(RuntimeException::new));
        actual.add(provider.get(2, 1).stream()
                .findFirst()
                .orElseThrow(RuntimeException::new));

        expected.sort(Comparator.comparingInt(OrderInfo::getOrderId));
        actual.sort(Comparator.comparingInt(OrderInfo::getOrderId));
        assertEquals(expected, actual);

        DataProvider<OrderInfo> providerFiltered =
                orderPlacementHandler.getListedOrderProvider("item1");

        List<OrderInfo> expected2 = new ArrayList<>();
        expected2.add(OrderInfo.create(3,
                listingUuid1,
                1,
                orderIssuer.getUuid(),
                1001.32,
                currencyUuid,
                20,
                20));
        List<OrderInfo> actual2 = new ArrayList<>();
        actual2.add(providerFiltered.get(0, 100).stream()
                .findFirst()
                .orElseThrow(RuntimeException::new));

        expected2.sort(Comparator.comparingInt(OrderInfo::getOrderId));
        actual2.sort(Comparator.comparingInt(OrderInfo::getOrderId));
        assertEquals(expected2, actual2);
    }

    private static class OrderIssuer implements IOrderIssuer {
        private final Map<OrderType, Set<Integer>> orderMap = new EnumMap<>(OrderType.class);

        private final UUID uuid;

        public OrderIssuer(UUID uuid) {
            this.uuid = uuid;
        }

        @Override
        public boolean addOrderId(OrderType type, int orderId) {
            return orderMap.computeIfAbsent(type, t -> new HashSet<>())
                    .add(orderId);
        }

        @Override
        public boolean hasOrderId(OrderType type, int orderId) {
            Set<Integer> orderSet = orderMap.get(type);
            if (orderSet == null)
                return false;

            return orderSet.contains(orderId);
        }

        @Override
        public boolean removeOrderId(OrderType type, int orderId) {
            return orderMap.computeIfAbsent(type, t -> new HashSet<>())
                    .remove(orderId);
        }

        @Override
        public Collection<Integer> getOrderIds(OrderType type) {
            return orderMap.computeIfAbsent(type, t -> new HashSet<>());
        }

        @Override
        public void handleTransactionResult(TradeInfo info,
                                            OrderType type,
                                            TradeMediator.TradeResult result) {

        }

        @Override
        public UUID getUuid() {
            return uuid;
        }

        @Override
        public BigDecimal balance(Currency currency) {
            return null;
        }

        @Override
        public boolean deposit(BigDecimal value, Currency currency) {
            return false;
        }

        @Override
        public boolean withdraw(BigDecimal value, Currency currency) {
            return false;
        }

        @Override
        public int realizeAsset(Asset asset) {
            return 0;
        }

        @Override
        public IMemento saveState() {
            return null;
        }

        @Override
        public void restoreState(IMemento savedState) {

        }
    }
}