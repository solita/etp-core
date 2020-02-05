#!/usr/bin/env bash
set -e

if [ -z "$1" ]
then
  echo "Command is missing!"
  echo "Usage: $0 [migrate or clean]"
  exit 1
fi

cd ../etp-db
clojure -m solita.etp.db.flywaydb $1
DB_URL="jdbc:postgresql://localhost:5432/etp_dev" clojure -m solita.etp.db.flywaydb $1
