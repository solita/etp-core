create table kayttaja (
  id int GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
  etunimi text NOT NULL,
  sukunimi text NOT NULL,
  henkilotunnus text NOT NULL,
  email text NOT NULL,
  puhelinnumero text,
  katuosoite text NOT NULL,
  postinumero text NOT NULL,
  postitoimipaikka text NOT NULL,
  maa char(2) DEFAULT 'FI' NOT NULL REFERENCES country (id),
  passivoitu boolean DEFAULT false NOT NULL,
  rooli int DEFAULT 0 NOT NULL,
  cognito_id text
);
