ARA - Energiatodistuspalvelu
===

The components of the system are:
- User interface for different stakeholders
- Backend / application layer
- Database (PostgreSQL)

Backend / Application layer
--------------
The backend contains the following components:
- [etp-backend](/etp-backend) - backend services for both the public and
  authorities -sections
- [etp-db](/etp-db) - database migration tool

User interface
---------------

Separate repository at https://github.com/solita/etp-front

Installing the development environment
-----------------------------

### Java 11

For Ubuntu:

    sudo apt install openjdk-11-jdk

### Docker

Installation for Linux by following the instructions at the following URL:

https://docs.docker.com/install/linux/docker-ce/ubuntu/#install-using-the-repository

Remember to follow the [Docker postinstall] -guide. Then logout and login.

Docker-compose is also required:

https://docs.docker.com/compose/install/

### Clojure + CLI Tools

For Ubuntu:

https://clojure.org/guides/getting_started#_installation_on_linux

Starting the development environment
--------------------------------

Start [the required services](/docker) (database etc):

    cd docker
    docker-compose up

Run [migrations](/etp-db) to database.

    cd etp-db
    TODO commands 

Start [the backend](/etp-backend). Backend developers should start the REPL from
their IDE and start the services from there by calling the ´´´reset´´´ function.
The application can be also started from the command line with the following
command:

    cd etp-backend
    clojure -m solita.etp.core

Other environments
---

TODO How to uberjar?
TODO How to run unit- and integration tests?
TODO How to run e2e-tests?

[Docker postinstall](https://docs.docker.com/install/linux/linux-postinstall/)
