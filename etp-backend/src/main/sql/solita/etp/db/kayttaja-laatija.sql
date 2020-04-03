-- name: select-whoami
SELECT k.id id, k.etunimi etunimi, k.sukunimi sukunimi, k.email email, k.rooli rooli, l.henkilotunnus henkilotunnus, l.id laatija FROM kayttaja k LEFT JOIN laatija l ON k.id = l.kayttaja WHERE email = :email
