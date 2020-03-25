-- name: insert-kayttaja<!
INSERT INTO kayttaja (etunimi, sukunimi, email, puhelin) VALUES (:etunimi, :sukunimi, :email, :puhelin) RETURNING id

-- name: update-kayttaja!
UPDATE kayttaja SET etunimi = :etunimi, sukunimi = :sukunimi, email = :email, puhelin = :puhelin, passivoitu = :passivoitu, rooli = :rooli WHERE id = :id

-- name: select-kayttaja
select id, etunimi, sukunimi, email, puhelin, passivoitu, rooli, login, ensitallennus, cognito_id as cognitoid from kayttaja where id = :id
