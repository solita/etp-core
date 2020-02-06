
-- name: insert-yritys!
insert into yritys (data) values (:data :: JSONB)

-- name: select-yritys
select id, data from yritys where id = :id