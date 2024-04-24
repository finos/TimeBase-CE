FROM amazoncorretto:17-alpine3.18

RUN addgroup -S deltix --gid 1666 && adduser -S deltix -G deltix -u 1666 && \
    mkdir -p /timebase-home && \
    chown deltix:deltix -R /timebase-home

ENV DELTIX_HOME /timebase-server
ENV DEFAULT_JVM_OPTS --add-opens=java.base/sun.nio.ch=ALL-UNNAMED --add-opens=java.base/java.io=ALL-UNNAMED --add-opens=java.base/java.lang=ALL-UNNAMED --add-opens=java.rmi/sun.rmi.transport=ALL-UNNAMED

USER deltix
WORKDIR /timebase-home

COPY timebase /timebase-server
EXPOSE 8011
ENTRYPOINT ["/bin/sh", "-c", "/timebase-server/bin/tdbserver.sh -home /timebase-home"]