-- name: select-whoami
SELECT k.id id, k.etunimi etunimi, k.sukunimi sukunimi, k.email email, k.rooli rooli, k.cognito_id cognitoid, l.henkilotunnus henkilotunnus, l.id laatija FROM kayttaja k LEFT JOIN laatija l ON k.id = l.kayttaja WHERE email = :email OR (cognito_id IS NOT NULL AND cognito_id = :cognitoid)
