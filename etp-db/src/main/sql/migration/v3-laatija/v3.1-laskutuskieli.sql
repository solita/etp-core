CREATE TABLE laskutuskieli (
  id int PRIMARY KEY,
  label_fi text,
  label_sv text,
  valid boolean NOT NULL DEFAULT false
);
