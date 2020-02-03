#!/usr/bin/env bash
set -e

rm -rf target
clojure -A:uberjar
docker build . --tag etp-db
