CREATE TABLE IF NOT EXISTS ticket_purchased (
        id serial PRIMARY KEY,
        "type" varchar,
        issued_at timestamp,
        exp timestamp,
        zid varchar,
        jws varchar,
        user_id bigint
);