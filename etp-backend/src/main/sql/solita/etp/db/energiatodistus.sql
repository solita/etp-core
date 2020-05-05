-- name: insert-energiatodistus<!
insert into energiatodistus (versio, laatija_id, data)
values (:versio, :laatija-id, :data :: JSONB) returning id

-- name: update-energiatodistus-luonnos!
update energiatodistus set data = :data :: JSONB
where allekirjoitusaika is null and id = :id

-- name: delete-energiatodistus-luonnos!
delete from energiatodistus
where allekirjoitusaika is null and id = :id

-- name: select-energiatodistus
select energiatodistus.id, energiatodistus.versio,
       energiatodistus.allekirjoituksessaaika,
       energiatodistus.allekirjoitusaika,
       energiatodistus.laatija_id "laatija-id",
       fullname(kayttaja.*) "laatija-fullname",
       energiatodistus.data
from energiatodistus
  inner join laatija on laatija.id = energiatodistus.laatija_id
  inner join kayttaja on kayttaja.id = laatija.kayttaja
where energiatodistus.id = :id

-- name: select-energiatodistukset-by-laatija
select energiatodistus.id, energiatodistus.versio,
       energiatodistus.allekirjoituksessaaika,
       energiatodistus.allekirjoitusaika,
       fullname(kayttaja.*) "laatija-fullname",
       energiatodistus.data
from energiatodistus
  inner join laatija on laatija.id = energiatodistus.laatija_id
  inner join kayttaja on kayttaja.id = laatija.kayttaja
where energiatodistus.laatija_id = :laatija-id and (
    (:tila-id::integer is null) or
    (:tila-id::integer = 0 and energiatodistus.allekirjoitusaika is null) or
    (:tila-id::integer = 1 and energiatodistus.allekirjoitusaika is not null))

-- name: select-kayttotarkoitusluokat-by-versio
select id, label_fi "label-fi", label_sv "label-sv", deleted
from kayttotarkoitusluokka where versio = :versio
order by ordinal asc

-- name: select-alakayttotarkoitusluokat-by-versio
select id, kayttotarkoitusluokka_id "kayttotarkoitusluokka-id", label_fi "label-fi", label_sv "label-sv", deleted
from alakayttotarkoitusluokka where versio = :versio
order by ordinal asc

-- name: update-energiatodistus-allekirjoituksessaaika!
update energiatodistus set allekirjoituksessaaika = now()
where allekirjoituksessaaika is null and allekirjoitusaika is null
and id = :id

-- name: update-energiatodistus-allekirjoitusaika!
update energiatodistus set allekirjoitusaika = now()
where allekirjoituksessaaika is not null and allekirjoitusaika is null
and id = :id
