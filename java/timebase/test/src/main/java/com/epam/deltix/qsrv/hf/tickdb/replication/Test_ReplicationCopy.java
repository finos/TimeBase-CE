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
package com.epam.deltix.qsrv.hf.tickdb.replication;

import com.epam.deltix.qsrv.hf.tickdb.TDBRunner;
import com.epam.deltix.qsrv.hf.tickdb.pub.*;
import com.epam.deltix.qsrv.hf.tickdb.server.ServerRunner;
import com.epam.deltix.qsrv.hf.tickdb.ui.tbshell.TickDBShell;
import com.epam.deltix.qsrv.test.messages.TradeMessage;
import com.epam.deltix.util.collections.generated.ObjectToLongHashMap;
import com.epam.deltix.util.lang.StringUtils;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;

/**
 * @author Alexei Osipov
 */
public class Test_ReplicationCopy {

    static {
        System.setProperty(TickDBFactory.VERSION_PROPERTY, "5.0");
    }

    @Test
    public void test() throws Exception {

        TDBRunner runner1 = new ServerRunner(true, true);
        runner1.startup();

        TDBRunner runner2 = new ServerRunner(true, true);
        runner2.startup();

//        DXTickDB db1 = TickDBFactory.connect("localhost", runner1.getPort(), false);
//        db1.open(false);
//
//        DXTickDB db2 = TickDBFactory.connect("localhost", runner2.getPort(), false);
//        db2.open(false);

        String srcStreamKey = "replication_src";
        DXTickStream srcStream = StreamTestHelpers.createTestStream(runner1.getServerDb(), srcStreamKey, true);

        String dstStreamKey = "replication_dst";
        DXTickStream dstStream = StreamTestHelpers.createTestStream(runner2.getServerDb(), dstStreamKey, true);

        String[] symbols = {"AAA", "BBB", "CCC", "DDD"};
        StreamTestHelpers.MessageGenerator loaderRunnable = StreamTestHelpers.createDefaultLoaderRunnable(srcStream, Integer.MAX_VALUE,
                symbols);

        Thread loaderThread = new Thread(loaderRunnable);
        Thread reader = new Thread(new ReaderRunnable(dstStream, symbols.length));

        loaderThread.start();
        reader.start();

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    runReplication(runner1.getPort(), runner2.getPort());
                } catch (IOException | InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();

        Thread.sleep(30000);
        reader.interrupt();
        loaderThread.interrupt();

        loaderThread.join();
        reader.join();

        runner1.shutdown();
        runner2.shutdown();
    }

    private static void runReplication(int port1, int port2) throws IOException, InterruptedException {
        TickDBShell shell = new TickDBShell();
        shell.setConfirm(false);

        String[] lines = new String[] {
                "set db dxtick://localhost:" + port2,
                "open",
                "set srcdb dxtick://localhost:" + port1,
                "set srcstream replication_src",
                "set cpmode live",
                "set timeoffset 0H",
                "replicate replication_dst",
                "close", ""
        };
        Reader rd = new StringReader(StringUtils.join("\r\n", lines));
        shell.runScript("", rd, false, true);
    }

    private static class ReaderRunnable implements Runnable {
        private final DXTickStream stream;
        private final int symbolsCount;

        public ReaderRunnable(DXTickStream dstStream, int symbols) {
            this.stream = dstStream;
            this.symbolsCount = symbols;
        }

        @Override
        public void run() {
            ObjectToLongHashMap<String> times = new ObjectToLongHashMap<String>();

            SelectionOptions so = new SelectionOptions();
            so.live = true;
            TickCursor cursor = stream.select(0, so);
            long count = 0;
            while (cursor.next()) {
                TradeMessage msg = (TradeMessage) cursor.getMessage();
                long index = (long) msg.getNetPriceChange();

                long last = times.get(msg.getSymbol().toString(), -1);
                if (index - last > symbolsCount && last >= 0)
                    Assert.fail("Message Order Mismatch! Got " + index + " > " + last + " diff = " + (index - last));

                times.put(msg.getSymbol().toString(), index);

                count++;

                if (count % 100_000 == 0) {
                    System.out.println("Got " + count);
                }
            }
        }
    }
}
