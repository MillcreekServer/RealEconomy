package io.github.wysohn.realeconomy.inject.module;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Module;
import com.google.inject.Provides;
import io.github.wysohn.rapidframework3.core.inject.annotations.PluginDirectory;
import io.github.wysohn.rapidframework3.interfaces.IMemento;
import io.github.wysohn.rapidframework3.interfaces.io.IPluginResourceProvider;
import io.github.wysohn.rapidframework3.interfaces.paging.DataProvider;
import io.github.wysohn.rapidframework3.testmodules.MockShutdownModule;
import io.github.wysohn.realeconomy.interfaces.banking.IOrderIssuer;
import io.github.wysohn.realeconomy.interfaces.trade.IOrderPlacementHandler;
import io.github.wysohn.realeconomy.manager.asset.Asset;
import io.github.wysohn.realeconomy.manager.asset.listing.OrderInfo;
import io.github.wysohn.realeconomy.manager.asset.listing.OrderType;
import io.github.wysohn.realeconomy.manager.asset.listing.TradeInfo;
import io.github.wysohn.realeconomy.manager.asset.signature.AssetSignature;
import io.github.wysohn.realeconomy.manager.currency.Currency;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.math.BigDecimal;
import java.util.*;
import java.util.function.Consumer;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isNull;
import static org.mockito.Mockito.*;

public class OrderPlacementHandlerModuleTest {
    List<Module> moduleList = new LinkedList<>();

    @Before
    public void init() {
        moduleList.add(new OrderPlacementHandlerModule());
        moduleList.add(new OrderSQLModule());
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
        IOrderPlacementHandler orderPlacementHandler = Guice.createInjector(moduleList)
                .getInstance(IOrderPlacementHandler.class);

        UUID listingUuid = UUID.randomUUID();
        IOrderIssuer orderIssuer = new OrderIssuer(UUID.randomUUID());
        Currency currency = mock(Currency.class);
        UUID currencyUuid = UUID.randomUUID();

        when(currency.getKey()).thenReturn(currencyUuid);

        orderPlacementHandler.addOrder(listingUuid,
                OrderType.SELL,
                orderIssuer,
                2000.55,
                currency,
                20);
        orderPlacementHandler.addOrder(listingUuid,
                OrderType.SELL,
                orderIssuer,
                1034.55,
                currency,
                5);
        orderPlacementHandler.addOrder(listingUuid,
                OrderType.SELL,
                orderIssuer,
                2050.55,
                currency,
                30);

        Consumer<TradeInfo> consumer = mock(Consumer.class);

        orderPlacementHandler.peekMatchingOrders(consumer);

        verify(consumer).accept(isNull(TradeInfo.class));

        orderPlacementHandler.addOrder(listingUuid,
                OrderType.BUY,
                orderIssuer,
                1000.0,
                currency,
                30);

        orderPlacementHandler.addOrder(listingUuid,
                OrderType.BUY,
                orderIssuer,
                3000.0,
                currency,
                30);

        orderPlacementHandler.addOrder(listingUuid,
                OrderType.BUY,
                orderIssuer,
                5000.0,
                currency,
                30);

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
                listingUuid)));
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
        IOrderPlacementHandler orderPlacementHandler = Guice.createInjector(moduleList)
                .getInstance(IOrderPlacementHandler.class);

        UUID listingUuid1 = UUID.randomUUID();
        UUID listingUuid2 = UUID.randomUUID();
        UUID listingUuid3 = UUID.randomUUID();

        IOrderIssuer orderIssuer = new OrderIssuer(UUID.randomUUID());
        Currency currency = mock(Currency.class);
        UUID currencyUuid = UUID.randomUUID();

        when(currency.getKey()).thenReturn(currencyUuid);

        orderPlacementHandler.addOrder(listingUuid1,
                OrderType.SELL,
                orderIssuer,
                2000.55,
                currency,
                20);
        orderPlacementHandler.addOrder(listingUuid1,
                OrderType.SELL,
                orderIssuer,
                1045.33,
                currency,
                20);
        orderPlacementHandler.addOrder(listingUuid1,
                OrderType.SELL,
                orderIssuer,
                1001.32,
                currency,
                20);
        orderPlacementHandler.addOrder(listingUuid2,
                OrderType.SELL,
                orderIssuer,
                3463.55,
                currency,
                5);
        orderPlacementHandler.addOrder(listingUuid2,
                OrderType.SELL,
                orderIssuer,
                5534.55,
                currency,
                5);
        orderPlacementHandler.addOrder(listingUuid2,
                OrderType.SELL,
                orderIssuer,
                3940.23,
                currency,
                5);
        orderPlacementHandler.addOrder(listingUuid3,
                OrderType.SELL,
                orderIssuer,
                980.42,
                currency,
                30);
        orderPlacementHandler.addOrder(listingUuid3,
                OrderType.SELL,
                orderIssuer,
                1212.34,
                currency,
                30);
        orderPlacementHandler.addOrder(listingUuid3,
                OrderType.SELL,
                orderIssuer,
                1050.53,
                currency,
                30);

        DataProvider<OrderInfo> provider =
                orderPlacementHandler.getListedOrderProvider();

        List<OrderInfo> expected = new ArrayList<>();
        expected.add(OrderInfo.create(3,
                listingUuid1,
                orderIssuer.getUuid(),
                1001.32,
                currencyUuid,
                20,
                20));
        expected.add(OrderInfo.create(4,
                listingUuid2,
                orderIssuer.getUuid(),
                3463.55,
                currencyUuid,
                5,
                5));
        expected.add(OrderInfo.create(7,
                listingUuid3,
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
                orderPlacementHandler.getListedOrderProvider(listingUuid1);

        List<OrderInfo> expected2 = new ArrayList<>();
        expected2.add(OrderInfo.create(3,
                listingUuid1,
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
        public IMemento saveState() {
            return null;
        }

        @Override
        public void restoreState(IMemento savedState) {

        }

        @Override
        public void addAsset(Asset asset) {

        }

        @Override
        public int removeAsset(AssetSignature signature, int amount) {
            return 0;
        }

        @Override
        public DataProvider<Asset> assetDataProvider() {
            return null;
        }
    }
}