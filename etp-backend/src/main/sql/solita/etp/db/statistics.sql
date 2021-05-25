-- name: select-e-luokka-counts
SELECT e.t$e_luokka e_luokka, count(e.id) FROM energiatodistus e
LEFT JOIN postinumero p ON e.pt$postinumero = p.id
LEFT JOIN kunta k ON p.kunta_id = k.id
WHERE e.t$e_luokka IS NOT NULL AND e.versio = :versio
AND (:postinumero::int IS NULL OR e.pt$postinumero::text = ltrim(:postinumero, '0'))
AND (:kunta::text IS NULL OR k.label_fi ILIKE :kunta OR k.label_sv ILIKE :kunta)
AND ((:alakayttotarkoitus-ids) IS NULL OR e.pt$kayttotarkoitus IN (:alakayttotarkoitus-ids))
AND (:valmistumisvuosi-min::numeric IS NULL OR e.pt$valmistumisvuosi >= :valmistumisvuosi-min)
AND (:valmistumisvuosi-max::numeric IS NULL OR e.pt$valmistumisvuosi <= :valmistumisvuosi-max)
AND (:lammitetty-nettoala-min::numeric IS NULL OR e.lt$lammitetty_nettoala >= :lammitetty-nettoala-min)
AND (:lammitetty-nettoala-max::numeric IS NULL OR e.lt$lammitetty_nettoala <= :lammitetty-nettoala-max)
GROUP BY 1 ORDER BY 1