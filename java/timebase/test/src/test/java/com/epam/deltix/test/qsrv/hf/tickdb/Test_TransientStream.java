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
package com.epam.deltix.test.qsrv.hf.tickdb;

import com.epam.deltix.qsrv.hf.tickdb.StreamConfigurationHelper;
import com.epam.deltix.qsrv.hf.tickdb.TDBRunner;
import com.epam.deltix.qsrv.test.messages.BarMessage;
import com.epam.deltix.qsrv.test.messages.TradeMessage;
import com.epam.deltix.timebase.messages.IdentityKey;

import com.epam.deltix.qsrv.hf.tickdb.pub.*;

import com.epam.deltix.util.time.TimeKeeper;
import com.epam.deltix.util.lang.Util;
import org.junit.*;
import static org.junit.Assert.assertTrue;

import java.util.GregorianCalendar;

import org.junit.experimental.categories.Category;
import com.epam.deltix.util.JUnitCategories.TickDBFast;

/**
 *  Test transient stream features.
 */
@Category(TickDBFast.class)
public class Test_TransientStream {
    public static final String      STREAM_KEY = "test.stream";

    private DXTickDB     db;

    @Before
    public final void           startup() throws Throwable {
        db = TickDBFactory.create (TDBRunner.getTemporaryLocation());

        db.format ();
    }

    @After
    public final void           teardown () {
        db.close ();
    }

    @Test
    public void testSelect() throws Exception {
        StreamOptions               options =
            new StreamOptions (
                StreamScope.TRANSIENT,
                "Stream Name",
                "Description Line1\nLine 2\nLine 3",
                1
            );

        options.setFixedType (StreamConfigurationHelper.mkUniversalTradeMessageDescriptor ());
        (options.bufferOptions = new BufferOptions()).lossless = true;

        final DXTickStream      stream = db.createStream (STREAM_KEY, options);
        TickCursor cursor = stream.select(Long.MAX_VALUE, null, new String[0], new IdentityKey[0]);

        cursor.subscribeToAllEntities();
        cursor.subscribeToAllTypes();

        assertTrue(!cursor.next());
    }

    class TransientStreamTester extends TickDBTest {
        public TransientStreamTester () {
        }

        @Override
        public void                 run (DXTickDB db) throws Exception {
            StreamOptions               options =
                new StreamOptions (
                    StreamScope.TRANSIENT,
                    "Stream Name",
                    "Description Line1\nLine 2\nLine 3",
                    1
                );

            options.setFixedType (StreamConfigurationHelper.mkUniversalTradeMessageDescriptor ());

            final DXTickStream      stream = db.createStream (STREAM_KEY, options);

            TradeMessage msg = new TradeMessage();
            msg.setSymbol("DLTX");

            TickLoader              loader = stream.createLoader ();

            msg.setTimeStampMs(TimeKeeper.currentTime);
            loader.send (msg);

            loader.close ();

            long []                 tr = stream.getTimeRange ();
        }
    }

     class TransientStreamTester1 extends TickDBTest {
        public TransientStreamTester1 () {
        }

        @Override
        public void                 run (DXTickDB db) throws Exception {
            StreamOptions               options =
                new StreamOptions (
                    StreamScope.TRANSIENT,
                    "Stream Name",
                    "Description Line1\nLine 2\nLine 3",
                    1
                );

            options.setFixedType (StreamConfigurationHelper.mkUniversalTradeMessageDescriptor ());

            final DXTickStream      stream = db.createStream (STREAM_KEY, options);

            TDBRunner.TradesGenerator generator =
                    new TDBRunner.TradesGenerator(
                        new GregorianCalendar(2009, 1, 1),
                            (int) BarMessage.BAR_MINUTE, -1, "DLTX", "ORCL");

            boolean passed = false;
            TickLoader        loader = null;
            try {
                loader = stream.createLoader ();
                int count = 0;
                while (generator.next()) {
                    loader.send(generator.getMessage());
                    count++;
                    if (count == 1000)
                        stream.delete();
                }
                loader.close();
                loader = null;
            }
            catch (WriterAbortedException e) {
                // valid case
                passed = true;
            }
            finally {
                Util.close(loader);
            }
            
            assertTrue(passed);
        }
    }

    @Test (timeout=60000)
    public void             transStreamTestLocal () throws Exception {
        new TransientStreamTester ().run (db);
    }

    @Test (timeout=60000)
    public void             transStreamTestRemote () throws Exception {
        new TransientStreamTester ().runRemote (db);
    }

    @Test (timeout=60000)
    public void             testDelete() throws Exception {
        new TransientStreamTester1 ().runRemote (db);
    }    
}
