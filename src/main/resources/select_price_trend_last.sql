select * from trade_logs
where currency_uuid = ? -- target currency
    and listing_uuid = ? -- target listing
order by `timestamp` desc
limit 1