package io.github.wysohn.realeconomy.interfaces.trade;

import io.github.wysohn.rapidframework3.interfaces.paging.DataProvider;
import io.github.wysohn.realeconomy.interfaces.banking.IOrderIssuer;
import io.github.wysohn.realeconomy.manager.asset.listing.OrderInfo;
import io.github.wysohn.realeconomy.manager.asset.listing.OrderType;
import io.github.wysohn.realeconomy.manager.asset.listing.TradeInfo;
import io.github.wysohn.realeconomy.manager.currency.Currency;

import java.sql.SQLException;
import java.util.UUID;
import java.util.function.Consumer;

public interface IOrderPlacementHandler {
    void addOrder(UUID listingUuid,
                  OrderType type,
                  IOrderIssuer issuer,
                  double price,
                  Currency currency,
                  int stock) throws SQLException;

    void cancelOrder(int orderId,
                     OrderType type,
                     Consumer<Integer> callback) throws SQLException;

    void peekMatchingOrders(Consumer<TradeInfo> consumer);

    DataProvider<OrderInfo> getListedOrderProvider(UUID listingUuid);
}
