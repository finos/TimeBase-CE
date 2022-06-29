FROM artifactory.epam.com:6190/thirdparty.docker/open-jdk-11:latest

RUN addgroup -S deltix --gid 8001 && adduser -S deltix -G deltix -u 8001

USER deltix
ENV DELTIX_HOME /timebase-client
COPY --chown=deltix:deltix timebase /timebase-client
ENTRYPOINT ["/bin/sh", "-c", "/timebase-client/bin/tickdb.sh"]
