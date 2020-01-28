ARA - Energiatodistuspalvelu
===

TODO

Järjestelmä muodostuu:
- Käyttöliittymistä eri sidosryhmille
- Sovelluskerroksesta
- Tietokannasta

Sovelluskerros
--------------
Sovelluskerros sisältää komponentit:
- [etp-backend](/etp-backend) - julkiset ja viranomaisille suunnatut taustapalvelut
- [etp-db](/etp-db) - tietokannan migraatiotyökalu

Käyttöliittymät
---------------

Käyttöliittymä: https://github.com/solita/etp-front

Kehitysympäristön asentaminen
-----------------------------

### Java 11

Ubuntulla asennetaan seuraavasti:

    sudo apt install openjdk-11-jdk

### Docker

Ubuntulla seuraavia ohjeita noudattaen:

https://docs.docker.com/install/linux/docker-ce/ubuntu/#install-using-the-repository

Muista suorittaa ohjeet myös [Dockerin post-install] -kohdasta.
Tämän jälkeen kirjaudu ulos ja takaisin sisään.

### Clojure + CLI Tools

Ubuntulla seuraavia ohjeita noudattaen:

https://clojure.org/guides/getting_started#_installation_on_linux

Kehitysympäristön käynnistäminen
--------------------------------

Käynnistä [oheispalvelut](/docker) esim. tietokanta:

    cd docker
    docker-compose up

Luo [skeema](/etp-db) tietokantaan.

    cd etp-db
    TODO komennot

Käynnistä [backend-palvelu](/etp-backend). Backend-kehittäjien kannattaa
käynnistää oman IDEnsa kautta REPL ja käynnistää palvelin sieltä kutsumalla
´´´reset´´´-funktiota. Komentoriviltä sovelluksen saa käyntiin nopeasti näin:

    cd etp-backend
    TODO komennot

Muut ympäristöt
---

TODO miten tehdään uberjar?
TODO halutaanko ajaa kontissa lokaalisti?
TODO miten ajetaan yksikkö- ja integraatiotestit?
TODO miten ajetaan e2e-testit?

[Dockerin post-install](https://docs.docker.com/install/linux/linux-postinstall/)
