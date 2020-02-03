#!/usr/bin/env bash
set -e

package='target/etp-db.jar';

if [[ ! -f $package ]]; then
  echo "Database migration tool package $package does not exists.";
  echo "Build migration tool using: clj -A:uberjar.";
  exit 1;
fi

java -cp $package clojure.main -m solita.etp.db.flywaydb $1
