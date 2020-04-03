#!/usr/bin/env bash
set -e

aliases=$1

containername=${1:-'etp-db'}

./build.sh "$aliases"

docker build . --tag $containername
