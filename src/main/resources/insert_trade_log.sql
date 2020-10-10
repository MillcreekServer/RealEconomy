INSERT INTO trade_logs(listing_uuid,
  timestamp,
  seller,
  buyer,
  price,
  currencyUuid,
  amount)
VALUES (?,
  CURRENT_TIME(),
  ?,
  ?,
  ?,
  ?,
  ?);