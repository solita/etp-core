-- name: insert-energiatodistus<!
insert into energiatodistus (laatija_id, data)
values (:laatija-id, :data :: JSONB) returning id

-- name: update-energiatodistus-when-luonnos!
update energiatodistus set data = :data :: JSONB where tila = 'luonnos' and id = :id

-- name: update-energiatodistus-as-valmis!
update energiatodistus set tila = 'valmis' where id = :id

-- name: delete-energiatodistus-when-luonnos!
delete from energiatodistus where tila = 'luonnos' and id = :id

-- name: select-energiatodistus
select id, tila, laatija_id "laatija-id", data from energiatodistus where id = :id

-- name: select-all-energiatodistukset
select id, tila, laatija_id "laatija-id", data from energiatodistus

-- name: select-all-luonnos-energiatodistukset
select id, tila, laatija_id "laatija-id", data from energiatodistus where tila = 'luonnos'

-- name: select-kayttotarkoitusluokat-by-versio
select id, label_fi "label-fi", label_sv "label-sv", deleted
from kayttotarkoitusluokka where versio = :versio
order by ordinal asc

-- name: select-alakayttotarkoitusluokat-by-versio
select id, kayttotarkoitusluokka_id "kayttotarkoitusluokka-id", label_fi "label-fi", label_sv "label-sv", deleted
from alakayttotarkoitusluokka where versio = :versio
order by ordinal asc
