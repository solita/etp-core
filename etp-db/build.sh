#!/usr/bin/env bash
set -e

rm -rf target
clojure -A:uberjar
unzip target/etp-db.jar -d target/etp-db
rm -f target/etp-db.jar