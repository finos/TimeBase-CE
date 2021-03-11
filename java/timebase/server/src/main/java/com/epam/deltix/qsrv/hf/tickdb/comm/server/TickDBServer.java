package com.epam.deltix.qsrv.hf.tickdb.comm.server;

import com.epam.deltix.qsrv.hf.tickdb.comm.server.aeron.AeronThreadTracker;
import com.epam.deltix.qsrv.hf.tickdb.comm.server.aeron.DXServerAeronContext;
import com.epam.deltix.qsrv.hf.tickdb.impl.topic.CopyTopicToStreamTaskManager;
import com.epam.deltix.qsrv.hf.tickdb.impl.topic.TopicRegistryFactory;
import com.epam.deltix.qsrv.hf.tickdb.impl.topic.TopicSupportWrapper;
import com.epam.deltix.qsrv.hf.tickdb.impl.topic.topicregistry.DirectTopicRegistry;
import com.epam.deltix.qsrv.hf.tickdb.comm.TDBProtocol;
import com.epam.deltix.qsrv.hf.tickdb.pub.*;
import com.epam.deltix.util.concurrent.QuickExecutor;
import com.epam.deltix.util.vsocket.TLSContext;
import com.epam.deltix.util.vsocket.TransportProperties;
import com.epam.deltix.util.vsocket.VSServer;

import java.io.*;
import java.net.InetAddress;
import java.util.logging.*;

/**
 * 
 */
public class TickDBServer {
    public static final Logger  LOGGER = Logger.getLogger ("deltix.tickdb.server");

    //private final QuickExecutor         executor;
    private volatile int                port = TDBProtocol.DEFAULT_PORT;
    private VSServer                    server;
    private InetAddress                 address;
    private final DXTickDB              db;
    private TLSContext ssl;
    private TransportProperties         transportProperties;

    private final DXServerAeronContext aeronContext;
    private final AeronThreadTracker aeronThreadTracker = new AeronThreadTracker();
    private final DirectTopicRegistry topicRegistry;

    public TickDBServer (int port, DXTickDB db) {
        this(port, db, null, null);
    }

    public TickDBServer (int port, DXTickDB db, TLSContext ssl, TransportProperties transportProperties) {
        this.port = port;
        this.ssl = ssl;
        this.transportProperties = transportProperties;
        this.aeronContext = DXServerAeronContext.createDefault(port, null, null);

        this.topicRegistry = TopicRegistryFactory.initRegistryAtQSHome(aeronContext);

        // TODO: Design a way to use shared QuickExecutor
        QuickExecutor qeForTopics = QuickExecutor.createNewInstance("TickDBServer-Topics", null);
        AeronThreadTracker aeronThreadTracker = new AeronThreadTracker();

        // Wrap the DB to provide topics support for local instances
        this.db = TopicSupportWrapper.wrap(db, this.aeronContext, this.topicRegistry, qeForTopics, aeronThreadTracker);

        CopyTopicToStreamTaskManager copyTopicToStreamManager = new CopyTopicToStreamTaskManager(db, aeronContext, aeronThreadTracker, topicRegistry);
        copyTopicToStreamManager.startCopyToStreamThreadsForAllTopics();
    }

    public int                  getPort () {
        return port;
    }

    public void                 setPort (int port) {
        this.port = port;
    }

    public InetAddress getAddress() {
        return address;
    }

    public void setAddress(InetAddress address) {
        this.address = address;
    }

    /**
     * @param waitForCompleteShutdown if false this method will only signal shutdown but will not wait for it to finish
     * @throws InterruptedException if method times out while waiting for complete shutdown
     */
    public synchronized void    shutdown (boolean waitForCompleteShutdown)
        throws InterruptedException
    {
        if (server != null) {
            server.close ();

            if (waitForCompleteShutdown)
                server.join ();

            aeronContext.stop();
        }
    }   

    public synchronized void    start () {
        start(new VSConnectionHandler (db, null, aeronContext, aeronThreadTracker, topicRegistry));
    }

    public synchronized void    start (VSConnectionHandler handler) {
        this.aeronContext.start();

        if (server != null)
            throw new IllegalStateException (this + " already started.");

        try {
            server = new VSServer(port, address, ssl, transportProperties);
        } catch (IOException iox) {
            LOGGER.log (Level.SEVERE, "Failed to start", iox);
            return;
        }

        if (port == 0)
            port = server.getLocalPort ();

        server.setConnectionListener (handler);
        server.start ();
    }

    public DXTickDB               getDB () {
        return db;
    }

    @Override
    public String               toString () {
        return ("TickDBServer (" + db.getId () + ")");
    }
}
