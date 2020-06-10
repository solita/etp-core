-- name: select-whoami
SELECT k.id id, k.etunimi etunimi, k.sukunimi sukunimi, k.email email, k.rooli rooli, k.cognito_id cognitoid, l.henkilotunnus henkilotunnus FROM kayttaja k LEFT JOIN laatija l ON k.id = l.id WHERE (k.email IS NOT NULL AND k.email  = :email) OR (l.henkilotunnus IS NOT NULL AND l.henkilotunnus = :henkilotunnus) OR (k.cognito_id IS NOT NULL AND k.cognito_id = :cognitoid)

-- name: update-kayttaja-with-whoami!
UPDATE kayttaja SET login = now(), email = :email, cognito_id = :cognitoid WHERE id = :id
