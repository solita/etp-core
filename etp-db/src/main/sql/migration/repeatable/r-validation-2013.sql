
insert into validation_required_column (versio, column_name, ordinal)
values
(2013, 'pt$kieli', 0),
(2013, 't$laskentatyokalu', 3),

(2013, 'pt$nimi', 4),
(2013, 'pt$valmistumisvuosi', 5),
(2013, 'pt$rakennusosa', 6),
(2013, 'pt$katuosoite_fi', 7),
(2013, 'pt$katuosoite_sv', 8),
(2013, 'pt$postinumero', 9),
--(2013, 'pt$rakennustunnus', 10),
(2013, 'pt$kayttotarkoitus', 11),
--(2013, 'pt$keskeiset_suositukset_fi', 12),
--(2013, 'pt$keskeiset_suositukset_sv', 13),

(2013, 'lt$lammitetty_nettoala', 14),
(2013, 'lt$rakennusvaippa$ilmanvuotoluku', 15),
(2013, 'lt$rakennusvaippa$ulkoseinat$ala', 16),
(2013, 'lt$rakennusvaippa$ylapohja$ala', 17),
(2013, 'lt$rakennusvaippa$alapohja$ala', 18),
(2013, 'lt$rakennusvaippa$ikkunat$ala', 19),
(2013, 'lt$rakennusvaippa$ulkoovet$ala', 20),
(2013, 'lt$rakennusvaippa$ulkoseinat$U', 21),
(2013, 'lt$rakennusvaippa$ylapohja$U', 22),
(2013, 'lt$rakennusvaippa$alapohja$U', 23),
(2013, 'lt$rakennusvaippa$ikkunat$U', 24),
(2013, 'lt$rakennusvaippa$ulkoovet$U', 25),
(2013, 'lt$rakennusvaippa$kylmasillat_UA', 26),

(2013, 'lt$ikkunat$etela$U', 27),
(2013, 'lt$ikkunat$etela$g_ks', 28),
(2013, 'lt$ikkunat$ita$U', 29),
(2013, 'lt$ikkunat$ita$g_ks', 30),
(2013, 'lt$ikkunat$kaakko$U', 31),
(2013, 'lt$ikkunat$kaakko$g_ks', 32),
(2013, 'lt$ikkunat$koillinen$U', 33),
(2013, 'lt$ikkunat$koillinen$g_ks', 34),
(2013, 'lt$ikkunat$lansi$U', 35),
(2013, 'lt$ikkunat$lansi$g_ks', 36),
(2013, 'lt$ikkunat$lounas$U', 37),
(2013, 'lt$ikkunat$lounas$g_ks', 38),
(2013, 'lt$ikkunat$luode$U', 39),
(2013, 'lt$ikkunat$luode$g_ks', 40),
(2013, 'lt$ikkunat$pohjoinen$U', 41),
(2013, 'lt$ikkunat$pohjoinen$g_ks', 42),

(2013, 'lt$ilmanvaihto$tyyppi_id', 43),
(2013, 'lt$ilmanvaihto$kuvaus_fi', 44),
(2013, 'lt$ilmanvaihto$kuvaus_sv', 45),
(2013, 'lt$ilmanvaihto$paaiv$tulo', 46),
(2013, 'lt$ilmanvaihto$paaiv$poisto', 47),
(2013, 'lt$ilmanvaihto$erillispoistot$tulo', 48),
(2013, 'lt$ilmanvaihto$erillispoistot$poisto', 49),
(2013, 'lt$ilmanvaihto$ivjarjestelma$tulo', 50),
(2013, 'lt$ilmanvaihto$ivjarjestelma$poisto', 51),

(2013, 'lt$lammitys$lammitysmuoto_1$id', 52),
(2013, 'lt$lammitys$lammitysmuoto_1$kuvaus_fi', 53),
(2013, 'lt$lammitys$lammitysmuoto_1$kuvaus_sv', 54),
(2013, 'lt$lammitys$lammitysmuoto_2$kuvaus_fi', 55),
(2013, 'lt$lammitys$lammitysmuoto_2$kuvaus_sv', 56),
(2013, 'lt$lammitys$lammonjako$id', 57),
(2013, 'lt$lammitys$lammonjako$kuvaus_fi', 58),
(2013, 'lt$lammitys$lammonjako$kuvaus_sv', 59),
(2013, 'lt$lammitys$tilat_ja_iv$tuoton_hyotysuhde', 60),
(2013, 'lt$lammitys$tilat_ja_iv$jaon_hyotysuhde', 61),
(2013, 'lt$lammitys$tilat_ja_iv$apulaitteet', 62),
(2013, 'lt$lammitys$lammin_kayttovesi$tuoton_hyotysuhde', 63),
(2013, 'lt$lammitys$lammin_kayttovesi$jaon_hyotysuhde', 64),

(2013, 'lt$lkvn_kaytto$ominaiskulutus', 65),
(2013, 'lt$lkvn_kaytto$lammitysenergian_nettotarve', 66)

on conflict (column_name, versio) do update
  set ordinal = excluded.ordinal;

insert into validation_sisainen_kuorma (
  versio, kayttotarkoitusluokka_id,

  valaistus$kayttoaste,
  valaistus$lampokuorma,
  kuluttajalaitteet$kayttoaste,
  kuluttajalaitteet$lampokuorma,
  henkilot$kayttoaste,
  henkilot$lampokuorma)
values
(2013, 1, 0.1, 8, 0.6, 3, 0.6, 2),
(2013, 2, 0.1, 8, 0.6, 3, 0.6, 2),
(2013, 3, 0.1, 11, 0.6, 4, 0.6, 3),
(2013, 4, 0.65, 12, 0.65, 12, 0.65, 5),
(2013, 5, 1, 19, 1, 1, 1, 2),
(2013, 6, 0.3, 14, 0.3, 4, 0.3, 4),
(2013, 7, 0.6, 18, 0.6, 8, 0.6, 14),
(2013, 8, 0.5, 12, 0.5, 0, 0.5, 5),
(2013, 10, 0.5, 12, 0.5, 0, 0.5, 5),
(2013, 9, 0.6, 9, 0.6, 9, 0.6, 8)
on conflict (kayttotarkoitusluokka_id, versio) do update
  set valaistus$kayttoaste = excluded.valaistus$kayttoaste,
      valaistus$lampokuorma = excluded.valaistus$lampokuorma,
      kuluttajalaitteet$kayttoaste = excluded.kuluttajalaitteet$kayttoaste,
      kuluttajalaitteet$lampokuorma = excluded.kuluttajalaitteet$lampokuorma,
      henkilot$kayttoaste = excluded.henkilot$kayttoaste,
      henkilot$lampokuorma = excluded.henkilot$lampokuorma;