-- name: select-all-sivut
select
  id,
  parent_id,
  ordinal,
  published,
  title
from sivu
where :paakayttaja or published;

-- name: select-sivu
select
  id,
  parent_id,
  ordinal,
  published,
  title,
  body
from sivu
where
  id = :id and
  (:paakayttaja or published);

-- name: insert-sivu<!
insert into sivu (title, body, parent_id, ordinal, published)
values (:title, :body, :parent-id, :ordinal, :published)
returning id;
