
-- name: select-valvonta
select valvonta active from energiatodistus where id = :id

-- name: update-valvonta!
update energiatodistus set valvonta = :active? where id = :id