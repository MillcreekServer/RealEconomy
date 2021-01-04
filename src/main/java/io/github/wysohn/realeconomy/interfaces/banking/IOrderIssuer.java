package io.github.wysohn.realeconomy.interfaces.banking;

import io.github.wysohn.rapidframework3.interfaces.IPluginObject;
import io.github.wysohn.realeconomy.interfaces.IFinancialEntity;
import io.github.wysohn.realeconomy.manager.listing.OrderType;
import io.github.wysohn.realeconomy.manager.listing.TradeInfo;
import io.github.wysohn.realeconomy.mediator.TradeMediator;

import java.util.Collection;

public interface IOrderIssuer extends IPluginObject, IFinancialEntity {
    boolean addOrderId(OrderType type, int orderId);

    boolean hasOrderId(OrderType type, int orderId);

    boolean removeOrderId(OrderType type, int orderId);

    Collection<Integer> getOrderIds(OrderType type);

    void handleTransactionResult(TradeInfo info, OrderType type, TradeMediator.TradeResult result);
}
