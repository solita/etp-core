
-- name: delete-energiatodistus-luonnos!
update energiatodistus set tila_id = et_tilat.poistettu
from et_tilat
where tila_id = et_tilat.luonnos and id = :id

-- name: select-energiatodistus
select energiatodistus.*,
       fullname(kayttaja.*) "laatija-fullname",
       korvaava_energiatodistus.id as korvaava_energiatodistus_id
from energiatodistus
  inner join kayttaja on kayttaja.id = energiatodistus.laatija_id
  left join energiatodistus korvaava_energiatodistus on korvaava_energiatodistus.korvattu_energiatodistus_id = energiatodistus.id
where energiatodistus.id = :id
  and energiatodistus.tila_id <> (select poistettu FROM et_tilat);

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

-- name: update-energiatodistus-luonnos!
update energiatodistus set tila_id = et_tilat.luonnos
from et_tilat
where tila_id = et_tilat.allekirjoituksessa and laatija_id = :laatija-id and id = :id

-- name: update-energiatodistus-allekirjoitettu!
update energiatodistus set
  tila_id = et_tilat.allekirjoitettu,
  allekirjoitusaika = now(),
  voimassaolo_paattymisaika = timezone('Europe/Helsinki', now()::date::timestamp) + interval '10 year' + interval '1 day'

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

-- name: select-required-columns
select column_name
from validation_required_column where versio = :versio
order by ordinal asc;

-- name: select-sisaiset-kuormat
select
  kayttotarkoitusluokka_id,
  henkilot$kayttoaste,
  henkilot$lampokuorma,
  kuluttajalaitteet$kayttoaste,
  kuluttajalaitteet$lampokuorma,
  valaistus$kayttoaste,
  valaistus$lampokuorma
from validation_sisainen_kuorma where versio = :versio;
