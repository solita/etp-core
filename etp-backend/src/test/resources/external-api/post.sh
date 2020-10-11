#!/usr/bin/env bash
set -e

file=${1-'complete-2018.json'}
server=${2-'localhost:8080'}
user=${3-'laatija1@example.com:asdfasdf'}

curl -v -d "@$file" \
  -H "Content-Type: application/json" \
  -u $user \
  "http://$server/api/external/energiatodistukset/2018"
