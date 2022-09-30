CREATE TABLE IF NOT EXISTS ticket_purchased (
        id serial PRIMARY KEY,
        "type" varchar,
        issued_at timestamp,
        exp timestamp,
        zid varchar,
        jws varchar,
        user_id bigint
);

CREATE TABLE IF NOT EXISTS user_details (
        id serial PRIMARY KEY,
        nickname varchar,
        "name" varchar,
        address varchar,
        date_of_birth timestamp,
        telephone_number bigint
);