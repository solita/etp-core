FROM alpine:3.18

RUN apk add --upgrade openjdk11 curl \
    && curl -L -O https://github.com/Nilhcem/FakeSMTP/raw/gh-pages/downloads/fakeSMTP-latest.zip \
    && unzip fakeSMTP-latest.zip

EXPOSE 25
CMD ["java", "-jar", "fakeSMTP-2.0.jar", "-s", "-b"]
