insert into rooli (id, label_fi, label_sv)
values
(-1, 'Järjestelmä', 'System'),
(0, 'Laatija', 'Laatija SV'),
(1, 'Pätevyyden toteaja', 'Pätevyyden toteaja SV'),
(2, 'Pääkäyttäjä', 'Pääkäyttäjä SV')
on conflict (id) do update set
  label_fi = excluded.label_fi,
  label_sv = excluded.label_sv
