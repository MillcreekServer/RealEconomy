package io.github.wysohn.realeconomy.manager.asset.listing;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Objects;
import java.util.UUID;

public class TradeInfo {
    private final int sellId;
    private final UUID seller;
    private final double ask;
    private final int stock;
    private final int buyId;
    private final UUID buyer;
    private final double bid;
    private final int amount;
    private final UUID currencyUuid;
    private final UUID listingUuid;

    private TradeInfo(int sellId,
                      UUID seller,
                      double ask,
                      int stock,
                      int buyId,
                      UUID buyer,
                      double bid,
                      int amount,
                      UUID currencyUuid,
                      UUID listingUuid) {
        this.sellId = sellId;
        this.seller = seller;
        this.ask = ask;
        this.stock = stock;
        this.buyId = buyId;
        this.buyer = buyer;
        this.bid = bid;
        this.amount = amount;
        this.currencyUuid = currencyUuid;
        this.listingUuid = listingUuid;
    }

    public int getSellId() {
        return sellId;
    }

    public UUID getSeller() {
        return seller;
    }

    public double getAsk() {
        return ask;
    }

    public int getStock() {
        return stock;
    }

    public int getBuyId() {
        return buyId;
    }

    public UUID getBuyer() {
        return buyer;
    }

    public double getBid() {
        return bid;
    }

    public int getAmount() {
        return amount;
    }

    public UUID getCurrencyUuid() {
        return currencyUuid;
    }

    public UUID getListingUuid() {
        return listingUuid;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TradeInfo tradeInfo = (TradeInfo) o;
        return sellId == tradeInfo.sellId &&
                Double.compare(tradeInfo.ask, ask) == 0 &&
                stock == tradeInfo.stock &&
                buyId == tradeInfo.buyId &&
                Double.compare(tradeInfo.bid, bid) == 0 &&
                amount == tradeInfo.amount &&
                seller.equals(tradeInfo.seller) &&
                buyer.equals(tradeInfo.buyer) &&
                currencyUuid.equals(tradeInfo.currencyUuid) &&
                listingUuid.equals(tradeInfo.listingUuid);
    }

    @Override
    public int hashCode() {
        return Objects.hash(sellId, seller, ask, stock, buyId, buyer, bid, amount, currencyUuid, listingUuid);
    }

    @Override
    public String toString() {
        return "TradeInfo{" +
                "sellId=" + sellId +
                ", seller=" + seller +
                ", ask=" + ask +
                ", stock=" + stock +
                ", buyId=" + buyId +
                ", buyer=" + buyer +
                ", bid=" + bid +
                ", amount=" + amount +
                ", currencyUuid=" + currencyUuid +
                ", listingUuid=" + listingUuid +
                '}';
    }

    public static TradeInfo read(ResultSet rs) throws SQLException {
        int sellId = rs.getInt("sell_id");
        UUID sellerUuid = UUID.fromString(rs.getString("seller_uuid"));
        double ask = rs.getDouble("ask");
        int stock = rs.getInt("stock");
        int buyId = rs.getInt("buy_id");
        UUID buyerUuid = UUID.fromString(rs.getString("buyer_uuid"));
        double bid = rs.getDouble("bid");
        int amount = rs.getInt("amount");
        UUID currencyUuid = UUID.fromString(rs.getString("currency"));
        UUID listingUuid = UUID.fromString(rs.getString("listing_uuid"));
        return new TradeInfo(sellId, sellerUuid, ask, stock, buyId, buyerUuid, bid, amount, currencyUuid, listingUuid);
    }

    public static TradeInfo create(int sellId, UUID seller, double ask, int stock,
                                   int buyId, UUID buyer, double bid, int amount,
                                   UUID currencyUuid, UUID listingUuid) {
        return new TradeInfo(sellId, seller, ask, stock, buyId, buyer, bid, amount, currencyUuid, listingUuid);
    }
}
