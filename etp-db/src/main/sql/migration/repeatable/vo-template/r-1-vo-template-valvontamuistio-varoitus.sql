insert into vo_template (id, label_fi, label_sv, ordinal, toimenpidetype_id, language, content)
values (6, 'Valvontamuistion varoitus FI', 'TODO', 6, 10, 'fi', 
$$
<div class="otsikko">
    <b>VAROITUS</b> <br/>
    <b>{{päivä}}</b> <br/>
    {{diaarinumero}}
</div>

<p class="oikeellisuus-kohde">
    <span class="isot-kirjaimet">{{#laatija}} {{etunimi}} {{sukunimi}} {{/laatija}}</span> <br/>

    {{#energiatodistus}}
    Kohde: {{nimi}} <br/>
    Todistustunnus: {{tunnus}} <br/>
    Valvontamuistion päivämäärä: {{#valvontamuistio}} {{valvontamuistio-pvm}} {{/valvontamuistio}} <br />
    Kehotuksen päivämäärä: {{#valvontamuistio}} {{valvontamuistio-kehotus-pvm}} {{/valvontamuistio}}
    {{/energiatodistus}}
</p>


<p>Asumisen rahoitus- ja kehittämiskeskuksen (ARA) tehtävänä on valvoa energiatodistusten oikeellisuutta.
    Oikeellisuustarkastukset kohdistuvat energiatodistusten lähtötietoihin, energiatehokkuusluvun laskentaan sekä
    säästösuositusten oikeellisuuteen.</p>

<p>ARA on lähettänyt teille tästä energiatodistuksesta valvontamuistion. ARA on valvontamuistiossa todennut, että
    energiatodistus voi olla olennaisesti virheellinen. <b>ARA antaa teille varoituksen ja vaatii korjaamaan
    energiatodistuksen {{määräpäivä}} mennessä.</b> </p>


<p>ARAlla on oikeus saada valvontaa varten tarvittavat tiedot ja asiakirjat, mukaan lukien toimeksiantoja koskevat
    tiedot. Laatijan on säilytettävä valmisteluasiakirjat, laskelmat ja muut tiedot, jotka laatija on tehnyt tai
    hankkinut todistuksen laatimista varten sekä tiedot todistuksen kohteessa tehdystä havainnoinnista. Laatijan on
    pidettävä arkistoa laatimistaan todistuksista. Asiakirjat, tiedot ja todistukset on säilytettävä vähintään 12
    vuotta.</p>

<p>Jos energiatodistuksen laatija ei täytä säädettyjä velvollisuuksia, ARA kehottaa korjaamaan asian ja antaa määräajan
    korjaukselle. Jos asiaa ei korjata määräajassa, ARA antaa laatijalle varoituksen ja uuden määräajan. Jos asiaa ei
    edelleenkään korjata, ARA laittaa todistuksen käyttökieltoon ja velvoittaa energiatodistuksen laatijan korvaamaan
    virheellisen todistuksen uudella todistuksella. Energiatodistus voidaan tarvittaessa teettää myös toisella
    energiatodistuksen laatijalla. Uuden todistuksen kustannuksista vastaa virheellisen todistuksen laatinut
    energiatodistuksen laatija. </p>

<p>ARAlla on myös mahdollisuus antaa laatijalle laatimiskielto, jos laatija on toiminut olennaisella tai merkittävällä
    tavalla säännösten vastaisesti.</p>

<p>
    {{#valvoja}}
    {{etunimi}} {{sukunimi}}
    {{/valvoja}}<br/>
    energia-asiantuntija
</p>

<table class="sarake">
    <tr>
        <td class="sarake-otsikko"><b>Sovelletut säännökset:</b></td>
        <td class="sarake-sisalto">Laki rakennuksen energiatodistuksesta (50/2013)</td>
    </tr>
    <tr>
        <td class="sarake-otsikko"><b>Tiedoksi:</b></td>
        <td class="sarake-sisalto"><div>{{#tiedoksi}}{{.}}<br />{{/tiedoksi}}</div></td>
    </tr>
</table>
<table class="sarake">
    <tr>
        <td class="sarake-otsikko"><b>Lisätietoja:</b></td>
        <td class="sarake-sisalto"><a href="https://www.energiatodistusrekisteri.fi">www.energiatodistusrekisteri.fi</a></td>
    </tr>
</table>
$$)
on conflict (id) do update set
  label_fi = excluded.label_fi,
  label_sv = excluded.label_sv,
  ordinal = excluded.ordinal,
  toimenpidetype_id = excluded.toimenpidetype_id,
  language = excluded.language,
  content = excluded.content;