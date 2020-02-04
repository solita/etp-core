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

Remember to follow the
[Docker postinstall](https://docs.docker.com/install/linux/linux-postinstall/)
-guide. Then logout and login.

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
    clojure -m solita.etp.db.flywaydb migrate

Start [the backend](/etp-backend). Backend developers should start the REPL from
their IDE and start the services from there by calling the ´´´reset´´´ function.
The application can be also started from the command line with the following
command:

    cd etp-backend
    clojure -m solita.etp.core

About database usage
--------------------

The default ```postgres``` database is used. There are two users for the
database:

 * ```etp```: has all privileges to ```postgres``` database.
 * ```etp_app```: can read and write to tables in ```postgres``` database.

In production and test environments the database is used normally. It's created
during instance setup and migrations are ran as ```etp```. ```etp_app```
writes and reads data from tables.

In development environment the ```postgres``` database is used as a template
that can be used for setting up new databases. The dockerized Postgres sets up
a function that ```etp_app``` can call to copy the database under a new name.

Tests will use this extensively as each test will create their own database
using this function. During development the ```reset``` function will also
call that function to create a ```etp_dev``` database.

Other environments
---

### Uberjars

Both projects contain script ```build-docker-container.sh``` which can be
used to build the uberjars and related docker containers. The build containers
can be executed by running ```docker run [etp-db or etp-backend]```.

### TODO

 * TODO How to run unit- and integration tests?
 * TODO How to run e2e-tests?
