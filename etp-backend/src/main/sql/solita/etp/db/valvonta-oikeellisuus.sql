
-- name: select-valvonnat
select
  energiatodistus.*,
  fullname(kayttaja) "laatija-fullname",
  korvaava_energiatodistus.id as korvaava_energiatodistus_id,
  coalesce(last_toimenpide.type_id not in (0, 1, 15), false) valvonta$ongoing,
  last_toimenpide.id      last_toimenpide$id,
  last_toimenpide.type_id last_toimenpide$type_id,
  last_toimenpide.author_id last_toimenpide$author_id,
  last_toimenpide.energiatodistus_id last_toimenpide$energiatodistus_id,
  last_toimenpide.create_time last_toimenpide$create_time,
  last_toimenpide.publish_time last_toimenpide$publish_time,
  last_toimenpide.deadline_date last_toimenpide$deadline_date,
  last_toimenpide.template_id last_toimenpide$template_id,
  last_toimenpide.diaarinumero last_toimenpide$diaarinumero
from energiatodistus
  inner join kayttaja on kayttaja.id = energiatodistus.laatija_id
  left join energiatodistus korvaava_energiatodistus on korvaava_energiatodistus.korvattu_energiatodistus_id = energiatodistus.id
  left join lateral (
    select * from vo_toimenpide toimenpide
    where energiatodistus.id = toimenpide.energiatodistus_id
    order by coalesce(toimenpide.publish_time, toimenpide.create_time) desc
    limit 1) last_toimenpide on true
where energiatodistus.valvonta$pending or last_toimenpide.type_id not in (0, 1, 15)
order by coalesce(last_toimenpide.publish_time, last_toimenpide.create_time) desc
limit :limit offset :offset;

-- name: count-valvonnat
select count(*)
from energiatodistus left join lateral (
  select * from vo_toimenpide toimenpide
  where energiatodistus.id = toimenpide.energiatodistus_id
  order by coalesce(toimenpide.publish_time, toimenpide.create_time) desc
  limit 1) last_toimenpide on true
where valvonta$pending or last_toimenpide.type_id not in (0, 1, 15);

-- name: select-valvonta
select
  energiatodistus.id,
  energiatodistus.valvonta$pending pending,
  energiatodistus.valvonta$valvoja_id valvoja_id,
  coalesce(last_toimenpide.type_id not in (0, 1, 15), false) ongoing
from energiatodistus left join lateral (
  select * from vo_toimenpide toimenpide
  where energiatodistus.id = toimenpide.energiatodistus_id
  order by coalesce(toimenpide.publish_time, toimenpide.create_time) desc
  limit 1) last_toimenpide on true
where energiatodistus.id = :id;

--name: select-last-diaarinumero
select diaarinumero from vo_toimenpide
where energiatodistus_id = :id and diaarinumero is not null
order by coalesce(publish_time, create_time) desc
limit 1;

-- name: select-toimenpide
select * from vo_toimenpide where id = :id;

-- name: select-toimenpiteet
select * from vo_toimenpide where energiatodistus_id = :energiatodistus-id;

-- name: select-templates
select id, toimenpidetype_id, label_fi, label_sv, valid, language from vo_template;

-- name: select-virhetypes
select id, label_fi, label_sv, valid, description_fi, description_sv from vo_virhetype;

-- name: update-toimenpide-published!
update vo_toimenpide set publish_time = transaction_timestamp() where id = :id;

-- name: select-toimenpide-virheet
select type_id, description from vo_virhe where toimenpide_id = :toimenpide-id;

-- name: delete-toimenpide-virheet!
delete from vo_virhe where toimenpide_id = :toimenpide-id;