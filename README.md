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

Kehitysympäristön käynnistäminen
--------------------------------

Asenna [java][java], [docker][docker] ja [leiningen][leiningen].

Käynnistä [oheispalvelut](/docker) esim. tietokanta

    cd docker
    docker-compose up

Luo [skeema](/etp-db) tietokantaan

    cd etp-db
    lein with-profiles +test-data do run clear-db, run update-db

Käynnistä [backend-palvelu](/etp-backend)

    cd etp-backend
    lein ring server-headless

Muut ympäristöt
---

TODO


[java]: https://openjdk.java.net/
[docker]: https://www.docker.com/
[leiningen]: https://leiningen.org/
