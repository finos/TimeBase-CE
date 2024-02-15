FROM amazoncorretto:17-alpine3.18

ENV DELTIX_HOME /timebase-client
ENV DEFAULT_JVM_OPTS --add-opens=java.base/sun.nio.ch=ALL-UNNAMED --add-opens=java.base/java.io=ALL-UNNAMED --add-opens=java.base/java.lang=ALL-UNNAMED --add-opens=java.rmi/sun.rmi.transport=ALL-UNNAMED --add-opens=java.desktop/com.sun.java.swing.plaf.windows=ALL-UNNAMED

RUN addgroup -S deltix --gid 1666 && adduser -S deltix -G deltix -u 1666

USER deltix
COPY timebase /timebase-client
ENTRYPOINT ["/bin/sh", "-c", "/timebase-client/bin/tickdb.sh"]