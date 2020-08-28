INSERT INTO laatija (id, patevyystaso, toteamispaivamaara, toteaja, jakeluosoite, postinumero, postitoimipaikka)
SELECT id, 1, date '2020-01-01', 'KIINKO', 'Hämeenkatu 1', '33100', 'Tampere' FROM kayttaja WHERE email = 'laatija1@example.com'
union all
SELECT id, 2, date '2019-01-01', 'FISE', 'Hämeenkatu 1', '33100', 'Tampere' FROM kayttaja WHERE email = 'laatija2@example.com'
union all
SELECT id, 2, date '2019-01-01', 'FISE', 'Hämeenkatu 1', '33100', 'Tampere' FROM kayttaja WHERE email = 'ulla.laatija@example.com'
union all
SELECT id, 2, date '2019-01-01', 'FISE', 'Hämeenkatu 1', '33100', 'Tampere' FROM kayttaja WHERE email = 'kirsi.laatija@example.com'
union all
SELECT id, 2, date '2019-01-01', 'FISE', 'Hämeenkatu 1', '33100', 'Tampere' FROM kayttaja WHERE email = 'ari.laatija@example.com'
union all
SELECT id, 2, date '2019-01-01', 'FISE', 'Hämeenkatu 1', '33100', 'Tampere' FROM kayttaja WHERE email = 'risto.laatija@example.com'
union all
SELECT id, 2, date '2019-01-01', 'FISE', 'Hämeenkatu 1', '33100', 'Tampere' FROM kayttaja WHERE email = 'marko.laatija@example.com'
union all
SELECT id, 2, date '2019-01-01', 'FISE', 'Hämeenkatu 1', '33100', 'Tampere' FROM kayttaja WHERE email = 'aleksi.laatija@example.com';
