
-- name: insert-liite<!
insert into liite (data) values (:data :: JSONB)
returning id

-- name: select-liite
select liite.nimi, liite.contenttype from liite
where id = :id

-- name: select-liite-by-energiatodistus-id
select distinct on (a.id) l.id, a.modifytime createtime,
  fullname(k.*) "author-fullname", l.nimi, l.contenttype, l.url
from liite l
inner join audit.liite a on l.id = a.id
inner join kayttaja k on a.modifiedby_id = k.id
where l.energiatodistus_id = :energiatodistus-id
order by a.id desc

-- name: delete-liite!
delete from liite where id = :id
