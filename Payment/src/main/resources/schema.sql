CREATE TABLE IF NOT EXISTS transactions (
    id serial PRIMARY KEY,
    transaction_id bigint,
    nickname varchar,
    price double precision,
    status varchar
);