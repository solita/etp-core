
create type ostettu_polttoaine as (
  nimi text,
  yksikko text,
  muunnoskerroin numeric,
  maara_vuodessa numeric
);

create type toimenpide as (
  nimi_fi text,
  nimi_sv text,
  lampo numeric,
  sahko numeric,
  jaahdytys numeric,
  eluvun_muutos numeric
);

create type userdefined_energiamuoto as (
  nimi text,
  muotokerroin numeric,
  ostoenergia numeric
);

create type userdefined_energia as (
  nimi_fi text,
  nimi_sv text,
  vuosikulutus numeric
);

create table energiatodistus (
 id int generated by default as identity primary key,
 versio int not null,
 tila_id int not null default 0 references energiatodistustila (id),
 allekirjoitusaika timestamp with time zone,
 laatija_id int not null references laatija (id),
 korvattu_energiatodistus_id integer references energiatodistus (id),

 pt$havainnointikaynti text,
 pt$katuosoite_fi text,
 pt$katuosoite_sv text,
 pt$kayttotarkoitus text,
 pt$keskeiset_suositukset_fi text,
 pt$keskeiset_suositukset_sv text,
 pt$kieli integer,
 pt$kiinteistotunnus text,
 pt$laatimisvaihe integer,
 pt$uudisrakennus boolean,
 pt$nimi text,
 pt$onko_julkinen_rakennus boolean,
 pt$postinumero text,
 pt$rakennusosa text,
 pt$rakennustunnus text,
 pt$tilaaja text,
 pt$valmistumisvuosi integer,
 pt$yritys$katuosoite text,
 pt$yritys$nimi text,
 pt$yritys$postinumero text,
 pt$yritys$postitoimipaikka text,

 lt$ikkunat$etela$U numeric,
 lt$ikkunat$etela$ala numeric,
 lt$ikkunat$etela$g_ks numeric,
 lt$ikkunat$ita$U numeric,
 lt$ikkunat$ita$ala numeric,
 lt$ikkunat$ita$g_ks numeric,
 lt$ikkunat$kaakko$U numeric,
 lt$ikkunat$kaakko$ala numeric,
 lt$ikkunat$kaakko$g_ks numeric,
 lt$ikkunat$katto$U numeric,
 lt$ikkunat$katto$ala numeric,
 lt$ikkunat$katto$g_ks numeric,
 lt$ikkunat$koillinen$U numeric,
 lt$ikkunat$koillinen$ala numeric,
 lt$ikkunat$koillinen$g_ks numeric,
 lt$ikkunat$lansi$U numeric,
 lt$ikkunat$lansi$ala numeric,
 lt$ikkunat$lansi$g_ks numeric,
 lt$ikkunat$lounas$U numeric,
 lt$ikkunat$lounas$ala numeric,
 lt$ikkunat$lounas$g_ks numeric,
 lt$ikkunat$luode$U numeric,
 lt$ikkunat$luode$ala numeric,
 lt$ikkunat$luode$g_ks numeric,
 lt$ikkunat$pohjoinen$U numeric,
 lt$ikkunat$pohjoinen$ala numeric,
 lt$ikkunat$pohjoinen$g_ks numeric,
 lt$ikkunat$valokupu$U numeric,
 lt$ikkunat$valokupu$ala numeric,
 lt$ikkunat$valokupu$g_ks numeric,
 lt$ilmanvaihto$erillispoistot$poisto numeric,
 lt$ilmanvaihto$erillispoistot$sfp numeric,
 lt$ilmanvaihto$erillispoistot$tulo numeric,
 lt$ilmanvaihto$ivjarjestelma$poisto numeric,
 lt$ilmanvaihto$ivjarjestelma$sfp numeric,
 lt$ilmanvaihto$ivjarjestelma$tulo numeric,
 lt$ilmanvaihto$kuvaus_fi text,
 lt$ilmanvaihto$kuvaus_sv text,
 lt$ilmanvaihto$lto_vuosihyotysuhde numeric,
 lt$ilmanvaihto$paaiv$jaatymisenesto numeric,
 lt$ilmanvaihto$paaiv$lampotilasuhde numeric,
 lt$ilmanvaihto$paaiv$poisto numeric,
 lt$ilmanvaihto$paaiv$sfp numeric,
 lt$ilmanvaihto$paaiv$tulo numeric,
 lt$jaahdytysjarjestelma$jaahdytyskauden_painotettu_kylmakerroin numeric,
 lt$lammitetty_nettoala numeric,
 lt$lammitys$ilmanlampopumppu$maara integer,
 lt$lammitys$ilmanlampopumppu$tuotto numeric,
 lt$lammitys$kuvaus_fi text,
 lt$lammitys$kuvaus_sv text,
 lt$lammitys$lammin_kayttovesi$apulaitteet numeric,
 lt$lammitys$lammin_kayttovesi$jaon_hyotysuhde numeric,
 lt$lammitys$lammin_kayttovesi$lampokerroin numeric,
 lt$lammitys$lammin_kayttovesi$tuoton_hyotysuhde numeric,
 lt$lammitys$takka$maara integer,
 lt$lammitys$takka$tuotto numeric,
 lt$lammitys$tilat_ja_iv$apulaitteet numeric,
 lt$lammitys$tilat_ja_iv$jaon_hyotysuhde numeric,
 lt$lammitys$tilat_ja_iv$lampokerroin numeric,
 lt$lammitys$tilat_ja_iv$tuoton_hyotysuhde numeric,
 lt$lkvn_kaytto$ominaiskulutus numeric,
 lt$lkvn_kaytto$lammitysenergian_nettotarve numeric,
 lt$rakennusvaippa$alapohja$U numeric,
 lt$rakennusvaippa$alapohja$ala numeric,
 lt$rakennusvaippa$ikkunat$U numeric,
 lt$rakennusvaippa$ikkunat$ala numeric,
 lt$rakennusvaippa$ilmanvuotoluku numeric,
 lt$rakennusvaippa$kylmasillat_UA numeric,
 lt$rakennusvaippa$ulkoovet$U numeric,
 lt$rakennusvaippa$ulkoovet$ala numeric,
 lt$rakennusvaippa$ulkoseinat$U numeric,
 lt$rakennusvaippa$ulkoseinat$ala numeric,
 lt$rakennusvaippa$ylapohja$U numeric,
 lt$rakennusvaippa$ylapohja$ala numeric,
 lt$sis_kuorma$henkilot$kayttoaste numeric,
 lt$sis_kuorma$henkilot$lampokuorma numeric,
 lt$sis_kuorma$kuluttajalaitteet$kayttoaste numeric,
 lt$sis_kuorma$kuluttajalaitteet$lampokuorma numeric,
 lt$sis_kuorma$valaistus$kayttoaste numeric,
 lt$sis_kuorma$valaistus$lampokuorma numeric,

 t$kaytettavat_energiamuodot$fossiilinen_polttoaine numeric,
 t$kaytettavat_energiamuodot$kaukojaahdytys numeric,
 t$kaytettavat_energiamuodot$kaukolampo numeric,
 t$kaytettavat_energiamuodot$sahko numeric,
 t$kaytettavat_energiamuodot$uusiutuva_polttoaine numeric,
 t$kaytettavat_energiamuodot$muu userdefined_energiamuoto[],
 t$lampokuormat$aurinko numeric,
 t$lampokuormat$ihmiset numeric,
 t$lampokuormat$kuluttajalaitteet numeric,
 t$lampokuormat$kvesi numeric,
 t$lampokuormat$valaistus numeric,
 t$laskentatyokalu text,
 t$nettotarve$ilmanvaihdon_lammitys_vuosikulutus numeric,
 t$nettotarve$jaahdytys_vuosikulutus numeric,
 t$nettotarve$kayttoveden_valmistus_vuosikulutus numeric,
 t$nettotarve$tilojen_lammitys_vuosikulutus numeric,
 t$tekniset_jarjestelmat$iv_sahko numeric,
 t$tekniset_jarjestelmat$jaahdytys$kaukojaahdytys numeric,
 t$tekniset_jarjestelmat$jaahdytys$lampo numeric,
 t$tekniset_jarjestelmat$jaahdytys$sahko numeric,
 t$tekniset_jarjestelmat$kayttoveden_valmistus$lampo numeric,
 t$tekniset_jarjestelmat$kayttoveden_valmistus$sahko numeric,
 t$tekniset_jarjestelmat$kuluttajalaitteet_ja_valaistus_sahko numeric,
 t$tekniset_jarjestelmat$tilojen_lammitys$lampo numeric,
 t$tekniset_jarjestelmat$tilojen_lammitys$sahko numeric,
 t$tekniset_jarjestelmat$tuloilman_lammitys$lampo numeric,
 t$tekniset_jarjestelmat$tuloilman_lammitys$sahko numeric,
 t$uusiutuvat_omavaraisenergiat$aurinkolampo numeric,
 t$uusiutuvat_omavaraisenergiat$aurinkosahko numeric,
 t$uusiutuvat_omavaraisenergiat$lampopumppu numeric,
 t$uusiutuvat_omavaraisenergiat$muulampo numeric,
 t$uusiutuvat_omavaraisenergiat$muusahko numeric,
 t$uusiutuvat_omavaraisenergiat$tuulisahko numeric,
 t$uusiutuvat_omavaraisenergiat$muu userdefined_energia[],

 to$kaukojaahdytys_vuosikulutus_yhteensa numeric,
 to$kaukolampo_vuosikulutus_yhteensa numeric,
 to$ostettu_energia$kaukojaahdytys_vuosikulutus numeric,
 to$ostettu_energia$kaukolampo_vuosikulutus numeric,
 to$ostettu_energia$kayttajasahko_vuosikulutus numeric,
 to$ostettu_energia$kiinteistosahko_vuosikulutus numeric,
 to$ostettu_energia$kokonaissahko_vuosikulutus numeric,
 to$ostettu_energia$muu userdefined_energia[],

 to$ostetut_polttoaineet$kevyt_polttooljy numeric,
 to$ostetut_polttoaineet$pilkkeet_havu_sekapuu numeric,
 to$ostetut_polttoaineet$pilkkeet_koivu numeric,
 to$ostetut_polttoaineet$puupelletit numeric,
 to$ostetut_polttoaineet$vapaa ostettu_polttoaine[],
 to$polttoaineet_vuosikulutus_yhteensa numeric,
 to$sahko_vuosikulutus_yhteensa numeric,

 h$alapohja_ylapohja$teksti_fi text,
 h$alapohja_ylapohja$teksti_sv text,
 h$alapohja_ylapohja$toimenpide toimenpide[],
 h$iv_ilmastointi$teksti_fi text,
 h$iv_ilmastointi$teksti_sv text,
 h$iv_ilmastointi$toimenpide toimenpide[],
 h$lammitys$teksti_fi text,
 h$lammitys$teksti_sv text,
 h$lammitys$toimenpide toimenpide[],
 h$lisatietoja_fi text,
 h$lisatietoja_sv text,
 h$suositukset_fi text,
 h$suositukset_sv text,
 h$valaistus_muut$teksti_fi text,
 h$valaistus_muut$teksti_sv text,
 h$valaistus_muut$toimenpide toimenpide[],
 h$ymparys$teksti_fi text,
 h$ymparys$teksti_sv text,
 h$ymparys$toimenpide toimenpide[],

 lisamerkintoja_fi text,
 lisamerkintoja_sv text,

 foreign key (pt$kayttotarkoitus, versio)
   references alakayttotarkoitusluokka (id, versio));

create table energiatodistus_tila_history (
  id int generated by default as identity primary key,
  modifytime timestamp with time zone not null default now(),
  modifiedby_id int not null default current_kayttaja_id() references kayttaja (id),
  energiatodistus_id int not null references energiatodistus (id),
  tila_id int not null references energiatodistustila (id)
);

create function energiatodistus_tila_audit() returns trigger as
$$
begin return new; end;
$$
language 'plpgsql';

create trigger energiatodistus_tila_insert_trigger
  after insert on energiatodistus for each row
execute procedure etp.energiatodistus_tila_audit();

create trigger energiatodistus_tila_update_trigger
  after update on energiatodistus for each row
  when ( (old.tila_id) is distinct from (new.tila_id) )
execute procedure etp.energiatodistus_tila_audit();

