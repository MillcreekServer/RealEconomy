INSERT INTO sell_orders(listing_uuid,
    timestamp,
    issuer,
    price,
    currency_uuid,
    amount,
    maximum)
VALUES (?,
    ?,
    ?,
    ?,
    ?,
    ?,
    ?);