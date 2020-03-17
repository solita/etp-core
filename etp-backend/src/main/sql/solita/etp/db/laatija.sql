-- name: insert-laatija<!
insert into laatija (data) values (:data :: JSONB) returning id

-- name: update-laatija!
update laatija set data = :data :: JSONB where id = :id

-- name: select-laatija
select id, data from laatija where id = :id

-- name: select-laatija-with-henkilotunnus
select id, data from laatija where data->> 'henkilotunnus' = :henkilotunnus