insert into vk_toimitustapa  (id, ordinal, label_fi, label_sv, description)
values
(0, 1, 'Suomi.fi', 'Suomi.fi', ''),
(1, 2, 'Sähköposti', 'Sähköposti (sv)', ''),
(2, 3, 'Muu, mikä?', 'Muu, mikä (sv)', '')
on conflict (id) do update set
  label_fi = excluded.label_fi,
  label_sv = excluded.label_sv,
  description = excluded.description,
  ordinal = excluded.ordinal;