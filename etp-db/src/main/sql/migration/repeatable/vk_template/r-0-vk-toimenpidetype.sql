insert into vk_toimenpidetype (id, label_fi, label_sv, ordinal)
values
(0, 'Valvonnan aloitus', 'Valvonnan aloitus (sv)', 1),
(1, 'Tietopyyntö', 'Begäran om uppgifter', 2),
(2, 'Kehotus', ' Uppmaning', 3),
(3, 'Varoitus', 'Varning', 4),
(4, 'Käskypäätös', 'Käskypäätös (sv)', 5),
(5, 'Valvonnan lopetus', 'Valvonnan lopetus (sv)', 6)
on conflict (id) do update set
  label_fi = excluded.label_fi,
  label_sv = excluded.label_sv,
  ordinal = excluded.ordinal;