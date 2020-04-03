#!/usr/bin/env bash
set -e

aliases=$1

rm -rf target

if [ -z "$aliases" ]
then
    clojure -A:uberjar
else
    clojure -A:uberjar --aliases "$aliases"
fi

unzip target/etp-db.jar -d target/etp-db
rm -f target/etp-db.jar
