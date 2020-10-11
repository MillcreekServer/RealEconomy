/*query by currency*/
SELECT * FROM sell_orders
WHERE currencyUuid = ?
    AND id > ?
ORDER BY id DESC
LIMIT ?;