INSERT INTO lammonjako (id, label_fi, label_sv, ordinal)
VALUES
(0, 'Vesikiertoinen patterilämmitys', '', 1),
(1, 'Vesikiertoinen lattialämmitys', '', 2),
(2, 'Vesikiertoinen kattosäteilylämmity', '', 3),
(3, 'Sähköpatterilämmitys', '', 4),
(4, 'Sähköinen lattialämmitys', '', 5),
(5, 'Sähköinen kattolämmitys', '', 6),
(6, 'Ilmalämmitys', '', 7),
(7, 'Uuni- tai kamiinalämmitys', '', 8),
(8, 'Vesikiertoinen lattia-/patterilämmitys', '', 9),
(9, 'Vesikiertoinen lämmitys/ märkätiloissa sähköinen lattialämmitys', '', 10),
(10, 'Ilmalämmitys/vesikiertoinen patterilämmitys', '', 11),
(11, 'Ilmalämmitys/sähköpatterilämmitys', '', 12),
(12, 'Muu, mikä', '', 13)
ON CONFLICT (id) DO UPDATE
SET
label_fi = excluded.label_fi,
label_sv = excluded.label_sv;
