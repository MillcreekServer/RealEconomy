select * from sell_orders
where currency_uuid = ?
    and listing_uuid = ?
order by price asc
limit 1