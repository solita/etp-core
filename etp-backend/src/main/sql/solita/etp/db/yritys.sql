-- name: insert-yritys<!
INSERT INTO yritys (ytunnus, nimi, verkkolaskuosoite, jakeluosoite, postinumero, postitoimipaikka, maa)
VALUES (:ytunnus, :nimi, :verkkolaskuosoite, :jakeluosoite, :postinumero, :postitoimipaikka, :maa)
RETURNING id

-- name: update-yritys!
UPDATE yritys SET nimi = :nimi, verkkolaskuosoite = :verkkolaskuosoite, jakeluosoite = :jakeluosoite, postinumero = :postinumero, postitoimipaikka = :postitoimipaikka, maa = :maa
WHERE id = :id

-- name: select-yritys
SELECT id, ytunnus, nimi, verkkolaskuosoite, jakeluosoite, postinumero, postitoimipaikka, maa
FROM yritys
WHERE id = :id

-- name: select-all-yritykset
SELECT id, ytunnus, nimi, verkkolaskuosoite, jakeluosoite, postinumero, postitoimipaikka, maa
FROM yritys
