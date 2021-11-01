
insert into validation_required_column (versio, column_name, ordinal, bypass_allowed)
values
(2018, 'pt$kieli', 0, false),
(2018, 'pt$laatimisvaihe', 1, false),
(2018, 'pt$havainnointikaynti', 2, false),
(2018, 't$laskentatyokalu', 3, false),

(2018, 'pt$nimi', 4, false),
(2018, 'pt$valmistumisvuosi', 5, false),
(2018, 'pt$katuosoite_fi', 6, false),
(2018, 'pt$katuosoite_sv', 7, false),
(2018, 'pt$postinumero', 8, false),
(2018, 'pt$rakennustunnus', 9, true),
(2018, 'pt$kayttotarkoitus', 10, false),
(2018, 'pt$keskeiset_suositukset_fi', 11, false),
(2018, 'pt$keskeiset_suositukset_sv', 12, false),

(2018, 'lt$lammitetty_nettoala', 13, false),
(2018, 'lt$rakennusvaippa$ilmanvuotoluku', 14, false),
(2018, 'lt$rakennusvaippa$ulkoseinat$ala', 15, false),
(2018, 'lt$rakennusvaippa$ylapohja$ala', 16, false),
(2018, 'lt$rakennusvaippa$alapohja$ala', 17, false),
(2018, 'lt$rakennusvaippa$ikkunat$ala', 18, false),
(2018, 'lt$rakennusvaippa$ulkoovet$ala', 19, false),
(2018, 'lt$rakennusvaippa$ulkoseinat$U', 20, false),
(2018, 'lt$rakennusvaippa$ylapohja$U', 21, false),
(2018, 'lt$rakennusvaippa$alapohja$U', 22, false),
(2018, 'lt$rakennusvaippa$ikkunat$U', 23, false),
(2018, 'lt$rakennusvaippa$ulkoovet$U', 24, false),
(2018, 'lt$rakennusvaippa$kylmasillat_UA', 25, false),

(2018, 'lt$ikkunat$etela$U', 26, false),
(2018, 'lt$ikkunat$etela$g_ks', 27, false),
(2018, 'lt$ikkunat$ita$U', 28, false),
(2018, 'lt$ikkunat$ita$g_ks', 29, false),
(2018, 'lt$ikkunat$kaakko$U', 30, false),
(2018, 'lt$ikkunat$kaakko$g_ks', 31, false),
(2018, 'lt$ikkunat$koillinen$U', 32, false),
(2018, 'lt$ikkunat$koillinen$g_ks', 33, false),
(2018, 'lt$ikkunat$lansi$U', 34, false),
(2018, 'lt$ikkunat$lansi$g_ks', 35, false),
(2018, 'lt$ikkunat$lounas$U', 36, false),
(2018, 'lt$ikkunat$lounas$g_ks', 37, false),
(2018, 'lt$ikkunat$luode$U', 38, false),
(2018, 'lt$ikkunat$luode$g_ks', 39, false),
(2018, 'lt$ikkunat$pohjoinen$U', 40, false),
(2018, 'lt$ikkunat$pohjoinen$g_ks', 41, false),

(2018, 'lt$ilmanvaihto$tyyppi_id', 42, false),
(2018, 'lt$ilmanvaihto$kuvaus_fi', 43, false),
(2018, 'lt$ilmanvaihto$kuvaus_sv', 44, false),
(2018, 'lt$ilmanvaihto$paaiv$tulo', 45, false),
(2018, 'lt$ilmanvaihto$paaiv$poisto', 46, false),
(2018, 'lt$ilmanvaihto$erillispoistot$tulo', 47, false),
(2018, 'lt$ilmanvaihto$erillispoistot$poisto', 48, false),
(2018, 'lt$ilmanvaihto$ivjarjestelma$tulo', 49, false),
(2018, 'lt$ilmanvaihto$ivjarjestelma$poisto', 50, false),

(2018, 'lt$lammitys$lammitysmuoto_1$id', 51, false),
(2018, 'lt$lammitys$lammitysmuoto_1$kuvaus_fi', 52, false),
(2018, 'lt$lammitys$lammitysmuoto_1$kuvaus_sv', 53, false),
(2018, 'lt$lammitys$lammitysmuoto_2$kuvaus_fi', 54, false),
(2018, 'lt$lammitys$lammitysmuoto_2$kuvaus_sv', 55, false),
(2018, 'lt$lammitys$lammonjako$id', 56, false),
(2018, 'lt$lammitys$lammonjako$kuvaus_fi', 57, false),
(2018, 'lt$lammitys$lammonjako$kuvaus_sv', 58, false),
(2018, 'lt$lammitys$tilat_ja_iv$jaon_hyotysuhde', 59, false),
(2018, 'lt$lammitys$tilat_ja_iv$apulaitteet', 60, false),
(2018, 'lt$lammitys$lammin_kayttovesi$jaon_hyotysuhde', 61, false),

(2018, 'lt$lkvn_kaytto$ominaiskulutus', 62, false),
(2018, 'lt$lkvn_kaytto$lammitysenergian_nettotarve', 63, false),

(2018, 'h$ymparys$teksti_fi', 64, false),
(2018, 'h$ymparys$teksti_sv', 65, false)

on conflict (column_name, versio) do update set
  ordinal = excluded.ordinal,
  bypass_allowed = excluded.bypass_allowed;

insert into validation_sisainen_kuorma (
  versio, kayttotarkoitusluokka_id,

  valaistus$kayttoaste,
  valaistus$lampokuorma,
  kuluttajalaitteet$kayttoaste,
  kuluttajalaitteet$lampokuorma,
  henkilot$kayttoaste,
  henkilot$lampokuorma)
values
(2018, 1, 0.1, 6, 0.6, 3, 0.6, 2),
(2018, 2, 0.1, 9, 0.6, 4, 0.6, 3),
(2018, 3, 0.65, 10, 0.65, 12, 0.65, 5),
(2018, 4, 1, 19, 1, 1, 1, 2),
(2018, 5, 0.3, 11, 0.3, 4, 0.3, 4),
(2018, 6, 0.6, 14, 0.6, 8, 0.6, 14),
(2018, 7, 0.5, 10, 0.5, 0, 0.5, 5),
(2018, 8, 0.6, 7, 0.6, 9, 0.6, 8)
on conflict (kayttotarkoitusluokka_id, versio) do update
  set valaistus$kayttoaste = excluded.valaistus$kayttoaste,
      valaistus$lampokuorma = excluded.valaistus$lampokuorma,
      kuluttajalaitteet$kayttoaste = excluded.kuluttajalaitteet$kayttoaste,
      kuluttajalaitteet$lampokuorma = excluded.kuluttajalaitteet$lampokuorma,
      henkilot$kayttoaste = excluded.henkilot$kayttoaste,
      henkilot$lampokuorma = excluded.henkilot$lampokuorma;
