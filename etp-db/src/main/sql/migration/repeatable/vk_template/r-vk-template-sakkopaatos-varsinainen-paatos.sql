insert into vk_template (id,
                         label_fi,
                         label_sv,
                         ordinal,
                         toimenpidetype_id,
                         language,
                         valid,
                         tiedoksi,
                         content)
values (9, 'Sakkopäätös / varsinainen päätös', 'Sakkopäätös / varsinainen päätös (sv)', 1, 15, 'fi', true, true,
        $$
<div class="otsikko-ja-vastaanottaja-container">
    <div class="otsikko">
        <b>Päätös</b>
        <b>{{päivä}}</b> <br/>
        Dnro: {{diaarinumero}}
    </div>

    <div class="vastaanottaja">
        {{#omistaja-henkilo}}
        {{etunimi}} {{sukunimi}}<br/>
        {{jakeluosoite}}<br/>
        {{postinumero}} {{postitoimipaikka}}
        {{/omistaja-henkilo}}
        {{#omistaja-yritys}}
        {{nimi}}<br/>
        {{vastaanottajan-tarkenne}}<br/>
        {{jakeluosoite}}<br/>
        {{postinumero}} {{postitoimipaikka}}
        {{/omistaja-yritys}}
    </div>
</div>

<h1>Uhkasakon tuomitseminen maksettavaksi</h1>

<div>
    Vastaanottaja/asianosainen: {{#omistaja-henkilo}}{{etunimi}} {{sukunimi}}{{/omistaja-henkilo}}{{#omistaja-yritys}}{{nimi}}{{/omistaja-yritys}}<br/>
</div>

<h2>Päätös</h2>

<p>Asumisen rahoitus- ja kehittämiskeskus (ARA) tuomitsee
    {{#omistaja-henkilo}}
    {{etunimi}} {{sukunimi}}
    {{/omistaja-henkilo}}
    {{#omistaja-yritys}}
    {{nimi}}
    {{/omistaja-yritys}}
    (jäljempänä Asianosainen) maksettavaksi
    hänelle ARAn päätöksessä {{#aiemmat-toimenpiteet}}{{varsinainen-paatos-pvm}} (dnro {{varsinainen-paatos-diaarinumero}}, jäljempänä ARAn päätös){{/aiemmat-toimenpiteet}} asetetun {{#tyyppikohtaiset-tiedot}}{{fine}}{{/tyyppikohtaiset-tiedot}} euron
    kiinteän uhkasakon, koska päävelvoitetta ei ole noudatettu, eikä noudattamatta jättämiseen ole pätevää syytä.
    Uhkasakko tuomitaan maksettavaksi kerralla.
    Tilisiirtolomake uhkasakon maksamiseksi on tämän päätöksen liitteenä. Uhkasakko maksetaan valtiolle ja sen
    perinnästä vastaa Oikeusrekisterikeskus.
</p>

<h2>Kuuleminen</h2>

{{#aiemmat-toimenpiteet}}
<p>ARA varasi {{sakkopaatos-kuulemiskirje-pvm}} päivätyllä kirjeellään (dnro {{sakkopaatos-kuulemiskirje-diaarinumero}}) Asianosaiselle uhkasakkolain 22 §:n ja
    siinä viitatun hallintolain (434/2003) 34 §:n mukaisesti ennen asian ratkaisemista tilaisuuden lausua
    mielipiteensä asiasta sekä antaa selityksensä sellaisista vaatimuksista ja selvityksistä, jotka saattavat
    vaikuttaa asian ratkaisuun.</p>
{{/aiemmat-toimenpiteet}}

<p class="respect-new-lines">{{#tyyppikohtaiset-tiedot}}{{vastaus-fi}}{{/tyyppikohtaiset-tiedot}}</p>

<h3>Päätöksen perustelut</h3>

<p>Rakennuksen energiatodistuksesta annetun lain (50/2013, energiatodistuslaki) 6 §:n mukaan rakennusta, huoneistoa tai
    niiden hallintaoikeutta myytäessä tai vuokrattaessa täytyy olla energiatodistus. Energiatodistus on annettava joko
    alkuperäisenä tai jäljennöksenä ostajalle tai vuokralaiselle. Kohteen julkisessa myynti- tai vuokrausilmoituksessa
    on mainittava myytävän tai vuokrattavan kohteen energiatehokkuusluokka. Rakennuksen omistaja tai haltija vastaa
    siitä, että rakennuksen energiatodistus hankitaan ja sitä käytetään laissa säädetyissä tilanteissa
    (energiatodistuslain 2 §).</p>

<p>Seuraamuksista säädetään energiatodistuslain 24 §:ssä. Jos rakennuksen omistaja ei täytä energiatodistuslaissa
    säädettyjä velvollisuuksia tai toimii muutoin energiatodistuslain tai sen nojalla annettujen säännösten
    vastaisesti, ARAn on kehotettava korjaamaan asiantila ja asetettava määräaika asiantilan korjaamiselle. Jos asiaa
    ei korjata määräajassa, ARAn on annettava asianomaiselle taholle varoitus ja uusi määräaika. Jos asiantilaa ei
    määräajassa korjata, ARAn tulee antaa tilanteen mukainen käsky- tai kieltopäätös. ARA voi tehostaa käskyä tai
    kieltoa vakavissa tai olennaisissa rikkomuksissa uhkasakolla tai teettämis- tai keskeyttämisuhalla, joista
    säädetään uhkasakkolaissa (1113/1990).</p>

<p>Uhkasakkolain (1113/1990) 10 §:n mukaan uhkasakon asettanut viranomainen voi tuomita uhkasakon maksettavaksi, jos
    päävelvoitetta ei ole noudatettu, eikä noudattamatta jättämiseen ole pätevää syytä. Edellytyksenä uhkasakon
    tuomitsemiselle maksettavaksi on myös, että uhkasakon asettamista koskeva päätös on lainvoimainen, jollei päätöstä
    ole säädetty tai määrätty noudatettavaksi muutoksenhausta huolimatta. Lain 7 §:n mukaan kullekin asianosaiselle on
    asetettava eri uhkasakko, jos uhkasakko kohdistetaan useisiin asianosaisiin.</p>

<p>Asianosainen on laiminlyönyt noudattaa energiatodistuslain 6 §:ssä säädettyä velvollisuutta käyttää energiatodistusta
    rakennuksen myyntitilanteessa. ARA antoi laiminlyönnin johdosta Asianosaiselle energiatodistuslain 24 §:n mukaisen
    kehotuksen ja varoituksen energiatodistuksen hankkimiseen, mutta niitä ei noudatettu. Näin ollen ARA teki
    {{#aiemmat-toimenpiteet}}{{varsinainen-paatos-pvm}}{{/aiemmat-toimenpiteet}} päätöksen, jolla se käski Asianosaisen hankkimaan osoitteessa {{#kohde}}{{katuosoite}} {{postinumero}} {{postitoimipaikka}}{{/kohde}}
    sijaitsevalle rakennukselle energiatodistuksen viimeistään {{#aiemmat-toimenpiteet}}{{varsinainen-paatos-maarapaiva}}{{/aiemmat-toimenpiteet}} ja esittämään energiatodistuksen ARAlle.
    Päätöksen tehosteeksi ARA asetti {{#aiemmat-toimenpiteet}}{{varsinainen-paatos-fine}}{{/aiemmat-toimenpiteet}} euron suuruisen kiinteän uhkasakon. Päätöksen mukaan uhkasakko tuomitaan
    maksettavaksi, mikäli ARAn käskyä ei noudateta annetussa määräajassa.</p>

{{#tyyppikohtaiset-tiedot}}
<p class="respect-new-lines">{{statement-fi}}</p>
{{/tyyppikohtaiset-tiedot}}

<h2>Maksuohjeet</h2>

<p>Tilisiirtolomake uhkasakon maksamiseksi on tämän päätöksen liitteenä.</p>

<h2>Sovelletut oikeusohjeet</h2>

<ul>
    <li class="no-indicator">Laki rakennuksen energiatodistuksesta (50/2013) 2 §, 6 § ja 24 §</li>
    <li class="no-indicator">Uhkasakkolaki (1113/1990) 7 § ja 10 §</li>
</ul>

<h2>Muutoksenhaku</h2>

<p>Tähän päätökseen tyytymätön saa hakea siihen muutosta {{#tyyppikohtaiset-tiedot}}{{oikeus-fi}}{{/tyyppikohtaiset-tiedot}}. Valitus on tehtävä ja
    toimitettava edellä mainittuun hallinto-oikeuteen 30 päivän kuluessa tämän päätöksen tiedoksisaannista.</p>

<p>Tarkemmat ohjeet valituksen tekemisestä ovat tämän päätöksen liitteenä.</p>

{{#valvoja}}
<p>Lisätietoja päätöksestä antaa energia-asiantuntija {{etunimi}} {{sukunimi}}, <br/>puh. {{puhelin}} / s-posti
    {{email}}.</p>
{{/valvoja}}

<table class="max-width">
    <tr>
        <td>
            {{#tyyppikohtaiset-tiedot}}
            <div>{{department-head-name}}</div>
            <div>{{department-head-title-fi}}</div>
            {{/tyyppikohtaiset-tiedot}}
        </td>

        <td>
            {{#valvoja}}
            <div>{{etunimi}} {{sukunimi}}</div>
            {{/valvoja}}
            <div>energia-asiantuntija</div>
        </td>
    </tr>
</table>
$$)
on conflict (id) do update set label_fi          = excluded.label_fi,
                               label_sv          = excluded.label_sv,
                               ordinal           = excluded.ordinal,
                               toimenpidetype_id = excluded.toimenpidetype_id,
                               language          = excluded.language,
                               valid             = excluded.valid,
                               tiedoksi          = excluded.tiedoksi,
                               content           = excluded.content;
