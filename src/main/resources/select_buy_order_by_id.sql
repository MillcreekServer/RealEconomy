/*query by currency*/
SELECT order_id, listing_uuid, category_id, timestamp, issuer, price, currency_uuid, amount, maximum
FROM buy_orders
WHERE order_id=?;