CREATE TABLE trading_lock (
    id bigint PRIMARY KEY CHECK (id = 1)
);

INSERT INTO trading_lock (id) VALUES (1);
