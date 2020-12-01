INSERT INTO laatija (id, patevyystaso, toteamispaivamaara, toteaja, jakeluosoite, postinumero, postitoimipaikka)
SELECT id, 2, date '2020-01-01', 'KIINKO', 'Peltokatu 26', '33100', 'Tampere' FROM kayttaja WHERE email ILIKE '%solita.fi%' AND rooli = 0
UNION ALL
SELECT id, 2, date '2020-01-01', 'FISE', 'Vesijärvenkatu 11 A', '15140', 'Lahti' FROM kayttaja WHERE email ILIKE '%ara.fi%' AND rooli = 0
UNION ALL
SELECT id, 1, date '2020-10-01', 'FISE', 'Hämeenkatu 1', '33100', 'Tampere' FROM kayttaja WHERE email NOT ILIKE '%solita.fi%' AND email NOT ILIKE '%ara.fi%' AND rooli = 0;
