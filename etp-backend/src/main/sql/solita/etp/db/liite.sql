
-- name: insert-liite<!
insert into liite (data) values (:data :: JSONB)
returning id

-- name: select-liite
select liite.nimi, liite.contenttype from liite
where id = :id

-- name: select-liite-by-energiatodistus-id
select liite.id, liite.createtime, fullname(kayttaja.*) "author-fullname",
  liite.nimi, liite.contenttype, liite.url
from liite inner join kayttaja on kayttaja.id = liite.createdby_id
where energiatodistus_id = :energiatodistus-id
order by liite.createtime desc
