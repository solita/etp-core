CREATE TABLE lammonjako (
  id int PRIMARY KEY,
  label_fi text,
  label_sv text,
  ordinal int NOT NULL DEFAULT 0,
  valid boolean NOT NULL default true
);
