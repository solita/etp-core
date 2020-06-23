CREATE TABLE verkkolaskuoperaattori (
  id int PRIMARY KEY,
  valittajatunnus text unique,
  nimi text NOT NULL,
  deleted boolean NOT NULL DEFAULT false
);
