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
       fullname(kayttaja.*) "laatija-fullname"
from energiatodistus
  inner join kayttaja on kayttaja.id = energiatodistus.laatija_id
where energiatodistus.id = :id

-- name: select-energiatodistukset-by-laatija
select energiatodistus.*,
       fullname(kayttaja.*) "laatija-fullname"
from et_tilat, energiatodistus
  inner join kayttaja on kayttaja.id = energiatodistus.laatija_id
where energiatodistus.laatija_id = :laatija-id and
      tila_id <> et_tilat.poistettu and (
      (:tila-id::integer is null) or
      (:tila-id::integer = 0 and energiatodistus.allekirjoitusaika is null) or
      (:tila-id::integer = 1 and energiatodistus.allekirjoitusaika is not null))

-- name: select-signed-energiatodistukset-like-id
select energiatodistus.id
from energiatodistus, et_tilat
where energiatodistus.tila_id = et_tilat.allekirjoitettu and
      energiatodistus.id::text like :id::text || '%'
limit 100

-- name: update-energiatodistus-allekirjoituksessa!
update energiatodistus set tila_id = et_tilat.allekirjoituksessa
from et_tilat
where tila_id = et_tilat.luonnos and laatija_id = :laatija-id and id = :id

-- name: update-energiatodistus-allekirjoitettu!
update energiatodistus set
  tila_id = et_tilat.allekirjoitettu,
  allekirjoitusaika = now()
from et_tilat
where tila_id = et_tilat.allekirjoituksessa and laatija_id = :laatija-id and id = :id
