FROM artifactory.epam.com:6190/thirdparty.docker/open-jdk-11:latest
ENV DELTIX_HOME /timebase-client
COPY timebase /timebase-client
ENTRYPOINT ["/bin/sh", "-c", "/timebase-client/bin/tickdb.sh"]
