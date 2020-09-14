
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

insert into validation_required_column (versio, column_name, ordinal)
values
(2018, 'pt$kieli', 0),
(2018, 'pt$laatimisvaihe', 1),
(2018, 'pt$havainnointikaynti', 2),
(2018, 't$laskentatyokalu', 3),

(2018, 'pt$nimi', 4),
(2018, 'pt$valmistumisvuosi', 5),
(2018, 'pt$rakennusosa', 6),
(2018, 'pt$katuosoite_fi', 7),
(2018, 'pt$katuosoite_sv', 8),
(2018, 'pt$postinumero', 9),
(2018, 'pt$rakennustunnus', 10),
(2018, 'pt$kayttotarkoitus', 11),
(2018, 'pt$keskeiset_suositukset_fi', 12),
(2018, 'pt$keskeiset_suositukset_sv', 13),

(2018, 'lt$lammitetty_nettoala', 14),
(2018, 'lt$rakennusvaippa$ilmanvuotoluku', 15),
(2018, 'lt$rakennusvaippa$ulkoseinat$ala', 16),
(2018, 'lt$rakennusvaippa$ylapohja$ala', 17),
(2018, 'lt$rakennusvaippa$alapohja$ala', 18),
(2018, 'lt$rakennusvaippa$ikkunat$ala', 19),
(2018, 'lt$rakennusvaippa$ulkoovet$ala', 20),
(2018, 'lt$rakennusvaippa$ulkoseinat$U', 21),
(2018, 'lt$rakennusvaippa$ylapohja$U', 22),
(2018, 'lt$rakennusvaippa$alapohja$U', 23),
(2018, 'lt$rakennusvaippa$ikkunat$U', 24),
(2018, 'lt$rakennusvaippa$ulkoovet$U', 25),
(2018, 'lt$rakennusvaippa$kylmasillat_UA', 26),

(2018, 'lt$ikkunat$etela$U', 27),
(2018, 'lt$ikkunat$etela$g_ks', 28),
(2018, 'lt$ikkunat$ita$U', 29),
(2018, 'lt$ikkunat$ita$g_ks', 30),
(2018, 'lt$ikkunat$kaakko$U', 31),
(2018, 'lt$ikkunat$kaakko$g_ks', 32),
(2018, 'lt$ikkunat$koillinen$U', 33),
(2018, 'lt$ikkunat$koillinen$g_ks', 34),
(2018, 'lt$ikkunat$lansi$U', 35),
(2018, 'lt$ikkunat$lansi$g_ks', 36),
(2018, 'lt$ikkunat$lounas$U', 37),
(2018, 'lt$ikkunat$lounas$g_ks', 38),
(2018, 'lt$ikkunat$luode$U', 39),
(2018, 'lt$ikkunat$luode$g_ks', 40),
(2018, 'lt$ikkunat$pohjoinen$U', 41),
(2018, 'lt$ikkunat$pohjoinen$g_ks', 42),

(2018, 'lt$ilmanvaihto$tyyppi_id', 43),
(2018, 'lt$ilmanvaihto$kuvaus_fi', 44),
(2018, 'lt$ilmanvaihto$kuvaus_sv', 45),
(2018, 'lt$ilmanvaihto$paaiv$tulo', 46),
(2018, 'lt$ilmanvaihto$paaiv$poisto', 47),
(2018, 'lt$ilmanvaihto$erillispoistot$tulo', 48),
(2018, 'lt$ilmanvaihto$erillispoistot$poisto', 49),
(2018, 'lt$ilmanvaihto$ivjarjestelma$tulo', 50),
(2018, 'lt$ilmanvaihto$ivjarjestelma$poisto', 51),

(2018, 'lt$lammitys$lammitysmuoto_1$id', 52),
(2018, 'lt$lammitys$lammitysmuoto_1$kuvaus_fi', 53),
(2018, 'lt$lammitys$lammitysmuoto_1$kuvaus_sv', 54),
(2018, 'lt$lammitys$lammitysmuoto_2$kuvaus_fi', 55),
(2018, 'lt$lammitys$lammitysmuoto_2$kuvaus_sv', 56),
(2018, 'lt$lammitys$lammonjako$id', 57),
(2018, 'lt$lammitys$lammonjako$kuvaus_fi', 58),
(2018, 'lt$lammitys$lammonjako$kuvaus_sv', 59),
(2018, 'lt$lammitys$tilat_ja_iv$tuoton_hyotysuhde', 60),
(2018, 'lt$lammitys$tilat_ja_iv$jaon_hyotysuhde', 61),
(2018, 'lt$lammitys$tilat_ja_iv$apulaitteet', 62),
(2018, 'lt$lammitys$lammin_kayttovesi$tuoton_hyotysuhde', 63),
(2018, 'lt$lammitys$lammin_kayttovesi$jaon_hyotysuhde', 64),

(2018, 'lt$lkvn_kaytto$ominaiskulutus', 65),
(2018, 'lt$lkvn_kaytto$lammitysenergian_nettotarve', 66)

on conflict (column_name, versio) do update
  set ordinal = excluded.ordinal