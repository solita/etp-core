-- name: insert-yritys<!
insert into yritys (ytunnus, data) values (:ytunnus, :data :: JSONB) returning id

-- name: update-yritys!
update yritys set data = :data :: JSONB where id = :id

-- name: select-yritys
select id, ytunnus, data from yritys where id = :id

-- name: select-all-yritykset
select id, ytunnus, data from yritys
