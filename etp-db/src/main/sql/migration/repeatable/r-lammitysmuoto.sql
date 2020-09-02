INSERT INTO lammitysmuoto (id, label_fi, label_sv, ordinal)
VALUES
(0, 'Kaukolämpö', '', 1),
(1, 'Sähkö', '', 2),
(2, 'Puu', '', 3),
(3, 'Pelletti', '', 4),
(4, 'Öljy', '', 5),
(5, 'Kaasu', '', 6),
(6, 'MILP', '', 7),
(7, 'VILP / IILP', '', 8),
(8, 'PILP', '', 9),
(9, 'Muu, mikä', '', 10)
ON CONFLICT (id) DO UPDATE
SET
label_fi = excluded.label_fi,
label_sv = excluded.label_sv;
