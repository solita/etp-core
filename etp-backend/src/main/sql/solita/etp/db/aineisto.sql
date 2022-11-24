-- name: select-kayttaja-aineistot
select
  aineisto_id, valid_until, ip_address
from kayttaja_aineisto
where kayttaja_id = :kayttaja-id and
      (:ip-address)::inet <<= ip_address::inet and
      valid_until > now();

-- name: insert-kayttaja-aineisto!
insert into kayttaja_aineisto (kayttaja_id, aineisto_id, valid_until, ip_address)
values (:kayttaja-id, :aineisto-id, :valid-until, ((:ip-address)::inet)::text)
on conflict (kayttaja_id, aineisto_id) do update set
  valid_until = excluded.valid_until,
  ip_address = excluded.ip_address;

-- name: delete-kayttaja-aineisto!
delete from kayttaja_aineisto
where kayttaja_id = :kayttaja-id and aineisto_id = :aineisto-id;
