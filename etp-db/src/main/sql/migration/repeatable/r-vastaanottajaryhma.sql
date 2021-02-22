insert into vastaanottajaryhma (id, label_fi, label_sv, ordinal)
values
(0, 'Valvojat', 'Valvojat', 1),
(1, 'Laatijat', 'Laatijat', 2)
on conflict (id) do update set
  label_fi = excluded.label_fi,
  label_sv = excluded.label_sv;