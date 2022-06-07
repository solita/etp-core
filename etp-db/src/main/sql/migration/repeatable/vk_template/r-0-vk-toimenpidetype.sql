insert into vk_toimenpidetype (id, label_fi, label_sv, ordinal, valid)
values
(0, 'Valvonnan aloitus', 'Valvonnan aloitus (sv)', 1, true),
(1, 'Tietopyyntö 2021', 'Begäran om uppgifter 2021', 2, true),
(2, 'Kehotus', ' Uppmaning', 3, true),
(3, 'Varoitus', 'Varning', 4, true),
(4, 'Käskypäätös', 'Käskypäätös (sv)', 5, true),
(5, 'Valvonnan lopetus', 'Valvonnan lopetus (sv)', 6, true)
on conflict (id) do update set
  label_fi = excluded.label_fi,
  label_sv = excluded.label_sv,
  ordinal = excluded.ordinal,
  valid = excluded.valid;
