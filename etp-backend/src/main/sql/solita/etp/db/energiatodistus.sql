-- name: insert-energiatodistus<!
insert into energiatodistus (data) values (:data :: JSONB) returning id

-- name: update-energiatodistus-when-luonnos!
update energiatodistus set data = :data :: JSONB where tila = 'luonnos' and id = :id

-- name: update-energiatodistus-as-valmis!
update energiatodistus set tila = 'valmis' where id = :id

-- name: delete-energiatodistus-when-luonnos!
delete from energiatodistus where tila = 'luonnos' and id = :id

-- name: select-energiatodistus
select id, tila, data from energiatodistus where id = :id

-- name: select-all-energiatodistukset
select id, tila, data from energiatodistus

-- name: select-all-luonnos-energiatodistukset
select id, tila, data from energiatodistus where tila = 'luonnos'
