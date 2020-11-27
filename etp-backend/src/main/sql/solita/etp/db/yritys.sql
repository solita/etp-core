-- name: insert-yritys<!
INSERT INTO yritys (ytunnus, nimi, verkkolaskuoperaattori, verkkolaskuosoite, laskutuskieli, jakeluosoite, vastaanottajan_tarkenne, postinumero, postitoimipaikka, maa)
VALUES (:ytunnus, :nimi, :verkkolaskuoperaattori, :verkkolaskuosoite, :laskutuskieli, :jakeluosoite, :vastaanottajan-tarkenne, :postinumero, :postitoimipaikka, :maa)
RETURNING id

-- name: update-yritys!
UPDATE yritys SET nimi = :nimi, verkkolaskuoperaattori = :verkkolaskuoperaattori, verkkolaskuosoite = :verkkolaskuosoite, laskutuskieli = :laskutuskieli, jakeluosoite = :jakeluosoite, vastaanottajan_tarkenne = :vastaanottajan-tarkenne, postinumero = :postinumero, postitoimipaikka = :postitoimipaikka, maa = :maa
WHERE id = :id

-- name: select-yritys
SELECT id, ytunnus, nimi, verkkolaskuoperaattori, verkkolaskuosoite, laskutuskieli, jakeluosoite, vastaanottajan_tarkenne as "vastaanottajan-tarkenne", postinumero, postitoimipaikka, maa
FROM yritys
WHERE id = :id

-- name: select-all-yritykset
SELECT id, ytunnus, nimi, verkkolaskuoperaattori, verkkolaskuosoite, laskutuskieli, jakeluosoite, vastaanottajan_tarkenne as "vastaanottajan-tarkenne", postinumero, postitoimipaikka, maa
FROM yritys

--name: select-all-laskutuskielet
SELECT id, label_fi as "label-fi", label_sv as "label-sv", valid FROM laskutuskieli;

-- name: select-all-verkkolaskuoperaattorit
SELECT id, valittajatunnus, nimi FROM verkkolaskuoperaattori;

-- name: select-laatijat
with audit as (
  select row_number() over (
    partition by laatija_id
    order by modifytime desc, event_id desc) as event_order,
         modifytime,
         modifiedby_id,
         laatija_id
  from audit.laatija_yritys audit
  where yritys_id = :id
)
select
  laatija.etunimi,
  laatija.sukunimi,
  audit.modifytime,
  fullname(modifier) modifiedby_name,
  laatija_yritys.tila_id
from laatija_yritys
  inner join kayttaja laatija on laatija.id = laatija_yritys.laatija_id
  left join audit on audit.laatija_id = laatija_yritys.laatija_id and
                     audit.event_order = 1
  left join kayttaja modifier on modifier.id = audit.modifiedby_id
where laatija_yritys.yritys_id = :id
