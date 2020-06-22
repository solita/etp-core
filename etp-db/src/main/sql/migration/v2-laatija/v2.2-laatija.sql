-- Make sure Finland exists since it's the default country for laatijat
INSERT INTO country (id, alpha3, numeric, name, label_fi, label_sv) VALUES ('FI', 'FIN', 246, 'Finland', 'Suomi', 'Finland');

create table laatija (
  id int primary key references kayttaja (id) not null,
  henkilotunnus text NOT NULL,
  patevyystaso int NOT NULL,
  toteamispaivamaara date NOT NULL,
  toteaja text NOT NULL,
  laatimiskielto boolean NOT NULL DEFAULT false,
  toimintaalue int,
  muut_toimintaalueet int[] NOT NULL DEFAULT '{}',
  wwwosoite text,
  julkinen_puhelin boolean NOT NULL DEFAULT false,
  julkinen_email boolean NOT NULL DEFAULT false,
  julkinen_osoite boolean NOT NULL DEFAULT false,
  laskutuskieli int DEFAULT 0,
  jakeluosoite text NOT NULL,
  vastaanottajan_tarkenne text,
  postinumero text NOT NULL,
  postitoimipaikka text NOT NULL,
  maa char(2) REFERENCES country (id) NOT NULL DEFAULT 'FI'
);
