FROM openjdk:11

COPY target /target
COPY db.sh /

CMD ["./db.sh", "migrate"]
