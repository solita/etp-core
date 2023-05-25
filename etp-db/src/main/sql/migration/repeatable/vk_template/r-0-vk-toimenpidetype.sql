insert into vk_toimenpidetype (id, label_fi, label_sv, ordinal, valid)
values
(0, 'Valvonnan aloitus', 'Valvonnan aloitus (sv)', 1, true),
(1, 'Tietopyyntö 2021', 'Begäran om uppgifter 2021', 2, false),
(2, 'Kehotus', ' Uppmaning', 3, true),
(3, 'Varoitus', 'Varning', 4, true),
(4, 'Käskypäätös', 'Käskypäätös (sv)', 5, true),
(5, 'Valvonnan lopetus', 'Valvonnan lopetus (sv)', 6, true),
(6, 'HaO käsittely', 'HaO käsittely (sv)', 7, true),
(7, 'Käskypäätös / kuulemiskirje', 'Käskypäätös / kuulemiskirje (sv)', 8, true),
(8, 'Käskypäätös / varsinainen päätös', 'Käskypäätös / varsinainen päätös (sv)', 9, true),
(9, 'Käskypäätös / tiedoksianto (ensimmäinen postitus)', 'Käskypäätös / tiedoksianto (ensimmäinen postitus) (sv)', 10, true),
(10, 'Käskypäätös / tiedoksianto (toinen postitus)', 'Käskypäätös / tiedoksianto (toinen postitus) (sv)', 11, true),
(11, 'Käskypäätös / tiedoksianto (Haastemies)', 'Käskypäätös / tiedoksianto (Haastemies) (sv)', 12, true),
(12, 'Käskypäätös / odotetaan valitusajan umpeutumista', 'Käskypäätös / odotetaan valitusajan umpeutumista (sv)', 13, true),
(13, 'Käskypäätös / valitusaika umpeutunut', 'Käskypäätös / valitusaika umpeutunut (sv)', 14, true),
(14, 'Sakkopäätös / kuulemiskirje', 'Sakkopäätös / kuulemiskirje (sv)', 15, true),
(15, 'Sakkopäätös / varsinainen päätös', 'Sakkopäätös / varsinainen päätös (sv)', 16, true),
(16, 'Sakkopäätös / tiedoksianto (ensimmäinen postitus)', 'Sakkopäätös / tiedoksianto (ensimmäinen postitus) (sv)', 17, true),
(17, 'Sakkopäätös / tiedoksianto (toinen postitus)', 'Sakkopäätös / tiedoksianto (toinen postitus) (sv)', 18, true),
(18, 'Sakkopäätös / tiedoksianto (Haastemies)', 'Sakkopäätös / tiedoksianto (Haastemies) (sv)', 19, true),
(19, 'Sakkopäätös / odotetaan valitusajan umpeutumista', 'Sakkopäätös / odotetaan valitusajan umpeutumista (sv)', 20, true),
(20, 'Sakkopäätös / valitusaika umpeutunut', 'Sakkopäätös / valitusaika umpeutunut (sv)', 21, true),
(21, 'Sakkoluettelon lähetys menossa', 'Sakkoluettelon lähetys menossa (sv)', 22, true)
on conflict (id) do update set
  label_fi = excluded.label_fi,
  label_sv = excluded.label_sv,
  ordinal = excluded.ordinal,
  valid = excluded.valid;
