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
package com.epam.deltix.test.qsrv.hf.tickdb.tb5;

import com.epam.deltix.qsrv.dtb.store.pub.TSRoot;
import com.epam.deltix.qsrv.hf.tickdb.StreamConfigurationHelper;
import com.epam.deltix.qsrv.test.messages.TradeMessage;
import com.epam.deltix.timebase.messages.InstrumentMessage;

import com.epam.deltix.qsrv.hf.tickdb.TDBRunner;
import com.epam.deltix.qsrv.hf.tickdb.pub.*;
import com.epam.deltix.util.text.SimpleStringCodec;
import com.epam.deltix.util.time.GMT;
import org.junit.Test;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.GregorianCalendar;
import java.util.Random;

/**
 *
 */
public class Test_CompressionSmoke {

    private final static Random         rnd = new Random();

    private final static String         TB_LOCATION = TDBRunner.getTemporaryLocation();
    private final static String         STREAM_CONFIG_FILE = "/data/config.properties";
    private final static String         STREAM_NAME = "smoke";
    private final static int            numMsg = 10000000;
    //private TDBRunner                   runner;

    private final static long           initialTs = rnd.nextInt(10000000) * 1000;
    private final static int            tsInterval = 1;

    private final static double         initialPrice = rnd.nextDouble()*100;
    private final static double         priceInterval = rnd.nextDouble()*10;
    private final static double         priceMax = 10000;

    private final static double         initialSize = rnd.nextDouble()*1000;
    private final static double         sizeInterval = rnd.nextDouble()*10;
    private final static double         sizeMax = 10000;

    private final static short          currencyCode = (short) rnd.nextInt(500);

    public class TradesGenerator extends TDBRunner.BaseGenerator<InstrumentMessage> {
        private int count;

        private long curTs = initialTs;
        private double curPrice = initialPrice;
        private double curSize = initialSize;

        public TradesGenerator(GregorianCalendar calendar, int interval, int count, String ... symbols) {
            super(calendar, interval, symbols);
            this.count = count;
        }

        @Override
        public boolean next() {
            if (isAtEnd())
                return false;

            TradeMessage message = new TradeMessage();
            message.setSymbol(symbols.next());
            message.setTimeStampMs(curTs);

            message.setPrice(curPrice);
            message.setSize(curSize);
            message.setCurrencyCode(currencyCode);

            current = message;
            count--;

            curTs += tsInterval;
            curPrice += priceInterval;
            if (curPrice > priceMax)
                curPrice = 3.14;
            curSize += sizeInterval;
            if (curSize > sizeMax)
                curSize = 6.28;

            return true;
        }

        @Override
        public boolean isAtEnd() {
            return count == 0;
        }
    }

    @Test
    public void test() throws Throwable {
        TDBRunner runner = new TDBRunner(true, true, TB_LOCATION);
        runner.startup();
        runner.setDoFormat(false);

        createStream(runner, "default", null);
        writeMessages(runner, "default");
        readCheckMessages(runner, "default");

        createStream(runner, "lz4_stream", "lz4(5)");
        writeMessages(runner, "lz4_stream");
        readCheckMessages(runner, "lz4_stream");

        createStream(runner, "snappy_stream", "snappy");
        writeMessages(runner, "snappy_stream");
        readCheckMessages(runner, "snappy_stream");

        createStream(runner, "zlib_stream", "zlib");
        writeMessages(runner, "zlib_stream");
        readCheckMessages(runner, "zlib_stream");

        runner.shutdown();
    }

    private void writeMessages(TDBRunner runner, String name) throws Throwable {
        TradesGenerator tgen = new TradesGenerator(new GregorianCalendar(), 1, numMsg);
        TickLoader loader = runner.getTickDb().getStream(name).createLoader(new LoadingOptions(false));
        while (tgen.next()) {
            loader.send(tgen.getMessage());
        }
        loader.close();
    }

    private void readCheckMessages(TDBRunner runner, String name) throws Throwable {
        runner.shutdown();
        runner.startup();

        DXTickStream stream = runner.getTickDb().getStream(name);

        TickCursor cursor = stream.select(Long.MIN_VALUE, new SelectionOptions(false, false));
        TradesGenerator tgen = new TradesGenerator(new GregorianCalendar(), 1, numMsg);
        while (cursor.next() && tgen.next()) {
            TradeMessage streamMsg = (TradeMessage) cursor.getMessage();
            TradeMessage etalonMsg = (TradeMessage) tgen.getMessage();
            checkEquals(streamMsg, etalonMsg);
        }
        cursor.close();

        assert cursor.isAtEnd() : "Stream cursor is not at end!";
        assert tgen.isAtEnd() : "Generator is not at end!";
    }

    private void checkEquals(TradeMessage msg1, TradeMessage msg2) {
        assert msg1.getNanoTime() == msg2.getNanoTime() :
            GMT.formatNanos(msg1.getNanoTime()) + " != " + GMT.formatNanos(msg2.getNanoTime());

        assert Double.compare(msg1.getPrice(), msg2.getPrice()) == 0 :
            "price: " + msg1.getPrice() + " != " + msg2.getPrice();

        assert Double.compare(msg1.getSize(), msg2.getSize()) == 0 :
            "size: " + msg1.getPrice() + " != " + msg2.getPrice();

        assert msg1.getCurrencyCode() == msg2.getCurrencyCode() :
            "currency: " + msg1.getCurrencyCode() + " != " + msg2.getCurrencyCode();
    }

    private void createStream(TDBRunner runner, String name, String compression) throws Throwable {
        StreamOptions options = new StreamOptions(StreamScope.DURABLE, name, "", 1);
        options.setFixedType(StreamConfigurationHelper.mkUniversalTradeMessageDescriptor());
        runner.getTickDb().createStream(name, options);

        if (compression != null) {
            runner.shutdown();
            writeConfigFile(name, compression);
            runner.startup();
        }
    }

    private void writeConfigFile(String name, String compression) throws IOException {
        String encodedName = SimpleStringCodec.DEFAULT_INSTANCE.encode(name);
        File configFile = new File(TB_LOCATION + "/" + encodedName + STREAM_CONFIG_FILE);

        FileWriter fw = new FileWriter(configFile.getAbsoluteFile());
        fw.write("compression=" + compression + "\n");
        fw.write("maxFileSize=" + TSRoot.MAX_FILE_SIZE_DEF + "\n");
        fw.write("maxFolderSize=" + TSRoot.MAX_FOLDER_SIZE_DEF + "\n");
        fw.close();

        System.out.println("Config file '" + configFile.toString() + "' has been updated.");
    }

}