
create table vo_note (
  id int generated by default as identity primary key,
  energiatodistus_id int not null references etp.energiatodistus (id),
  deleted boolean not null default false,
  description text not null
);

call audit.activate('vo_note'::name);