INSERT INTO instruments (seccode, type, lot_size, min_step, currency) VALUES
    ('SBER', 'STOCK', 10, 0.01, 'RUB'),
    ('GAZP', 'STOCK', 10, 0.01, 'RUB');

INSERT INTO accounts (id, user_id, balance, currency) VALUES
    (101, 1, 100000, 'RUB'),
    (201, 2, 10000, 'RUB'),
    (202, 2, 10000, 'CNY');

INSERT INTO positions (account_id, seccode, quantity) VALUES
    (201, 'SBER', 1000),
    (201, 'GAZP', 1000);
