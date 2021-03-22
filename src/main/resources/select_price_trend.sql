select * from trade_logs
where DATEDIFF(CURDATE(), `timestamp`) < ? -- last N days
    and currency_uuid = ? -- target currency
    and listing_uuid = ? -- target listing
order by `timestamp`