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

insert into vo_template (id, label_fi, label_sv, ordinal, toimenpidetype_id, language)
values
(1, 'Taustamateriaalin toimituspyyntö FI', 'TODO', 1, 3, 'fi'),
(2, 'Taustamateriaalin kehotus FI', 'TODO', 2, 5, 'fi'),
(3, 'Taustamateriaalin varoitus FI', 'TODO', 3, 6, 'fi'),
(4, 'Valvontamuistio FI', 'TODO', 4, 7, 'fi'),
(5, 'Valvontamuistion kehotus FI', 'TODO', 5, 9, 'fi'),
(6, 'Valvontamuistion varoitus FI', 'TODO', 6, 10, 'fi')
on conflict (id) do update set
  label_fi = excluded.label_fi,
  label_sv = excluded.label_sv,
  ordinal = excluded.ordinal,
  toimenpidetype_id = excluded.toimenpidetype_id,
  language = excluded.language;