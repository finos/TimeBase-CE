FROM adoptopenjdk/openjdk11:jdk-11.0.11_9-slim
RUN mkdir timebase-home
ENV DELTIX_HOME /timebase-server
COPY timebase /timebase-server
EXPOSE 8011
ENTRYPOINT ["/bin/sh", "-c", "/timebase-server/bin/tdbserver.sh -tb -home /timebase-home"]
