/*find best match*/
SELECT sell_orders.id as sell_id,
	sell_orders.issuer as seller_uuid,
	sell_orders.price as ask,
	sell_orders.amount as stock,
	buy_orders.id as buy_id,
	buy_orders.issuer as buyer_uuid,
	buy_orders.price as bid,
	buy_orders.amount as amount,
	sell_orders.currencyUuid as currency
FROM sell_orders JOIN buy_orders
ON sell_orders.currencyUuid = buy_orders.currencyUuid
	AND sell_orders.price <= buy_orders.price
ORDER BY buy_orders.id ASC, sell_orders.price ASC
LIMIT 1;