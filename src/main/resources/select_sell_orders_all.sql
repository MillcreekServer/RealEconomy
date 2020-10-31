/*query by currency*/
SELECT order_id, listing_uuid, category_id, timestamp, issuer, MIN(price) as price, currency_uuid, amount, maximum
FROM sell_orders
GROUP BY listing_uuid
ORDER BY order_id
LIMIT ?, ?;