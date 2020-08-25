create table yritys (
  id int generated by default as identity primary key,
  ytunnus text unique,
  nimi text NOT NULL,
  verkkolaskuoperaattori int REFERENCES verkkolaskuoperaattori (id),
  verkkolaskuosoite text,
  laskutuskieli int REFERENCES laskutuskieli (id) DEFAULT 0,
  jakeluosoite text NOT NULL,
  vastaanottajan_tarkenne text,
  postinumero text NOT NULL,
  postitoimipaikka text NOT NULL,
  maa char(2) REFERENCES country (id) NOT NULL DEFAULT 'FI'
);
