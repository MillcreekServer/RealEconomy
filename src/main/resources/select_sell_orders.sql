/*query by currency*/
SELECT * FROM sell_orders
WHERE listing_uuid = ?
    AND currencyUuid = ?
    AND id > 0
ORDER BY id DESC;