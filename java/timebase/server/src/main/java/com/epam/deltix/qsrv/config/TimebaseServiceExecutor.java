/*
 * Copyright 2021 EPAM Systems, Inc
 *
 * See the NOTICE file distributed with this work for additional information
 * regarding copyright ownership. Licensed under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.epam.deltix.qsrv.config;

import com.epam.deltix.qsrv.QSHome;
import com.epam.deltix.qsrv.SSLProperties;
import com.epam.deltix.qsrv.dtb.fs.pub.FSFactory;
import com.epam.deltix.qsrv.dtb.fs.pub.FSType;
import com.epam.deltix.qsrv.hf.tickdb.comm.server.ServerParameters;
import com.epam.deltix.qsrv.hf.tickdb.comm.server.VSConnectionHandler;
import com.epam.deltix.qsrv.hf.tickdb.comm.server.aeron.AeronThreadTracker;
import com.epam.deltix.qsrv.hf.tickdb.comm.server.aeron.DXServerAeronContext;
import com.epam.deltix.qsrv.hf.tickdb.http.AbstractHandler;
import com.epam.deltix.qsrv.hf.tickdb.http.rest.RESTHandshakeHandler;
import com.epam.deltix.qsrv.hf.tickdb.pub.*;
import com.epam.deltix.util.net.NetworkInterfaceUtil;
import com.epam.deltix.qsrv.hf.tickdb.impl.TickDBImpl;
import com.epam.deltix.qsrv.hf.tickdb.impl.topic.TopicRegistryFactory;
import com.epam.deltix.qsrv.hf.tickdb.impl.topic.TopicSupportWrapper;
import com.epam.deltix.qsrv.hf.tickdb.impl.topic.topicregistry.DirectTopicRegistry;
import com.epam.deltix.qsrv.hf.tickdb.pub.mon.PropertyMonitor;
import com.epam.deltix.qsrv.hf.tickdb.pub.mon.TBMonitor;
import com.epam.deltix.qsrv.hf.tickdb.pub.mon.TBObjectMonitor;
import com.epam.deltix.qsrv.snmp.model.timebase.TimeBase;
import com.epam.deltix.qsrv.snmp.modimpl.TimeBaseImpl;
import com.epam.deltix.qsrv.util.servlet.ShutdownServlet;
import com.epam.deltix.snmp.QuantServerSnmpObjectContainer;
import com.epam.deltix.util.ContextContainer;
import com.epam.deltix.util.lang.StringUtils;
import com.epam.deltix.util.lang.Util;
import com.epam.deltix.util.net.SSLContextProvider;
import com.epam.deltix.util.security.TimebaseAccessController;
import com.epam.deltix.util.time.Interval;
import com.epam.deltix.util.time.TimeKeeper;
import com.epam.deltix.util.vsocket.TLSContext;
import com.epam.deltix.util.vsocket.TransportProperties;
import com.epam.deltix.util.vsocket.TransportType;
import com.epam.deltix.util.vsocket.VSCompression;
import com.epam.deltix.util.vsocket.VSProtocol;
import com.epam.deltix.util.vsocket.VSServerFramework;
import org.apache.catalina.Context;

import javax.annotation.Nonnull;
import java.io.File;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class TimebaseServiceExecutor implements ServiceExecutor {

    protected static Logger LOGGER = Logger.getLogger ("deltix.config");

    private DXTickDB                        TDB;
    //private Driver                          amqpDriver;
    private TimebaseAccessController MAC;
    private DXServerAeronContext            aeronContext;
    @SuppressWarnings("FieldCanBeLocal")
    private DirectTopicRegistry             topicRegistry;
    private int                             port;

    @Override
    public void run (QuantServiceConfig ... apps)  {
        assert apps.length == 1 && apps[0] != null;

        QuantServiceConfig config = apps[0];

        ContextContainer contextContainer = new ContextContainer();
        contextContainer.setQuickExecutorName("TimebaseService Executor");

        port = config.getPort();
        AeronThreadTracker aeronThreadTracker = new AeronThreadTracker();

        String publicAddressForAeron = getPublicAddressForAeron(config);
        if (publicAddressForAeron != null)
            LOGGER.log(Level.INFO, "External Address: " + publicAddressForAeron);

        aeronContext = DXServerAeronContext.createDefault(port, contextContainer.getAffinityConfig(), publicAddressForAeron);
        aeronContext.start();

        long cacheSize = getCacheSize(config);
        int numFiles = config.getInt("maxNumOpenFiles", Integer.MAX_VALUE);
        double ratio = config.getInt("preallocateCacheRatio", 0) / 100.0;

        long shutdownTimeout = config.getLong("shutdownTimeout", Long.MAX_VALUE);

        System.setProperty(TickDBImpl.VERSION_PROPERTY, "5.0");

        File tbFolder = QSHome.getFile(TickDBImpl.getFolderName());
        if (!tbFolder.exists() && !tbFolder.mkdirs())
            throw new IllegalStateException("Unable to create " + tbFolder);

        DataCacheOptions cacheOptions = new DataCacheOptions(numFiles, cacheSize, ratio);
        cacheOptions.shutdownTimeout = shutdownTimeout;

        cacheOptions.fs = getFsOptionsFromConfig(config);

        boolean safeMode = config.getBoolean("safeMode", false);
        System.setProperty(TickDBImpl.SAFE_MODE_PROPERTY, String.valueOf(safeMode));

        if (safeMode)
            LOGGER.warning("Timebase running using \"SAVE MODE\"");

        // TODO: We should pass contextContainer to TickDBImpl somehow. Shouldn't we?
        // TODO: MODULARIZATION
        String uid = config.getString("uid");
        TDB = new TickDBImpl(uid, cacheOptions, tbFolder);

        if (config.getBoolean("highTimeResolution", false)) {
            LOGGER.info("Setting TimeKeeper time resolution to " + TimeKeeper.Mode.HIGH_RESOLUTION_SYNC_BACK);
            TimeKeeper.setMode(TimeKeeper.Mode.HIGH_RESOLUTION_SYNC_BACK);
        }

        this.topicRegistry = TopicRegistryFactory.initRegistryAtQSHome(aeronContext);
        LOGGER.info("Opening and warming up " + TDB.getId() + " ...");

        // TODO: MODULARIZATION - SNMP
/*
        if (QSMIB != null && TDB instanceof TBMonitor) {
            ((TBMonitor)TDB).addObjectMonitor((TBObjectMonitor) QSMIB.getTimeBase());
            ((TBMonitor)TDB).addPropertyMonitor("RAMDisk", (PropertyMonitor) QSMIB.getTimeBase().getDataCache());
        }
*/
        boolean readOnly = config.getBoolean("readOnly", false);
        TDB.open(readOnly);
        if (readOnly)
            LOGGER.info(TDB.getId() + " opened in read-only mode.");

        ShutdownServlet.localhostOnly = config.getBoolean("localhostShutdown", false);

        // create wrapper with TopicDB support
        DXTickDB wrapper = TopicSupportWrapper.wrap(TDB, aeronContext, topicRegistry, contextContainer.getQuickExecutor(), aeronThreadTracker);

        new Thread("TimeBase Warm-Up Thread") {
            @Override
            public void run() {
                TDB.warmUp();
            }
        }.start();

        /// init connections handlers

        Interval interval = Interval.parse(VSProtocol.LINGER_INTERVAL);
        String value = config.getString("lingerInterval");
        try {
            if (value != null)
                interval = Interval.valueOf(value);
        } catch (Exception e) {
            LOGGER.warning("Unable to parse 'lingerInterval' " + value);
        }

        String compression = config.getString("compression", VSCompression.AUTO.toString());
        int maxConnections = config.getInt("maxConnections", VSServerFramework.MAX_CONNECTIONS);
        short maxSocketsPerConnection = (short) config.getInt("maxSocketsPerConnection", VSServerFramework.MAX_SOCKETS_PER_CONNECTION);

        contextContainer.getQuickExecutor().reuseInstance();

        VSServerFramework framework = new VSServerFramework(contextContainer.getQuickExecutor(),
                (int) interval.toMilliseconds(),
                Enum.valueOf(VSCompression.class, compression),
                maxConnections,
                maxSocketsPerConnection, contextContainer);

        if (framework.getCompression() == VSCompression.OFF)
            LOGGER.info("Timebase communication compression disabled.");

        else if (framework.getCompression() == VSCompression.ON)
            LOGGER.info("Timebase communication compression enabled.");

        long bandwidth = config.getLong("maxBandwidth", Long.MAX_VALUE);

        TLSContext tlsContext = null;
        try {
            SSLProperties ssl = config.getSSLConfig();
            if (ssl != null && ssl.enableSSL) {
                tlsContext = new TLSContext(!ssl.sslForLoopback, ssl.sslPort);
                tlsContext.context = SSLContextProvider.createSSLContext(ssl.keystoreFile, ssl.keystorePass, false);
                framework.initSSLSocketFactory(tlsContext);
            }
        } catch (GeneralSecurityException | IOException e ) {
            throw new RuntimeException(e);
        }

        framework.initTransport(getTransportProperties(config));
        framework.setConnectionListener(new VSConnectionHandler(TDB, new ServerParameters(bandwidth), QuantServerExecutor.SC, MAC, aeronContext, aeronThreadTracker, topicRegistry));

        QuantServerExecutor.HANDLER.addHandler((byte)0, framework);
        QuantServerExecutor.HANDLER.addHandler((byte)24, new RESTHandshakeHandler(TDB, QuantServerExecutor.SC, contextContainer, tlsContext));

/*        ConnectionHandshakeHandler snmpConnectionHandler = null;
        QuantServerExecutor.HANDLER.addHandler(
                (byte)48, // BER.SEQUENCE
                snmpConnectionHandler);*/

//        /// AMQP initialization
//        if (config.getBoolean("AMQP", false)) {
//            int amqpPort = config.getInt("AMQPInfo.AMQPPort", 8012);
//            int max = config.getInt("AMQPInfo.maxConnections", 100);
//            initAMQPDriver(amqpPort, max, config.getSSLConfig());
//        }

        // Register server - it's ready to use
        TimeBaseServerRegistry.registerServer(port, wrapper);

        AbstractHandler.TDB = TDB;
        AbstractHandler.SC = QuantServerExecutor.SC;
    }

    private String getPublicAddressForAeron(QuantServiceConfig config) {
        String timebaseHost = config.getString(QuantServiceConfig.HOST_PROP);
        String publicAddressForAeron;
        if (!StringUtils.isEmpty(timebaseHost) && !NetworkInterfaceUtil.isLocal(timebaseHost)) {
            // Use value from config
            publicAddressForAeron = timebaseHost;
        } else {
            publicAddressForAeron = NetworkInterfaceUtil.getOwnPublicAddressAsText();
        }
        return publicAddressForAeron;
    }

    @Override
    public void registerSnmpObjects(QuantServerSnmpObjectContainer snmpContextHolder) {
        if (TDB instanceof TBMonitor) {
            TimeBase snmpObject = new TimeBaseImpl();
            ((TBMonitor)TDB).addObjectMonitor((TBObjectMonitor) snmpObject);
            ((TBMonitor)TDB).addPropertyMonitor("RAMDisk", (PropertyMonitor) snmpObject.getDataCache());
            snmpContextHolder.setTimeBaseSnmpInfo(snmpObject);
        }
    }

    @Nonnull
    public static FSOptions getFsOptionsFromConfig(QuantServiceConfig config) {
        FSOptions options = new FSOptions();
        options.compression = config.getString("fileSystem.compression", options.compression);
        options.maxFolderSize = config.getInt("fileSystem.maxFolderSize", options.maxFolderSize);
        options.maxFileSize = config.getInt("fileSystem.maxFileSize", options.maxFileSize);

        FSType fs = getFSType(config);
        if (fs == FSType.HDFS) {
            String url = config.getString("fileSystem.url");
            if (StringUtils.isEmpty(url))
                throw new IllegalStateException("HDFS url is empty!");
            options.url = url;
        } else if (fs == FSType.AZURE) {
            options.url = FSFactory.AZURE_PROTOCOL_ID + FSFactory.SCHEME_SEPARATOR;
        }
        return options;
    }

    @Override
    public void             configure(Context context) {

    }

    public static final String TRANSPORT_TYPE              = "transportType";
    public static final String TRANSPORT_DIR               = "transportDir";

    public TransportProperties getTransportProperties(QuantServiceConfig config) {
        TransportType transportType = TransportType.valueOf(config.getString(TRANSPORT_TYPE, TransportType.SOCKET_TCP.name()));
        String transportSubDirDef = (transportType == TransportType.AERON_IPC) ? "dxipc/aeron" : "dxipc/offheap";
        String transportDir = config.getString(TRANSPORT_DIR, QSHome.getPath(transportSubDirDef));
        return new TransportProperties(transportType, transportDir);
    }


//    private void initAMQPDriver(int port, int maxConnections, SSLProperties sslProperties){
//        try{
//            amqpDriver = new Driver(TDB, port, maxConnections, QuantServerExecutor.SC, sslProperties);
////            amqpDriver.listen(port);
//            LOGGER.info ("Begins accepting AMQP connections on port "+port+".");
//        }
//        catch (Exception e) {
//            LOGGER.warning("Cannot start AMQP server!");
//            LOGGER.warning(e.getMessage());
//        }
//    }

    private static FSType getFSType(QuantServiceConfig config) {
        try {
            return Enum.valueOf(FSType.class, config.getString("fileSystem"));
        } catch (Throwable t) {
            return FSType.LOCAL;
        }
    }

    private static long getCacheSize(QuantServiceConfig config) {
        return config.getBoolean("useNewMemoryControl", true) ?
                config.getLong("memorySize.1", DataCacheOptions.DEFAULT_CACHE_SIZE) :
                config.getLong("ramCacheSize", DataCacheOptions.DEFAULT_CACHE_SIZE);
    }

//    private Object[]         initPlugins() throws IOException, InterruptedException {
//
//        //PlugInRepository repository = PlugInRepository.getInstance();
//        Collection<PlugInDescriptor> items = PlugInRepository.getInstance().getItems(new RepositoryItemFilter<PlugInDescriptor>() {
//            @Override
//            public boolean accepted(PlugInDescriptor item) {
//                return item.type == PlugInType.TIMEBASE_ACCESS_CONTROLLER;
//            }
//        });
//
//        ArrayList<Object> plugins = new ArrayList<>(items.size());
//
//        for (PlugInDescriptor entry : items) {
//
//            Object instance = null;
//
//            try {
//                plugins.add(instance = PlugInInstance.create(entry).getInstance());
//            } catch (Exception e) {
//                LOGGER.log(Level.SEVERE, "Error while initializing " + entry.getKey() + " plugin: ", e);
//            }
//
//            if (instance instanceof TimebaseAccessController) {
//                if (MAC == null) {
//                    MAC = (TimebaseAccessController) instance;
//                    LOGGER.log(Level.INFO, "Found message access control plugin [" + entry.getKey() + "," + entry.mainClass + "]");
//                } else {
//                    LOGGER.log(Level.WARNING, "Message access control plugin " + entry.getKey() + " skipped. Only one (first) plugin can be used.");
//                }
//            }
//        }
//
//        return plugins.size() > 0 ? plugins.toArray() : null;
//    }

    @Override
    public void close() throws IOException {

        //amqpDriver = Util.close(amqpDriver);

        TimeBaseServerRegistry.unregisterServer(port);
        TDB = Util.close(TDB);

        aeronContext.stop();
    }
}