package com.epam.deltix.qsrv.comm.cat;

import java.io.IOException;

import com.epam.deltix.qsrv.config.QuantServerExecutor;
import com.epam.deltix.qsrv.config.QuantServiceConfig;
import com.epam.deltix.qsrv.config.ServiceExecutor;
import com.epam.deltix.util.collections.generated.ObjectToObjectHashMap;
import static com.epam.deltix.qsrv.config.QuantServiceConfig.Type;

public class StartConfiguration {

    private static ObjectToObjectHashMap<Type, String> DEFAULTS = new ObjectToObjectHashMap<>();
    static {
        DEFAULTS.put(Type.TimeBase, "deltix.qsrv.config.TimebaseServiceExecutor");
        DEFAULTS.put(Type.Aggregator, "deltix.qsrv.hf.aggregator.AggregatorExecutor");
        DEFAULTS.put(Type.ExecutionServer, "deltix.qsrv.hf.server.es.server.ExecutionServerStarter");
        DEFAULTS.put(Type.StrategyServer, "");
        DEFAULTS.put(Type.UHF, "");
        DEFAULTS.put(Type.QuantServer, QuantServerExecutor.class.getName());
    }

    public QuantServiceConfig   tb;
    public QuantServiceConfig   agg;
    public QuantServiceConfig   uhf;
    public QuantServiceConfig   sts;
    public QuantServiceConfig   es;
    public QuantServiceConfig   quantServer;

    public int                  port;

    private ObjectToObjectHashMap<Type, ServiceExecutor> executors = new ObjectToObjectHashMap<>();


    public static StartConfiguration create(boolean timebase, boolean aggregator, boolean uhf) throws IOException {
        return create(timebase, aggregator, uhf, false, false, -1);
    }

    public static StartConfiguration create(boolean timebase, boolean aggregator, boolean es, boolean sts) throws IOException {
        return create(timebase, aggregator, false, es, sts, -1);
    }

    private static StartConfiguration create(boolean timebase, boolean aggregator, boolean uhf, boolean es, boolean sts, int port) throws IOException {
        StartConfiguration config = new StartConfiguration();
        config.port = port;

        config.tb = (timebase) ? QuantServiceConfig.forService(Type.TimeBase) : null;
        config.agg = (aggregator) ? QuantServiceConfig.forService(Type.Aggregator) : null;
        config.uhf = (uhf) ? QuantServiceConfig.forService(Type.UHF) : null;

        config.es = (es) ? QuantServiceConfig.forService(Type.ExecutionServer) : null;

        config.sts = (sts) ? QuantServiceConfig.forService(Type.StrategyServer) : null;

        config.quantServer = QuantServiceConfig.forService(Type.QuantServer);

        return config;
    }

    public ServiceExecutor          getExecutor(Type type) {
        ServiceExecutor executor = executors.get(type, null);

        if (executor == null) {
            String clazz = DEFAULTS.get(type, null);

            if (clazz == null)
                throw new IllegalArgumentException("Service type: " + type + " is unknown");

            try {
                executors.put(type, executor = (ServiceExecutor) Class.forName(clazz).newInstance());
            } catch (InstantiationException | ClassNotFoundException | IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }

        return executor;
    }

    public void                     setExecutor(Type type, ServiceExecutor exe) {
        executors.put(type, exe);
    }

}
