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
package com.epam.deltix.test.qsrv.hf.tickdb.tool;

import com.epam.deltix.qsrv.hf.pub.md.FloatDataType;
import com.epam.deltix.qsrv.hf.pub.md.RecordClassDescriptor;
import com.epam.deltix.qsrv.hf.tickdb.pub.DXTickDB;
import com.epam.deltix.qsrv.hf.tickdb.pub.DXTickStream;
import com.epam.deltix.qsrv.hf.tickdb.pub.LoadingOptions;
import com.epam.deltix.qsrv.hf.tickdb.StreamConfigurationHelper;
import com.epam.deltix.qsrv.hf.tickdb.pub.StreamOptions;
import com.epam.deltix.qsrv.hf.tickdb.pub.StreamScope;
import com.epam.deltix.qsrv.hf.tickdb.pub.TickDBFactory;
import com.epam.deltix.qsrv.hf.tickdb.pub.TickLoader;
import com.epam.deltix.qsrv.test.messages.AggressorSide;
import com.epam.deltix.qsrv.test.messages.MarketEventType;
import com.epam.deltix.qsrv.test.messages.TradeMessage;
import com.epam.deltix.util.cmdline.DefaultApplication;

import java.util.Random;
import java.util.concurrent.TimeUnit;

/**
 * @author Alexei Osipov
 */
public class GenerateRandomStreamWithSpaces extends DefaultApplication {

    public GenerateRandomStreamWithSpaces(String[] args) {
        super(args);
    }

    protected void increment(StringBuffer symbol, int index) {
        if (symbol.charAt(index) == (int) 'Z') {
            increment(symbol, index - 1);
            symbol.setCharAt(index, 'A');
        } else
            symbol.setCharAt(index, (char) (symbol.charAt(index) + 1));
    }

    @Override
    protected void run() throws Throwable {
        String tb = getArgValue("-db", "dxtick://localhost:8011");
        int size = getIntArgValue("-size", 100);
        int df = getIntArgValue("-df", 0);
        int symbols = getIntArgValue("-symbols", 1000);
        int total = getIntArgValue("-total", 10_000_000);
        int spaces = getIntArgValue("-spaces", 4);
        String streamKey = getArgValue("-stream");
        if (streamKey == null) {
            System.out.println("Set stream name using -stream");
            System.exit(-1);
        }

        System.out.println("Data generator: ");
        System.out.println(String.format("    timebase: %s", tb));
        System.out.println("    stream: " + streamKey);
        System.out.println("    message size: " + size + " bytes");
        System.out.println("    Distribution Factor: " + df);
        System.out.println("    number of symbols: " + symbols);
        System.out.println("    number of spaces: " + spaces);

        System.out.println(String.format("    number of messages (per space): %,d", +total));


        RecordClassDescriptor mcd = StreamConfigurationHelper.mkMarketMessageDescriptor(null, false);
        RecordClassDescriptor descriptor = StreamConfigurationHelper.mkTradeMessageDescriptor(
                mcd, null, null, FloatDataType.ENCODING_SCALE_AUTO, FloatDataType.ENCODING_SCALE_AUTO);

        StringBuffer ch = new StringBuffer("AAAAAAAA");
        String[] names = new String[symbols];
        for (int i = 0; i < names.length; i++) {
            names[i] = ch.toString();
            increment(ch, 4);
        }

        long t0 = System.currentTimeMillis();
        try (DXTickDB db = TickDBFactory.createFromUrl(tb)) {
            db.open(false);

            DXTickStream oldStream = db.getStream(streamKey);
            if (oldStream != null) {
                oldStream.delete();
            }

            StreamOptions options = StreamOptions.fixedType(StreamScope.DURABLE, streamKey, streamKey, df, descriptor);
            options.version = "5.0";
            DXTickStream stream = db.createStream(streamKey, options);

            Thread[] loaders = new Thread[spaces];


            long endTime = System.currentTimeMillis();
            long startTime = endTime - TimeUnit.DAYS.toMillis(1);

            for (int loaderIndex = 0; loaderIndex < spaces; loaderIndex++) {
                LoadingOptions loadingOptions = new LoadingOptions(false);
                String space = "space_" + loaderIndex;
                loadingOptions.space = space;
                TickLoader loader = stream.createLoader(loadingOptions);

                String[] namesForSpace = new String[symbols];
                for (int i = 0; i < symbols; i++)
                    namesForSpace[i] = (i + 1) + names[i];

                MessageGenerator generator = new MessageGenerator(loader, total, namesForSpace, startTime, endTime, space);
                Thread thread = new Thread(generator);
                loaders[loaderIndex] = thread;
                thread.start();

            }


            for (Thread loader : loaders) {
                loader.join();
            }

            //stream.delete();
        }
        long t1 = System.currentTimeMillis();
        double s = (t1 - t0) * 0.001;
        System.out.printf("Finished in %,.3fs\n", s);

    }


    private static class MessageGenerator implements Runnable {
        private final TickLoader loader;

        private final int total;
        private final String[] symbols;
        private final long startTime;
        private final long endTime;
        private final String space;

        final TradeMessage trade = new TradeMessage();

        final Random random = new Random();

        private MessageGenerator(TickLoader loader, int total, String[] symbols, long startTime, long endTime, String space) {
            this.loader = loader;
            this.total = total;
            this.symbols = symbols;
            this.startTime = startTime;
            this.endTime = endTime;
            this.space = space;
        }

        @Override
        public void run() {
            long timeIntervalMs = endTime - startTime;
            long endNanoTime = TimeUnit.MILLISECONDS.toNanos(endTime);
            int averageIntervalNanos = (int) (TimeUnit.MILLISECONDS.toNanos(timeIntervalMs) / total);

            long count = 0;

            long t0 = System.currentTimeMillis();

            long nextNanoTime = TimeUnit.MILLISECONDS.toNanos(startTime);
            while (count < total && nextNanoTime <= endNanoTime) {
//                trade.setSymbol(symbols[(int) (count % symbols.length)]);
                //trade.setNanoTime(System.nanoTime());
                //trade.timestamp = System.nanoTime();
//                trade.setNanoTime(nextNanoTime);
                fillTrade(count, nextNanoTime);
                loader.send(trade);

                nextNanoTime += random.nextInt(averageIntervalNanos);
                count++;
            }


            long t1 = System.currentTimeMillis();
            double s = (t1 - t0) * 0.001;
            System.out.printf(
                    "%s: Write %,d messages in %,.3fs; speed: %,.0f msg/s\n",
                    space,
                    count,
                    s,
                    count / s
            );

            loader.close();
        }

        private void fillTrade(long count, long nextNanoTime) {
            int index = (int) (count % symbols.length);
            trade.setSymbol(symbols[index]);
            trade.setNanoTime(nextNanoTime);

            trade.setPrice(random.nextDouble() * 1000);
            trade.setSize(random.nextDouble() * 10000);
            trade.setAggressorSide(AggressorSide.values()[random.nextInt(AggressorSide.values().length)]);
            trade.setEventType(MarketEventType.values()[random.nextInt(MarketEventType.values().length)]);
            trade.setCondition("SAMPLE");
            trade.setNetPriceChange(random.nextDouble() * 100);

            trade.setCurrencyCode((short) random.nextInt(1000));
            trade.setSourceId(random.nextLong());
        }
    }

    public static void main(String[] args) {
        new GenerateRandomStreamWithSpaces(args).start();
    }
}