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



import com.epam.deltix.qsrv.hf.pub.md.FloatDataType;
import com.epam.deltix.qsrv.hf.tickdb.TDBRunner;
import com.epam.deltix.qsrv.hf.tickdb.pub.DXTickDB;
import com.epam.deltix.qsrv.hf.tickdb.pub.DXTickStream;
import com.epam.deltix.qsrv.hf.tickdb.pub.LoadingError;
import com.epam.deltix.qsrv.hf.tickdb.pub.LoadingErrorListener;
import com.epam.deltix.qsrv.hf.tickdb.pub.LoadingOptions;
import com.epam.deltix.qsrv.hf.tickdb.pub.SelectionOptions;
import com.epam.deltix.qsrv.hf.tickdb.StreamConfigurationHelper;
import com.epam.deltix.qsrv.hf.tickdb.pub.StreamOptions;
import com.epam.deltix.qsrv.hf.tickdb.pub.StreamScope;
import com.epam.deltix.qsrv.hf.tickdb.pub.TickCursor;
import com.epam.deltix.qsrv.hf.tickdb.pub.TickDBFactory;
import com.epam.deltix.qsrv.hf.tickdb.pub.TickLoader;
import com.epam.deltix.qsrv.test.messages.BarMessage;
import com.epam.deltix.test.qsrv.hf.tickdb.server.ServerRunner;
import com.epam.deltix.util.io.GUID;
import com.epam.deltix.util.lang.Util;
import com.epam.deltix.util.time.GMT;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Random;

/**
 * Adds a big amount of symbols to a durable stream.
 */
public class StressTest_MultipleSymbolAddition {

    private static TDBRunner runner;

    @BeforeClass
    public static void start() throws Throwable {
        System.setProperty(TickDBFactory.VERSION_PROPERTY, "4.3");

        runner = new ServerRunner(true, true, getTemporaryLocationAtCustomDir("E:\\dev\\timebase", "tickdb"));
        runner.startup();
    }

    public static String        getTemporaryLocationAtCustomDir(String basePath, String subpath) {
        File random = new File(basePath + File.separator + "temp" + File.separator + new GUID().toString() + File.separator + subpath);
        if (random.mkdirs())
            random.deleteOnExit();

        return random.getAbsolutePath();
    }

    @AfterClass
    public static void stop() throws Throwable {
        runner.shutdown();
        runner = null;
    }

    @Test
    public void testAddManySymbols() throws InterruptedException {
        String name = "testAddManySymbols";

        DXTickDB db = runner.getServerDb();

        final DXTickStream stream = db.createStream(name,
                StreamOptions.fixedType(StreamScope.DURABLE, name, name, 0,
                        StreamConfigurationHelper.mkBarMessageDescriptor(null, null, null,
                                FloatDataType.ENCODING_SCALE_AUTO,
                                FloatDataType.ENCODING_SCALE_AUTO)));

        LoadingOptions options = new LoadingOptions();

        final int total = 10_000_000;
        GregorianCalendar calendar = new GregorianCalendar(2008, 1, 1);

        //final long time = new GregorianCalendar(2008, 1, 1, 12, 0, 0).getTimeInMillis();
        final long lastTime = calendar.getTimeInMillis() + BarMessage.BAR_MINUTE * total;

        final Thread consumer = new Thread("Consumer") {
            @Override
            public void run() {
                final TickCursor live = stream.select(Long.MIN_VALUE, new SelectionOptions(false, true));

                int count = 0;
                long time = 0;
                System.out.println("Last time: " + GMT.formatDateTimeMillis(lastTime));

                while (time < lastTime && live.next()) {
                    time = live.getMessage().getTimeStampMs();
                    //System.out.println(live.getMessage());
                    count++;
                    if (count % 10000 == 0) {
                        System.out.println("read " + count + " message:" + live.getMessage());
                        System.out.println(GMT.formatDateTimeMillis(stream.getTimeRange(live.getMessage())[1]));
                    }

//                    if (count++ == 30000)
//                       live.reset(Long.MIN_VALUE);
                }

                live.close();

                System.out.println("Consumer finished having recieved " + count + " messages.");
            }
        };

        consumer.start();

        TickLoader loader = stream.createLoader (options);

        final int[] errors = new int[] {0};
        loader.addEventListener(new LoadingErrorListener() {

            public void onError(LoadingError e) {
                errors[0]++;
                //e.printStackTrace(System.out);
            }
        });

        try {
            Random rnd = new Random();
            int count = 0;

            while (count < total) {
                BarMessage message = new BarMessage();
                message.setSymbol("ES" + (count));
                calendar.add(Calendar.MINUTE, 1);
                message.setTimeStampMs(calendar.getTimeInMillis());

                message.setHigh(rnd.nextDouble()*100);
                message.setOpen(message.getHigh() - rnd.nextDouble()*10);
                message.setClose(message.getHigh() - rnd.nextDouble()*10);
                message.setLow(Math.min(message.getOpen(), message.getClose()) - rnd.nextDouble()*10);
                message.setVolume(rnd.nextInt(10000));
                message.setCurrencyCode((short)840);
                loader.send(message);

                message.setSymbol("CS" + (count % 3));
                loader.send(message);
                Thread.sleep(5);

//                if (count == 10000) {
//                    stream.truncate(time);
//                }


                count++;
            }

            System.out.println("Send " + count + " messages.");
        } finally {
            Util.close(loader);
        }

        //consumer.start();
        consumer.join();
        //live.close();


        stream.delete();
    }


}
