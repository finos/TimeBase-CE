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
import com.epam.deltix.qsrv.hf.tickdb.pub.*;
import com.epam.deltix.qsrv.hf.tickdb.comm.server.TickDBServer;
import com.epam.deltix.qsrv.hf.tickdb.comm.client.TickDBClient;

import com.epam.deltix.qsrv.test.messages.BarMessage;
import com.epam.deltix.qsrv.testsetup.TickDBCreator;
import com.epam.deltix.util.JUnitCategories;
import com.epam.deltix.util.lang.Util;
import com.epam.deltix.util.time.TimeKeeper;
import com.epam.deltix.util.time.Interval;
import org.junit.Before;
import org.junit.After;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;

import static org.junit.Assert.assertTrue;

@Category(JUnitCategories.TickDBFast.class)
public class Test_AsyncChangeFilter {
    private static DXTickDB realDB;
    private static TickDBServer server;
    private static DXTickDB remoteDB;

    @Before
    public void setUp() throws InterruptedException {
        String location = TDBRunner.getTemporaryLocation();
        realDB = TickDBFactory.createFromUrl (location);
        realDB.format ();

        System.setProperty ("deltix.qsrv.home", new File(location).getParent());

        if (!Util.QUIET)
            System.out.println (realDB.getId () + " has been created.");

        server = new TickDBServer(0, realDB);
        server.start();
        connect(false);
    }

    @After
    public void tearDown() throws InterruptedException {
        disconnect();
        server.shutdown(true);
        realDB.close();
    }

    private static void connect(boolean ro) {
        remoteDB = new TickDBClient("localhost", server.getPort());
        remoteDB.open(ro);

        if (!Boolean.getBoolean("quiet"))
            System.out.println("Connected to " + remoteDB.getId());
    }

    private static void disconnect() {
        if (remoteDB != null) {
            remoteDB.close();
            remoteDB = null;
        }
    }

    @Test(timeout = 30_000)
    public void test() throws IOException, ParseException, InterruptedException {
        DXTickStream            stream =
            remoteDB.createStream (
                TickDBCreator.BARS_STREAM_KEY, "bars", "bars", 0
            );

        StreamConfigurationHelper.setBar (
            stream, "", null, Interval.MINUTE,
            "DECIMAL(4)",
            "DECIMAL(0)"
        );

        Thread th2 = new AppleSupplier(realDB);
        th2.start();

        Thread th1 = new AyncClient(stream);
        th1.start();

        th1.join (/*20000*/);
        th2.join();
    }

    private volatile boolean stopped = false;

    private class AppleSupplier extends Thread {
        private WritableTickStream stream;
        private final int BAR_SIZE = 500;

        public AppleSupplier(DXTickDB db) {
            super("Apple Supplier");
            setDaemon(true);
            stream = db.getStream(TickDBCreator.BARS_STREAM_KEY);
        }

        @Override
        public void     run () {
            BarMessage msg = new BarMessage();
            msg.setSymbol("AAPL");
            //msg.barSize = BAR_SIZE;
            msg.setOpen(12.34);
            msg.setHigh(msg.getOpen());
            msg.setLow(msg.getOpen());
            msg.setClose(msg.getOpen());

            msg.setVolume(0);

            try (TickLoader loader = stream.createLoader()) {
                while (!stopped) {
                    msg.setTimeStampMs(TimeKeeper.currentTime);
                    loader.send(msg);
                    Thread.sleep(BAR_SIZE);
                    msg.setVolume(msg.getVolume() + 1);
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private class AyncClient extends Thread {
        private final TickStream stream;

        private AyncClient(TickStream stream) {
            super("Aynchronous Client");
            this.stream = stream;
        }

        @Override
        public void run() {
            try (TickCursor cur = stream.select (TimeKeeper.currentTime, new SelectionOptions(false, true)))
            {
                for (int ii = 0; ii < 5; ii++) {
                    cur.next ();
                    System.out.println (cur.getMessage ());
                }

                System.out.println ("setFilter restricted");
                cur.clearAllEntities();

                Thread.sleep(2000);
                System.out.println("setFilter unrestricted");

                cur.subscribeToAllEntities();

                for (int ii = 0; ii < 5; ii++) {
                    cur.next ();
                    System.out.println (cur.getMessage ());
                }

                stopped = true;
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}