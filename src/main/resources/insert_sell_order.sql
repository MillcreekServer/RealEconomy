INSERT INTO sell_orders(listing_uuid,
    timestamp,
    issuer,
    price,
    currencyUuid,
    amount,
    maximum)
VALUES (?,
    ?,
    ?,
    ?,
    ?,
    ?,
    ?);