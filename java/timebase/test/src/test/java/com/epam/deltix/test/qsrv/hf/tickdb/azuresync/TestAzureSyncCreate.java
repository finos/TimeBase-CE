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
package com.epam.deltix.test.qsrv.hf.tickdb.azuresync;

import com.epam.deltix.qsrv.hf.tickdb.StreamConfigurationHelper;
import com.epam.deltix.qsrv.hf.tickdb.pub.DXTickDB;
import com.epam.deltix.qsrv.hf.tickdb.pub.DXTickStream;
import com.epam.deltix.qsrv.hf.tickdb.pub.SelectionOptions;
import com.epam.deltix.qsrv.hf.tickdb.pub.StreamOptions;
import com.epam.deltix.qsrv.hf.tickdb.pub.StreamScope;
import com.epam.deltix.qsrv.hf.tickdb.pub.TickCursor;
import com.epam.deltix.qsrv.hf.tickdb.pub.TickDBFactory;
import com.epam.deltix.qsrv.hf.tickdb.pub.TickLoader;
import com.epam.deltix.qsrv.test.messages.TradeMessage;

import java.util.concurrent.TimeUnit;

/**
 *
 */
public class TestAzureSyncCreate {
    private static final String STREAM_KEY = "testStream";
    private static final String STREAM2_KEY = "test stream2";

    //private static final int PORT = 8078;
    //private static final int PORT = 8056;

    public static void main(String[] argv) throws InterruptedException {
/*
        int port = PORT;
        if (argv.length > 0)
            port = Integer.valueOf(argv[0]);
*/
        String url1 = "dxtick://localhost:" + 8078;
        String url2 = "dxtick://localhost:" + 8079;
        System.out.println("Connecting to " + url1 + "...");
        try (DXTickDB db1 = TickDBFactory.openFromUrl(url1, false);DXTickDB db2 = TickDBFactory.openFromUrl(url2, false)) {
            main(db1, db2);
        }
    }

    private static void main(DXTickDB db1, DXTickDB db2) throws InterruptedException {

        DXTickStream oldStream = db1.getStream(STREAM_KEY);
        if (oldStream != null) {
            System.out.println("Deleting old stream");
            oldStream.delete();
            System.out.println("Deleted old stream");
        }

        DXTickStream oldStream2 = db1.getStream(STREAM2_KEY);
        if (oldStream2 != null) {
            System.out.println("Deleting old stream2");
            oldStream2.delete();
            System.out.println("Deleted old stream2");
        }

        Thread.sleep(15_000);
        System.out.println("Checking streams on DB2");
        long t0 = System.currentTimeMillis();
        boolean streamsDeleted = false;
        while (!streamsDeleted && (System.currentTimeMillis() - t0 > TimeUnit.SECONDS.toMillis(30))) {
            // Wait for stream deletion
            streamsDeleted = (db2.getStream(STREAM_KEY) == null && db2.getStream(STREAM2_KEY) == null);
            if (!streamsDeleted) {
                Thread.sleep(1_000);
            }
        }
        if (!streamsDeleted) {
            throw new AssertionError("Streams were not deleted as expected");
        }
        Thread.sleep(10_000);
        System.out.println("Starting main test");

        StreamOptions streamOptions = new StreamOptions();
        streamOptions.setFixedType (StreamConfigurationHelper.mkUniversalTradeMessageDescriptor ());
        streamOptions.scope = StreamScope.DURABLE;
        DXTickStream stream1 = db1.createStream(STREAM_KEY, streamOptions);
        //DXTickStream stream1 = oldStream;

        System.out.println("Sending messages...");
        TickLoader loader = stream1.createLoader();

        TradeMessage message = new TradeMessage();
        message.setTimeStampMs(111111);
        int msgCount = 5;
        for (int i = 0; i < msgCount; i++) {
            loader.send(message);
        }
        loader.close();
        System.out.println("Messages sent");


        SelectionOptions options = new SelectionOptions();
        TickCursor cursor1 = stream1.createCursor(options);
        System.out.println("Created cursor");

        int count1 = 0;
        cursor1.subscribeToAllTypes();
        cursor1.subscribeToAllTypes();
        cursor1.subscribeToAllEntities();
        cursor1.reset(Long.MIN_VALUE);
        while (cursor1.next()) {
            count1 ++;
        }
        cursor1.close();
        if (count1 != msgCount) {
            throw new AssertionError("Message count1 mismatch");
        }


        System.out.println("Waiting for stream to appear on TB2");
        Thread.sleep(15_000);
        long time0 = System.currentTimeMillis();
        DXTickStream stream2 = null;
        while (System.currentTimeMillis() - time0 < TimeUnit.MINUTES.toMillis(2)) {
            stream2 = db2.getStream(STREAM_KEY);
            if (stream2 != null) {
                break;
            }
        }
        if (stream2 == null) {
            throw new AssertionError("Timeout on waiting for stream on TB2");
        }
        System.out.println("Got stream on TB2");
        Thread.sleep(30_000); // We need this delay because data stream is not checked on other server
        //stream2 = db2.getStream(STREAM_KEY); // Reload stream
        TickCursor cursor2 = stream2.createCursor(options);
        cursor2.subscribeToAllTypes();
        cursor2.subscribeToAllEntities();
        cursor2.reset(Long.MIN_VALUE);
        int count2 = 0;
        while (cursor2.next()) {
            count2 ++;
        }
        cursor2.close();
        if (count2 != msgCount) {
            throw new AssertionError("Message count2 mismatch: " + count2 + " (" + msgCount + " expected)");
        }
        System.out.println("Going to rename stream on base 2");
        stream2.rename(STREAM2_KEY);
        System.out.println("Renamed stream " + STREAM_KEY + " to " + STREAM2_KEY + " on TB2");

        DXTickStream stream4;
        while (System.currentTimeMillis() - time0 < TimeUnit.MINUTES.toMillis(2)) {
            stream4 = db2.getStream(STREAM_KEY);
            if (stream4 != null) {
                break;
            }
        }

        System.out.println("SUCCESS");
    }
}