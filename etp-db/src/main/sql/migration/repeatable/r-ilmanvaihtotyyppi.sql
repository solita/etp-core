INSERT INTO ilmanvaihtotyyppi (id, label_fi, label_sv, ordinal)
VALUES
(0, 'Painovoimainen ilmanvaihtojärjestelmä', '', 1),
(1, 'Koneellinen poistoilmanvaihtojärjestelmä', '', 2),
(2, 'Koneellinen tulo- ja poistoilmanvaihtojärjeslmä', '', 3),
(3, 'Poistoilmalämpöpumppu (vain käyttötarkoitusluokka 1)', '', 4),
(4, 'Painovoimainen/koneellinen poisto', '', 5),
(5, 'Painovoimainen/koneellinen tulo- ja poisto', '', 6),
(6, 'Muu, mikä', '', 7)
ON CONFLICT (id) DO UPDATE
SET
label_fi = excluded.label_fi,
label_sv = excluded.label_sv;
