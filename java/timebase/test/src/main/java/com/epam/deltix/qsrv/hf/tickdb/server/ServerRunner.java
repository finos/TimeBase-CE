package com.epam.deltix.qsrv.hf.tickdb.server;

import com.epam.deltix.qsrv.hf.tickdb.TDBRunner;
import com.epam.deltix.qsrv.hf.tickdb.comm.server.TestServer;
import com.epam.deltix.qsrv.hf.tickdb.pub.DataCacheOptions;

import java.io.File;

public class ServerRunner extends TDBRunner {

    public ServerRunner() {
        this(true, true, getTemporaryLocation());
    }

    public ServerRunner(boolean isRemote, boolean doFormat) {
        this(isRemote, doFormat, getTemporaryLocation());
    }

    public ServerRunner(boolean doFormat, String location) {
        this(true, doFormat, location);
    }

    public ServerRunner(boolean isRemote, boolean doFormat, String location) {
        super(isRemote, doFormat, location, new TestServer(new File(location)));
    }

    public ServerRunner(boolean isRemote, boolean doFormat, DataCacheOptions options) {
        super(isRemote, doFormat, getTemporaryLocation(), new TestServer(options, new File(getTemporaryLocation())));
    }

    public static ServerRunner create(boolean isRemote, boolean doFormat, long cacheSize) {
        DataCacheOptions dataCacheOptions = new DataCacheOptions();
        dataCacheOptions.cacheSize = cacheSize;
        return new ServerRunner(isRemote, doFormat, dataCacheOptions);
    }
}
