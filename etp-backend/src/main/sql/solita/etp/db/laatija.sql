--name: select-laatijat
SELECT k.id,
       k.etunimi,
       k.sukunimi,
       k.puhelin,
       k.email,
       k.ensitallennus,
       k.henkilotunnus,
       k.login,
       l.patevyystaso,
       l.toteamispaivamaara,
       patevyys_paattymisaika(l) voimassaolo_paattymisaika,
       patevyys_voimassa(l) as voimassa,
       l.wwwosoite,
       l.toimintaalue,
       l.muut_toimintaalueet muuttoimintaalueet,
       l.jakeluosoite,
       l.postinumero,
       l.postitoimipaikka,
       l.maa,
       l.laatimiskielto,
       l.julkinen_email julkinenemail,
       l.julkinen_puhelin julkinenpuhelin,
       l.julkinen_wwwosoite julkinenwwwosoite,
       l.julkinen_osoite julkinenosoite,
       array(select yritys_id from laatija_yritys where laatija_id = l.id) as yritys,
       coalesce(current_timestamp < login + interval '6 month', false) as aktiivinen
FROM laatija l
INNER JOIN kayttaja k
ON l.id = k.id
ORDER BY k.sukunimi, k.etunimi

--name: select-laatija-by-id
select
  l.id, l.patevyystaso,
  l.toteamispaivamaara, l.toteaja, l.laatimiskielto,
  patevyys_paattymisaika(l) voimassaolo_paattymisaika,
  patevyys_voimassa(l) as voimassa,
  l.toimintaalue, l.muut_toimintaalueet as muuttoimintaalueet,
  l.julkinen_puhelin as julkinenpuhelin,
  l.julkinen_email as julkinenemail,
  l.julkinen_osoite as julkinenosoite,
  l.julkinen_wwwosoite as julkinenwwwosoite,
  l.laskutuskieli,
  l.vastaanottajan_tarkenne, l.jakeluosoite,
  l.postinumero, l.postitoimipaikka, l.wwwosoite, l.maa
from laatija l where l.id = :id

--name: select-laatija-by-henkilotunnus
SELECT l.id, k.henkilotunnus, l.patevyystaso,
       l.toteamispaivamaara, l.toteaja, l.laatimiskielto,
       l.toimintaalue, l.muut_toimintaalueet as muuttoimintaalueet,
       l.julkinen_puhelin as julkinenpuhelin, l.julkinen_email as julkinenemail, l.julkinen_osoite as julkinenosoite, l.julkinen_wwwosoite as julkinenwwwosoite,
       l.laskutuskieli, l.vastaanottajan_tarkenne,
       l.jakeluosoite, l.postinumero, l.postitoimipaikka, l.wwwosoite, l.maa
FROM laatija l INNER JOIN kayttaja k ON l.id = k.id WHERE k.henkilotunnus = :henkilotunnus

-- name: select-laatija-yritykset
select distinct on (laatija_yritys.yritys_id)
    laatija_yritys.yritys_id id,
    laatija_yritys.tila_id,
    audit.modifytime, fullname(modifier) modifiedby_name
  from laatija_yritys
  left join audit.laatija_yritys audit
    on audit.yritys_id = laatija_yritys.yritys_id and
       audit.laatija_id = laatija_yritys.laatija_id
  left join kayttaja modifier on modifier.id = audit.modifiedby_id
where laatija_yritys.laatija_id = :id
order by laatija_yritys.yritys_id, audit.modifytime desc, audit.event_id desc

-- name: insert-laatija-yritys!
insert into laatija_yritys (laatija_id, yritys_id)
values (:laatija-id, :yritys-id)
on conflict (laatija_id, yritys_id) do update set tila_id = 0

-- name: delete-laatija-yritys!
update laatija_yritys set tila_id = 2
where laatija_id = :laatija-id and yritys_id = :yritys-id

-- name: select-laatija-laskutusosoitteet
select
  -1 id, null ytunnus, k.etunimi || ' ' || k.sukunimi nimi,
  l.vastaanottajan_tarkenne, l.jakeluosoite,
  l.postinumero, l.postitoimipaikka, l.maa,
  null verkkolaskuoperaattori, null verkkolaskuosoite
from laatija l inner join kayttaja k on l.id = k.id
where l.id = :id
union all
select
  y.id, y.ytunnus, y.nimi,
  y.vastaanottajan_tarkenne, y.jakeluosoite,
  y.postinumero, y.postitoimipaikka, y.maa,
  y.verkkolaskuoperaattori, y.verkkolaskuosoite
from yritys y
where exists (
  select 1 from laatija_yritys
  where laatija_yritys.laatija_id = :id and
        laatija_yritys.yritys_id = y.id and
        laatija_yritys.tila_id = 1);
