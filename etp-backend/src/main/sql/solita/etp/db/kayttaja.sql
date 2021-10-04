-- name: select-kayttaja
select id, etunimi, sukunimi, email, puhelin, passivoitu, rooli_id as rooli,
       login, ensitallennus, cognito_id as cognitoid,
       virtu$localid, virtu$organisaatio, henkilotunnus
from kayttaja where id = :id

-- name: select-kayttajat
select id, etunimi, sukunimi, email, puhelin, passivoitu, rooli_id as rooli,
  login, ensitallennus, cognito_id as cognitoid,
  virtu$localid, virtu$organisaatio, henkilotunnus
from kayttaja where rooli_id in (1, 2, 3)
