
create table kayttotarkoitusluokka (
  id int,
  versio int,
  label_fi text,
  label_sv text,
  ordinal int not null default 0,
  deleted boolean not null default false,
  primary key (id, versio)
);

create table alakayttotarkoitusluokka (
  id varchar(4),
  versio int,
  kayttotarkoitusluokka_id int,
  label_fi text,
  label_sv text,
  ordinal int not null default 0,
  deleted boolean not null default false,
  primary key (id, versio),
  foreign key (kayttotarkoitusluokka_id, versio)
    references kayttotarkoitusluokka (id, versio)
);