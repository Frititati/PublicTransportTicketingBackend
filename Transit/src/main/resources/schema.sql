CREATE TABLE IF NOT EXISTS ticket_validated (
     id serial PRIMARY KEY,
     ticket_id UUID NOT NULL,
     validation_date timestamp,
     zid varchar,
     user_nickname varchar
);