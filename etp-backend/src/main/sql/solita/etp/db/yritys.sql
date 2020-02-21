
-- name: insert-yritys<!
insert into yritys (data) values (:data :: JSONB) returning id

-- name: update-yritys!
update yritys set data = :data :: JSONB where id = :id

-- name: select-yritys
select id, data from yritys where id = :id

