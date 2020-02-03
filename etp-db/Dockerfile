FROM openjdk:11

COPY target/etp-db.jar /target/
COPY db.sh /

CMD ["./db.sh", "migrate"]
