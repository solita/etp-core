
-- name: select-all-viestiketjut
select
  viestiketju.id, viestiketju.subject,
  viestiketju.vastaanottajaryhma_id, viestiketju.energiatodistus_id,
  (select array_agg(vastaanottaja_id) from vastaanottaja
  where vastaanottaja.viestiketju_id = viestiketju.id) vastaanottajat
from viestiketju
order by viestiketju.id desc
limit :limit offset :offset;

-- name: select-viestiketjut-for-laatija
select
  viestiketju.id, viestiketju.subject,
  viestiketju.vastaanottajaryhma_id, viestiketju.energiatodistus_id,
  (select array_agg(vastaanottaja_id) from vastaanottaja
   where vastaanottaja.viestiketju_id = viestiketju.id) vastaanottajat
from viestiketju
where from_id = :laatija-id or vastaanottajaryhma_id = 1 or
      exists (
        select 1 from vastaanottaja
        where vastaanottaja.vastaanottaja_id = :laatija-id and
              vastaanottaja.viestiketju_id = viestiketju.id
      )
order by viestiketju.id desc
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
  viestiketju.id, viestiketju.subject,
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
  kayttaja.sukunimi from$sukunimi
from viesti
  inner join kayttaja on kayttaja.id = viesti.from_id
where viesti.viestiketju_id = :id
order by viesti.sent_time asc;