INSERT INTO laatija (kayttaja, henkilotunnus, patevyystaso, toteamispaivamaara, toteaja, jakeluosoite, postinumero, postitoimipaikka) VALUES ((SELECT id FROM kayttaja WHERE email = 'laatija1@example.com'), '271190-836F', 0, '2020-01-01', 'KIINKO', 'Hämeenkatu 1', '33100', 'Tampere');

INSERT INTO laatija (kayttaja, henkilotunnus, patevyystaso, toteamispaivamaara, toteaja, jakeluosoite, postinumero, postitoimipaikka) VALUES ((SELECT id FROM kayttaja WHERE email = 'laatija2@example.com'), '100183-563U', 1, '2019-01-01', 'FISE', 'Hämeenkatu 1', '33100', 'Tampere');
