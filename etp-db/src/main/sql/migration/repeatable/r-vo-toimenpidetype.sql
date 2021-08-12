insert into vo_toimenpidetype (id, label_fi, label_sv, ordinal)
values
(0,  'Katsottu', 'TODO', 1),
(1,  'Poikkeama', 'TODO', 2),
(2,  'Valvonnan aloitus', 'TODO', 3),
(3,  'Tietopyyntö', 'TODO', 4),
(4,  'Tietopyyntö / Vastaus', 'TODO', 5),
(5,  'Tietopyyntö / Kehotus', 'TODO', 6),
(6,  'Tietopyyntö / Varoitus', 'TODO', 7),
(7,  'Valvontamuistio', 'TODO', 8),
(8,  'Valvontamuistio / Vastaus', 'TODO', 9),
(9,  'Valvontamuistio / Kehotus', 'TODO', 10),
(10, 'Valvontamuistio / Varoitus', 'TODO', 11),
(11, 'Lisäselvityspyyntö', 'TODO', 12),
(12, 'Lisäselvityspyyntö / Vastaus', 'TODO', 13),
(13, 'Kieltopäätös', 'TODO', 14),
(14, 'Valvonnan lopetus', 'TODO', 15)
on conflict (id) do update set
  label_fi = excluded.label_fi,
  label_sv = excluded.label_sv,
  ordinal = excluded.ordinal;

insert into vo_template (id, label_fi, label_sv, ordinal, toimenpidetype_id, language, content)
values
(1, 'Taustamateriaalin toimituspyyntö FI', 'TODO', 1, 3, 'fi', 
'<div class="otsikko">
    <b>Toimituspyyntö</b> <br/>
    <b>{{päivä}}</b> <br/>
    {{diaarinumero}}
</div>

<p class="oikeellisuus-kohde">
    <span class="isot-kirjaimet">{{#laatija}} {{etunimi}} {{sukunimi}} {{/laatija}}</span> <br/>

    {{#energiatodistus}}
    Kohde: {{nimi}} <br/>
    Todistustunnus: {{tunnus}} <br/>
    {{/energiatodistus}}
</p>


<p>Asumisen rahoitus- ja kehittämiskeskuksen (ARA) tehtävänä on valvoa energiatodistusten oikeellisuutta.
    Oikeellisuustarkastukset kohdistuvat energiatodistusten lähtötietoihin, energiatehokkuusluvun laskentaan sekä
    säästösuositusten oikeellisuuteen.</p>

<p><b>ARA tulee tarkastamaan tämän todistuksen oikeellisuuden.</b> Pyydämme, että toimitatte seuraavat todistuksen
    laadinnassa käytetyt taustamateriaalit ARAn energiatodistusrekisteriin {{määräpäivä}} mennessä:</p>

<ul>
    <li>Pääpiirustukset (asema-, pohja-, julkisivu- ja leikkauspiirustukset sekä U-arvot)</li>
    <li>E-lukulaskentaan vaikuttavat ilmanvaihto-, jäähdytys- ja lämmitysjärjestelmien laskelmat ja tekniset tiedot (ei
        pohjakuvia)
    </li>
    <li>Valaistuslaskelmat, jos E-lukulaskennassa on käytetty tarpeenmukaista valaistusta</li>
    <li>Tiiveysmittausraportti, jos mittaus on suoritettu</li>
    <li>Energiaselvitys (uudiskohteet)</li>
    <li>Havainnointipöytäkirja ja muu materiaali paikan päällä käynnistä (olemassa olevat rakennukset)</li>
</ul>

<div class="sivunvaihto"></div>

<p>ARAlla on oikeus saada valvontaa varten tarvittavat tiedot ja asiakirjat, mukaan lukien toimeksiantoja koskevat
    tiedot. Laatijan on säilytettävä valmisteluasiakirjat, laskelmat ja muut tiedot, jotka laatija on tehnyt tai
    hankkinut todistuksen laatimista varten sekä tiedot todistuksen kohteessa tehdystä havainnoinnista. Laatijan on
    pidettävä arkistoa laatimistaan todistuksista. Asiakirjat, tiedot ja todistukset on säilytettävä vähintään 12
    vuotta.</p>

<p>Jos energiatodistuksen laatija ei täytä säädettyjä velvollisuuksia, ARA kehottaa korjaamaan asian ja antaa määräajan
    korjaukselle. Jos asiaa ei korjata määräajassa, ARA antaa laatijalle varoituksen ja uuden määräajan. Jos asiaa ei
    edelleenkään korjata, ARA voi antaa laatijalle velvoittavan käskypäätöksen/käyttökieltopäätöksen, jota voidaan
    tehostaa uhkasakolla. ARAlla on myös mahdollisuus antaa laatijalle laatimiskielto, jos laatija on toiminut
    olennaisella tai merkittävällä tavalla säännösten vastaisesti.</p>

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
        <td class="sarake-otsikko"><b>Lisätietoja:</b></td>
        <td class="sarake-sisalto"><a href="https://www.energiatodistusrekisteri.fi">www.energiatodistusrekisteri.fi</a></td>
    </tr>
</table>'),
(2, 'Taustamateriaalin kehotus FI', 'TODO', 2, 5, 'fi', 
'<div class="otsikko">
    <b>KEHOTUS</b> <br/>
    <b>{{päivä}}</b> <br/>
    {{diaarinumero}}
</div>

<p class="oikeellisuus-kohde">
    <span class="isot-kirjaimet">{{#laatija}} {{etunimi}} {{sukunimi}} {{/laatija}}</span> <br/>

    {{#energiatodistus}}
    Kohde: {{nimi}} <br/>
    Todistustunnus: {{tunnus}} <br/>
    Toimituspyynnön päivämäärä: {{#taustamateriaali}} {{taustamateriaali-pvm}} {{/taustamateriaali}}
    {{/energiatodistus}}
</p>


<p>Asumisen rahoitus- ja kehittämiskeskuksen (ARA) tehtävänä on valvoa energiatodistusten oikeellisuutta.
    Oikeellisuustarkastukset kohdistuvat energiatodistusten lähtötietoihin, energiatehokkuusluvun laskentaan sekä
    säästösuositusten oikeellisuuteen.</p>

<p>ARA on lähettänyt teille tästä energiatodistuksesta taustamateriaalin toimituspyynnön. ARA pyytää toimittamaan
    taustamateriaalin kuukauden kuluessa tämän kehotuksen päiväyksestä. <b>ARA tulee tarkastamaan todistuksen
        oikeellisuuden tämän materiaalin pohjalta.</b> Pyydämme, että toimitatte seuraavat todistuksen laadinnassa
    käytetyt taustamateriaalit ARAn energiatodistusrekisteriin {{määräpäivä}} mennessä:</p>

<ul>
    <li>Pääpiirustukset (asema-, pohja-, julkisivu- ja leikkauspiirustukset sekä U-arvot)</li>
    <li>E-lukulaskentaan vaikuttavat ilmanvaihto-, jäähdytys- ja lämmitysjärjestelmien laskelmat ja tekniset tiedot (ei
        pohjakuvia)
    </li>
    <li>Valaistuslaskelmat, jos E-lukulaskennassa on käytetty tarpeenmukaista valaistusta</li>
    <li>Tiiveysmittausraportti, jos mittaus on suoritettu</li>
    <li>Energiaselvitys (uudiskohteet)</li>
    <li>Havainnointipöytäkirja ja muu materiaali paikan päällä käynnistä (olemassa olevat rakennukset)</li>
</ul>

<p>ARAlla on oikeus saada valvontaa varten tarvittavat tiedot ja asiakirjat, mukaan lukien toimeksiantoja koskevat
    tiedot. Laatijan on säilytettävä valmisteluasiakirjat, laskelmat ja muut tiedot, jotka laatija on tehnyt tai
    hankkinut todistuksen laatimista varten sekä tiedot todistuksen kohteessa tehdystä havainnoinnista. Laatijan on
    pidettävä arkistoa laatimistaan todistuksista. Asiakirjat, tiedot ja todistukset on säilytettävä vähintään 12
    vuotta.</p>

<p>Jos energiatodistuksen laatija ei täytä säädettyjä velvollisuuksia, ARA kehottaa korjaamaan asian ja antaa määräajan
    korjaukselle. Jos asiaa ei korjata määräajassa, ARA antaa laatijalle varoituksen ja uuden määräajan. Jos asiaa ei
    edelleenkään korjata, ARA voi antaa laatijalle velvoittavan käskypäätöksen/käyttökieltopäätöksen, jota voidaan
    tehostaa uhkasakolla. ARAlla on myös mahdollisuus antaa laatijalle laatimiskielto, jos laatija on toiminut
    olennaisella tai merkittävällä tavalla säännösten vastaisesti.</p>

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
        <td class="sarake-otsikko"><b>Lisätietoja:</b></td>
        <td class="sarake-sisalto"><a href="https://www.energiatodistusrekisteri.fi">www.energiatodistusrekisteri.fi</a></td>
    </tr>
</table>'),
(3, 'Taustamateriaalin varoitus FI', 'TODO', 3, 6, 'fi', 
'<div class="otsikko">
    <b>VAROITUS</b> <br/>
    <b>{{päivä}}</b> <br/>
    {{diaarinumero}}
</div>

<p class="oikeellisuus-kohde">
    <span class="isot-kirjaimet">{{#laatija}} {{etunimi}} {{sukunimi}} {{/laatija}}</span> <br/>

    {{#energiatodistus}}
    Kohde: {{nimi}} <br/>
    Todistustunnus: {{tunnus}} <br/>
    Toimituspyynnön päivämäärä: {{#taustamateriaali}} {{taustamateriaali-pvm}} {{/taustamateriaali}} <br />
    Kehotuksen päivämäärä: {{#taustamateriaali}} {{taustamateriaali-kehotus-pvm}} {{/taustamateriaali}}
    {{/energiatodistus}}
</p>


<p>Asumisen rahoitus- ja kehittämiskeskuksen (ARA) tehtävänä on valvoa energiatodistusten oikeellisuutta.
    Oikeellisuustarkastukset kohdistuvat energiatodistusten lähtötietoihin, energiatehokkuusluvun laskentaan sekä
    säästösuositusten oikeellisuuteen.</p>

<p>ARA on lähettänyt teille tästä energiatodistuksesta taustamateriaalin toimituspyynnön ja kehotuksen. ARA antaa
    varoituksen ja vaatii toimittamaan taustamateriaalin kuukauden kuluessa tämän varoituksen päiväyksestä.
    <b>ARA tulee tarkastamaan todistuksen oikeellisuuden tämän materiaalin pohjalta.</b> Pyydämme, että toimitatte
    seuraavat todistuksen laadinnassa käytetyt taustamateriaalit ARAn energiatodistusrekisteriin {{määräpäivä}}
    mennessä:</p>

<ul>
    <li>Pääpiirustukset (asema-, pohja-, julkisivu- ja leikkauspiirustukset sekä U-arvot)</li>
    <li>E-lukulaskentaan vaikuttavat ilmanvaihto-, jäähdytys- ja lämmitysjärjestelmien laskelmat ja tekniset tiedot (ei
        pohjakuvia)
    </li>
    <li>Valaistuslaskelmat, jos E-lukulaskennassa on käytetty tarpeenmukaista valaistusta</li>
    <li>Tiiveysmittausraportti, jos mittaus on suoritettu</li>
    <li>Energiaselvitys (uudiskohteet)</li>
    <li>Havainnointipöytäkirja ja muu materiaali paikan päällä käynnistä (olemassa olevat rakennukset)</li>
</ul>

<p>ARAlla on oikeus saada valvontaa varten tarvittavat tiedot ja asiakirjat, mukaan lukien toimeksiantoja koskevat
    tiedot. Laatijan on säilytettävä valmisteluasiakirjat, laskelmat ja muut tiedot, jotka laatija on tehnyt tai
    hankkinut todistuksen laatimista varten sekä tiedot todistuksen kohteessa tehdystä havainnoinnista. Laatijan on
    pidettävä arkistoa laatimistaan todistuksista. Asiakirjat, tiedot ja todistukset on säilytettävä vähintään 12
    vuotta.</p>

<p>Jos energiatodistuksen laatija ei täytä säädettyjä velvollisuuksia, ARA kehottaa korjaamaan asian ja antaa määräajan
    korjaukselle. Jos asiaa ei korjata määräajassa, ARA antaa laatijalle varoituksen ja uuden määräajan. Jos asiaa ei
    edelleenkään korjata, ARA voi antaa laatijalle velvoittavan käskypäätöksen/käyttökieltopäätöksen, jota voidaan
    tehostaa uhkasakolla. ARAlla on myös mahdollisuus antaa laatijalle laatimiskielto, jos laatija on toiminut
    olennaisella tai merkittävällä tavalla säännösten vastaisesti.</p>

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
        <td class="sarake-otsikko"><b>Lisätietoja:</b></td>
        <td class="sarake-sisalto"><a href="https://www.energiatodistusrekisteri.fi">www.energiatodistusrekisteri.fi</a></td>
    </tr>
</table>'),
(4, 'Valvontamuistio FI', 'TODO', 4, 7, 'fi', 
'<div class="otsikko">
    <b>VALVONTAMUISTIO</b> <br/>
    <b>{{päivä}}</b> <br/>
    {{diaarinumero}}
</div>

<p class="oikeellisuus-kohde">
    <span class="isot-kirjaimet">{{#laatija}} {{etunimi}} {{sukunimi}} {{/laatija}}</span> <br/>

    {{#energiatodistus}}
    Kohde: {{nimi}} <br/>
    Todistustunnus: {{tunnus}} <br/>
    {{/energiatodistus}}
</p>


<p>Asumisen rahoitus- ja kehittämiskeskus (ARA) on energiatodistuslain (50/2013) 18 §:n nojalla tarkastanut laatimanne energiatodistuksen. Oikeellisuustarkastus on kohdistunut energiatodistuksen lähtötietoihin, energiatehokkuusluvun laskentaan sekä säästösuositusten oikeellisuuteen. Tarkastuksen perusteella kiinnitettiin huomiota seuraaviin asioihin:</p>

<ul>
    {{#valvontamuistio}}
        {{#virheet}}
            <li>{{description}}</li>
        {{/virheet}}
    {{/valvontamuistio}}
</ul>

{{#valvontamuistio}}
    {{#vakavuus}}
        {{#ei-huomioitavaa}}
            <p>ARA toteaa, että energiatodistuksessa ei ole havaittu huomautettavaa.</p>
        {{/ei-huomioitavaa}}
        {{#ei-toimenpiteitä}}
            <p>ARA toteaa, että energiatodistuksessa ei ole havaittu muuta huomautettavaa kuin edellä mainitut. <u>Havainnot eivät
               edellytä laatijalta toimenpiteitä.</u></p>
        {{/ei-toimenpiteitä}}
        {{#virheellinen}}
          <p>ARA toteaa, että energiatodistus voi edellä luetelluin perustein olla olennaisesti virheellinen eikä todistuksen
             sisältöä käyty muilta osin lävitse. ARA pyytää esittämään omat perustelunne tarkastuksessa tehdyistä havainnoista ja
             toimittamaan ne ARAn energiatodistusrekisteriin {{määräpäivä}} mennessä.</p>
        {{/virheellinen}}
    {{/vakavuus}}
{{/valvontamuistio}}


<p>Jos energiatodistuksen laatija ei täytä säädettyjä velvollisuuksia, ARA kehottaa korjaamaan asian ja antaa määräajan korjaukselle. Jos asiaa ei korjata määräajassa, ARA antaa laatijalle varoituksen ja uuden määräajan. Jos asiaa ei edelleenkään korjata, ARA laittaa todistuksen käyttökieltoon ja velvoittaa energiatodistuksen laatijan korvaamaan virheellisen todistuksen uudella todistuksella. Energiatodistus voidaan tarvittaessa teettää myös toisella energiatodistuksen laatijalla. Uuden todistuksen kustannuksista vastaa virheellisen todistuksen laatinut energiatodistuksen laatija. </p>

<p>ARAlla on myös mahdollisuus antaa laatijalle laatimiskielto, jos laatija on toiminut olennaisella tai merkittävällä tavalla säännösten vastaisesti.</p>

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
</table>'),
(5, 'Valvontamuistion kehotus FI', 'TODO', 5, 9, 'fi', 
'<div class="otsikko">
    <b>KEHOTUS</b> <br/>
    <b>{{päivä}}</b> <br/>
    {{diaarinumero}}
</div>

<p class="oikeellisuus-kohde">
    <span class="isot-kirjaimet">{{#laatija}} {{etunimi}} {{sukunimi}} {{/laatija}}</span> <br/>

    {{#energiatodistus}}
    Kohde: {{nimi}} <br/>
    Todistustunnus: {{tunnus}} <br/>
    Valvontamuistion päivämäärä: {{#valvontamuistio}} {{valvontamuistio-pvm}} {{/valvontamuistio}}
    {{/energiatodistus}}
</p>


<p>Asumisen rahoitus- ja kehittämiskeskuksen (ARA) tehtävänä on valvoa energiatodistusten oikeellisuutta.
    Oikeellisuustarkastukset kohdistuvat energiatodistusten lähtötietoihin, energiatehokkuusluvun laskentaan sekä
    säästösuositusten oikeellisuuteen.</p>

<p>ARA on lähettänyt teille tästä energiatodistuksesta valvontamuistion. ARA on valvontamuistiossa todennut, että
    energiatodistus voi olla olennaisesti virheellinen. <b>ARA kehottaa teitä korjaamaan energiatodistuksen {{määräpäivä}}
    mennessä.</b></p>


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
</table>'),
(6, 'Valvontamuistion varoitus FI', 'TODO', 6, 10, 'fi', 
'<div class="otsikko">
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
</table>')
on conflict (id) do update set
  label_fi = excluded.label_fi,
  label_sv = excluded.label_sv,
  ordinal = excluded.ordinal,
  toimenpidetype_id = excluded.toimenpidetype_id,
  language = excluded.language,
  content = excluded.content;