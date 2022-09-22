CREATE TABLE IF NOT EXISTS ticket_validated (
     id serial PRIMARY KEY,
     validation_date timestamp,
     zid varchar
);