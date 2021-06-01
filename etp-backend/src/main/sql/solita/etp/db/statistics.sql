-- name: select-counts
SELECT e.t$e_luokka e_luokka,
       e.lt$lammitys$lammitysmuoto_1$id lammitysmuoto_id,
       e.lt$ilmanvaihto$tyyppi_id ilmanvaihtotyyppi_id,
       count(1)
FROM energiatodistus e
LEFT JOIN postinumero p ON e.pt$postinumero = p.id
LEFT JOIN kunta k ON p.kunta_id = k.id
LEFT JOIN toimintaalue t ON k.toimintaalue_id = t.id
WHERE e.versio = :versio
AND e.tila_id = 2
AND e.voimassaolo_paattymisaika > now()
AND (:keyword::text IS NULL
     OR e.pt$postinumero::text = ltrim(:keyword, '0')
     OR k.label_fi ILIKE :keyword
     OR k.label_sv ILIKE :keyword
     OR t.label_fi ILIKE :keyword
     OR t.label_sv ILIKE :keyword)
AND ((:alakayttotarkoitus-ids) IS NULL OR e.pt$kayttotarkoitus IN (:alakayttotarkoitus-ids))
AND (:valmistumisvuosi-min::numeric IS NULL OR e.pt$valmistumisvuosi >= :valmistumisvuosi-min)
AND (:valmistumisvuosi-max::numeric IS NULL OR e.pt$valmistumisvuosi <= :valmistumisvuosi-max)
AND (:lammitetty-nettoala-min::numeric IS NULL OR e.lt$lammitetty_nettoala >= :lammitetty-nettoala-min)
AND (:lammitetty-nettoala-max::numeric IS NULL OR e.lt$lammitetty_nettoala <= :lammitetty-nettoala-max)
GROUP BY GROUPING SETS (e.t$e_luokka, e.lt$lammitys$lammitysmuoto_1$id, e.lt$ilmanvaihto$tyyppi_id);

-- name: select-e-luku-statistics
SELECT round(avg(e.t$e_luku), 2) avg, min(e.t$e_luku),
       percentile_cont(0.15) WITHIN GROUP (ORDER BY e.t$e_luku) percentile_15
FROM energiatodistus e
LEFT JOIN postinumero p ON e.pt$postinumero = p.id
LEFT JOIN kunta k ON p.kunta_id = k.id
LEFT JOIN toimintaalue t ON k.toimintaalue_id = t.id
WHERE e.versio = :versio
AND e.tila_id = 2
AND e.voimassaolo_paattymisaika > now()
AND (:keyword::text IS NULL
     OR e.pt$postinumero::text = ltrim(:keyword, '0')
     OR k.label_fi ILIKE :keyword
     OR k.label_sv ILIKE :keyword
     OR t.label_fi ILIKE :keyword
     OR t.label_sv ILIKE :keyword)
AND ((:alakayttotarkoitus-ids) IS NULL OR e.pt$kayttotarkoitus IN (:alakayttotarkoitus-ids))
AND (:valmistumisvuosi-min::numeric IS NULL OR e.pt$valmistumisvuosi >= :valmistumisvuosi-min)
AND (:valmistumisvuosi-max::numeric IS NULL OR e.pt$valmistumisvuosi <= :valmistumisvuosi-max)
AND (:lammitetty-nettoala-min::numeric IS NULL OR e.lt$lammitetty_nettoala >= :lammitetty-nettoala-min)
AND (:lammitetty-nettoala-max::numeric IS NULL OR e.lt$lammitetty_nettoala <= :lammitetty-nettoala-max);

-- name: select-common-averages
SELECT round(avg(e.lt$rakennusvaippa$ilmanvuotoluku), 1) ilmanvuotoluku,
       round(avg(e.lt$rakennusvaippa$ulkoseinat$u), 2) ulkoseinat_u,
       round(avg(e.lt$rakennusvaippa$ylapohja$u), 2) ylapohja_u,
       round(avg(e.lt$rakennusvaippa$alapohja$u), 2) alapohja_u,
       round(avg(e.lt$rakennusvaippa$ikkunat$u), 2) ikkunat_u,
       round(avg(e.lt$rakennusvaippa$ulkoovet$u), 2) ulkoovet_u,
       round(avg(e.lt$lammitys$takka$maara), 1) takka,
       round(avg(e.lt$lammitys$ilmalampopumppu$maara), 1) ilmalampopumppu,
       round(avg(e.lt$lammitys$tilat_ja_iv$lampokerroin), 1) tilat_ja_iv_lampokerroin,
       round(avg(e.lt$lammitys$lammin_kayttovesi$lampokerroin), 1) lammin_kayttovesi_lampokerroin,
       round(avg(e.lt$ilmanvaihto$lto_vuosihyotysuhde), 1) lto_vuosihyotysuhde,
       round(avg(e.lt$ilmanvaihto$ivjarjestelma$sfp), 1) ivjarjestelma_sfp
FROM energiatodistus e
LEFT JOIN postinumero p ON e.pt$postinumero = p.id
LEFT JOIN kunta k ON p.kunta_id = k.id
LEFT JOIN toimintaalue t ON k.toimintaalue_id = t.id
WHERE e.tila_id = 2
AND e.voimassaolo_paattymisaika > now()
AND (:keyword::text IS NULL
     OR e.pt$postinumero::text = ltrim(:keyword, '0')
     OR k.label_fi ILIKE :keyword
     OR k.label_sv ILIKE :keyword
     OR t.label_fi ILIKE :keyword
     OR t.label_sv ILIKE :keyword)
AND ((:alakayttotarkoitus-ids) IS NULL OR e.pt$kayttotarkoitus IN (:alakayttotarkoitus-ids))
AND (:valmistumisvuosi-min::numeric IS NULL OR e.pt$valmistumisvuosi >= :valmistumisvuosi-min)
AND (:valmistumisvuosi-max::numeric IS NULL OR e.pt$valmistumisvuosi <= :valmistumisvuosi-max)
AND (:lammitetty-nettoala-min::numeric IS NULL OR e.lt$lammitetty_nettoala >= :lammitetty-nettoala-min)
AND (:lammitetty-nettoala-max::numeric IS NULL OR e.lt$lammitetty_nettoala <= :lammitetty-nettoala-max);

-- name: select-uusiutuvat-omavaraisenergiat-counts
SELECT count(t$uusiutuvat_omavaraisenergiat$aurinkolampo) FILTER (WHERE t$uusiutuvat_omavaraisenergiat$aurinkolampo > 0) as aurinkolampo,
       count(t$uusiutuvat_omavaraisenergiat$aurinkosahko) FILTER (WHERE t$uusiutuvat_omavaraisenergiat$aurinkosahko > 0) as aurinkosahko,
       count(t$uusiutuvat_omavaraisenergiat$tuulisahko) FILTER (WHERE t$uusiutuvat_omavaraisenergiat$tuulisahko > 0) as tuulisahko,
       count(t$uusiutuvat_omavaraisenergiat$lampopumppu) FILTER (WHERE t$uusiutuvat_omavaraisenergiat$lampopumppu > 0) as lampopumppu,
       count(t$uusiutuvat_omavaraisenergiat$muusahko) FILTER (WHERE t$uusiutuvat_omavaraisenergiat$muusahko > 0) as muusahko,
       count(t$uusiutuvat_omavaraisenergiat$muulampo) FILTER (WHERE t$uusiutuvat_omavaraisenergiat$muulampo > 0) as muulampo
FROM energiatodistus e
LEFT JOIN postinumero p ON e.pt$postinumero = p.id
LEFT JOIN kunta k ON p.kunta_id = k.id
LEFT JOIN toimintaalue t ON k.toimintaalue_id = t.id
WHERE e.versio = :versio
AND e.tila_id = 2
AND e.voimassaolo_paattymisaika > now()
AND (:keyword::text IS NULL
     OR e.pt$postinumero::text = ltrim(:keyword, '0')
     OR k.label_fi ILIKE :keyword
     OR k.label_sv ILIKE :keyword
     OR t.label_fi ILIKE :keyword
     OR t.label_sv ILIKE :keyword)
AND ((:alakayttotarkoitus-ids) IS NULL OR e.pt$kayttotarkoitus IN (:alakayttotarkoitus-ids))
AND (:valmistumisvuosi-min::numeric IS NULL OR e.pt$valmistumisvuosi >= :valmistumisvuosi-min)
AND (:valmistumisvuosi-max::numeric IS NULL OR e.pt$valmistumisvuosi <= :valmistumisvuosi-max)
AND (:lammitetty-nettoala-min::numeric IS NULL OR e.lt$lammitetty_nettoala >= :lammitetty-nettoala-min)
AND (:lammitetty-nettoala-max::numeric IS NULL OR e.lt$lammitetty_nettoala <= :lammitetty-nettoala-max);
