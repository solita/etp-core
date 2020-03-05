#!/usr/bin/env bash
set -e

containername=${1:-'etp-db'}

./build.sh
docker build . --tag $containername
