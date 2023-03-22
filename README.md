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

Separate repositories at https://github.com/solita/etp-front and https://github.com/solita/etp-public

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
    ./start.sh

Start script starts docker-compose, creates template and dev databases
and runs migrations for both of them.

Start [the backend](/etp-backend). Backend developers should start the REPL from
their IDE and start the services from there by calling the `reset` function.
The application can be also started from the command line with the following
command:

    cd etp-backend
    clojure -m solita.etp.core

Tests can be run (parallel) with

    cd etp-backend
    clojure -M:dev:test

Test coverage report (without API layer) can be generated with

    cd etp-backend
    clojure -M:dev:coverage

Check dependencies for vulnerabilities

    cd etp-backend
    ./nvd.sh

Check outdated dependencies

    cd etp-backend
    clojure -M:outdated

About database usage
--------------------

There are two users for the database:

 * ```etp```: has all privileges to ```postgres``` database.
 * ```etp_app```: can read and write to tables in ```postgres``` database.

In production and test environments the ```postgres``` database is used
normally. It needs to be created during instance setup and migrations are ran as
```etp``` user. ```etp_app``` user writes and reads data from the tables.

In development environment the ```postgres``` database is used as a template
that can be used for setting up new databases. The dockerized Postgres sets up
a second database ```etp_dev``` which should be used locally during
development. For convenience, [docker](/docker) directory contains script
[flyway.sh](/docker/flyway.sh) that can be used for migrating and cleaning
both databases with a single call.

Tests will utilize ```postgres``` database as template extensively as each test
will create their own database from it.

Third-party licenses
--------------------

To generate a site of used licenses:

    cd etp-backend
    clojure -Spom
    # Now add Clojars to repositories in generated pom.xml
    mvn project-info-reports:dependencies

Other environments
---

### Uberjars

Both projects contain script ```build-docker-image.sh``` which can be
used to build the uberjars and related docker containers. The build containers
can be executed by running ```docker run [etp-db or etp-backend]```.
