package io.github.wysohn.realeconomy.interfaces.banking;

import io.github.wysohn.rapidframework3.interfaces.IPluginObject;

public interface IOrderIssuer extends IPluginObject {
    boolean addOrderId(long orderId);

    boolean hasOrderId(long orderId);

    boolean removeOrderId(long orderId);
}
