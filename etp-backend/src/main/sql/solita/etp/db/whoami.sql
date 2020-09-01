-- name: select-whoami
SELECT k.id id, k.etunimi etunimi, k.sukunimi sukunimi, k.email email, k.rooli rooli, k.cognito_id cognitoid, k.virtu_localid virtulocalid, k.virtu_organisaatio virtuorganisaatio,
       k.henkilotunnus henkilotunnus
FROM kayttaja k
WHERE (k.email IS NOT NULL AND k.email  = :email) OR
      (k.henkilotunnus IS NOT NULL AND k.henkilotunnus = :henkilotunnus) OR
      (k.cognito_id IS NOT NULL AND k.cognito_id = :cognitoid) OR
      ((k.virtu_localid IS NOT NULL AND k.virtu_localid = :virtulocalid) AND
       (k.virtu_organisaatio IS NOT NULL AND k.virtu_organisaatio = :virtuorganisaatio))

-- name: update-kayttaja-with-whoami!
UPDATE kayttaja SET login = now(), email = :email, cognito_id = :cognitoid, virtu_localid = :virtulocalid, virtu_organisaatio = :virtuorganisaatio  WHERE id = :id
