insert into vk_template (id,
                         label_fi,
                         label_sv,
                         ordinal,
                         toimenpidetype_id,
                         language,
                         valid,
                         tiedoksi,
                         content)
values (5, 'Käskypäätös / kuulemiskirje', 'Käskypäätös / kuulemiskirje (sv)', 1, 7, 'fi', true, true,
        $$
<div class="otsikko">
    <b>Kuulemiskirje</b>
    <b>{{päivä}}</b> <br/>
    Dnro: {{diaarinumero}}
</div>

<!-- TODO: Tähän osoite oikeaan kohtaan kirjekuoren ikkunasta näkyväksi -->
<div class="vastaanottaja">
    {{#omistaja-henkilo}} {{sukunimi}} {{etunimi}} <br/>
    {{jakeluosoite}} <br/>
    {{postinumero}} {{postitoimipaikka}}
    {{/omistaja-henkilo}}
</div>

<h1>Kuuleminen uhkasakon asettamisesta</h1>


<div>
    <!--    TODO: Onko tässä tarpeen olla myös yritysomistaja? -->
    Rakennuksen omistaja: {{#omistaja-henkilo}} {{sukunimi}} {{etunimi}} {{/omistaja-henkilo}}<br/>
    Rakennus: {{#kohde}}{{katuosoite}} {{postinumero}} {{postitoimipaikka}} {{/kohde}}
</div>

<h2>Asian tausta</h2>

<p>Rakennuksen energiatodistuksesta annetun lain (50/2013, energiatodistuslaki) 6 §:n mukaan rakennusta, huoneistoa tai
    niiden hallintaoikeutta myytäessä tai vuokrattaessa täytyy olla energiatodistus. Energiatodistus on annettava joko
    alkuperäisenä tai jäljennöksenä ostajalle tai vuokralaiselle. Kohteen julkisessa myynti- tai vuokrausilmoituksessa
    on mainittava myytävän tai vuokrattavan kohteen energiatehokkuusluokka. Rakennuksen omistaja tai haltija vastaa
    siitä, että rakennuksen energiatodistus hankitaan ja sitä käytetään laissa säädetyissä tilanteissa
    (energiatodistuslain 2 §).</p>

<p>Seuraamuksista säädetään energiatodistuslain 24 §:ssä. Jos rakennuksen omistaja ei täytä energiatodistuslaissa
    säädettyjä velvollisuuksiaan tai toimii muutoin energiatodistuslain tai sen nojalla annettujen säännösten
    vastaisesti, ARAn on kehotettava korjaamaan asiantila ja asetettava määräaika asiantilan korjaamiselle. Jos asiaa ei
    korjata määräajassa, ARAn on annettava asianomaiselle taholle varoitus ja uusi määräaika. Jos asiantilaa ei
    määräajassa korjata, ARAn tulee antaa tilanteen mukainen käsky- tai kieltopäätös. ARA voi tehostaa käskyä tai
    kieltoa vakavissa tai olennaisissa rikkomuksissa uhkasakolla tai teettämis- tai keskeyttämisuhalla, joista
    säädetään uhkasakkolaissa (1113/1990).</p>

{{#kohde}}
<p>Omistamaanne omakotitaloa osoitteessa {{katuosoite}} {{postinumero}} {{postitoimipaikka}} on markkinoitu julkisesti
    {{ilmoituspaikka}} -sivustolla ilman energiatodistusta.
    {{/kohde}}
    {{#aiemmat-toimenpiteet}}Tästä syystä ARA ryhtyi energiatodistuslain 24 §:n
    mukaisiin toimenpiteisiin ja on lähettänyt Teille {{kehotus-pvm}} kehotuksen.</p>

<p>Kehotuksessa Teitä on pyydetty {{kehotus-maarapaiva}} mennessä antamaan ARAlle tietoa mahdollisesta
    energiatodistuksesta tai
    jos todistusta ei ole, sen laadinta-aikataulusta. Kun kehotusta ei noudatettu, ARA antoi Teille varoituksen, jonka
    määräaika oli {{varoitus-maarapaiva}}. Asiantilaa ei kehotuksesta ja varoituksesta huolimatta korjattu, ja ARAn
    energiatodistusrekisteristä ilmenee, ettei kohteelle ole edelleenkään laadittu energiatodistusta<sup>1</sup>.</p>
{{/aiemmat-toimenpiteet}}


<p>ARA voi energiatodistuslain 24 §:n mukaisena viimesijaisena keinona antaa käskypäätöksen, jolla Teidät velvoitetaan
    hankkimaan energiatodistus. Käskyä voidaan tehostaa uhkasakolla, jonka määrä on arviolta {{sakko}} euroa.</p>

<h2>Asianosaisen kuuleminen</h2>

<p>Ennen asian ratkaisemista ARA varaa Teille hallintolain (434/2003) 34 §:n mukaisen tilaisuuden lausua mielipiteenne
    asiasta sekä antaa selitys sellaisista vaatimuksista ja selvityksistä, jotka voivat vaikuttaa asian ratkaisuun.
    Vastaus on annettava viimeistään {{määräpäivä}}. Vastauksen antamatta jättäminen ei estä
    asian ratkaisemista.</p>

<p>Vastaus pyydetään toimittamaan ARAn kirjaamoon joko sähköpostitse kirjaamo.ara@ara.fi tai postitse Asumisen rahoitus-
    ja kehittämiskeskus, PL 30, 15141 Lahti.</p>

{{#valvoja}}
{{etunimi}} {{sukunimi}}
{{/valvoja}}<br/>
energia-asiantuntija

<p><sup>1</sup> ARAn energiatodistusrekisterissä ovat 1.5.2015 jälkeen laaditut energiatodistukset.</p>
$$)
on conflict (id) do update set label_fi          = excluded.label_fi,
                               label_sv          = excluded.label_sv,
                               ordinal           = excluded.ordinal,
                               toimenpidetype_id = excluded.toimenpidetype_id,
                               language          = excluded.language,
                               valid             = excluded.valid,
                               tiedoksi          = excluded.tiedoksi,
                               content           = excluded.content;
