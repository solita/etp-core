insert into hallinto_oikeus (id, ordinal, label_fi, label_sv)
values (0, 1, 'Helsingin hallinto-oikeus', 'Helsingfors förvaltningsdomstol'),
       (1, 2, 'Hämeenlinnan hallinto-oikeus', 'Tavastehus förvaltningsdomstol'),
       (2, 3, 'Itä-Suomen hallinto-oikeus', 'Östra Finlands förvaltningsdomstol'),
       (3, 4, 'Pohjois-Suomen hallinto-oikeus', 'Norra Finlands förvaltningsdomstol'),
       (4, 5, 'Turun hallinto-oikeus', 'Åbo förvaltningsdomstol'),
       (5, 6, 'Vaasan hallinto-oikeus', 'Vasa förvaltningsdomstol')
on conflict (id) do update set label_fi = excluded.label_fi,
                               label_sv = excluded.label_sv,
                               ordinal  = excluded.ordinal;
