package com.epam.deltix.qsrv.hf.tickdb.comm.server;

import com.epam.deltix.qsrv.hf.tickdb.impl.TickDBImpl;
import com.epam.deltix.qsrv.hf.tickdb.pub.DXTickDB;
import com.epam.deltix.qsrv.hf.tickdb.pub.DataCacheOptions;
import com.epam.deltix.qsrv.hf.tickdb.test.EmbeddedServer;
import com.epam.deltix.util.vsocket.TLSContext;
import com.epam.deltix.util.vsocket.TransportProperties;

import java.io.File;

public class TestServer implements EmbeddedServer {
    private TickDBServer        server;
    private int                 port;

    private final TransportProperties transportProperties;
    private final DataCacheOptions options;
    private final File location;

    private final TLSContext ssl;

    public TestServer (int port, TLSContext ssl, TransportProperties transportProperties, DataCacheOptions options, File location) {
        this.port = port;
        this.ssl = ssl;
        this.transportProperties = transportProperties;
        this.options = options;
        this.location = location;
    }

    public TestServer(DataCacheOptions options, File location) {
        this.options = options;
        this.location = location;
        this.transportProperties = null;
        this.ssl = null;
    }

    public TestServer(File location) {
        this.options = new DataCacheOptions();
        this.location = location;
        this.transportProperties = null;
        this.ssl = null;
    }

    @Override
    public int              start () {
        DXTickDB db = new TickDBImpl(options, location);
        db.open(false);

        server = new TickDBServer(port, db, ssl, transportProperties);
        server.start ();

        return server.getPort();
    }

    @Override
    public void             stop() throws Exception {
        if (server != null) {
            server.getDB().close();
            server.shutdown(true);
        }
    }

    @Override
    public DXTickDB         getDB() {
        return server.getDB();
    }

    @Override
    public int getPort() {
        return server.getPort();
    }
}
