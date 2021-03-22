package io.github.wysohn.realeconomy.manager.listing;

import io.github.wysohn.realeconomy.inject.module.OrderSQLModule;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;
import java.util.UUID;

public class PricePoint {
    private final int orderId;
    private final UUID listingUuid;
    private final int categoryId;
    private final Date timestamp;
    private final UUID seller;
    private final UUID buyer;
    private final BigDecimal price;
    private final UUID currencyUuid;
    private final int amount;

    private PricePoint(int orderId,
                      UUID listingUuid,
                      int categoryId,
                      Date timestamp,
                      UUID seller,
                      UUID buyer,
                      BigDecimal price,
                      UUID currencyUuid,
                      int amount) {
        this.orderId = orderId;
        this.listingUuid = listingUuid;
        this.categoryId = categoryId;
        this.timestamp = timestamp;
        this.seller = seller;
        this.buyer = buyer;
        this.price = price;
        this.currencyUuid = currencyUuid;
        this.amount = amount;
    }

    public int getOrderId() {
        return orderId;
    }

    public UUID getListingUuid() {
        return listingUuid;
    }

    public int getCategoryId() {
        return categoryId;
    }

    public Date getTimestamp() {
        return timestamp;
    }

    public UUID getSeller() {
        return seller;
    }

    public UUID getBuyer() {
        return buyer;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public UUID getCurrencyUuid() {
        return currencyUuid;
    }

    public int getAmount() {
        return amount;
    }

    public static PricePoint read(ResultSet rs) throws SQLException {
        int orderId = rs.getInt(OrderSQLModule.ORDER_ID);

        // check null result before working on other stuff
        if (rs.wasNull())
            return null;

        UUID listingUuid = UUID.fromString(rs.getString("listing_uuid"));
        int categoryId = rs.getInt("category_id");
        Date timestamp = rs.getDate("timestamp");
        UUID seller = UUID.fromString(rs.getString("seller"));
        UUID buyer = UUID.fromString(rs.getString("buyer"));
        BigDecimal price = rs.getBigDecimal("price");
        UUID currencyUuid = UUID.fromString(rs.getString("currency_uuid"));
        int amount = rs.getInt("amount");

        return new PricePoint(orderId, listingUuid, categoryId, timestamp, seller, buyer, price, currencyUuid, amount);
    }
}
