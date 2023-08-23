insert into hallinto_oikeus (id, ordinal, label_fi, label_sv, attachment_name)
values (0, 1, 'Helsingin hallinto-oikeus', 'Helsingfors förvaltningsdomstol', 'Valitusosoitus_30_pv_HELSINGIN_HAO.pdf'),
       (1, 2, 'Hämeenlinnan hallinto-oikeus', 'Tavastehus förvaltningsdomstol', 'Valitusosoitus_30_pv_HAMEENLINNAN_HAO.pdf'),
       (2, 3, 'Itä-Suomen hallinto-oikeus', 'Östra Finlands förvaltningsdomstol', 'Valitusosoitus_30_pv_ITA-SUOMEN_HAO.pdf'),
       (3, 4, 'Pohjois-Suomen hallinto-oikeus', 'Norra Finlands förvaltningsdomstol', 'Valitusosoitus_30_pv_POHJOIS-SUOMEN_HAO.pdf'),
       (4, 5, 'Turun hallinto-oikeus', 'Åbo förvaltningsdomstol', 'Valitusosoitus_30_pv_TURUN_HAO.pdf'),
       (5, 6, 'Vaasan hallinto-oikeus', 'Vasa förvaltningsdomstol', 'Valitusosoitus_30_pv_VAASAN_HAO.pdf')
on conflict (id) do update set label_fi = excluded.label_fi,
                               label_sv = excluded.label_sv,
                               ordinal  = excluded.ordinal,
                               attachment_name = excluded.attachment_name;
