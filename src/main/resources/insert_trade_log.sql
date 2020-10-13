INSERT INTO trade_logs(listing_uuid,
  timestamp,
  seller,
  buyer,
  price,
  currency_uuid,
  amount)
VALUES (?,
  ?,
  ?,
  ?,
  ?,
  ?,
  ?);