--name: select-laatijat
SELECT k.id,
       k.etunimi,
       k.sukunimi,
       k.puhelin,
       l.patevyystaso,
       l.toteamispaivamaara,
       l.toimintaalue,
       l.postinumero,
       array(select yritys_id from laatija_yritys where laatija_id = l.id) as yritys
FROM laatija l
INNER JOIN kayttaja k
ON l.id = k.id

--name: select-laatija-by-id
SELECT id, henkilotunnus, patevyystaso, toteamispaivamaara, toteaja, laatimiskielto, toimintaalue, muut_toimintaalueet as muuttoimintaalueet, julkinen_puhelin as julkinenpuhelin, julkinen_email as julkinenemail, julkinen_osoite as julkinenosoite, jakeluosoite, postinumero, postitoimipaikka, wwwosoite, maa FROM laatija WHERE id = :id

--name: select-laatija-with-henkilotunnus
SELECT id, henkilotunnus, patevyystaso, toteamispaivamaara, toteaja, laatimiskielto, toimintaalue, muut_toimintaalueet as muuttoimintaalueet, julkinen_puhelin as julkinenpuhelin, julkinen_email as julkinenemail, julkinen_osoite as julkinenosoite, jakeluosoite, postinumero, postitoimipaikka, wwwosoite, maa FROM laatija WHERE henkilotunnus = :henkilotunnus

-- name: select-laatija-yritykset
select yritys_id "yritys-id" from laatija_yritys where laatija_id = :id

-- name: insert-laatija-yritys!
insert into laatija_yritys (laatija_id, yritys_id)
values (:laatija-id, :yritys-id)
on conflict (laatija_id, yritys_id) do nothing

-- name: delete-laatija-yritys!
delete from laatija_yritys where laatija_id = :laatija-id and yritys_id = :yritys-id
