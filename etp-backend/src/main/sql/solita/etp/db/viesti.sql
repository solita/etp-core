
-- name: select-all-viestiketjut
select
  viestiketju.id, viestiketju.kasittelija_id, viestiketju.kasitelty, viestiketju.subject,
  viestiketju.vastaanottajaryhma_id, viestiketju.energiatodistus_id,
  (select array_agg(vastaanottaja_id) from vastaanottaja
    where vastaanottaja.viestiketju_id = viestiketju.id) vastaanottajat
  from viestiketju
order by (select max(sent_time) from viesti where viestiketju_id = viestiketju.id) desc
limit :limit offset :offset;

-- name: select-viestiketjut-for-kayttaja
select
  viestiketju.id, viestiketju.kasittelija_id, viestiketju.kasitelty, viestiketju.subject,
  viestiketju.vastaanottajaryhma_id, viestiketju.energiatodistus_id,
  (select array_agg(vastaanottaja_id) from vastaanottaja
    where vastaanottaja.viestiketju_id = viestiketju.id) vastaanottajat
from viestiketju
where from_id = :kayttaja-id or vastaanottajaryhma_id = :vastaanottajaryhma-id or
      exists (
        select 1 from vastaanottaja
        where vastaanottaja.vastaanottaja_id = :kayttaja-id and
              vastaanottaja.viestiketju_id = viestiketju.id
      )
order by (select max(sent_time) from viesti where viestiketju_id = viestiketju.id) desc
limit :limit offset :offset;

-- name: select-count-all-viestiketjut
select count(*) count from viestiketju;

-- name: select-count-viestiketjut-for-laatija
select count(*) count
from viestiketju
where from_id = :laatija-id or vastaanottajaryhma_id = 1 or
  exists (
    select 1 from vastaanottaja
    where vastaanottaja.vastaanottaja_id = :laatija-id and
        vastaanottaja.viestiketju_id = viestiketju.id
  );

-- name: select-viestiketju
select
  viestiketju.id, viestiketju.kasittelija_id, viestiketju.kasitelty, viestiketju.subject,
  viestiketju.vastaanottajaryhma_id, viestiketju.energiatodistus_id,
  (select array_agg(vastaanottaja_id) from vastaanottaja
    where vastaanottaja.viestiketju_id = viestiketju.id) vastaanottajat
from viestiketju
where id = :id;

-- name: select-viestit
select
  viesti.sent_time, viesti.body,
  kayttaja.id       from$id,
  kayttaja.rooli_id from$rooli_id,
  kayttaja.etunimi  from$etunimi,
  kayttaja.sukunimi from$sukunimi,
  viesti_reader.read_time
from viesti
  inner join kayttaja on kayttaja.id = viesti.from_id
  left join viesti_reader
    on viesti_reader.viesti_id = viesti.id and
       viesti_reader.reader_id = :reader-id
where viesti.viestiketju_id = :id
 order by viesti.sent_time asc;

-- name: select-kayttajat
select id, etunimi, sukunimi, rooli_id from kayttaja;

-- name: select-kasittelijat
select id, etunimi, sukunimi, rooli_id from kayttaja WHERE rooli_id IN (2, 3);

--name: read-ketju!
insert into viesti_reader (viesti_id)
select viesti.id from viesti where viesti.viestiketju_id = :viestiketju-id
on conflict (viesti_id, reader_id) do nothing
