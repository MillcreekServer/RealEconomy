package io.github.wysohn.realeconomy.manager.asset.listing;

public enum OrderType {
    // The enum order must match with the value used in SQL table
    // as enum.ordinal() method will be used.
    SELL,
    BUY
}
