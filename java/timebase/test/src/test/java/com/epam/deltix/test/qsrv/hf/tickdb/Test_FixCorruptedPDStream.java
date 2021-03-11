package com.epam.deltix.test.qsrv.hf.tickdb;

import com.epam.deltix.qsrv.hf.pub.RawMessage;
import com.epam.deltix.qsrv.hf.tickdb.StreamConfigurationHelper;
import com.epam.deltix.qsrv.hf.tickdb.TDBRunner;
import com.epam.deltix.qsrv.hf.tickdb.comm.server.TomcatServer;
import com.epam.deltix.qsrv.hf.tickdb.pub.*;
import com.epam.deltix.qsrv.test.messages.TradeMessage;
import com.epam.deltix.util.io.Home;
import com.epam.deltix.util.io.IOUtil;
import com.epam.deltix.util.text.SimpleStringCodec;
import com.epam.deltix.util.vfs.ZipFileSystem;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.List;

import org.junit.experimental.categories.Category;
import com.epam.deltix.util.JUnitCategories.TickDBFast;

@Category(TickDBFast.class)
public class Test_FixCorruptedPDStream {

    private static String           TB_LOCATION_GEN = Home.getPath("temp/PDStreamCorruptedGen/timebase");
    private static String           TB_LOCATION = Home.getPath("temp/PDStreamCorrupted/timebase");
    private static String           CORRUPTED_STREAMS_ZIP = Home.getPath("testdata/tickdb/tb5streams/corrupted");

    private static final String     STREAM_NAME = "testdata";
    private static final int        NUM_MESSAGES = 13000;

    @BeforeClass
    public static void start() throws Throwable {
    }

    @AfterClass
    public static void end() throws Exception {
    }


    public void testPDStreamGenerate() throws Throwable {
        IOUtil.removeRecursive(new File(TB_LOCATION_GEN));
        TDBRunner runner = new TDBRunner(false, true, TB_LOCATION_GEN, new TomcatServer());
        runner.startup();
        DXTickDB db = runner.getTickDb();

        StreamOptions options = new StreamOptions(StreamScope.DURABLE, STREAM_NAME, STREAM_NAME, 0);
        options.setFixedType(StreamConfigurationHelper.mkUniversalTradeMessageDescriptor());
        options.location = "";
        DXTickStream stream = db.createStream(STREAM_NAME, options);

        TickLoader loader = stream.createLoader(new LoadingOptions(false));

        TradeMessage msg = new TradeMessage();
        msg.setSymbol("AAA");
        msg.setTimeStampMs(1000);

        for (int i = 0; i < NUM_MESSAGES; ++i) {
            msg.setTimeStampMs(msg.getTimeStampMs() + 1);
            loader.send(msg);
        }

        loader.close();

        runner.shutdown();
    }

    @Test
    public void testPDStreamRestoreCorrupted() throws Throwable {
        File tbLocationDir = new File(TB_LOCATION);
        IOUtil.removeRecursive(tbLocationDir);
        tbLocationDir.mkdirs();

        File[] streamZipFiles = new File(CORRUPTED_STREAMS_ZIP).listFiles();
        String[] streamKeys = new String[streamZipFiles.length];
        for (int i = 0; i < streamZipFiles.length; ++i) {
            File zipFile = streamZipFiles[i];
            ZipFileSystem.unzip(new FileInputStream(zipFile), tbLocationDir);

            String name = zipFile.getName();
            streamKeys[i] = name.substring(0, name.lastIndexOf(".zip"));
            //streamKeys[i] = SimpleStringCodec.DEFAULT_INSTANCE.decode(name);
        }

        TDBRunner runner = new TDBRunner(false, false, TB_LOCATION, new TomcatServer());
        runner.startup();

        DXTickDB db = runner.getTickDb();

        List<DXTickStream> streams = new ArrayList<DXTickStream>();
        for (String streamKey : streamKeys) {
            DXTickStream stream = db.getStream(streamKey);
            if (stream == null)
                stream = db.getStream(SimpleStringCodec.DEFAULT_INSTANCE.decode(streamKey));

            streams.add(stream);

            TickCursor cursor = stream.select(Long.MIN_VALUE, new SelectionOptions(true, false));

            int msgCount = 0;
            while (cursor.next()) {
                RawMessage msg = (RawMessage) cursor.getMessage();
                ++msgCount;
            }

            System.out.println("Read " + streamKey + " stream: " + msgCount + " messages");
        }

        runner.shutdown();
    }

}