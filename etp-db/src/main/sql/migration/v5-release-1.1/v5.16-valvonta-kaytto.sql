call create_classification('vk_toimenpidetype'::name);
call create_classification('vk_toimitustapa'::name);

call create_classification('vk_template'::name);
alter table vk_template
  add column toimenpidetype_id int not null references vk_toimenpidetype (id),
  add column language text not null,
  add column content text;

);

call create_classification('vk_rooli'::name);
call create_classification('vk_ilmoituspaikka'::name);


create table vk_valvonta (
  id int generated by default as identity primary key,
  rakennustunnus text,
  katuosoite text not null,
  postinumero int references postinumero (id),
  ilmoituspaikka_id int references vk_ilmoituspaikka,
  ilmoituspaikka_description text,
  ilmoitustunnus text,
  havaintopaiva date,
  valvoja_id int not null default etp.current_kayttaja_id() references etp.kayttaja (id),
  deleted boolean not null default false
);

call audit.activate('vk_valvonta'::name);

create table vk_toimenpide (
  id int generated by default as identity primary key,
  type_id int not null references vk_toimenpidetype (id),
  author_id int not null default etp.current_kayttaja_id() references etp.kayttaja (id),

  create_time timestamp with time zone not null default transaction_timestamp(),
  publish_time timestamp with time zone,
  deadline_date date,
  template_id int references vk_template (id),
  diaarinumero text,
  description text,
  filename text,
  valvonta_id int references vk_valvonta (id)
);

call audit.activate('vk_toimenpide'::name);

create table vk_henkilo (
  id int generated by default as identity primary key,
  etunimi text not null,
  sukunimi text not null,
  henkilotunnus text,
  email text,
  puhelin text,
  jakeluosoite text,
  vastaanottajan_tarkenne text,
  postinumero text,
  postitoimipaikka text,
  maa char(2) references country (id),
  rooli_id int references vk_rooli (id),
  rooli_description text,
  toimitustapa_id int references vk_toimitustapa (id),
  toimitustapa_description text,
  deleted boolean not null default false,
  valvonta_id int references vk_valvonta (id)
);

call audit.activate('vk_henkilo'::name);

create table vk_yritys (
  id int generated by default as identity primary key,
  nimi text not null,
  ytunnus text,
  email text,
  puhelin text,
  jakeluosoite text,
  vastaanottajan_tarkenne text,
  postinumero text,
  postitoimipaikka text,
  maa char(2) references country (id),
  rooli_id int references vk_rooli (id),
  rooli_description text,
  toimitustapa_id int references vk_toimitustapa (id),
  toimitustapa_description text,
  deleted boolean not null default false,
  valvonta_id int references vk_valvonta (id)
);

call audit.activate('vk_yritys'::name);

create table vk_toimenpide_henkilo (
  toimenpide_id int not null references vk_toimenpide (id),
  henkilo_id int not null references vk_toimenpide (id)
);

call audit.activate('vk_toimenpide_henkilo'::name);

create table vk_toimenpide_yritys (
  toimenpide_id int not null references vk_toimenpide (id),
  yritys_id int not null references vk_toimenpide (id)
);

call audit.activate('vk_toimenpide_yritys'::name);

create table vk_valvonta_liite (
  id int generated by default as identity primary key,
  valvonta_id int not null references vk_valvonta (id),
  nimi text,
  contenttype text,
  url text,
  deleted boolean not null default false
);

call audit.activate('vk_valvonta_liite'::name);