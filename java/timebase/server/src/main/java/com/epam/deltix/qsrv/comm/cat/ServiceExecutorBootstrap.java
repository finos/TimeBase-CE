package com.epam.deltix.qsrv.comm.cat;

import com.epam.deltix.qsrv.config.QuantServerExecutor;
import com.epam.deltix.qsrv.config.QuantServiceConfig;
import com.epam.deltix.qsrv.config.ServiceExecutor;
import com.epam.deltix.snmp.QuantServerSnmpObjectContainer;
import com.epam.deltix.snmp.SNMPTransportFactory;
import com.epam.deltix.util.collections.generated.ObjectArrayList;
import com.epam.deltix.util.lang.Util;
import com.epam.deltix.util.tomcat.ConnectionHandshakeHandler;

public class ServiceExecutorBootstrap {

    private final StartConfiguration config;
    private final ObjectArrayList<ServiceExecutor> executors = new ObjectArrayList<>();

    public ServiceExecutorBootstrap(StartConfiguration config) {
        this.config = config;
    }

    public void start() {
        QuantServerExecutor executor = (QuantServerExecutor) config.getExecutor(QuantServiceConfig.Type.QuantServer);
        executor.run(config.quantServer);
        executors.add(0, executor);

        if (config.tb != null) {
            ServiceExecutor tb = config.getExecutor(QuantServiceConfig.Type.TimeBase);
            tb.run(config.tb);
            executors.add(0, tb);
        }

        if (config.agg != null) {
            ServiceExecutor agg = config.getExecutor(QuantServiceConfig.Type.Aggregator);
            agg.run(config.agg);
            executors.add(0, agg);

//            Aggregator.doRun(getCommonContext(), config.agg);
        }

        if (config.es != null) {
            ServiceExecutor es = config.getExecutor(QuantServiceConfig.Type.ExecutionServer);
            es.run(config.es, config.agg);
            executors.add(0, es);
            //ExecutionServerStarter.doRun(getCommonContext(), config.es, config.agg);
        }

        if (config.quantServer.getFlag("SNMP")) {
            QuantServerSnmpObjectContainer snmpObjectContainer = new QuantServerSnmpObjectContainer();
            for (ServiceExecutor serviceExecutor : executors) {
                serviceExecutor.registerSnmpObjects(snmpObjectContainer);
            }

            ConnectionHandshakeHandler connectionHandshakeHandler = SNMPTransportFactory.initializeSNMP(config.port, snmpObjectContainer);
            QuantServerExecutor.HANDLER.addHandler(
                    (byte)48, // BER.SEQUENCE
                    connectionHandshakeHandler);
        }
    }

    public void close() {
        // closing executors
        for (ServiceExecutor executor : executors)
            Util.close(executor);
    }
}
