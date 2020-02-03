FROM openjdk:11
COPY target/etp-backend.jar /target/
ENTRYPOINT ["java", "-cp", "target/etp-backend.jar", "clojure.main", "-m", "solita.etp.core"]
