#!/bin/bash

exec java -Djava.awt.headless=true \
     ${JAVA_OPTS} \
     -cp target/etp-backend.jar \
     clojure.main -m solita.etp.core
