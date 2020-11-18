create or replace function patevyys_paattymisaika(l laatija) returns timestamp as $$
begin
  return l.toteamispaivamaara + interval '7 years';
end;
$$ language plpgsql immutable;

create or replace function patevyys_voimassa(l laatija) returns boolean as $$
begin
  return current_date between l.toteamispaivamaara and patevyys_paattymisaika(l);
end;
$$ language plpgsql immutable;