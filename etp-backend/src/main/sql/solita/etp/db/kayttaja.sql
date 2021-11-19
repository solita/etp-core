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

-- name: select-kayttaja-history
select k.id,
       k.etunimi,
       k.sukunimi,
       k.email,
       k.puhelin,
       k.passivoitu,
       k.rooli_id as rooli,
       k.ensitallennus,
       k.cognito_id as cognitoid,
       --k.virtu$localid,
       --k.virtu$organisaatio,
       k.henkilotunnus,
       k.modifytime,
       fullname(modifier) modifiedby_name
from
  audit.kayttaja k
  join kayttaja modifier on k.modifiedby_id = modifier.id
where k.id = :id
order by modifytime, event_id;
