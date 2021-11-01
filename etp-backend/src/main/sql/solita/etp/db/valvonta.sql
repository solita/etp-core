
-- name: select-valvojat
select id, etunimi, sukunimi, rooli_id, passivoitu from kayttaja WHERE rooli_id = 2;

-- name: select-valvonta
select valvonta active from energiatodistus where id = :id

-- name: update-valvonta!
update energiatodistus set valvonta = :active? where id = :id
