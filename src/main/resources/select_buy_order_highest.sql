select * from buy_orders
where currency_uuid = ?
    and listing_uuid = ?
order by price desc
limit 1