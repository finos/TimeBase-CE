package com.epam.deltix.test.qsrv.hf.tickdb;

import com.epam.deltix.qsrv.hf.tickdb.TDBRunner;
import com.epam.deltix.qsrv.hf.tickdb.comm.server.TestServer;
import com.epam.deltix.qsrv.hf.tickdb.comm.server.TomcatServer;
import com.epam.deltix.qsrv.hf.tickdb.test.EmbeddedServer;
import org.junit.After;
import org.junit.Before;


public abstract class TDBTestBase extends TDBRunner {

    public TDBTestBase(boolean isRemote) {
        this(isRemote, true);
    }

    public TDBTestBase(boolean isRemote, boolean doFormat) {
        this(isRemote, doFormat, getTemporaryLocation());
    }

    public TDBTestBase(boolean isRemote, boolean doFormat, String location) {
        this(isRemote, doFormat, location, null, false);
    }

    public TDBTestBase(boolean isRemote, boolean doFormat, EmbeddedServer server) {
        this(isRemote, doFormat, getTemporaryLocation(), server);
    }

    public TDBTestBase(boolean isRemote, boolean doFormat, String location, EmbeddedServer server) {
        this(isRemote, doFormat, location, server, false);
    }

    public TDBTestBase(boolean isRemote, boolean doFormat, String location, EmbeddedServer server, boolean enableLocalTopics) {
        super(isRemote, doFormat, location, server == null ? new TomcatServer() : server, enableLocalTopics);
    }

    @Before
    public void startup() throws Exception {
        super.startup();
    }

    @After
    public void shutdown() throws Exception {
        super.shutdown();
    }
}
