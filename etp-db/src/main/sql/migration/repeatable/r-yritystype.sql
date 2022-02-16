insert into yritystype (id, label_fi, label_sv, ordinal)
values
(1, 'Elinkeinoelämä (03)', 'Elinkeinoelämä (03) (sv)', 2),
(2, 'Paikallishallinto (01)', 'Paikallishallinto (01) (sv)', 3)
on conflict (id) do update set
  label_fi = excluded.label_fi,
  label_sv = excluded.label_sv,
  ordinal = excluded.ordinal;
