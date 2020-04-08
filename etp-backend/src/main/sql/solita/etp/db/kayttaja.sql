-- name: select-kayttaja
select id, etunimi, sukunimi, email, puhelin, passivoitu, rooli, login, ensitallennus, cognito_id as cognitoid from kayttaja where id = :id

-- name: update-kayttaja-with-whoami!
UPDATE kayttaja SET login = now(), email = :email, cognito_id = :cognitoid WHERE id = :id
