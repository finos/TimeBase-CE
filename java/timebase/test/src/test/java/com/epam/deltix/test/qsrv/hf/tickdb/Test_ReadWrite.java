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
import com.epam.deltix.timebase.messages.InstrumentMessage;

import com.epam.deltix.qsrv.hf.tickdb.pub.*;
import com.epam.deltix.util.io.GUID;
import com.epam.deltix.util.lang.Util;
import com.epam.deltix.util.time.GMT;
import org.junit.experimental.categories.Category;

import java.io.File;
import java.util.Date;
import java.util.GregorianCalendar;

/**
 * Created by Alex Karpovich on 9/6/2018.
 */
@Category(Long.class)
public class Test_ReadWrite  {

    final static int TOTAL_MESSAGES = 10_000_000;

    static DataCacheOptions options = new DataCacheOptions(Integer.MAX_VALUE, 500 * 1024 * 1024);
    //static String location = getTemporaryLocation();

//    static {
//        options.fs.withMaxFileSize(1024 * 1024).withMaxFolderSize(20);
//    }

    public Test_ReadWrite() {
        //super(true, true, location, new TestServer(options, new File(location)));
    }

    private void increment(StringBuffer symbol, int index) {
        if (symbol.charAt(index) == (int)'Z') {
            increment(symbol, index - 1);
            symbol.setCharAt(index, 'A');
        }
        else
            symbol.setCharAt(index, (char) (symbol.charAt(index) + 1));
    }

    private void loadData(DXTickStream stream, Date time, String[] symbols, int count) {
        GregorianCalendar c = new GregorianCalendar(2010, 0, 1);
        if (time != null)
            c.setTime(time);

        TDBRunner.BarsGenerator g = new TDBRunner.BarsGenerator(c, (int) BarMessage.BAR_MINUTE, count, symbols);

        TickLoader loader = stream.createLoader();
        while (g.next())
            loader.send(g.getMessage());

        loader.close();

        System.out.println("Loaded range: " + GMT.formatDateTimeMillis(time) + ": " + g.getMessage().getTimeString());
    }

    public void test() throws InterruptedException {

        DXTickDB db = TickDBFactory.createFromUrl("dxtick://10.10.87.28:8011");
        db.open(false);

        // prepare streams
        for (int ii = 0; ii < 10; ii++) {
            String name = "source" + ii;

            DXTickStream s = db.getStream(name);
            if (s != null)
                s.delete();

            final DXTickStream stream = db.createStream(name,
                    StreamOptions.fixedType(StreamScope.DURABLE, name, null, 1,
                            StreamConfigurationHelper.mkUniversalBarMessageDescriptor()));

            String[] symbols = new String[1000];
            for (int i = 0; i < symbols.length; i++)
                symbols[i] = String.valueOf(i) + "000";

            Runnable producer = new Runnable() {
                @Override
                public void run() {
                    loadData(stream, null, symbols, TOTAL_MESSAGES);
                }
            };

            new Thread(producer).start();
        }

        Thread[] threads = new Thread[10];

        for (int i = 0; i < threads.length; i++)
            threads[i] = new Consumer(db.getStream("source" + i));

        for (Thread thread1 : threads) {
            thread1.start();
        }

        for (Thread thread : threads)
            thread.join();

        db.close();
    }

    static class Consumer extends Thread {

        private final DXTickStream  stream;
        private final TickLoader[]  loaders = new TickLoader[4];
        private int                 msgsReceived = 0;

        public Consumer (DXTickStream source) {
            super ("Consumer for " + source.getKey());
            this.stream = source;

            StreamOptions options = stream.getStreamOptions();

            for (int i = 0; i < loaders.length; i++) {
                options.name = new GUID().toString();
                DXTickStream target = stream.getDB().createStream(options.name, options);
                loaders[i] = target.createLoader();
            }

        }

        public void             process (InstrumentMessage msg) throws InterruptedException {
//            int interval = (int) (Math.random() * 10000 + 1000);
//
//            if (msgsReceived % interval == 0)
//                Thread.sleep(1);

            for (int i = 0; i < loaders.length; i++)
                loaders[i].send(msg);
    }

//        public void             report () {
//            System.out.printf (
//                    "    %s: received %,d; max latency: %,d ms\n",
//                    getName (),
//                    msgsReceived,
//                    maxLatency
//            );
//
//            maxLatency = 0;
//        }

        @Override
        public void             run () {
            TickCursor      cur =
                    stream.select (0, new SelectionOptions (false, true));

            System.out.println (getName () + " IS READY ...");

            try {
                while (cur.next()) {
                    process (cur.getMessage());
                    msgsReceived++;

                    if (msgsReceived == TOTAL_MESSAGES)
                        break;
                }
            } catch (InterruptedException e) {
                // do nothing
            } finally {
                Util.close (cur);

                for (TickLoader loader : loaders)
                    Util.close(loader);
            }
        }
    }
}
