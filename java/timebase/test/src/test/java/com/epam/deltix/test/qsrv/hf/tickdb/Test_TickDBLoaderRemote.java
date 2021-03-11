package com.epam.deltix.test.qsrv.hf.tickdb;

import com.epam.deltix.qsrv.hf.tickdb.TDBRunner;
import org.junit.Before;
import org.junit.After;
import com.epam.deltix.qsrv.hf.tickdb.pub.TickDBFactory;
import com.epam.deltix.qsrv.hf.tickdb.pub.DXTickDB;
import com.epam.deltix.qsrv.hf.tickdb.comm.server.TickDBServer;
import com.epam.deltix.qsrv.hf.tickdb.comm.client.TickDBClient;

import org.junit.experimental.categories.Category;
import com.epam.deltix.util.JUnitCategories.TickDBFast;

/**
 * User: BazylevD
 * Date: Apr 21, 2009
 * Time: 2:47:00 PM
 */
@Category(TickDBFast.class)
public class Test_TickDBLoaderRemote extends Test_TickDBLoader {
    private DXTickDB localDB;
    private TickDBServer server;

    @Before
    @Override
    public void         setUp () {
        localDB = TickDBFactory.create(TDBRunner.getTemporaryLocation());
        localDB.format ();

        // start server
        server = new TickDBServer(0, localDB);
        server.start();

        db = new TickDBClient("localhost", server.getPort ());
        db.open(false);
    }

    @After
    @Override
    public void         tearDown () {
        db.close();

        try {
            server.shutdown (true);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        localDB.close();
        //localDB.delete ();
    }
}
