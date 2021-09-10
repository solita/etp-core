insert into vk_toimenpidetype (id, label_fi, label_sv, ordinal)
values
(0,  'Valvonnan aloitus', 'Valvonnan aloitus (sv)', 1),
(1,  'Tietopyyntö', 'Tietopyyntö (sv)', 2),
(2,  'Tietopyyntö / Kehotus', 'Tietopyyntö / Kehotus (sv)', 3),
(3,  'Tietopyyntö / Varoitus', 'Tietopyyntö / Varoitus (sv)', 4),
(4, 'Käskypäätös', 'Käskypäätös (sv)', 5),
(5, 'Valvonnan lopetus', 'Valvonnan lopetus (sv)', 6)
on conflict (id) do update set
  label_fi = excluded.label_fi,
  label_sv = excluded.label_sv,
  ordinal = excluded.ordinal;