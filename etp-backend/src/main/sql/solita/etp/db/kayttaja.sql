-- name: select-kayttaja
select id, etunimi, sukunimi, email, puhelin, passivoitu, rooli, login, ensitallennus, cognito_id as cognitoid, virtu_id as virtuid, virtu_organisaatio as virtuorganisaatio, henkilotunnus from kayttaja where id = :id
