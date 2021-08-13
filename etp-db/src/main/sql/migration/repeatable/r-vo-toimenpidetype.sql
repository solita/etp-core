insert into vo_toimenpidetype (id, label_fi, label_sv, ordinal)
values
(0,  'Katsottu', 'TODO', 1),
(1,  'Poikkeama', 'TODO', 2),
(2,  'Valvonnan aloitus', 'TODO', 3),
(3,  'Tietopyyntö', 'TODO', 4),
(4,  'Tietopyyntö / Vastaus', 'TODO', 5),
(5,  'Tietopyyntö / Kehotus', 'TODO', 6),
(6,  'Tietopyyntö / Varoitus', 'TODO', 7),
(7,  'Valvontamuistio', 'TODO', 8),
(8,  'Valvontamuistio / Vastaus', 'TODO', 9),
(9,  'Valvontamuistio / Kehotus', 'TODO', 10),
(10, 'Valvontamuistio / Varoitus', 'TODO', 11),
(11, 'Lisäselvityspyyntö', 'TODO', 12),
(12, 'Lisäselvityspyyntö / Vastaus', 'TODO', 13),
(13, 'Kieltopäätös', 'TODO', 14),
(14, 'Valvonnan lopetus', 'TODO', 15)
on conflict (id) do update set
  label_fi = excluded.label_fi,
  label_sv = excluded.label_sv,
  ordinal = excluded.ordinal;