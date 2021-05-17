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
package com.epam.deltix.test.qsrv.hf.tickdb.replication;

import com.epam.deltix.qsrv.hf.tickdb.pub.DXTickDB;
import com.epam.deltix.qsrv.hf.tickdb.pub.DXTickStream;
import com.epam.deltix.qsrv.hf.tickdb.pub.SelectionOptions;
import com.epam.deltix.qsrv.hf.tickdb.pub.TickCursor;
import com.epam.deltix.qsrv.hf.tickdb.pub.TickDBFactory;
import com.epam.deltix.qsrv.test.messages.TradeMessage;

/**
 * @author Alexei Osipov
 */
public class GenerateRepliactionTestData {
    public static void main(String[] args) throws InterruptedException {

        DXTickDB db1 = TickDBFactory.connect("localhost", 8011, false);
        db1.open(false);

        DXTickDB db2 = TickDBFactory.connect("localhost", 8022, false);
        db2.open(true);

        String srcStreamKey = "replication_src";
        DXTickStream srcStream = StreamTestHelpers.createTestStream(db1, srcStreamKey, true);

        String dstStreamKey = "replication_dst";
        DXTickStream dstStream = StreamTestHelpers.createTestStream(db2, dstStreamKey, true);


        StreamTestHelpers.MessageGenerator loaderRunnable = StreamTestHelpers.createDefaultLoaderRunnable(srcStream);

        Thread loaderThread = new Thread(loaderRunnable);

        Thread readerThread = new Thread(new ReaderRunnable(dstStream));

        loaderThread.start();
        readerThread.start();


        loaderThread.join();
        readerThread.join();



    }

    private static class ReaderRunnable implements Runnable {
        private final DXTickStream dstStream;

        public ReaderRunnable(DXTickStream dstStream) {
            this.dstStream = dstStream;
        }

        @Override
        public void run() {
            SelectionOptions so = new SelectionOptions();
            so.live = true;
            TickCursor cursor = dstStream.select(Long.MIN_VALUE, so);
            long count = 0;
            long diff = 0;
            while (cursor.next()) {
                TradeMessage msg = (TradeMessage) cursor.getMessage();
                long val = (long) msg.getNetPriceChange();

                long expected = count + diff;
                long currentDiff = val - expected;
                if (currentDiff != 0) {
                    System.err.println("Value mismatch! Got " + val + " expected: " + expected + " diff: " + currentDiff);
                    diff = val - count;

                }
                count++;

                if (count % 100_000 == 0) {
                    System.out.println("Got " + count);
                }
            }
        }
    }
}
