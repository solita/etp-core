#!/usr/bin/env bash
set -e

package='target/etp-db';

if [[ ! -d $package ]]; then
  echo "Database migration tool directory $package does not exists.";
  exit 1;
fi

java -cp $package clojure.main -m solita.etp.db.flywaydb $1
