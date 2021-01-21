-- Create sequences for asiakastunnukset. Start with large numbers
-- since they are used to give temporary asiakastunnukset when
-- columns are added.
CREATE SEQUENCE yritys_laskutus_asiakastunnus_seq START 999999;
CREATE SEQUENCE laatija_laskutus_asiakastunnus_seq START 999999;

-- Create column for asiakastunnus to yritys table. Set default from new sequence.
ALTER TABLE yritys ADD COLUMN laskutus_asiakastunnus text NOT NULL UNIQUE
  DEFAULT 'L1' || lpad(nextval('yritys_laskutus_asiakastunnus_seq')::text, 8, '0');

-- Update asiakastunnus for every existing yritys. Use id, which works nicely
-- since yritys id = tilausasiakas id from old data.
UPDATE yritys SET laskutus_asiakastunnus = 'L1' || lpad(id::text, 8, '0');

-- Set the new asiakastunnus sequence for yritys to the same number that is in
-- the yritys id sequence so that they are incremented as a pair in the future
-- and provide the same value for both the id and asiakastunnus.
SELECT setval('yritys_laskutus_asiakastunnus_seq', (SELECT last_value FROM etp.yritys_id_seq));

-- Create column for asiakastunnus to laatija table. Set default from new sequence.
ALTER TABLE laatija ADD COLUMN laskutus_asiakastunnus text NOT NULL UNIQUE
  DEFAULT 'L0' || lpad(nextval('laatija_laskutus_asiakastunnus_seq')::text, 8, '0');

-- Two things that are only ran if conversion_etp.lasku table exists.
DO LANGUAGE plpgsql $$
  BEGIN
    IF (SELECT EXISTS (SELECT FROM information_schema.tables WHERE table_schema = 'conversion_etp' AND table_name = 'lasku')) THEN

      -- Set asiakastunnus sequence for laatija according to the max tilausasiakas id belonging
      -- to laatija in old data.
      PERFORM setval('laatija_laskutus_asiakastunnus_seq',
                     (SELECT max(t.id)+1
                        FROM conversion_etp.laatija l
                        LEFT JOIN conversion_etp.tilausasiakas t ON l.tilausasiakas = t.id));

      -- Update asiakastunnus for every existing laatija. Use tilausasiakas id joined from
      -- laatija table (both tables from old data). For laatijat without tilausasiakas row,
      -- fallback to newly set asiakastunnus sequence for laatijat. This is to make sure that
      -- from now on, each laatija has asiakastunnus.
      UPDATE laatija
       SET laskutus_asiakastunnus = 'L0' ||
           lpad(coalesce(
                 (SELECT conversion_laatija.tilausasiakas
                    FROM conversion_etp.laatija conversion_laatija
                    WHERE conversion_laatija.id = laatija.id),
                 nextval('laatija_laskutus_asiakastunnus_seq'))::text,
                8, '0');

      RAISE NOTICE 'LASKUTUS CONVERSION FROM OLD DATA READY';
    ELSE
      -- If old data was not available, reset new laatija sequence.
      -- This should only happen in every other environment except production.
      PERFORM setval('laatija_laskutus_asiakastunnus_seq', (SELECT last_value FROM etp.kayttaja_id_seq));
      RAISE NOTICE 'LASKUTUS DATA WAS NOT AVAILABLE. SET UP AS NEW DATABASE.';
    END IF;
  END;
$$;
