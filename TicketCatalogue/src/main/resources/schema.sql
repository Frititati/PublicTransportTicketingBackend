CREATE TABLE IF NOT EXISTS available_tickets (
     ticket_id serial PRIMARY KEY,
     price double precision,
     "type" varchar,
     -- Otherwise we cannot calculate it
     exp timestamp,
     -- To check the age of the user in case it's necessary
     min_age int,
     max_age int
);

CREATE TABLE IF NOT EXISTS orders (
    id serial PRIMARY KEY,
    nickname varchar,
    number_tickets integer,
    ticket_id double precision,
    status varchar,
    price double precision,
    purchase_date timestamp
);

-- TODO: aggiungere ticket dentro available_tickets con INSERT INTO