FROM alpine:latest

RUN apk add --upgrade openjdk11 curl \
    && curl -L -O http://nilhcem.github.com/FakeSMTP/downloads/fakeSMTP-latest.zip \
    && unzip fakeSMTP-latest.zip

EXPOSE 25
CMD ["java", "-jar", "fakeSMTP-2.0.jar", "-s", "-b"]
