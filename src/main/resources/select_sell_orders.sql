/*query by currency*/
SELECT order_id, listing_uuid, timestamp, issuer, MIN(price) as min_price, currencyUuid, amount, maximum
FROM sell_orders
WHERE currencyUuid = ?
GROUP BY listing_uuid
LIMIT ?, ?;