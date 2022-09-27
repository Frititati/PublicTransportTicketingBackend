CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

CREATE TABLE IF NOT EXISTS activation (
    id uuid DEFAULT uuid_generate_v4(),
   activation_code integer NOT NULL,
   counter integer NOT NULL,
   deadline timestamp NOT NULL,
   user_id integer NOT NULL,
   PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS devices (
    id serial PRIMARY KEY,
    "name" varchar(255) NOT NULL,
    "password" varchar(255) NOT NULL,
    "role" integer NOT NULL,
    "zone" varchar(255) NOT NULL
);

CREATE TABLE IF NOT EXISTS users (
  id serial PRIMARY KEY,
  active bool NOT NULL,
  email varchar(255) NOT NULL,
  nickname varchar(255) NOT NULL,
  "password" varchar(255) NOT NULL,
  "role" integer NOT NULL
);

ALTER TABLE activation DROP CONSTRAINT IF EXISTS user_constraint;
ALTER TABLE activation ADD CONSTRAINT user_constraint FOREIGN KEY (user_id) REFERENCES users(id);