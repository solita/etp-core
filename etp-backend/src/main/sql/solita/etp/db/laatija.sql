--name: select-laatijat
SELECT k.id,
       k.etunimi,
       k.sukunimi,
       k.puhelin,
       k.email,
       k.ensitallennus,
       k.henkilotunnus,
       l.patevyystaso,
       l.toteamispaivamaara,
       l.toimintaalue,
       l.postinumero,
       l.postitoimipaikka,
       l.laatimiskielto,
       array(select yritys_id from laatija_yritys where laatija_id = l.id) as yritys,
       current_date between l.toteamispaivamaara and l.toteamispaivamaara + interval '7 year' as voimassa
FROM laatija l
INNER JOIN kayttaja k
ON l.id = k.id
ORDER BY k.sukunimi, k.etunimi

--name: select-laatija-by-id
SELECT l.id, k.henkilotunnus, l.patevyystaso, l.toteamispaivamaara, l.toteaja, l.laatimiskielto, l.toimintaalue, l.muut_toimintaalueet as muuttoimintaalueet, l.julkinen_puhelin as julkinenpuhelin, l.julkinen_email as julkinenemail, l.julkinen_osoite as julkinenosoite, l.julkinen_wwwosoite as julkinenwwwosoite, l.laskutuskieli, l.jakeluosoite, l.vastaanottajan_tarkenne as "vastaanottajan-tarkenne", l.postinumero, l.postitoimipaikka, l.wwwosoite, l.maa
FROM laatija l INNER JOIN kayttaja k ON l.id = k.id WHERE l.id = :id

--name: select-laatija-with-henkilotunnus
SELECT l.id, k.henkilotunnus, l.patevyystaso, l.toteamispaivamaara, l.toteaja, l.laatimiskielto, l.toimintaalue, l.muut_toimintaalueet as muuttoimintaalueet, l.julkinen_puhelin as julkinenpuhelin, l.julkinen_email as julkinenemail, l.julkinen_osoite as julkinenosoite, l.julkinen_wwwosoite as julkinenwwwosoite, l.laskutuskieli, l.jakeluosoite, l.vastaanottajan_tarkenne as "vastaanottajan-tarkenne", l.postinumero, l.postitoimipaikka, l.wwwosoite, l.maa
FROM laatija l INNER JOIN kayttaja k ON l.id = k.id WHERE k.henkilotunnus = :henkilotunnus

-- name: select-laatija-yritykset
select yritys_id "yritys-id" from laatija_yritys where laatija_id = :id

-- name: insert-laatija-yritys!
insert into laatija_yritys (laatija_id, yritys_id)
values (:laatija-id, :yritys-id)
on conflict (laatija_id, yritys_id) do nothing

-- name: delete-laatija-yritys!
delete from laatija_yritys where laatija_id = :laatija-id and yritys_id = :yritys-id
