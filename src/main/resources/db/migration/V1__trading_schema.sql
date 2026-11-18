CREATE TABLE instruments (
    seccode varchar(32) PRIMARY KEY,
    type varchar(16) NOT NULL CHECK (type = 'STOCK'),
    lot_size bigint NOT NULL CHECK (lot_size > 0),
    min_step numeric(19,4) NOT NULL CHECK (min_step > 0),
    currency varchar(3) NOT NULL CHECK (currency ~ '^[A-Z]{3}$')
);

CREATE TABLE accounts (
    id bigint PRIMARY KEY,
    user_id bigint NOT NULL CHECK (user_id > 0),
    balance numeric(38,4) NOT NULL CHECK (balance >= 0),
    reserved numeric(38,4) NOT NULL DEFAULT 0 CHECK (reserved >= 0 AND reserved <= balance),
    currency varchar(3) NOT NULL CHECK (currency ~ '^[A-Z]{3}$')
);
CREATE INDEX accounts_user_idx ON accounts(user_id);

CREATE TABLE positions (
    account_id bigint NOT NULL REFERENCES accounts(id),
    seccode varchar(32) NOT NULL REFERENCES instruments(seccode),
    quantity bigint NOT NULL DEFAULT 0 CHECK (quantity >= 0),
    reserved bigint NOT NULL DEFAULT 0 CHECK (reserved >= 0 AND reserved <= quantity),
    PRIMARY KEY (account_id, seccode)
);

CREATE TABLE orders (
    id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    account_id bigint NOT NULL REFERENCES accounts(id),
    seccode varchar(32) NOT NULL REFERENCES instruments(seccode),
    type varchar(4) NOT NULL CHECK (type IN ('BUY', 'SELL')),
    price numeric(19,4) NOT NULL CHECK (price > 0),
    quantity bigint NOT NULL CHECK (quantity > 0 AND quantity <= 1000000000000),
    filled_quantity bigint NOT NULL DEFAULT 0 CHECK (filled_quantity >= 0 AND filled_quantity <= quantity),
    status varchar(20) NOT NULL CHECK (status IN ('ACTIVE','PARTIALLY_FILLED','FILLED','CANCELLED')),
    created_at timestamptz NOT NULL DEFAULT clock_timestamp(),
    updated_at timestamptz NOT NULL DEFAULT clock_timestamp(),
    replaces_order_id bigint UNIQUE REFERENCES orders(id),
    CHECK ((status = 'ACTIVE' AND filled_quantity = 0)
        OR (status = 'PARTIALLY_FILLED' AND filled_quantity > 0 AND filled_quantity < quantity)
        OR (status = 'FILLED' AND filled_quantity = quantity)
        OR (status = 'CANCELLED' AND filled_quantity < quantity))
);
CREATE INDEX orders_book_idx ON orders(seccode, type, price, id)
    WHERE status IN ('ACTIVE','PARTIALLY_FILLED');
CREATE INDEX orders_register_idx ON orders(account_id, created_at DESC, id DESC);

CREATE TABLE trades (
    id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    seccode varchar(32) NOT NULL REFERENCES instruments(seccode),
    buy_order_id bigint NOT NULL REFERENCES orders(id),
    sell_order_id bigint NOT NULL REFERENCES orders(id),
    price numeric(19,4) NOT NULL CHECK (price > 0),
    quantity bigint NOT NULL CHECK (quantity > 0),
    executed_at timestamptz NOT NULL DEFAULT clock_timestamp(),
    CHECK (buy_order_id <> sell_order_id)
);
CREATE INDEX trades_register_idx ON trades(executed_at DESC, id DESC);
CREATE INDEX trades_buy_idx ON trades(buy_order_id);
CREATE INDEX trades_sell_idx ON trades(sell_order_id);
