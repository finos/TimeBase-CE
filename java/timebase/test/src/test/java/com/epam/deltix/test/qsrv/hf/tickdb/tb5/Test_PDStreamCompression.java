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
package com.epam.deltix.test.qsrv.hf.tickdb.tb5;

import com.epam.deltix.data.stream.ConsumableMessageSource;
import com.epam.deltix.timebase.messages.InstrumentMessage;
import com.epam.deltix.qsrv.hf.pub.RawMessage;
import com.epam.deltix.qsrv.hf.stream.MessageFileHeader;
import com.epam.deltix.qsrv.hf.stream.Protocol;
import com.epam.deltix.qsrv.hf.tickdb.TDBRunner;
import com.epam.deltix.qsrv.hf.tickdb.comm.server.TomcatServer;
import com.epam.deltix.qsrv.hf.tickdb.pub.*;
import com.epam.deltix.util.io.Home;
import com.epam.deltix.util.io.IOUtil;
import com.epam.deltix.util.text.SimpleStringCodec;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 *
 */
public class Test_PDStreamCompression {
    private final static String             TB_LOCATION = Home.getPath("temp/tbcompression");
    private final static String             STREAM_CONFIG_FILE = "/data/config.properties";
    private final static String             STAT_FILE_NAME = "stat.csv";

    private final static String             TB_BARS_STREAM_LOCATION = Home.getPath("testdata/tickdb/misc/1minuteBarsEquities.qsmsg.gz");
    private final static String             TB_DE_STREAM_LOCATION = Home.getPath("testdata/tickdb/misc/equityFutureTrade.qsmsg.gz");
    private final static String             TB_MF_STREAM_LOCATION = Home.getPath("testdata/tickdb/misc/mfdata-sample.qsmsg.gz");

    private final static long               initialTs = 1356998400000L;
    private final static int                numMsg = 10000000;

    private final static String[]           DAT_FILES = new String[] {
        TB_BARS_STREAM_LOCATION,
        TB_DE_STREAM_LOCATION,
        TB_MF_STREAM_LOCATION
    };

    private final static String[]           COMPRESSIONS = new String[] {
        "",
        "Zlib",
        "Zlib(2)",
        "Zlib(5)",
        "Zlib(7)",
        "Zlib(9)",
        "LZ4",
        "LZ4(2)",
        "LZ4(5)",
        "LZ4(10)",
        "LZ4(15)",
        "LZ4(17)",
        "Snappy",
    };

    private TDBRunner runner;
    private String                          tbLocation;
    private Statistics                      curStatistics;
    private List<Statistics>                statisticses;

    public static class Statistics {
        public String       name;
        public long         msgCount;
        public long         writeMillis;
        public long         readMillis;
        public long         dataSize;
        public String       compression;
        public long         maxFileSize = 10485760;
        public long         maxFolderSize = 100;

        public static String        header() {
            return "Name, Msg Count, Data Size (KB), Write Ms, Read Ms, Write Data Rate (KB/s), Read Data Rate (KB/s), Compression Level";
        }

        @Override
        public String               toString() {
            return String.format("%s, %d, %d, %d, %d, %.3f, %.3f, %s",
                name, msgCount, dataSize / 1024, writeMillis, readMillis,
                (double) dataSize / writeMillis, (double) dataSize / readMillis, compression);
        }
    }

    @BeforeClass
    public static void start() throws Throwable {

    }

    @AfterClass
    public static void end() throws Exception {

    }

    @Test
    public void test() throws Throwable {
        runTest(DAT_FILES, COMPRESSIONS, TB_LOCATION, numMsg);
    }

    public void runTest(String[] datFiles, String[] compressions,
                        String tbLocation, int maxNumMsg) throws Throwable {

        this.tbLocation = tbLocation;
        System.out.println("Current Timebase location '" + tbLocation + "' will be removed");
        IOUtil.removeRecursive(new File(tbLocation));
        runner = new TDBRunner(false, true, tbLocation, new TomcatServer());
        runner.startup();

        statisticses = new ArrayList<>();

        long curOrigSize = 1;

        for (int i = 0; i < datFiles.length; ++i) {
            for (int j = 0; j < compressions.length; ++j) {
                File datFile = new File(datFiles[i]);

                curStatistics = new Statistics();
                curStatistics.name = datFile.getName() + "__" + compressions[j];
                curStatistics.compression = compressions[j];

                writePDStreamFromDat(datFile, maxNumMsg);
                readPDStream();

                statisticses.add(curStatistics);

                if (j == 0)
                    curOrigSize = curStatistics.dataSize;

                System.out.println(
                    String.format(
                        "Stream: %s, Compression: %s, Size: %d, ratio: %.3f, WriteTime (Ms): %d, ReadTime: %d, ",
                        curStatistics.name, curStatistics.compression,
                        curStatistics.dataSize, ((double)curStatistics.dataSize / (double) curOrigSize) * 100,
                        curStatistics.writeMillis, curStatistics.readMillis));
            }
        }

        dumpStatistics(tbLocation);

        runner.shutdown();
    }

    private void readPDStream() {
        DXTickStream stream = runner.getTickDb().getStream(curStatistics.name);

        TickCursor cursor = stream.select(Long.MIN_VALUE, new SelectionOptions(true, false));

        long startTime = System.currentTimeMillis();

        int msgCount = 0;
        while (cursor.next()) {
            RawMessage msg = (RawMessage) cursor.getMessage();
            ++msgCount;
        }
        cursor.close();

        assert msgCount == curStatistics.msgCount;
        curStatistics.readMillis = System.currentTimeMillis() - startTime;
    }

    private void writePDStreamFromDat(File datFile, int maxMsgNum) throws Throwable {
        DXTickStream stream = createStream(Protocol.readHeader(datFile));

        ConsumableMessageSource<InstrumentMessage> consumer = Protocol.openRawReader(datFile);
        List<RawMessage> messages = new ArrayList<>();

        while (consumer.next())
            messages.add((RawMessage) consumer.getMessage().clone());

        long startTime = System.currentTimeMillis();
        long ts = initialTs;
        TickLoader loader = stream.createLoader(new LoadingOptions(true));
        for (int i = 0; i < numMsg; ++i) {
            RawMessage msg = messages.get(i % messages.size());
            msg.setTimeStampMs(ts++);
            loader.send(msg);
        }
        loader.close();
        curStatistics.writeMillis = (System.currentTimeMillis() - startTime);

        curStatistics.msgCount = numMsg;

        String name = SimpleStringCodec.DEFAULT_INSTANCE.encode(curStatistics.name);
        curStatistics.dataSize = folderSize(new File(tbLocation + "/" + name));
    }

    private DXTickStream createStream(MessageFileHeader header) throws Throwable {
        StreamOptions options = new StreamOptions();
        options.name = curStatistics.name;
        if (header.getTypes().length > 1)
            options.setPolymorphic(header.getTypes());
        else
            options.setFixedType(header.getTypes()[0]);

        runner.getTickDb().createStream(curStatistics.name, options);
        runner.shutdown();

        writeConfigFile(curStatistics);

        runner.setDoFormat(false);
        runner.startup();

        return runner.getTickDb().getStream(curStatistics.name);
    }

    private void writeConfigFile(Statistics statistics) throws IOException {
        String name = SimpleStringCodec.DEFAULT_INSTANCE.encode(statistics.name);
        File configFile = new File(tbLocation + "/" + name + STREAM_CONFIG_FILE);

        FileWriter fw = new FileWriter(configFile.getAbsoluteFile());
        fw.write("compression=" + statistics.compression + "\n");
        fw.write("maxFileSize=" + statistics.maxFileSize + "\n");
        fw.write("maxFolderSize=" + statistics.maxFolderSize + "\n");
        fw.close();

        System.out.println("Config file '" + configFile.toString() + "' has been updated.");
    }

    private void dumpStatistics(String path) throws IOException {
        File file = new File(path + "/" + STAT_FILE_NAME);
        FileWriter fw = new FileWriter(file.getAbsoluteFile());

        fw.write(Statistics.header() + "\n");
        for (Statistics statistics : statisticses) {
            fw.write(statistics.toString() + "\n");
        }
        fw.close();

        System.out.println("Statistics has been saved to file '" + file.toString() + "'");
    }

    public long folderSize(File directory) {
        long length = 0;
        for (File file : directory.listFiles()) {
            if (file.isFile())
                length += file.length();
            else
                length += folderSize(file);
        }
        return length;
    }

}
