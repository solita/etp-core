
-- name: insert-yritys!
insert into yritys (data) values (:data :: JSONB) returning id

-- name: select-yritys
select id, data from yritys where id = :id

-- name: insert-laskutusosoite<!
insert into laskutusosoite (yritysid, data) values (:yritysid, :data :: JSONB) returning id

-- name: select-laskutusosoitteet
select id, yritysid, data from laskutusosoite
where yritysid = :yritysid
