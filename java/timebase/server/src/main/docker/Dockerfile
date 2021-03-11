FROM artifactory.epam.com:6190/thirdparty.docker/alpine-c-jdk:11.0.6_p10-r1-alpine20200319
RUN mkdir timebase-home
ENV DELTIX_HOME /timebase-server
COPY timebase /timebase-server
EXPOSE 8011
ENTRYPOINT ["/bin/sh", "-c", "/timebase-server/bin/tdbserver.sh -tb -home /timebase-home"]
