package com.epam.deltix.test.qsrv.hf.tickdb.select;

/*  ## TICKDB.FAST## */

import com.epam.deltix.qsrv.QSHome;
import com.epam.deltix.qsrv.hf.tickdb.*;
import com.epam.deltix.qsrv.hf.tickdb.comm.client.TickDBClient;
import com.epam.deltix.qsrv.hf.tickdb.pub.*;
import java.util.Random;

import static com.epam.deltix.qsrv.testsetup.TickDBCreator.*;

import com.epam.deltix.test.qsrv.hf.tickdb.TickDBTest;
import com.epam.deltix.util.JUnitCategories;
import org.junit.*;
import org.junit.experimental.categories.Category;

@Category(JUnitCategories.TickDBFast.class)
public class Test_DynamicSubChange {
    private static final int        MIN_NUM_TEST = 4;
    private static final int        MAX_NUM_TEST = 20;
    private static final int        NUM_SEGMENTS = 80;

    private final String            LOCATION = TDBRunner.getTemporaryLocation();
    private DXTickDB                db;

    @Before
    public final void           startup() throws Throwable {
        QSHome.set(LOCATION);
        db = openStdTicksTestDB (LOCATION);
    }

    @After
    public final void           teardown () {
        db.close ();
    }

    private static class FilterTester extends TickDBTest {
        private final boolean       raw;

        public FilterTester (boolean raw) {
            this.raw = raw;
        }

        @Override
        public void                 run (DXTickDB db) throws Exception {
            Random          r = new Random (2010);
            CursorTester    ct = new CursorTester (db, new SelectionOptions (raw, false));
            boolean         typesMightMix = db instanceof TickDBClient;

            if (typesMightMix)
                ct.addTypes (2);    // Float only

            try {
                for (int n = 0; n < NUM_SEGMENTS; n++) {
                    int         action = r.nextInt (11);

                    switch (action) {
                        default:
                            ct.setAllEntities ();
                            if (!typesMightMix)
                                ct.setAllTypes ();
                            ct.addStreams ((1 << NUM_TEST_STREAMS) - 1);
                            ct.reset (0, 0, 0);
                            break;

                        case 0: {
                            int     entMask = 1 + r.nextInt ((1 << NUM_SYMBOLS) - 1);

                            ct.addEntities (entMask);
                            break;
                        }

                        case 1: {
                            int     entMask = 1 + r.nextInt ((1 << NUM_SYMBOLS) - 1);

                            ct.removeEntities (entMask);
                            break;
                        }

                        case 2:
                            ct.setAllEntities ();
                            break;

                        case 3:
                            if (typesMightMix)
                                continue;

                            ct.addTypes (1 + r.nextInt ((1 << NUM_TYPES) - 1));
                            break;
                        

                        case 4:
                            if (typesMightMix)
                                continue;

                            ct.removeTypes (1 + r.nextInt ((1 << NUM_TYPES) - 1));
                            break;                        

                        case 5:
                            if (typesMightMix)
                                continue;

                            ct.setAllTypes ();                            
                            break;

                        case 6: 
                            ct.addStreams (1 + r.nextInt ((1 << NUM_TEST_STREAMS) - 1));
                            break;
                        

                        case 7: 
                            ct.removeStreams (1 + r.nextInt ((1 << NUM_TEST_STREAMS) - 1));
                            break;                        

                        case 8: {
                            int     seq = r.nextInt (NUM_MESSAGES);
                            int     streamIdx = r.nextInt (NUM_TEST_STREAMS);
                            int     entIdx = r.nextInt (NUM_SYMBOLS);

                            ct.reset (seq, streamIdx, entIdx);
                        }
                    }

                    int         numCheck = MIN_NUM_TEST + r.nextInt (MAX_NUM_TEST);

                    for (int ii = 0; ii < numCheck; ii++) {
                        if (!ct.checkOne (true))
                            break;
                    }
                }
            } finally {
                ct.close ();
            }
        }
    }

    @Test
    public void             filterTestLocalRaw () throws Exception {
        new FilterTester(true).run (db);
    }

    @Test
    public void             filterTestLocalNative () throws Exception {
        new FilterTester(false).run (db);
    }

    @Test
    public void             filterTestRemoteRaw () throws Exception {
        new FilterTester(true).runRemote (db);
    }

    @Test
    public void             filterTestRemoteNative () throws Exception {
        new FilterTester(false).runRemote (db);
    }
}
