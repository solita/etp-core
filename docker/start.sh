#!/usr/bin/env bash
set -e
cd $(dirname $0)

find docker/sftp/ssh -iname "*_key" -exec chmod 600 {} \;

docker-compose up -d

# Wait naively for PostgreSQL to start
sleep 2

./flyway.sh migrate
