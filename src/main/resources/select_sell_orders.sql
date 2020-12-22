/*query by currency*/
select order_id, listing_uuid, category_id, timestamp, issuer, price, currency_uuid, amount, maximum
from sell_orders tbl1
join (
    -- Search the lowest price per listing_uuid
    select listing_uuid as uuid, min(price) as p
    from sell_orders
    group by uuid
    ) tbl2
    on tbl1.price = tbl2.p
where category_id = ?
-- since the sub-query may return multiple rows if there are multiple exact same priced items,
-- choose the first one as output
group by listing_uuid
LIMIT ?, ?;