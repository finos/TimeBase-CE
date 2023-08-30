/*
 * Copyright 2023 EPAM Systems, Inc
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
import com.epam.deltix.qsrv.hf.tickdb.pub.*;

import java.util.Arrays;

import com.epam.deltix.qsrv.test.messages.TradeMessage;
import org.junit.*;
import static org.junit.Assert.*;

import org.junit.experimental.categories.Category;
import com.epam.deltix.util.JUnitCategories.TickDBFast;

/**
 *  Tests live cursors.
 */
@Category(TickDBFast.class)
public class Test_OutOfSequenceMessage {
    private static final String     STREAM_KEY = "stream";
    private static final int        NUM_ENTITIES = 5;
    private static final String []  SYMBOLS = new String [NUM_ENTITIES];

    static {
        for (int ii = 0; ii < NUM_ENTITIES; ii++)
            SYMBOLS [ii] = "DLX" + ii;  // Deltix' subsidiaries :)
    }
    
    protected final DXTickDB        localDB =
        TickDBFactory.create (TDBRunner.getTemporaryLocation());

    class OOSMessageTest extends TickDBTest {
        private TradeMessage msg = new TradeMessage();
        private long []             timestamps = new long [NUM_ENTITIES];

        public OOSMessageTest () {

            msg.setSize(1);

            Arrays.fill (timestamps, System.currentTimeMillis ());
        }

        private void            test (TickLoader loader, int numMsgsToFailure) {
            //  We should be able to load entities sequentially.
            for (int ient = 0; ient < NUM_ENTITIES; ient++) {                
                msg.setSymbol(SYMBOLS [ient]);

                for (int imsg = 0; imsg < numMsgsToFailure; imsg++) {
                    msg.setTimeStampMs(timestamps [ient]);
                    msg.setPrice(imsg);

                    loader.send (msg);

                    if ((imsg % 3) == 0)
                        timestamps [ient]++;
                }
            }

            ErrorListener listener = new ErrorListener();
            loader.addEventListener(listener);

            //  Now send out-of-sequence messages
            for (int ient = 0; ient < NUM_ENTITIES; ient++) {
                msg.setSymbol(SYMBOLS [ient]);
                msg.setTimeStampMs(timestamps [ient] - 1);
                msg.setPrice(0);

                loader.send (msg);
            }
            assertTrue ("Exception was not thrown", listener.count > 0);
        }

        @Override
        public void             run (DXTickDB db) throws Exception {
            WritableTickStream      stream = db.getStream (STREAM_KEY);
            LoadingOptions options = new LoadingOptions();
            options.addErrorAction(LoadingError.class,
                LoadingOptions.ErrorAction.NotifyAndContinue);
            TickLoader              loader = stream.createLoader (options);

            test (loader, 3);

            //  Test recovery and longer numbers (to make sure buffers play no role)
            test (loader, 300);

            //  Last time...
            test (loader, 8000);

            loader.close();
        }

        final class ErrorListener implements LoadingErrorListener {
            public int count = 0;

            public void saveChanges() {
            }

            public void onError(LoadingError e) {
                count++;
            }           
        }
    }

    @Before
    public void             setup () {
        localDB.format ();

        DXTickStream        s =
            localDB.createStream (STREAM_KEY, null, null, 0);

        StreamConfigurationHelper.setTradeNoExchNoCur (s);
    }

    @After
    public void             teardown () {
        localDB.close ();
    }

    @Test 
    public void             testLocal () throws Exception {
        new OOSMessageTest ().run (localDB);
    }
    
    @Test @Ignore
    public void             testRemote () throws Exception {
        new OOSMessageTest ().runRemote (localDB);
    }
}