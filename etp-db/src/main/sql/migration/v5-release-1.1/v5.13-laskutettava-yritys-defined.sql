alter table etp.energiatodistus
  add column laskutettava_yritys_defined bool not null default false;

update etp.energiatodistus
  set laskutettava_yritys_defined = true
where tila_id in (1, 2, 3, 4);