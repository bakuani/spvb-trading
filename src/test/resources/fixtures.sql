TRUNCATE trades, orders, positions, accounts, instruments RESTART IDENTITY CASCADE;

INSERT INTO instruments VALUES
    ('SBER', 'STOCK', 10, 0.05, 'RUB'),
    ('GAZP', 'STOCK', 10, 0.01, 'RUB');

INSERT INTO accounts(id, user_id, balance, currency) VALUES
    (101, 1, 100000, 'RUB'),
    (102, 1, 0, 'RUB'),
    (201, 2, 10000, 'RUB'),
    (202, 2, 10000, 'CNY'),
    (301, 3, 10000, 'RUB');

INSERT INTO positions(account_id, seccode, quantity) VALUES
    (201, 'SBER', 1000),
    (201, 'GAZP', 1000),
    (301, 'SBER', 1000),
    (102, 'SBER', 1000);
