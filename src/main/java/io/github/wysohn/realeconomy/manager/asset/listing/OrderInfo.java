package io.github.wysohn.realeconomy.manager.asset.listing;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

public class OrderInfo {
    private final int orderId;
    private final UUID listingUuid;
    // private final long timestamp;
    private final UUID issuer;
    private final double price;
    private final UUID currencyUuid;
    private final int amount;
    private final int max;

    private OrderInfo(int orderId,
                      UUID listingUuid,
                      //long timestamp,
                      UUID issuer,
                      double price,
                      UUID currencyUuid,
                      int amount, int max) {
        this.orderId = orderId;
        this.listingUuid = listingUuid;
        // this.timestamp = timestamp;
        this.issuer = issuer;
        this.price = price;
        this.currencyUuid = currencyUuid;
        this.amount = amount;
        this.max = max;
    }

    public int getOrderId() {
        return orderId;
    }

    public UUID getListingUuid() {
        return listingUuid;
    }

//    public long getTimestamp() {
//        return timestamp;
//    }

    public UUID getIssuer() {
        return issuer;
    }

    public double getPrice() {
        return price;
    }

    public UUID getCurrencyUuid() {
        return currencyUuid;
    }

    public int getAmount() {
        return amount;
    }

    public int getMax() {
        return max;
    }

    public static OrderInfo read(ResultSet rs) throws SQLException {
        int orderId = rs.getInt("id");
        UUID listing_uuid = UUID.fromString(rs.getString("listing_uuid"));
        //long timestamp = rs.getTimestamp("timestamp").getTime();
        UUID issuer = UUID.fromString(rs.getString("issuer"));
        double price = rs.getDouble("price");
        UUID currencyUuid = UUID.fromString(rs.getString("currencyUuid"));
        int amount = rs.getInt("amount");
        int max = rs.getInt("maximum");

        return new OrderInfo(orderId, listing_uuid, issuer, price, currencyUuid, amount, max);
    }
}
