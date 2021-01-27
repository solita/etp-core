FROM alpine:latest

RUN apk add --upgrade \
    bash \
    shadow \
    openssh \
    openssh-sftp-server \
    && addgroup -S etp && adduser -S etp -G etp --shell /bin/bash \
    && echo "etp:etp" | chpasswd 

EXPOSE 22
CMD ["/usr/sbin/sshd", "-D"]
