FROM artifactory.epam.com:6190/thirdparty.docker/open-jdk-11:latest
RUN mkdir timebase-home
ENV DELTIX_HOME /timebase-server
COPY timebase /timebase-server
EXPOSE 8011
ENTRYPOINT ["/bin/sh", "-c", "/timebase-server/bin/tdbserver.sh -tb -home /timebase-home"]
