
create unique index if not exists yritys_ytunnus_nimi_key on yritys (ytunnus, nimi) where (not deleted);
