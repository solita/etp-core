
-- name: select-valvonnat-paakayttaja
select
  energiatodistus.*,
  fullname(kayttaja) "laatija-fullname",
  korvaava_energiatodistus.id as korvaava_energiatodistus_id,
  coalesce(last_toimenpide.type_id not in (0, 1, 15), false) valvonta$ongoing,
  last_toimenpide.id      last_toimenpide$id,
  last_toimenpide.type_id last_toimenpide$type_id,
  last_toimenpide.energiatodistus_id last_toimenpide$energiatodistus_id,
  last_toimenpide.create_time last_toimenpide$create_time,
  last_toimenpide.publish_time last_toimenpide$publish_time,
  case when (last_toimenpide.type_id in (4, 8, 12))
    then last_toimenpide.previous_deadline_date
    else last_toimenpide.deadline_date
  end last_toimenpide$deadline_date,
  last_toimenpide.template_id last_toimenpide$template_id,
  last_toimenpide.diaarinumero last_toimenpide$diaarinumero
from energiatodistus
  inner join kayttaja on kayttaja.id = energiatodistus.laatija_id
  left join energiatodistus korvaava_energiatodistus on korvaava_energiatodistus.korvattu_energiatodistus_id = energiatodistus.id
  left join lateral (
    select toimenpide.*,
      lag(toimenpide.deadline_date) over (order by toimenpide.id) previous_deadline_date
    from vo_toimenpide toimenpide
    where energiatodistus.id = toimenpide.energiatodistus_id
    order by coalesce(toimenpide.publish_time, toimenpide.create_time) desc
    limit 1) last_toimenpide on true
where
  (energiatodistus.valvonta$pending or
    last_toimenpide.type_id not in (0, 1, 15) or
    (:include-closed and last_toimenpide.id is not null)) and
  (energiatodistus.valvonta$valvoja_id = :valvoja-id or
    (energiatodistus.valvonta$valvoja_id is not null) = :has-valvoja or
    (:valvoja-id::int is null and :has-valvoja::boolean is null))
order by coalesce(last_toimenpide.publish_time, last_toimenpide.create_time) desc
limit :limit offset :offset;

-- name: select-valvonnat-laatija
select
  energiatodistus.*,
  fullname(kayttaja) "laatija-fullname",
  korvaava_energiatodistus.id as korvaava_energiatodistus_id,
  coalesce(last_toimenpide.type_id not in (0, 1, 15), false) valvonta$ongoing,
  last_toimenpide.id      last_toimenpide$id,
  last_toimenpide.type_id last_toimenpide$type_id,
  last_toimenpide.energiatodistus_id last_toimenpide$energiatodistus_id,
  last_toimenpide.create_time last_toimenpide$create_time,
  last_toimenpide.publish_time last_toimenpide$publish_time,
  case when (last_toimenpide.type_id in (4, 8, 12))
    then last_toimenpide.previous_deadline_date
    else last_toimenpide.deadline_date
  end last_toimenpide$deadline_date,
  last_toimenpide.template_id last_toimenpide$template_id,
  last_toimenpide.diaarinumero last_toimenpide$diaarinumero
from energiatodistus
  inner join kayttaja on kayttaja.id = energiatodistus.laatija_id
  left join energiatodistus korvaava_energiatodistus on korvaava_energiatodistus.korvattu_energiatodistus_id = energiatodistus.id
  left join lateral (
    select toimenpide.*,
      etp.vo_toimenpide_visible_laatija(toimenpide) visible_laatija,
      lag(toimenpide.deadline_date) over (order by toimenpide.id) previous_deadline_date
    from vo_toimenpide toimenpide
    where energiatodistus.id = toimenpide.energiatodistus_id
    order by coalesce(toimenpide.publish_time, toimenpide.create_time) desc
    limit 1) last_toimenpide on true
where energiatodistus.laatija_id = :laatija-id and
      last_toimenpide.visible_laatija
order by coalesce(last_toimenpide.publish_time, last_toimenpide.create_time) desc
limit :limit offset :offset;

-- name: count-valvonnat-paakayttaja
select count(*)
from energiatodistus left join lateral (
  select * from vo_toimenpide toimenpide
  where energiatodistus.id = toimenpide.energiatodistus_id
  order by coalesce(toimenpide.publish_time, toimenpide.create_time) desc
  limit 1) last_toimenpide on true
where
  (energiatodistus.valvonta$pending or
   last_toimenpide.type_id not in (0, 1, 15) or
   (:include-closed and last_toimenpide.id is not null)) and
  (energiatodistus.valvonta$valvoja_id = :valvoja-id or
   (energiatodistus.valvonta$valvoja_id is not null) = :has-valvoja or
   (:valvoja-id::int is null and :has-valvoja::boolean is null));

-- name: count-valvonnat-laatija
select count(*)
from energiatodistus left join lateral (
  select * from vo_toimenpide toimenpide
  where energiatodistus.id = toimenpide.energiatodistus_id
  order by coalesce(toimenpide.publish_time, toimenpide.create_time) desc
  limit 1) last_toimenpide on true
where energiatodistus.laatija_id = :laatija-id and
      etp.vo_toimenpide_visible_laatija(last_toimenpide);

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
select
  toimenpide.id, toimenpide.type_id, toimenpide.energiatodistus_id,
  toimenpide.create_time, toimenpide.publish_time, toimenpide.deadline_date,
  toimenpide.template_id, toimenpide.diaarinumero, toimenpide.description, toimenpide.severity_id,
  toimenpide.author_id author$id, author.rooli_id author$rooli_id,
  author.etunimi author$etunimi, author.sukunimi author$sukunimi
from vo_toimenpide toimenpide
  inner join kayttaja author on author.id = toimenpide.author_id
where toimenpide.id = :id;

-- name: select-toimenpiteet
select
  toimenpide.id, toimenpide.type_id, toimenpide.energiatodistus_id,
  toimenpide.create_time, toimenpide.publish_time, toimenpide.deadline_date,
  toimenpide.template_id, toimenpide.diaarinumero, toimenpide.description, toimenpide.severity_id,
  toimenpide.author_id author$id, author.rooli_id author$rooli_id,
  author.etunimi author$etunimi, author.sukunimi author$sukunimi
  from vo_toimenpide toimenpide
    inner join kayttaja author on author.id = toimenpide.author_id
where toimenpide.energiatodistus_id = :energiatodistus-id and
      (:paakayttaja? or etp.vo_toimenpide_visible_laatija(toimenpide));

-- name: select-energiatodistus-valvonta-documents
select distinct on (type_id) *
from vo_toimenpide
where energiatodistus_id = :energiatodistus-id and type_id in (3, 5, 7, 9) and publish_time is not null
order by type_id, publish_time desc;

-- name: select-templates
select id, toimenpidetype_id, label_fi, label_sv, valid, language from vo_template;

-- name: select-template
select id, toimenpidetype_id, label_fi, label_sv, valid, language, content
from vo_template
where id = :id;

-- name: select-virhetypes
select id, label_fi, label_sv, valid, description_fi, description_sv from vo_virhetype;

-- name: update-toimenpide-published!
update vo_toimenpide set publish_time = transaction_timestamp() where id = :id;

-- name: select-toimenpide-virheet
select type_id, description from vo_virhe where toimenpide_id = :toimenpide-id;

-- name: delete-toimenpide-virheet!
delete from vo_virhe where toimenpide_id = :toimenpide-id;

-- name: select-toimenpide-liitteet
select distinct on (l.id) l.id, a.modifytime createtime,
  fullname(k) "author-fullname", l.nimi, l.contenttype, l.url
from liite l
     inner join audit.liite a on l.id = a.id
     inner join kayttaja k on a.modifiedby_id = k.id
where l.vo_toimenpide_id = :toimenpide-id and l.deleted = false
order by l.id, a.modifytime asc, a.event_id desc

-- name: select-valvonta-notes
select distinct on (note.id) note.id,
  a.modifytime create_time, a.modifiedby_id author_id,
  note.description
from etp.vo_note note inner join audit.vo_note a on note.id = a.id
where note.energiatodistus_id = :energiatodistus-id and note.deleted = false
order by note.id, a.modifytime asc, a.event_id desc