#!/usr/bin/env bash
set -e

docker-compose up -d

# Wait naively for PostgreSQL to start
sleep 2

./flyway.sh migrate
