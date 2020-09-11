
insert into validation_numeric_column
  (versio, column_name, warning$min, warning$max, error$min, error$max)
values
(2018, 'lt$rakennusvaippa$ulkoseinat$U', 0.08, 0.81, 0.05, 2),
(2018, 'lt$rakennusvaippa$ylapohja$U',   0.05, 0.47, 0.03, 2),
(2018, 'lt$rakennusvaippa$alapohja$U',   0.05, 0.60, 0.03, 4),
(2018, 'lt$rakennusvaippa$ikkunat$U',    0.05, 3.10, 0.04, 6.5),
(2018, 'lt$rakennusvaippa$ulkoovet$U',   0.08, 0.81, 0.05, 2)
on conflict (column_name, versio) do update
  set
    warning$min = excluded.warning$min,
    warning$max = excluded.warning$max,
    error$min = excluded.error$min,
    error$max = excluded.error$max;

insert into validation_required_column (versio, column_name)
values
(2018, 'pt$nimi'),
(2018, 'pt$kieli'),
(2018, 'pt$laatimisvaihe'),
(2018, 'pt$havainnointikaynti'),
(2018, 'pt$valmistumisvuosi'),
(2018, 'pt$rakennusosa'),
(2018, 'pt$katuosoite_fi'),
(2018, 'pt$katuosoite_sv'),
(2018, 'pt$postinumero'),
(2018, 'pt$rakennustunnus'),
(2018, 'pt$kayttotarkoitus'),
(2018, 'pt$keskeiset_suositukset_fi'),
(2018, 'pt$keskeiset_suositukset_sv'),

(2018, 'lt$lammitetty_nettoala'),

(2018, 'lt$rakennusvaippa$ilmanvuotoluku'),
(2018, 'lt$rakennusvaippa$ulkoseinat$ala'),
(2018, 'lt$rakennusvaippa$ylapohja$ala'),
(2018, 'lt$rakennusvaippa$alapohja$ala'),
(2018, 'lt$rakennusvaippa$ikkunat$ala'),
(2018, 'lt$rakennusvaippa$ulkoovet$ala'),
(2018, 'lt$rakennusvaippa$ulkoseinat$U'),
(2018, 'lt$rakennusvaippa$ylapohja$U'),
(2018, 'lt$rakennusvaippa$alapohja$U'),
(2018, 'lt$rakennusvaippa$ikkunat$U'),
(2018, 'lt$rakennusvaippa$ulkoovet$U'),
(2018, 'lt$rakennusvaippa$kylmasillat_UA'),

(2018, 'lt$ikkunat$etela$U'),
(2018, 'lt$ikkunat$etela$g_ks'),
(2018, 'lt$ikkunat$ita$U'),
(2018, 'lt$ikkunat$ita$g_ks'),
(2018, 'lt$ikkunat$kaakko$U'),
(2018, 'lt$ikkunat$kaakko$g_ks'),
(2018, 'lt$ikkunat$koillinen$U'),
(2018, 'lt$ikkunat$koillinen$g_ks'),
(2018, 'lt$ikkunat$lansi$U'),
(2018, 'lt$ikkunat$lansi$g_ks'),
(2018, 'lt$ikkunat$lounas$U'),
(2018, 'lt$ikkunat$lounas$g_ks'),
(2018, 'lt$ikkunat$luode$U'),
(2018, 'lt$ikkunat$luode$g_ks'),
(2018, 'lt$ikkunat$pohjoinen$U'),
(2018, 'lt$ikkunat$pohjoinen$g_ks'),

(2018, 'lt$ilmanvaihto$tyyppi_id'),
(2018, 'lt$ilmanvaihto$kuvaus_fi'),
(2018, 'lt$ilmanvaihto$kuvaus_sv'),
(2018, 'lt$ilmanvaihto$paaiv$poisto'),
(2018, 'lt$ilmanvaihto$paaiv$tulo'),
(2018, 'lt$ilmanvaihto$erillispoistot$poisto'),
(2018, 'lt$ilmanvaihto$erillispoistot$tulo'),
(2018, 'lt$ilmanvaihto$ivjarjestelma$poisto'),
(2018, 'lt$ilmanvaihto$ivjarjestelma$tulo'),

(2018, 'lt$lammitys$lammitysmuoto_1$id'),
(2018, 'lt$lammitys$lammitysmuoto_1$kuvaus_fi'),
(2018, 'lt$lammitys$lammitysmuoto_1$kuvaus_sv'),
(2018, 'lt$lammitys$lammitysmuoto_2$id'),
(2018, 'lt$lammitys$lammitysmuoto_2$kuvaus_fi'),
(2018, 'lt$lammitys$lammitysmuoto_2$kuvaus_sv'),
(2018, 'lt$lammitys$lammonjako$id'),
(2018, 'lt$lammitys$lammonjako$kuvaus_fi'),
(2018, 'lt$lammitys$lammonjako$kuvaus_sv'),
(2018, 'lt$lammitys$lammin_kayttovesi$jaon_hyotysuhde'),
(2018, 'lt$lammitys$lammin_kayttovesi$tuoton_hyotysuhde'),
(2018, 'lt$lammitys$tilat_ja_iv$apulaitteet'),
(2018, 'lt$lammitys$tilat_ja_iv$jaon_hyotysuhde'),
(2018, 'lt$lammitys$tilat_ja_iv$tuoton_hyotysuhde'),

(2018, 'lt$lkvn_kaytto$ominaiskulutus'),
(2018, 'lt$lkvn_kaytto$lammitysenergian_nettotarve'),

(2018, 't$laskentatyokalu')
on conflict (column_name, versio) do nothing