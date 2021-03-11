package com.epam.deltix.test.qsrv.hf.tickdb.select;

import com.epam.deltix.qsrv.QSHome;
import com.epam.deltix.qsrv.hf.tickdb.*;
import com.epam.deltix.qsrv.hf.tickdb.comm.client.TickDBClient;
import com.epam.deltix.qsrv.hf.tickdb.pub.*;

import static com.epam.deltix.qsrv.testsetup.TickDBCreator.*;

import com.epam.deltix.test.qsrv.hf.tickdb.TickDBTest;
import org.junit.*;

import org.junit.experimental.categories.Category;
import com.epam.deltix.util.JUnitCategories.TickDBFast;

@Category(TickDBFast.class)
public class Test_Smoke {
    private DXTickDB                db;
    private final String            LOCATION = TDBRunner.getTemporaryLocation();

    @Before
    public final void           startup() throws Throwable {
        QSHome.set(LOCATION);
        db = openStdTicksTestDB (LOCATION);
    }

    @After
    public final void           teardown () {
        db.close ();
    }

    private class SmokeTester extends TickDBTest {
        private final boolean       raw;

        public SmokeTester (boolean raw) {
            this.raw = raw;
        }

        @Override
        public void                 run (DXTickDB db) throws Exception {
            CursorTester    ct = new CursorTester (db, new SelectionOptions (raw, false));
            boolean         checkType = !(db instanceof TickDBClient);

            try {
                ct.reset (0, 0, 0);
                ct.setAllEntities ();
                ct.setAllTypes ();
                ct.addStreams ((1 << NUM_TEST_STREAMS) - 1);

                while (ct.checkOne (checkType)) {
                    // nothing
                }
            } finally {
                ct.close ();
            }
        }
    }

    @Test
    public void             smokeTestLocalRaw () throws Exception {
        new SmokeTester (true).run (db);
    }

    @Test
    public void             smokeTestLocalNative () throws Exception {
        new SmokeTester (false).run (db);
    }

    @Test
    public void             smokeTestRemoteRaw () throws Exception {
        new SmokeTester (true).runRemote (db);
    }

    @Test
    public void             smokeTestRemoteNative () throws Exception {
        new SmokeTester (false).runRemote (db);
    }

    private class SmokeAndFiltersTester extends TickDBTest {
        private final boolean       raw;

        public SmokeAndFiltersTester (boolean raw) {
            this.raw = raw;
        }

        @Override
        public void                 run (DXTickDB db) throws Exception {
            CursorTester    ct = new CursorTester (db, new SelectionOptions (raw, false));
            boolean         checkType = !(db instanceof TickDBClient);

            try {
                ct.addEntities (3);
                ct.addStreams (6);
                ct.addTypes (5);
                ct.reset (0, 0, 0);

                while (ct.checkOne (checkType)) {
                    // nothing
                }
            } finally {
                ct.close ();
            }
        }
    }

    @Test
    public void             smokeAndFiltersTestLocalRaw () throws Exception {
        new SmokeAndFiltersTester (true).run (db);
    }

    //@Test
    public void             smokeAndFiltersTestLocalNative () throws Exception {
        new SmokeAndFiltersTester (false).run (db);
    }

    @Test
    public void             smokeAndFiltersTestRemoteRaw () throws Exception {
        new SmokeAndFiltersTester (true).runRemote (db);
    }

    @Test
    public void             smokeAndFiltersTestRemoteNative () throws Exception {
        new SmokeAndFiltersTester (true).runRemote (db);
    }
}
