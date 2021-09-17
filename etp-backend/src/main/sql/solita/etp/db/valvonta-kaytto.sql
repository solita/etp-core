-- name: select-valvonnat
select
  valvonta.id,
  rakennustunnus, katuosoite, postinumero,
  ilmoituspaikka_id, ilmoituspaikka_description, ilmoitustunnus,
  havaintopaiva, valvoja_id,
  last_toimenpide.id      last_toimenpide$id,
  last_toimenpide.type_id last_toimenpide$type_id,
  last_toimenpide.create_time last_toimenpide$create_time,
  last_toimenpide.publish_time last_toimenpide$publish_time,
  last_toimenpide.deadline_date last_toimenpide$deadline_date,
  last_toimenpide.diaarinumero last_toimenpide$diaarinumero,
  energiatodistus.id energiatodistus$id
from vk_valvonta valvonta
  left join lateral (
    select energiatodistus.*
    from energiatodistus
    where energiatodistus.pt$rakennustunnus = valvonta.rakennustunnus and
          energiatodistus.tila_id in (0, 1, 2) and
          coalesce(energiatodistus.voimassaolo_paattymisaika > transaction_timestamp(), true)
    order by energiatodistus.id desc
    limit 1) energiatodistus on true
  left join lateral (
    select toimenpide.*
    from vk_toimenpide toimenpide
    where valvonta.id = toimenpide.valvonta_id
    order by coalesce(toimenpide.publish_time, toimenpide.create_time) desc
    limit 1) last_toimenpide on true
where
  (last_toimenpide.id is null or last_toimenpide.type_id <> 5 or :include-closed) and
  (valvonta.valvoja_id = :valvoja-id or
    (valvonta.valvoja_id is not null) = :has-valvoja or
    (:valvoja-id::int is null and :has-valvoja::boolean is null))
order by
  last_toimenpide$deadline_date asc nulls last,
  coalesce(last_toimenpide.publish_time, last_toimenpide.create_time) desc,
  valvonta.id desc
limit :limit offset :offset;

-- name: select-valvonnat-count
select count(*) from vk_valvonta valvonta
left join lateral (
  select toimenpide.*
  from vk_toimenpide toimenpide
  where valvonta.id = toimenpide.valvonta_id
  order by coalesce(toimenpide.publish_time, toimenpide.create_time) desc
  limit 1) last_toimenpide on true
where
  (last_toimenpide.id is null or last_toimenpide.type_id <> 5 or :include-closed) and
  (valvonta.valvoja_id = :valvoja-id or
   (valvonta.valvoja_id is not null) = :has-valvoja or
   (:valvoja-id::int is null and :has-valvoja::boolean is null));

-- name: delete-valvonta!
update vk_valvonta set deleted = true where id = :id;

-- name: select-valvonta
select id,
       rakennustunnus,
       katuosoite,
       postinumero,
       ilmoituspaikka_id,
       ilmoituspaikka_description,
       ilmoitustunnus,
       havaintopaiva,
       valvoja_id
from vk_valvonta
where id = :id
  and deleted is false

--name: select-henkilot
select id,
       etunimi,
       sukunimi,
       henkilotunnus,
       email,
       puhelin,
       jakeluosoite,
       vastaanottajan_tarkenne,
       postinumero,
       postitoimipaikka,
       maa,
       rooli_id,
       rooli_description,
       toimitustapa_id,
       toimitustapa_description,
       valvonta_id
from vk_henkilo
where valvonta_id = :valvonta-id
  and deleted is false;

--name: select-henkilo
select id,
       etunimi,
       sukunimi,
       henkilotunnus,
       email,
       puhelin,
       jakeluosoite,
       vastaanottajan_tarkenne,
       postinumero,
       postitoimipaikka,
       maa,
       rooli_id,
       rooli_description,
       toimitustapa_id,
       toimitustapa_description,
       valvonta_id
from vk_henkilo
where id = :id
  and deleted is false;

-- name: delete-henkilo!
update vk_henkilo set deleted = true where id = :id;

--name: select-yritykset
select id,
       nimi,
       ytunnus,
       email,
       puhelin,
       jakeluosoite,
       vastaanottajan_tarkenne,
       postinumero,
       postitoimipaikka,
       maa,
       rooli_id,
       rooli_description,
       toimitustapa_id,
       toimitustapa_description,
       valvonta_id
from vk_yritys
where valvonta_id = :valvonta-id
  and deleted is false;

--name: select-yritys
select id,
       nimi,
       ytunnus,
       email,
       puhelin,
       jakeluosoite,
       vastaanottajan_tarkenne,
       postinumero,
       postitoimipaikka,
       maa,
       rooli_id,
       rooli_description,
       toimitustapa_id,
       toimitustapa_description,
       valvonta_id
from vk_yritys
where id = :id
  and deleted is false;

-- name: delete-yritys!
update vk_yritys set deleted = true where id = :id;

-- name: select-valvonta-documents
select distinct on (type_id) *
from vk_toimenpide
where valvonta_id = :valvonta-id and type_id in (1, 2, 3) and publish_time is not null
order by type_id, publish_time desc;

-- name: select-templates
select id, toimenpidetype_id, label_fi, label_sv, valid, language from vk_template;

-- name: select-template
select id, toimenpidetype_id, label_fi, label_sv, valid, language, content
from vk_template
where id = :id;

-- name: select-liite
select nimi, valvonta_id, contenttype, nimi as filename
from vk_valvonta_liite
where id = :id and deleted = false

-- name: select-liite-by-valvonta-id
select distinct on (l.id) l.id, a.modifytime createtime,
    fullname(k.*) "author-fullname", l.nimi, l.contenttype, l.url
from vk_valvonta_liite l
    inner join audit.vk_valvonta_liite a on l.id = a.id
    inner join kayttaja k on a.modifiedby_id = k.id
where l.valvonta_id = :valvonta-id and l.deleted = false
order by l.id, a.modifytime asc, a.event_id desc

-- name: delete-liite!
update vk_valvonta_liite set deleted = true where id = :id;

-- name: select-toimenpiteet
select
    toimenpide.id, toimenpide.type_id, toimenpide.valvonta_id,
    toimenpide.create_time, toimenpide.publish_time, toimenpide.deadline_date,
    toimenpide.template_id, toimenpide.diaarinumero, toimenpide.description,
    toimenpide.author_id author$id, author.rooli_id author$rooli_id,
    author.etunimi author$etunimi, author.sukunimi author$sukunimi
from vk_toimenpide toimenpide
         inner join kayttaja author on author.id = toimenpide.author_id
where toimenpide.valvonta_id = :valvonta-id;

-- name: select-toimenpide-henkilo
select
  id, etunimi, sukunimi, henkilotunnus,
  email, puhelin,
  vastaanottajan_tarkenne, jakeluosoite, postinumero, postitoimipaikka, maa,
  rooli_id, rooli_description,
  toimitustapa_id, toimitustapa_description,
  valvonta_id
from vk_toimenpide_henkilo henkilo
 inner join audit.vk_henkilo audit
   on henkilo.henkilo_id = audit.id and audit.event_id = henkilo.henkilo_versio
where henkilo.toimenpide_id = :toimenpide-id;

-- name: select-toimenpide-yritys
select
  id, nimi, ytunnus,
  email, puhelin,
  vastaanottajan_tarkenne, jakeluosoite, postinumero, postitoimipaikka, maa,
  rooli_id, rooli_description,
  toimitustapa_id, toimitustapa_description,
  valvonta_id
from vk_toimenpide_yritys yritys
  inner join audit.vk_yritys audit
    on yritys.yritys_id = audit.id and audit.event_id = yritys.yritys_versio
where yritys.toimenpide_id = :toimenpide-id;

-- name: select-last-diaarinumero
select diaarinumero from vk_toimenpide
where valvonta_id = :id and diaarinumero is not null
order by coalesce(publish_time, create_time) desc
limit 1;

--name: insert-toimenpide-henkilot!
insert into vk_toimenpide_henkilo (toimenpide_id, henkilo_id, henkilo_versio)
select distinct on (henkilo.id) :toimenpide-id, henkilo.id, audit.event_id
from etp.vk_henkilo henkilo
  inner join audit.vk_henkilo audit on henkilo.id = audit.id
where henkilo.valvonta_id = :valvonta-id and not henkilo.deleted
order by henkilo.id, audit.modifytime desc, audit.event_id desc;

--name: insert-toimenpide-yritykset!
insert into vk_toimenpide_yritys (toimenpide_id, yritys_id, yritys_versio)
select distinct on (yritys.id) :toimenpide-id, yritys.id, audit.event_id
from etp.vk_yritys yritys
     inner join audit.vk_yritys audit on yritys.id = audit.id
where yritys.valvonta_id = :valvonta-id and not yritys.deleted
order by yritys.id, audit.modifytime desc, audit.event_id desc;

-- name: select-valvonta-notes
select distinct on (note.id) note.id,
                             a.modifytime    create_time,
                             a.modifiedby_id author_id,
                             note.description
from etp.vk_note note
         inner join audit.vk_note a on note.id = a.id
where note.valvonta_id = :valvonta-id
  and note.deleted = false
order by note.id, a.modifytime asc, a.event_id desc