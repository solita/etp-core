insert into vk_toimenpidetype (id, label_fi, label_sv, ordinal)
values
(0,  'Valvonnan aloitus', 'TODO', 1),
(1,  'Tietopyyntö', 'TODO', 2),
(2,  'Tietopyyntö / Kehotus', 'TODO', 3),
(3,  'Tietopyyntö / Varoitus', 'TODO', 4),
(4, 'Käskypäätös', 'TODO', 5),
(5, 'Valvonnan lopetus', 'TODO', 6)
on conflict (id) do update set
  label_fi = excluded.label_fi,
  label_sv = excluded.label_sv,
  ordinal = excluded.ordinal;