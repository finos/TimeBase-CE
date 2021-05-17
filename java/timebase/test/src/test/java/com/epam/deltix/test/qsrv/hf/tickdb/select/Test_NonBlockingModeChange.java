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
package com.epam.deltix.test.qsrv.hf.tickdb.select;

import com.epam.deltix.qsrv.QSHome;
import com.epam.deltix.qsrv.hf.tickdb.*;
import com.epam.deltix.qsrv.hf.tickdb.comm.client.TickDBClient;
import com.epam.deltix.qsrv.hf.tickdb.pub.*;
import com.epam.deltix.test.qsrv.hf.tickdb.TickDBTest;
import com.epam.deltix.util.concurrent.NotifyingRunnable;

import static com.epam.deltix.qsrv.testsetup.TickDBCreator.*;
import org.junit.*;

import org.junit.experimental.categories.Category;
import com.epam.deltix.util.JUnitCategories.TickDBFast;

@Category(TickDBFast.class)
public class Test_NonBlockingModeChange {
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

    private class ModeSwitchTester extends TickDBTest {
        private final Runnable                      avlnr =
            new NotifyingRunnable ();            

        public ModeSwitchTester () {
        }

        @Override
        public void                 run (DXTickDB db) throws Exception {
            CursorTester    ct = new CursorTester (db, null);
            boolean         checkType = !(db instanceof TickDBClient);
            int             n = 0;
            boolean         sync = true;

            try {
                ct.reset (0, 0, 0);
                ct.setAllEntities ();
                ct.setAllTypes ();
                ct.addStreams ((1 << NUM_TEST_STREAMS) - 1);

                while (ct.checkOne (checkType, avlnr)) {
                    n++;

                    if (n % 33 == 0) {
                        sync = !sync;
                        
                        if (sync) 
                            ct.getCursor ().setAvailabilityListener (null);                        
                        else 
                            ct.getCursor ().setAvailabilityListener (avlnr);                        
                    }
                }
            } finally {
                ct.close ();
            }
        }
    }

    @Test
    public void             smokeTestLocal () throws Exception {
        new ModeSwitchTester ().run (db);
    }

    @Test
    public void             smokeTestRemote () throws Exception {
        new ModeSwitchTester ().runRemote (db);
    }   
}
