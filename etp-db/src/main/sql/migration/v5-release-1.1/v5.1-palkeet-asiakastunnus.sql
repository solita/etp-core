-- Create sequences for asiakastunnukset.
CREATE SEQUENCE palkeet_asiakastunnus_yritys_seq START 1;
CREATE SEQUENCE palkeet_asiakastunnus_laatija_seq START 1;

-- Create column for asiakastunnus to yritys table. Set default from new sequence.
ALTER TABLE yritys ADD COLUMN palkeet_asiakastunnus text NOT NULL UNIQUE
  DEFAULT 'L1' || lpad(nextval('palkeet_asiakastunnus_yritys_seq')::text, 8, '0');

-- Update asiakastunnus for every existing yritys. Use id, which works nicely
-- since yritys id = tilausasiakas id from old data.
UPDATE yritys SET palkeet_asiakastunnus = 'L1' || lpad(y.id::text, 8, '0')
  FROM yritys y
  WHERE yritys.id = y.id;

-- Set the new asiakastunnus sequence for yritys to the same number that is in
-- the yritys id sequence so that they are incremented as a pair in the future
-- and provide the same value for both the id and asiakastunnus.
SELECT setval('palkeet_asiakastunnus_yritys_seq', (SELECT last_value FROM etp.yritys_id_seq));

-- Create column for asiakastunnus to laatija table. Set default from new sequence.
ALTER TABLE laatija ADD COLUMN palkeet_asiakastunnus text NOT NULL UNIQUE
  DEFAULT 'L0' || lpad(nextval('palkeet_asiakastunnus_laatija_seq')::text, 8, '0');

-- Two things that are only ran if conversion_etp.lasku table exists.
DO LANGUAGE plpgsql $$
  BEGIN
    IF (SELECT EXISTS (SELECT FROM information_schema.tables WHERE table_schema = 'conversion_etp' AND table_name = 'lasku')) THEN

      -- Set asiakastunus sequence for laatija according to the max tilausasiakas id belonging
      -- to laatija in old data.
      PERFORM setval('palkeet_asiakastunnus_laatija_seq',
                     (SELECT max(t.id)+1
                        FROM conversion_etp.laatija l
                        LEFT JOIN conversion_etp.tilausasiakas t ON l.tilausasiakas = t.id));

      -- Update asiakastunnus for every existing laatija. Use tilausasiakas id joined from
      -- laatija table (both tables from old data). For laatijat without tilausasiakas row,
      -- fallback to newly set asiakastunnus sequence for laatijat. This is to make sure that
      -- from now on, each laatija has asiakastunnus.
      UPDATE laatija
        SET palkeet_asiakastunnus = 'L0' || lpad(subquery.tilausasiakas_id::text, 8, '0')
        FROM (SELECT l.id laatija_id,
                     coalesce(t.id, nextval('palkeet_asiakastunnus_laatija_seq')) tilausasiakas_id
                FROM conversion_etp.laatija l
                LEFT JOIN conversion_etp.tilausasiakas t ON l.tilausasiakas = t.id) subquery
                WHERE laatija.id = subquery.laatija_id;

      RAISE NOTICE 'PALKEET CONVERSION READY';
    END IF;
  END;
$$;
