package com.epam.deltix.qsrv.jetty;

import io.jooby.Jooby;
import io.jooby.ServerOptions;
import io.jooby.SneakyThrows;
import org.eclipse.jetty.server.*;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.util.thread.QueuedThreadPool;

import javax.annotation.Nonnull;
import java.net.BindException;
import java.util.ArrayList;
import java.util.List;

public class Jetty extends io.jooby.Server.Base {

    private static final int THREADS = 200;

    private Server server;

    private List<Jooby> applications = new ArrayList<>();

    private ServerOptions options = new ServerOptions()
            .setServer("jetty")
            .setWorkerThreads(THREADS);

    @Nonnull
    @Override public Jetty setOptions(@Nonnull ServerOptions options) {
        this.options = options
                .setWorkerThreads(options.getWorkerThreads(THREADS));
        return this;
    }

    @Nonnull @Override public ServerOptions getOptions() {
        return options;
    }

    @Nonnull @Override public io.jooby.Server start(Jooby application) {
        try {
            System.setProperty("org.eclipse.jetty.util.UrlEncoded.charset", "utf-8");
            /** Set max request size attribute: */
            System.setProperty("org.eclipse.jetty.server.Request.maxFormContentSize",
                    Long.toString(options.getMaxRequestSize()));

            applications.add(application);

            addShutdownHook();

            QueuedThreadPool executor = new QueuedThreadPool(options.getWorkerThreads());
            executor.setName("worker");

            fireStart(applications, executor);

            this.server = new Server(executor);
            server.setStopAtShutdown(false);
            HttpConfiguration httpConf = new HttpConfiguration();
            httpConf.setOutputBufferSize(options.getBufferSize());
            httpConf.setOutputAggregationSize(options.getBufferSize());
            httpConf.setSendXPoweredBy(false);
            httpConf.setSendDateHeader(options.getDefaultHeaders());
            httpConf.setSendServerVersion(false);

            List<ConnectionFactory> connectionFactories = new ArrayList<>();
            connectionFactories.add(new HttpConnectionFactory(httpConf));
            ServerConnector http = new ServerConnector(server,
                    connectionFactories.toArray(new ConnectionFactory[0]));
            http.setPort(options.getPort());
            http.setHost(options.getHost());

            server.addConnector(http);

            server.addConnector(http);

            if (options.isSSLEnabled()) {
                // do nothing yet
            }

            ContextHandler context = new ContextHandler();
            AbstractHandler handler = new JettyHandler(applications.get(0), options.getBufferSize(),
                    options.getMaxRequestSize(), options.getDefaultHeaders());
            context.setHandler(handler);
            server.setHandler(context);
            server.start();

            fireReady(applications);
        } catch (Exception x) {
            if (io.jooby.Server.isAddressInUse(x.getCause())) {
                x = new BindException("Address already in use: " + options.getPort());
            }
            throw SneakyThrows.propagate(x);
        }

        return this;
    }

    @Nonnull @Override public synchronized io.jooby.Server stop() {
        fireStop(applications);
        if (server != null) {
            try {
                server.stop();
            } catch (Exception x) {
                throw SneakyThrows.propagate(x);
            } finally {
                server = null;
            }
        }
        return this;
    }
}
