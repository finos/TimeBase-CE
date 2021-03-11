package com.epam.deltix.test.qsrv.hf.tickdb;

import com.epam.deltix.qsrv.hf.tickdb.TDBRunner;
import com.epam.deltix.qsrv.hf.tickdb.pub.DXTickDB;
import org.junit.Before;
import org.junit.After;
import org.junit.Test;
import org.junit.Ignore;
import com.epam.deltix.qsrv.hf.tickdb.comm.server.TickDBServer;
import com.epam.deltix.qsrv.hf.tickdb.comm.client.TickDBClient;
import com.epam.deltix.qsrv.testsetup.TickDBCreator;

import org.junit.experimental.categories.Category;
import com.epam.deltix.util.JUnitCategories.TickDBFast;

/**
 * User: BazylevD
 * Date: May 19, 2009
 * Time: 3:32:59 PM
 */
@Category(TickDBFast.class)
public class Test_TruncateRemote extends Test_Truncate {
    private DXTickDB localDB;
    private TickDBServer server;

    @Before
    @Override
    public void         setup () {
        localDB = TickDBCreator.createTickDB(TDBRunner.getTemporaryLocation(), false);

        // start server
        server = new TickDBServer(0, localDB);
        server.start();

        db = new TickDBClient("localhost", server.getPort ());
        db.open(false);
        stream = db.getStream (TickDBCreator.BARS_STREAM_KEY);
    }

    @After
    @Override
    public void         tearDown () throws InterruptedException {
        db.close();
        
        server.shutdown (true);
        localDB.close();
        localDB.delete ();
    }

    @Test
    @Ignore("doesn't work 100% reliable due to client-server synchronization issues")
    @Override
    public void                     chopAppleSequential () {
    }
}
