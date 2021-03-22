select * from trade_logs
where DATEDIFF(CURDATE(), `timestamp`) < ?
    and currency_uuid = ?
    and listing_uuid = ?
order by price desc
LIMIT 1