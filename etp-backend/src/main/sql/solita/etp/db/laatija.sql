-- name: insert-laatija<!
insert into laatija (data) values (:data :: JSONB) returning id

-- name: select-laatija
select id, data from laatija where id = :id
