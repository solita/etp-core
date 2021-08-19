-- name: select-valvonnat
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
where valvoja_id = :valvoja-id
  and deleted is false
limit :limit
offset :offset;

-- name: select-valvonnat-count
select count(*)
from vk_valvonta
where valvoja_id = :valvoja-id and
      deleted is false
limit :limit
offset :offset;

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
update vk_henkilo set deleted = true where id = :id and valvonta_id = :valvonta-id;

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
update vk_yritys set deleted = true where id = :id and valvonta_id = :valvonta-id;

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
select liite.nimi, liite.valvonta_id, liite.contenttype from vk_valvonta_liite
where id = :id valvonta_id = :valvonta-id and deleted = false

-- name: select-liite-by-valvonta-id
select distinct on (l.id) l.id, a.modifytime createtime,
    fullname(k.*) "author-fullname", l.nimi, l.contenttype, l.url
from vk_valvonta_liite l
    inner join audit.vk_valvonta_liite a on l.id = a.id
    inner join kayttaja k on a.modifiedby_id = k.id
where l.valvonta_id = :valvonta-id and l.deleted = false
order by l.id, a.modifytime asc, a.event_id desc

-- name: delete-liite!
update vk_valvonta_liite set deleted = true where id = :id and valvonta_id = :valvonta-id;

-- name: select-toimenpide
select
    toimenpide.id, toimenpide.type_id, toimenpide.valvonta_id,
    toimenpide.create_time, toimenpide.publish_time, toimenpide.deadline_date,
    toimenpide.template_id, toimenpide.diaarinumero, toimenpide.description,
    toimenpide.author_id author$id, author.rooli_id author$rooli_id,
    author.etunimi author$etunimi, author.sukunimi author$sukunimi
from vk_toimenpide toimenpide
         inner join kayttaja author on author.id = toimenpide.author_id
where toimenpide.id = :id;

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
select henkilo_id
from vk_toimenpide_henkilo
where toimenpide_id = :toimenpide-id;

-- name: select-toimenpide-yritys
select yritys_id
from vk_toimenpide_yritys
where toimenpide_id = :toimenpide-id;