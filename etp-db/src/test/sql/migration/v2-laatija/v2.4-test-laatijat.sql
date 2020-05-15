INSERT INTO laatija (id, kayttaja, henkilotunnus, patevyystaso, toteamispaivamaara, toteaja, jakeluosoite, postinumero, postitoimipaikka)
SELECT id, id, '271190-836F', 1, date '2020-01-01', 'KIINKO', 'Hämeenkatu 1', '33100', 'Tampere' FROM kayttaja WHERE email = 'laatija1@example.com'
union all
SELECT id, id, '100183-563U', 2, date '2019-01-01', 'FISE', 'Hämeenkatu 1', '33100', 'Tampere' FROM kayttaja WHERE email = 'laatija2@example.com'
union all
SELECT id, id, '010100-1111', 2, date '2019-01-01', 'FISE', 'Hämeenkatu 1', '33100', 'Tampere' FROM kayttaja WHERE email = 'ulla.laatija@example.com'
union all
SELECT id, id, '01010100-222L', 2, date '2019-01-01', 'FISE', 'Hämeenkatu 1', '33100', 'Tampere' FROM kayttaja WHERE email = 'kirsi.laatija@example.com'
union all
SELECT id, id, '01010100-3336', 2, date '2019-01-01', 'FISE', 'Hämeenkatu 1', '33100', 'Tampere' FROM kayttaja WHERE email = 'ari.laatija@example.com'
union all
SELECT id, id, '01010100-444S', 2, date '2019-01-01', 'FISE', 'Hämeenkatu 1', '33100', 'Tampere' FROM kayttaja WHERE email = 'risto.laatija@example.com'
union all
SELECT id, id, '01010100-555B', 2, date '2019-01-01', 'FISE', 'Hämeenkatu 1', '33100', 'Tampere' FROM kayttaja WHERE email = 'marko.laatija@example.com'
union all
SELECT id, id, '01010100-666X', 2, date '2019-01-01', 'FISE', 'Hämeenkatu 1', '33100', 'Tampere' FROM kayttaja WHERE email = 'aleksi.laatija@example.com';
