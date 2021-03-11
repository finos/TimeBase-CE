package com.epam.deltix.test.qsrv.hf.tickdb;

import com.epam.deltix.qsrv.hf.tickdb.pub.*;
import com.epam.deltix.util.lang.Util;
import org.junit.*;

import org.junit.experimental.categories.Category;
import com.epam.deltix.util.JUnitCategories.TickDBStress;


/**
 *  Tests live cursors.
 */
@Category(TickDBStress.class)
public class StressTest_LiveCursors extends LiveCursorTestBase {
    private static final long       TEST_TIMEOUT = 
        Util.IS64BIT ? 15000 : 600000;

    protected static final int      NUM_READERS =
        Integer.getInteger ("nr", 50);

    class StartReadersFirstTest extends TickDBTest {
        private final String        streamKey;
        private final int           numReaders;

        public StartReadersFirstTest (String streamKey, int numReaders) {
            this.streamKey = streamKey;
            this.numReaders = numReaders;
        }

        @Override
        public void                 run (DXTickDB db) throws Exception {            
            WritableTickStream          stream = db.getStream (streamKey);

            addRandomReaders (stream, numReaders);

            start ();

            Thread.sleep (250); // Let them settle down...

            generateData (stream, NUM_MSGS_PER_ENTITY, 0);

            join (TEST_TIMEOUT);
        }
    }

    class LadderTest extends TickDBTest {
        private final String        streamKey;
        private final int           numReaders;

        public LadderTest (String streamKey, int numReaders) {
            this.streamKey = streamKey;
            this.numReaders = numReaders;
        }

        @Override
        public void                 run (DXTickDB db) throws Exception {
            WritableTickStream          stream = db.getStream (streamKey);

            addRandomReaders (stream, numReaders);

            int                     init = NUM_MSGS_PER_ENTITY / 2;

            generateData (stream, init, 0);

            start ();

            generateData (stream, NUM_MSGS_PER_ENTITY - init, 0);

            join (TEST_TIMEOUT);
        }
    }

    @Test
    public void             readersFirstDistributedLocal () throws Exception {
        new StartReadersFirstTest (DIST_STREAM, NUM_READERS).run (localDB);
    }

    @Test
    public void             readersFirstConsolidatedLocal () throws Exception {
        new StartReadersFirstTest (CONS_STREAM, NUM_READERS).run (localDB);
    }

    @Test
    public void             readersFirstDistributedRemote () throws Exception {
        new StartReadersFirstTest (DIST_STREAM, NUM_READERS).runRemote (localDB);
    }

    @Test
    public void             readersFirstConsolidatedRemote () throws Exception {
        new StartReadersFirstTest (CONS_STREAM, NUM_READERS).runRemote (localDB);
    }

    @Test
    public void             ladderDistributedLocal () throws Exception {
        new LadderTest (DIST_STREAM, NUM_READERS).run (localDB);
    }

    @Test
    public void             ladderConsolidatedLocal () throws Exception {
        new LadderTest (CONS_STREAM, NUM_READERS).run (localDB);
    }

    @Test
    public void             ladderDistributedRemote () throws Exception {
        new LadderTest (DIST_STREAM, NUM_READERS).runRemote (localDB);
    }

    @Test
    public void             ladderConsolidatedRemote () throws Exception {
        new LadderTest (CONS_STREAM, NUM_READERS).runRemote (localDB);
    }
}
