-- name: select-kayttaja
select id, etunimi, sukunimi, email, puhelin, passivoitu, rooli, login, ensitallennus, cognito_id as cognitoid from kayttaja where id = :id
