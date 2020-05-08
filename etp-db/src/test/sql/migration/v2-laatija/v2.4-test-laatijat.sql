INSERT INTO laatija (id, kayttaja, henkilotunnus, patevyystaso, toteamispaivamaara, toteaja, jakeluosoite, postinumero, postitoimipaikka)
SELECT id, id, '271190-836F', 1, date '2020-01-01', 'KIINKO', 'Hämeenkatu 1', '33100', 'Tampere' FROM kayttaja WHERE email = 'laatija1@example.com'
union all
SELECT id, id, '100183-563U', 2, date '2019-01-01', 'FISE', 'Hämeenkatu 1', '33100', 'Tampere' FROM kayttaja WHERE email = 'laatija2@example.com';
