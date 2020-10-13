/*query by currency*/
SELECT order_id, listing_uuid, timestamp, issuer, MIN(price) as min_price, currency_uuid, amount, maximum
FROM sell_orders
WHERE listing_uuid = ?
GROUP BY listing_uuid
ORDER BY order_id
LIMIT ?, ?;