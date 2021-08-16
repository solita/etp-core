--alter table etp.energiatodistus
--  add column laskutettava_laatija_id int references laatija (id);

--update etp.energiatodistus
--  set laskutettava_laatija_id = laatija_id
--where tila_id in (1, 2, 3, 4) and laskutettava_yritys_id is null;


alter table etp.energiatodistus
  add column laskutettava_yritys_defined bool not null default false;

update etp.energiatodistus
  set laskutettava_yritys_defined = true
where tila_id in (1, 2, 3, 4);