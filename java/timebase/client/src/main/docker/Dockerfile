FROM artifactory.epam.com:6190/thirdparty.docker/alpine-c-jdk:11.0.6_p10-r1-alpine20200319
ENV DELTIX_HOME /timebase-client
COPY timebase /timebase-client
ENTRYPOINT ["/bin/sh", "-c", "/timebase-client/bin/tickdb.sh"]
