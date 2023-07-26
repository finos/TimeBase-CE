FROM amazoncorretto:11-alpine3.17

RUN addgroup -S deltix --gid 1666 && adduser -S deltix -G deltix -u 1666 && \
    mkdir -p /timebase-home && \
    chown deltix:deltix -R /timebase-home

USER deltix
WORKDIR /timebase-home
ENV DELTIX_HOME /timebase-server
COPY timebase /timebase-server
EXPOSE 8011
ENTRYPOINT ["/bin/sh", "-c", "/timebase-server/bin/tdbserver.sh -home /timebase-home"]