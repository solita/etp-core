-- name: update-login!
UPDATE kayttaja SET login = now(), cognito_id = :cognitoid WHERE id = :id

-- name: select-kayttaja
select id, etunimi, sukunimi, email, puhelin, passivoitu, rooli, login, ensitallennus, cognito_id as cognitoid from kayttaja where id = :id
