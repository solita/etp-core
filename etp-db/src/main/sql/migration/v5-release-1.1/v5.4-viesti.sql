
call create_classification('vastaanottajaryhma'::name);

create table viestiketju (
  id int generated by default as identity primary key,
  from_id int not null default etp.current_kayttaja_id() references kayttaja (id),
  vastaanottajaryhma_id int not null references vastaanottajaryhma (id),
  energiatodistus_id int references energiatodistus (id),
  subject text not null
);

create table viesti (
  id int generated by default as identity primary key,
  viestiketju_id int not null references viestiketju (id),
  from_id int not null default etp.current_kayttaja_id() references kayttaja (id),
  sent_time timestamp with time zone not null default transaction_timestamp(),
  body text not null
);

create table vastaanottaja (
  vastaanottaja_id int not null references kayttaja (id),
  viestiketju_id int not null references viestiketju (id),

  primary key (vastaanottaja_id, viestiketju_id)
);

-- left room for conversion ids:
select setval('viestiketju_id_seq', 5000, true);
select setval('viesti_id_seq', 5000, true);