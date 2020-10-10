CREATE TABLE IF NOT EXISTS buy_orders (
	id INTEGER NOT NULL AUTO_INCREMENT,
	listing_uuid CHAR(36) NOT NULL,
	timestamp DATETIME NOT NULL,
	issuer CHAR(36) NOT NULL,

	price DOUBLE PRECISION NOT NULL DEFAULT 0.0,
	currencyUuid CHAR(36) NOT NULL,

	amount INTEGER NOT NULL DEFAULT 0,
	maximum INTEGER NOT NULL DEFAULT 0,

	PRIMARY KEY(id)
);