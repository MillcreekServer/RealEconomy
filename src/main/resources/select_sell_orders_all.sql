/*query by currency*/
select order_id, ln2.listing_uuid, category_id, timestamp, issuer, price, currency_uuid, amount, maximum
from sell_orders tbl1 left join listing_names ln2
on tbl1.listing_uuid = ln2.listing_uuid
join (
    -- Search the lowest price per listing_uuid
    select listing_uuid as uuid, min(price) as p
    from sell_orders
    group by uuid
    ) tbl2
    on tbl1.price = tbl2.p
-- since the sub-query may return multiple rows if there are multiple exact same priced items,
-- choose the first one as output
group by listing_uuid
order by REVERSE(ln2.name)
LIMIT ?, ?;