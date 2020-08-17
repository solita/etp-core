#!/usr/bin/env bash
set -e
cd $(dirname $0)

docker run --name mpollux -p 127.0.0.1:53952:443 \
  -v `pwd`/mpollux/api:/usr/share/nginx/html:ro \
  -v `pwd`/mpollux/conf.d:/etc/nginx/conf.d:ro \
  -v `pwd`/../../etp-front/keys:/keys:ro \
  -d nginx
