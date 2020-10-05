#!/usr/bin/env bash
set -e

rm -rf target
clojure -Spom
clojure -M:jar
