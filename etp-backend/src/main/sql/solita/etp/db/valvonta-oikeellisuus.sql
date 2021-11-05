
-- name: select-valvonnat-paakayttaja
select
  energiatodistus.*,
  fullname(kayttaja) "laatija-fullname",
  korvaava_energiatodistus.id as korvaava_energiatodistus_id,
  coalesce(last_toimenpide.ongoing, false) valvonta$ongoing,
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
  left join vo_last_toimenpide_v1 last_toimenpide on last_toimenpide.energiatodistus_id = energiatodistus.id
where
  (energiatodistus.valvonta$pending or
    last_toimenpide.ongoing or
    (:include-closed and last_toimenpide.id is not null)) and
  (energiatodistus.valvonta$valvoja_id = :valvoja-id or
    (energiatodistus.valvonta$valvoja_id is not null) = :has-valvoja or
    (:valvoja-id::int is null and :has-valvoja::boolean is null))
order by
  case when last_toimenpide.id is null then energiatodistus.id end desc nulls last,
  case when last_toimenpide.type_id in (4, 8, 12) then last_toimenpide.publish_time end desc nulls last,
  last_toimenpide$deadline_date asc nulls last,
  coalesce(last_toimenpide.publish_time, last_toimenpide.create_time) desc
limit :limit offset :offset;

-- name: select-valvonnat-laatija
select
  energiatodistus.*,
  fullname(kayttaja) "laatija-fullname",
  korvaava_energiatodistus.id as korvaava_energiatodistus_id,
  coalesce(last_toimenpide.ongoing, false) valvonta$ongoing,
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
  left join vo_last_toimenpide_v1 last_toimenpide on last_toimenpide.energiatodistus_id = energiatodistus.id
where energiatodistus.laatija_id = :whoami-id and
      last_toimenpide.visible_laatija
order by coalesce(last_toimenpide.publish_time, last_toimenpide.create_time) desc
limit :limit offset :offset;

-- name: count-valvonnat-paakayttaja
select count(*)
from energiatodistus
  left join vo_last_toimenpide_v1 last_toimenpide on last_toimenpide.energiatodistus_id = energiatodistus.id
where
  (energiatodistus.valvonta$pending or
    last_toimenpide.ongoing or
    (:include-closed and last_toimenpide.id is not null)) and
  (energiatodistus.valvonta$valvoja_id = :valvoja-id or
    (energiatodistus.valvonta$valvoja_id is not null) = :has-valvoja or
    (:valvoja-id::int is null and :has-valvoja::boolean is null));

-- name: count-valvonnat-laatija
select count(*)
from energiatodistus
  left join vo_last_toimenpide_v1 last_toimenpide on last_toimenpide.energiatodistus_id = energiatodistus.id
where energiatodistus.laatija_id = :whoami-id and
      last_toimenpide.visible_laatija;

-- name: count-unfinished-valvonnat-paakayttaja
select count(*)
from energiatodistus
  left join vo_last_toimenpide_v1 last_toimenpide on last_toimenpide.energiatodistus_id = energiatodistus.id
where energiatodistus.valvonta$valvoja_id = :whoami-id and not last_toimenpide.unfinished_laatija and
  (energiatodistus.valvonta$pending or last_toimenpide.ongoing);

-- name: count-unfinished-valvonnat-laatija
select count(*)
from energiatodistus
  left join vo_last_toimenpide_v1 last_toimenpide on last_toimenpide.energiatodistus_id = energiatodistus.id
where energiatodistus.laatija_id = :whoami-id and last_toimenpide.unfinished_laatija and last_toimenpide.visible_laatija;

-- name: select-valvonta
select
  energiatodistus.id,
  energiatodistus.valvonta$pending pending,
  energiatodistus.valvonta$valvoja_id valvoja_id,
  coalesce(vo_toimenpide_ongoing(last_toimenpide), false) ongoing
from energiatodistus left join lateral (
  select * from vo_toimenpide toimenpide
  where energiatodistus.id = toimenpide.energiatodistus_id
  order by coalesce(toimenpide.publish_time, toimenpide.create_time) desc
  limit 1) last_toimenpide on true
where energiatodistus.id = :id;

--name: select-last-diaarinumero
select diaarinumero from vo_toimenpide
where energiatodistus_id = :id and diaarinumero is not null and deleted is false
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
where toimenpide.id = :id and toimenpide.deleted is false;

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
      (:paakayttaja? or etp.vo_toimenpide_visible_laatija(toimenpide)) and
      toimenpide.deleted is false;

-- name: select-energiatodistus-valvonta-documents
select distinct on (type_id) *
from vo_toimenpide
where energiatodistus_id = :energiatodistus-id and type_id in (3, 5, 7, 9) and publish_time is not null
      and deleted is false
order by type_id, publish_time desc;

-- name: select-templates
select id, toimenpidetype_id, label_fi, label_sv, valid, language from vo_template;

-- name: select-template
select id, toimenpidetype_id, label_fi, label_sv, valid, language, content
from vo_template
where id = :id;

-- name: select-virhetypes
select id, ordinal,
       label_fi, label_sv, valid,
       description_fi, description_sv
from vo_virhetype
order by ordinal asc;

-- name: insert-virhetype<!
insert into vo_virhetype (id, ordinal, valid,
                          label_fi, label_sv,
                          description_fi, description_sv)
values ((select coalesce(max(id) + 1, 1) from vo_virhetype),
        :ordinal, :valid,
        :label-fi, :label-sv,
        :description-fi, :description-sv)
returning id;

-- name: update-virhetype!
update vo_virhetype set
  ordinal = case when :ordinal > ordinal then :ordinal + 1 else :ordinal end,
  valid = :valid,
  label_fi = :label-fi, label_sv = :label-sv,
  description_fi = :description-fi, description_sv = :description-sv
where id = :id;

-- name: order-virhetypes!
update vo_virhetype
set ordinal = ordered.ordinal
from (
  select type.id,
    row_number() over (order by type.ordinal asc, nullif(type.id, :id) desc nulls first) ordinal
  from  vo_virhetype type
) as ordered
where vo_virhetype.id = ordered.id and vo_virhetype.ordinal <> ordered.ordinal;

-- name: update-toimenpide-published!
update vo_toimenpide set publish_time = transaction_timestamp() where id = :id and deleted is false;

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

-- name: select-toimenpide-tiedoksi
select name, email from vo_tiedoksi where toimenpide_id = :toimenpide-id;

-- name: delete-toimenpide-tiedoksi!
delete from vo_tiedoksi where toimenpide_id = :toimenpide-id;

-- name: delete-draft-toimenpide!
update vo_toimenpide set deleted = true where id = :toimenpide-id and publish_time is null;

-- name: update-default-valvoja!
update energiatodistus set valvonta$valvoja_id = :whoami-id where id = :id and valvonta$valvoja_id is null;
