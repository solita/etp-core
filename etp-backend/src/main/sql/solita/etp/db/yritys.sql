-- name: insert-yritys<!
INSERT INTO yritys (ytunnus, nimi, verkkolaskuoperaattori, verkkolaskuosoite, laskutuskieli, jakeluosoite, vastaanottajan_tarkenne, postinumero, postitoimipaikka, maa)
VALUES (:ytunnus, :nimi, :verkkolaskuoperaattori, :verkkolaskuosoite, :laskutuskieli, :jakeluosoite, :vastaanottajan-tarkenne, :postinumero, :postitoimipaikka, :maa)
RETURNING id

-- name: update-yritys!
UPDATE yritys SET nimi = :nimi, verkkolaskuoperaattori = :verkkolaskuoperaattori, verkkolaskuosoite = :verkkolaskuosoite, laskutuskieli = :laskutuskieli, jakeluosoite = :jakeluosoite, vastaanottajan_tarkenne = :vastaanottajan-tarkenne, postinumero = :postinumero, postitoimipaikka = :postitoimipaikka, maa = :maa
WHERE id = :id

-- name: select-yritys
SELECT id, ytunnus, nimi, verkkolaskuoperaattori, verkkolaskuosoite, laskutuskieli, jakeluosoite, vastaanottajan_tarkenne as "vastaanottajan-tarkenne", postinumero, postitoimipaikka, maa
FROM yritys
WHERE id = :id

-- name: select-all-yritykset
SELECT id, ytunnus, nimi, verkkolaskuoperaattori, verkkolaskuosoite, laskutuskieli, jakeluosoite, vastaanottajan_tarkenne as "vastaanottajan-tarkenne", postinumero, postitoimipaikka, maa
FROM yritys

--name: select-all-laskutuskielet
SELECT id, label_fi as "label-fi", label_sv as "label-sv", valid FROM laskutuskieli;

-- name: select-all-verkkolaskuoperaattorit
SELECT id, valittajatunnus, nimi FROM verkkolaskuoperaattori;
