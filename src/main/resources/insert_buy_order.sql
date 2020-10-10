INSERT INTO buy_orders(listing_uuid,
  timestamp,
  issuer,
  price,
  currencyUuid,
  amount,
  maximum)
VALUES (?,
  CURRENT_TIME(),
  ?,
  ?,
  ?,
  ?,
  ?);