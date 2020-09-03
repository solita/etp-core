-- name: insert-energiatodistus<!
insert into energiatodistus (versio, laatija_id, data)
values (:versio, :laatija-id, :data :: JSONB) returning id

-- name: update-energiatodistus-luonnos!
update energiatodistus set data = :data :: JSONB
from et_tilat
where tila_id = et_tilat.luonnos and id = :id

-- name: delete-energiatodistus-luonnos!
update energiatodistus set tila_id = et_tilat.poistettu
from et_tilat
where tila_id = et_tilat.luonnos and id = :id

-- name: update-rakennustunnus-when-energiatodistus-signed!
update energiatodistus
set pt$rakennustunnus = :rakennustunnus
where id = :id and tila_id = 2

-- name: select-energiatodistus
select energiatodistus.*,
       fullname(kayttaja.*) "laatija-fullname",
       korvaava_energiatodistus.id as korvaava_energiatodistus_id
from energiatodistus
  inner join kayttaja on kayttaja.id = energiatodistus.laatija_id
  left join energiatodistus korvaava_energiatodistus on korvaava_energiatodistus.korvattu_energiatodistus_id = energiatodistus.id
where energiatodistus.id = :id

-- name: select-energiatodistukset-by-laatija
select energiatodistus.*,
       fullname(kayttaja.*) "laatija-fullname",
       korvaava_energiatodistus.id as korvaava_energiatodistus_id
from et_tilat, energiatodistus
  inner join kayttaja on kayttaja.id = energiatodistus.laatija_id
  left join energiatodistus korvaava_energiatodistus on korvaava_energiatodistus.korvattu_energiatodistus_id = energiatodistus.id
where energiatodistus.laatija_id = :laatija-id and
      energiatodistus.tila_id <> et_tilat.poistettu and (
      (:tila-id::integer is null) or
      (:tila-id::integer = 0 and energiatodistus.allekirjoitusaika is null) or
      (:tila-id::integer = 1 and energiatodistus.allekirjoitusaika is not null))

-- name: select-replaceable-energiatodistukset-like-id
select energiatodistus.id
from energiatodistus,
     et_tilat
where energiatodistus.tila_id in (et_tilat.allekirjoitettu, et_tilat.hylatty)
  and energiatodistus.id::text like :id::text || '%'
  and not exists(select *
                 from energiatodistus et
                          inner join energiatodistus korvaava_energiatodistus
                                     on korvaava_energiatodistus.korvattu_energiatodistus_id = energiatodistus.id)
limit 100;

-- name: update-energiatodistus-allekirjoituksessa!
update energiatodistus set tila_id = et_tilat.allekirjoituksessa
from et_tilat
where tila_id = et_tilat.luonnos and laatija_id = :laatija-id and id = :id

-- name: update-energiatodistus-allekirjoitettu!
update energiatodistus set
  tila_id = et_tilat.allekirjoitettu,
  allekirjoitusaika = now(),
  voimassaolo_paattymisaika = current_date + interval '10 year' + interval '1 day'

from et_tilat
where tila_id = et_tilat.allekirjoituksessa and laatija_id = :laatija-id and id = :id

-- name: update-energiatodistus-korvattu!
update energiatodistus set
  tila_id = et_tilat.korvattu
from et_tilat
where tila_id = et_tilat.allekirjoitettu and laatija_id = :laatija-id and id = :id

-- name: select-numeric-validations
select column_name, warning$min, warning$max, error$min, error$max
from validation_numeric_column where versio = :versio;
