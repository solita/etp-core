-- name: select-whoami
SELECT k.id id, k.etunimi etunimi, k.sukunimi sukunimi, k.email email, k.rooli rooli, k.cognito_id cognitoid, k.virtu$localid, k.virtu$organisaatio,
       k.henkilotunnus henkilotunnus
FROM kayttaja k
WHERE (k.email IS NOT NULL AND k.email  = :email) OR
      (k.henkilotunnus IS NOT NULL AND k.henkilotunnus = :henkilotunnus) OR
      (k.cognito_id IS NOT NULL AND k.cognito_id = :cognitoid) OR
      ((k.virtu$localid IS NOT NULL AND k.virtu$localid = :virtu_localid) AND
       (k.virtu$organisaatio IS NOT NULL AND k.virtu$organisaatio = :virtu_organisaatio))

-- name: update-kayttaja-with-whoami!
UPDATE kayttaja SET login = now(), email = :email, cognito_id = :cognitoid, virtu$localid = :virtu_localid, virtu$organisaatio = :virtu_organisaatio  WHERE id = :id
