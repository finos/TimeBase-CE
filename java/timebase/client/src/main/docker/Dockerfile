FROM amazoncorretto:11-alpine3.18

ENV DELTIX_HOME /timebase-client

RUN addgroup -S deltix --gid 1666 && adduser -S deltix -G deltix -u 1666

USER deltix
COPY timebase /timebase-client
ENTRYPOINT ["/bin/sh", "-c", "/timebase-client/bin/tickdb.sh"]