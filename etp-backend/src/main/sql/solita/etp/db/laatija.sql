-- name: insert-laatija<!
insert into laatija (data) values (:data :: JSONB) returning id

-- name: update-laatija!
update laatija set data = :data :: JSONB where id = :id

-- name: select-laatija
select id, data from laatija where id = :id

-- name: select-laatija-with-henkilotunnus
select id, data from laatija where data->> 'henkilotunnus' = :henkilotunnus

-- name: select-laatijat
select id, data from laatija

-- name: select-laatija-yritykset
select yritys_id "yritys-id" from laatija_yritys where laatija_id = :id

-- name: insert-laatija-yritys!
insert into laatija_yritys (laatija_id, yritys_id)
values (:laatija-id, :yritys-id)
on conflict (laatija_id, yritys_id) do nothing