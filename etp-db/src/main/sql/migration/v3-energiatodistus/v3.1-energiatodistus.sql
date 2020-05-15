
create table energiatodistus (
  id int generated by default as identity primary key,
  versio int not null,
  tila_id int not null default 0 references energiatodistustila (id),
  allekirjoituksessaaika timestamp,
  allekirjoitusaika timestamp,
  laatija_id int not null references laatija (id),
  data jsonb
);

create table energiatodistus_tila_history (
  id int generated by default as identity primary key,
  modifytime timestamptz not null default now(),
  modifiedby_id int not null default current_kayttaja_id() references kayttaja (id),
  energiatodistus_id int not null references energiatodistus (id),
  tila_id int not null references energiatodistustila (id)
);

create function energiatodistus_tila_audit() returns trigger as
$$
begin return new; end;
$$
language 'plpgsql';

create trigger energiatodistus_tila_insert_trigger
  after insert on energiatodistus for each row
execute procedure etp.energiatodistus_tila_audit();

create trigger energiatodistus_tila_update_trigger
  after update on energiatodistus for each row
  when ( (old.tila_id) is distinct from (new.tila_id) )
execute procedure etp.energiatodistus_tila_audit();

