-- name: insert-laatija<!
INSERT INTO laatija (kayttaja, henkilotunnus, patevyystaso, toteamispaivamaara, toteaja, jakeluosoite, postinumero, postitoimipaikka) VALUES (:kayttaja, :henkilotunnus, :patevyystaso, :toteamispaivamaara, :toteaja, :jakeluosoite, :postinumero, :postitoimipaikka) returning id

-- name: update-laatija!
UPDATE laatija SET patevyystaso = :patevyystaso, toteamispaivamaara = :toteamispaivamaara, toteaja = :toteaja, laatimiskielto = :laatimiskielto, toimintaalue = :toimintaalue, muut_toimintaalueet = array_remove(ARRAY[ :muuttoimintaalueet ] ::int[], NULL), julkinen_puhelin = :julkinenpuhelin, julkinen_email = :julkinenemail, julkinen_osoite = :julkinenosoite, jakeluosoite = :jakeluosoite, postinumero = :postinumero, postitoimipaikka = :postitoimipaikka, maa = :maa WHERE id = :id

--name: select-laatija-with-kayttaja
SELECT id, kayttaja, henkilotunnus, patevyystaso, toteamispaivamaara, toteaja, laatimiskielto, toimintaalue, muut_toimintaalueet as muuttoimintaalueet, julkinen_puhelin as julkinenpuhelin, julkinen_email as julkinenemail, julkinen_osoite as julkinenosoite, jakeluosoite, postinumero, postitoimipaikka, maa FROM laatija WHERE kayttaja = :kayttaja

--name: select-laatija-with-henkilotunnus
SELECT id, kayttaja, henkilotunnus, patevyystaso, toteamispaivamaara, toteaja, laatimiskielto, toimintaalue, muut_toimintaalueet as muuttoimintaalueet, julkinen_puhelin as julkinenpuhelin, julkinen_email as julkinenemail, julkinen_osoite as julkinenosoite, jakeluosoite, postinumero, postitoimipaikka, maa FROM laatija WHERE henkilotunnus = :henkilotunnus

-- name: select-laatija-yritykset
select yritys_id "yritys-id" from laatija_yritys where laatija_id = :id

-- name: insert-laatija-yritys!
insert into laatija_yritys (laatija_id, yritys_id)
values (:laatija-id, :yritys-id)
on conflict (laatija_id, yritys_id) do nothing

-- name: delete-laatija-yritys!
delete from laatija_yritys where laatija_id = :laatija-id and yritys_id = :yritys-id
