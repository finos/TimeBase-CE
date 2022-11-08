FROM alpine:3.16.1

ENV DELTIX_HOME /timebase-client
ENV DEFAULT_JVM_OPTS --add-opens=java.base/sun.nio.ch=ALL-UNNAMED --add-opens=java.base/java.io=ALL-UNNAMED --add-opens=java.base/java.lang=ALL-UNNAMED --add-opens=java.rmi/sun.rmi.transport=ALL-UNNAMED

RUN apk add --no-cache openjdk17-jdk && \
    apk add --no-cache zlib=1.2.12-r3 && \
    addgroup -S deltix --gid 1666 && adduser -S deltix -G deltix -u 1666

USER deltix
COPY timebase /timebase-client
ENTRYPOINT ["/bin/sh", "-c", "/timebase-client/bin/tickdb.sh"]