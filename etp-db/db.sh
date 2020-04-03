#!/usr/bin/env bash
set -e

command=$1
alias=$2
package='target/etp-db';

if [[ ! -d $package ]]; then
  echo "Database migration tool directory $package does not exists.";
  exit 1;
fi

if [ -z "$alias" ]
then
    java -cp $package clojure.main -m solita.etp.db.flywaydb $command
else
    java -cp $package clojure.main -A:"$alias" -m solita.etp.db.flywaydb $command
fi
