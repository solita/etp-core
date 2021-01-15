-- name: select-kuukauden-laskutus
SELECT e.id energiatodistus_id, e.allekirjoitusaika,
       e.pt$kieli energiatodistus_kieli,
       k.etunimi || ' ' || k.sukunimi laatija_nimi,
       y.id yritys_id, y.ytunnus, v.valittajatunnus, y.verkkolaskuosoite,

       -- Fields depending on who should be invoiced.
       CASE WHEN e.laskutettava_yritys_id IS NULL THEN k.etunimi ||
                                                       ' ' ||
                                                       k.sukunimi
            WHEN e.laskutettava_yritys_id IS NOT NULL THEN y.nimi
       END nimi,

       CASE WHEN e.laskutettava_yritys_id IS NULL THEN 'L0' ||
                                                       lpad(l.id::text, 8, '0')
       WHEN e.laskutettava_yritys_id IS NOT NULL THEN 'L1' ||
                                                       lpad(y.id::text, 8, '0')
       END asiakastunnus,

       CASE WHEN e.laskutettava_yritys_id IS NULL THEN l.laskutuskieli
            WHEN e.laskutettava_yritys_id IS NOT NULL THEN y.laskutuskieli
       END laskutuskieli,

       CASE WHEN e.laskutettava_yritys_id IS NULL THEN l.jakeluosoite
            WHEN e.laskutettava_yritys_id IS NOT NULL THEN y.jakeluosoite
       END jakeluosoite,

       CASE WHEN e.laskutettava_yritys_id IS NULL THEN l.vastaanottajan_tarkenne
            WHEN e.laskutettava_yritys_id IS NOT NULL THEN y.vastaanottajan_tarkenne
       END vastaanottajan_tarkenne,

       CASE WHEN e.laskutettava_yritys_id IS NULL THEN l.postinumero
            WHEN e.laskutettava_yritys_id IS NOT NULL THEN y.postinumero
       END postinumero,

       CASE WHEN e.laskutettava_yritys_id IS NULL THEN l.postitoimipaikka
            WHEN e.laskutettava_yritys_id IS NOT NULL THEN y.postitoimipaikka
       END postitoimipaikka,

       CASE WHEN e.laskutettava_yritys_id IS NULL THEN l.maa
            WHEN e.laskutettava_yritys_id IS NOT NULL THEN y.maa
       END maa
  FROM energiatodistus e
  LEFT JOIN laatija l ON e.laatija_id = l.id
  LEFT JOIN kayttaja k ON l.id = k.id
  LEFT JOIN yritys y ON e.laskutettava_yritys_id = y.id
  LEFT JOIN verkkolaskuoperaattori v ON y.verkkolaskuoperaattori = v.id
  WHERE allekirjoitusaika IS NOT NULL AND
        allekirjoitusaika < date_trunc('month', now());
