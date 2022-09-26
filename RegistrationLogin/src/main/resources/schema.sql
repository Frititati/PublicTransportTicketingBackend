CREATE TABLE IF NOT EXISTS activation (
   id uuid PRIMARY KEY,
   activation_code int4 NOT NULL,
   counter int4 NOT NULL,
   deadline timestamp NOT NULL,
   user_id int8 NOT NULL,
);

CREATE TABLE IF NOT EXISTS devices (
    id int8 PRIMARY KEY,
    "name" varchar(255) NOT NULL,
    "password" varchar(255) NOT NULL,
    "role" int4 NOT NULL,
    "zone" varchar(255) NOT NULL,
);

CREATE TABLE IF NOT EXISTS users (
  id int8 PRIMARY KEY,
  active bool NOT NULL,
  email varchar(255) NOT NULL,
  nickname varchar(255) NOT NULL,
  "password" varchar(255) NOT NULL,
  "role" int4 NOT NULL,
);