insert into vk_template (id,
                         label_fi,
                         label_sv,
                         ordinal,
                         toimenpidetype_id,
                         language,
                         valid,
                         tiedoksi,
                         content)
values (6, 'Käskypäätös / varsinainen päätös', 'Käskypäätös / varsinainen päätös (sv)', 1, 8, 'fi', true, true,
        $$
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

<h1>Käsky hankkia energiatodistus ja uhkasakon asettaminen</h1>

<div>
    Vastaanottaja/asianosainen: {{#omistaja-henkilo}}{{sukunimi}} {{etunimi}}{{/omistaja-henkilo}}{{#omistaja-yritys}}{{nimi}}{{/omistaja-yritys}}<br/>
</div>

<h2>Päätös</h2>

<p>Asumisen rahoitus- ja kehittämiskeskus (ARA) katsoo, että
    {{#omistaja-henkilo}}
    {{etunimi}} {{sukunimi}}
    {{/omistaja-henkilo}}
    {{#omistaja-yritys}}
    {{nimi}}
    {{/omistaja-yritys}}
    (jäljempänä Asianosainen) on
    laiminlyönyt noudattaa rakennuksen energiatodistuksesta annetun lain (50/2013, energiatodistuslaki) 6 §:ssä
    säädettyä velvollisuutta käyttää energiatodistusta rakennuksen myyntitilanteessa. Energiatodistuslain 2 §:n mukaan
    rakennuksen omistaja vastaa siitä, että rakennuksen energiatodistus hankitaan ja että sitä tai siinä olevia tietoja
    käytetään laissa säädetyissä tilanteissa.</p>

<p>ARA käskee energiatodistuslain 24 §:n nojalla Asianosaisen hankkimaan osoitteessa {{#kohde}}{{katuosoite}}
    {{postinumero}} {{postitoimipaikka}}{{/kohde}} sijaitsevalle rakennukselle energiatodistuksen viimeistään
    {{määräpäivä}} ja
    esittämään energiatodistuksen ARAlle. ARA asettaa käskyn tehosteeksi uhkasakkolain (1113/1990) 6 §:ssä tarkoitetun
    kiinteän uhkasakon. Uhkasakon suuruus on {{#tyyppikohtaiset-tiedot}}{{fine}}{{/tyyppikohtaiset-tiedot}} euroa. Uhkasakko tuomitaan maksettavaksi, mikäli ARAn käskyä ei
    ole annetussa määräajassa noudatettu.</p>

<h2>Kuuleminen</h2>

<!--Kuulemiskirjeen päivämäärä-->
<p>ARA varasi {{#aiemmat-toimenpiteet}}{{kuulemiskirje-pvm}}{{/aiemmat-toimenpiteet}} päivätyllä kirjeellään (dnro
    [ARA-05.03.01-202x-xxx]) Asianosaiselle uhkasakkolain 22 §:n ja
    siinä viitatun hallintolain (434/2003) 34 §:n mukaisesti ennen asian ratkaisemista tilaisuuden lausua
    mielipiteensä asiasta sekä antaa selityksensä sellaisista vaatimuksista ja selvityksistä, jotka saattavat
    vaikuttaa asian ratkaisuun. Kuulemiskirjeessä ARA kertoi, että uhkasakon määrä on arviolta {{#tyyppikohtaiset-tiedot}}{{kuulemiskirje-sakko}}{{/tyyppikohtaiset-tiedot}} euroa.</p>

<!--TODO: Halutaanko tähän oikeasti se laatikko-->
{{vastaus}}

<h2>Päätöksen perustelut</h2>

<p>Energiatodistuslain 6 §:n mukaan rakennusta, huoneistoa tai niiden hallintaoikeutta myytäessä tai vuokrattaessa
    täytyy olla energiatodistus. Energiatodistus on annettava joko alkuperäisenä tai jäljennöksenä ostajalle tai
    vuokralaiselle. Kohteen julkisessa myynti- tai vuokrausilmoituksessa on mainittava myytävän tai vuokrattavan kohteen
    energiatehokkuusluokka. Rakennuksen omistaja tai haltija vastaa siitä, että rakennuksen energiatodistus hankitaan ja
    sitä käytetään laissa säädetyissä tilanteissa (energiatodistuslain 2 §).</p>

<p>Seuraamuksista säädetään energiatodistuslain 24 §:ssä. Jos rakennuksen omistaja ei täytä energiatodistuslaissa
    säädettyjä velvollisuuksia tai toimii muutoin energiatodistuslain tai sen nojalla annettujen säännösten
    vastaisesti, ARAn on kehotettava korjaamaan asiantila ja asetettava määräaika asiantilan korjaamiselle. Jos asiaa
    ei korjata määräajassa, ARAn on annettava asianomaiselle taholle varoitus ja uusi määräaika. Jos asiantilaa ei
    määräajassa korjata, ARAn tulee antaa tilanteen mukainen käsky- tai kieltopäätös. ARA voi tehostaa käskyä tai
    kieltoa vakavissa tai olennaisissa rikkomuksissa uhkasakolla tai teettämis- tai keskeyttämisuhalla, joista
    säädetään uhkasakkolaissa (1113/1990).</p>

<p>ARA on lähettänyt Asianosaiselle kehotuksen, sillä Asianosaisen omistamaa rakennusta osoitteessa
    {{#kohde}}{{katuosoite}} {{postinumero}} {{postitoimipaikka}} on
    markkinoitu {{ilmoituspaikka}} -sivustolla ilman energiatodistusta.{{/kohde}}
    Kehotuksessa Asianosaista on pyydetty {{#aiemmat-toimenpiteet}}{{kehotus-maarapaiva}}{{/aiemmat-toimenpiteet}}
    mennessä antamaan ARAlle tietoa mahdollisesta energiatodistuksesta tai jos todistusta ei ole, sen
    laadinta-aikataulusta.</p>

{{#aiemmat-toimenpiteet}}
<p>Kohteen markkinointia kuitenkin jatkettiin kehotuksesta huolimatta, eikä Asianosainen esittänyt energiatodistusta
    ARAlle. Tästä syystä ARA ryhtyi energiatodistuslain 24 §:n mukaisiin toimenpiteisiin ja antoi Asianosaiselle
    varoituksen esittää energiatodistus {{varoitus-maarapaiva}} mennessä. Asiantilaa ei kehotuksesta ja varoituksesta
    huolimatta
    korjattu, ja ARAn energiatodistusrekisteristä ilmenee, ettei kohteelle ole edelleenkään laadittu energiatodistusta
    <sup>1</sup>.</p>
{{/aiemmat-toimenpiteet}}

<p>ARA on siis antanut Asianosaiselle monta mahdollisuutta korjata toimintansa. ARA voi energiatodistuslain 24 §:n
    mukaisena viimesijaisena keinona antaa käskypäätöksen, jolla rakennuksen omistaja velvoitetaan hankkimaan
    energiatodistus. Käskyä voidaan tehostaa uhkasakolla, josta säädetään uhkasakkolaissa.</p>

<!--TODO: Halutaanko tähän oikeasti se laatikko-->
{{kannanotto}}

<h2>Sovelletut oikeusohjeet</h2>

<ul>
    <li class="no-indicator">Laki rakennuksen energiatodistuksesta (50/2013) 2 §, 6 § ja 24 §</li>
    <li class="no-indicator">Uhkasakkolaki (1113/1990) 4 §, 6 §, 8 § ja 22 §</li>
    <li class="no-indicator">Hallintolaki (434/2003) 34 §</li>
</ul>

<h2>Muutoksenhaku</h2>

<p>Tähän päätökseen tyytymätön saa hakea siihen muutosta {{hallinto-oikeus}} hallinto-oikeudelta. Valitus on tehtävä ja
    toimitettava edellä mainittuun hallinto-oikeuteen 30 päivän kuluessa tämän päätöksen tiedoksisaannista.</p>

<hr/>
<p class="viittaus mb-0"><sup>1</sup> ARAn energiatodistusrekisterissä ovat 1.5.2015 jälkeen laaditut
    energiatodistukset.</p>

<p>Tarkemmat ohjeet valituksen tekemisestä ovat tämän päätöksen liitteenä.</p>

{{#valvoja}}
<p>Lisätietoja päätöksestä antaa energia-asiantuntija {{etunimi}} {{sukunimi}}, puh. {{puhelin}} / s-posti
    {{email}}. </p>
{{/valvoja}}

<table class="max-width">
    <tr>
        <td>
            <div>[Kimmo Huovinen]*</div>
            <div>apulaisjohtaja</div>
        </td>

        <td>
            {{#valvoja}}
            <div>{{etunimi}} {{sukunimi}}</div>
            {{/valvoja}}
            <div>energia-asiantuntija</div>
        </td>
    </tr>
</table>

<div class="sivunvaihto"></div>
$$)
on conflict (id) do update set label_fi          = excluded.label_fi,
                               label_sv          = excluded.label_sv,
                               ordinal           = excluded.ordinal,
                               toimenpidetype_id = excluded.toimenpidetype_id,
                               language          = excluded.language,
                               valid             = excluded.valid,
                               tiedoksi          = excluded.tiedoksi,
                               content           = excluded.content;
