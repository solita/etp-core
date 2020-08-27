-- name: select-whoami
SELECT k.id id, k.etunimi etunimi, k.sukunimi sukunimi, k.email email, k.rooli rooli, k.cognito_id cognitoid, k.virtu_id virtuid, k.virtu_organisaatio virtuorganisaatio,
       l.henkilotunnus henkilotunnus
FROM kayttaja k
    LEFT JOIN laatija l ON k.id = l.id
WHERE (k.email IS NOT NULL AND k.email  = :email) OR
      (l.henkilotunnus IS NOT NULL AND l.henkilotunnus = :henkilotunnus) OR
      (k.cognito_id IS NOT NULL AND k.cognito_id = :cognitoid) OR
      ((k.virtu_id IS NOT NULL AND k.virtu_id = :virtuid) AND
       (k.virtu_organisaatio IS NOT NULL AND k.virtu_organisaatio = :virtuorganisaatio))

-- name: update-kayttaja-with-whoami!
UPDATE kayttaja SET login = now(), email = :email, cognito_id = :cognitoid, virtu_id = :virtuid, virtu_organisaatio = :virtuorganisaatio  WHERE id = :id
