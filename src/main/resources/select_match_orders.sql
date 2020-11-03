/*find best match*/
SELECT sell_orders.order_id as sell_id,
	sell_orders.issuer as seller_uuid,
	sell_orders.price as ask,
	sell_orders.amount as stock,
	buy_orders.order_id as buy_id,
	buy_orders.issuer as buyer_uuid,
	buy_orders.price as bid,
	buy_orders.amount as amount,
	sell_orders.currency_uuid as currency,
	sell_orders.listing_uuid as listing_uuid,
	sell_orders.category_id as category_id
FROM sell_orders JOIN buy_orders
ON sell_orders.listing_uuid = buy_orders.listing_uuid -- same asset
    AND sell_orders.currency_uuid = buy_orders.currency_uuid -- same currency
	AND sell_orders.price <= buy_orders.price -- price agreement
ORDER BY buy_orders.order_id ASC, sell_orders.price ASC
LIMIT 1;