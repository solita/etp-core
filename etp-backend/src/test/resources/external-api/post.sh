#!/usr/bin/env bash
set -e

local='http://localhost:8080'
dev='https://private.kehitys.energiatodistuspalvelu.com'
test='https://private.testi.energiatodistuspalvelu.com'

server=${!1}
file=${2-'complete-2018.json'}
user=${3-'laatija@solita.fi:asdfasdf'}

curl -v -d "@$file" \
  -H "Content-Type: application/json" \
  -u $user \
  "$server/api/external/energiatodistukset/2018"
