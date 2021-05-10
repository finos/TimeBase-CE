package com.epam.deltix.test.qsrv.hf.tickdb;

import com.epam.deltix.qsrv.hf.tickdb.TDBRunner;
import com.epam.deltix.qsrv.hf.tickdb.pub.*;
import com.epam.deltix.test.qsrv.hf.tickdb.server.ServerRunner;
import com.epam.deltix.timebase.messages.ConstantIdentityKey;
import com.epam.deltix.timebase.messages.IdentityKey;
import com.epam.deltix.timebase.messages.InstrumentMessage;

import com.epam.deltix.util.io.BasicIOUtil;
import com.epam.deltix.util.io.Home;
import com.epam.deltix.qsrv.hf.tickdb.util.ZIPUtil;
import com.epam.deltix.util.time.GMT;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.io.FileInputStream;
import java.text.ParseException;
import java.util.*;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.experimental.categories.Category;
import com.epam.deltix.util.JUnitCategories.TickDBFast;

@Category(TickDBFast.class)
public class Test_MessageOrder {

    private static File STREAM_FILE = Home.getFile("testdata//tickdb//misc//4.4//trade.zip");
    private static File STREAM_FILE1 = Home.getFile("testdata//tickdb//misc//4.4//daily.zip");
    private static File STREAM_FILE2 = Home.getFile("testdata//tickdb//misc//4.4//daily_bars.zip");
    private static File STREAM_FILE3 = Home.getFile("testdata//tickdb//misc//4.4//DailyBars.zip");
    private static ServerRunner runner;

    @BeforeClass
    public static void      start() throws Throwable {

        File folder = new File(TDBRunner.getTemporaryLocation());
        BasicIOUtil.deleteFileOrDir(folder);
        folder.mkdirs();

        FileInputStream is = new FileInputStream(STREAM_FILE);
        ZIPUtil.extractZipStream(is, folder);
        is.close();

        is = new FileInputStream(STREAM_FILE1);
        ZIPUtil.extractZipStream(is, folder);
        is.close();

        is = new FileInputStream(STREAM_FILE2);
        ZIPUtil.extractZipStream(is, folder);
        is.close();

        is = new FileInputStream(STREAM_FILE3);
        ZIPUtil.extractZipStream(is, folder);
        is.close();
        
        runner = new ServerRunner(true, false, folder.getAbsolutePath());
        runner.startup();
    }

    @AfterClass
    public static void      stop() throws Throwable {
        runner.shutdown();
        runner = null;
    }

    public static DXTickDB getTickDB() {
        return runner.getTickDb();
    }

    @Test
    public void testOrder() throws ParseException {
        DXTickStream stream = getTickDB().getStream("trade");
        
        TickCursor cursor1 = stream.createCursor(new SelectionOptions(false, false));
        TickCursor cursor2 = stream.createCursor(new SelectionOptions(false, false));

        ConstantIdentityKey[] keys = new ConstantIdentityKey[] {
                new ConstantIdentityKey("NQH9"),
                new ConstantIdentityKey("NQM9"),
                new ConstantIdentityKey("ESM9"),
                new ConstantIdentityKey("ESH9")
        };

        cursor1.addEntities(keys, 0, keys.length);
        cursor2.addEntities(keys, 0, keys.length);

        //long time = GMT.parseDateTime("2009-1-11 00:00:00").getTime();

        cursor1.reset(0);
        cursor2.reset(0);

        try {
            int count = 0;
            while (cursor1.next() && cursor2.next()) {
                count++;
                
//                TradeMessage msg = (TradeMessage) cursor1.getMessage();
//                System.out.format(
//                    "%s,%s,%.2f,%.0f   ",
//                    GMT.formatDateTimeMillis(new Date(msg.timestamp)),
//                    msg.symbol,
//                    msg.price,
//                    msg.size);
//
//                msg = (TradeMessage) cursor2.getMessage();
//                System.out.format(
//                    "%s,%s,%.2f,%.0f\n",
//                    GMT.formatDateTimeMillis(new Date(msg.timestamp)),
//                    msg.symbol,
//                    msg.price,
//                    msg.size);

                assertEquals(cursor1.getMessage().toString(), cursor2.getMessage().toString());

                if (count % 100 == 0) {
                    //cursor1.addTypes(TradeMessage.class.getName());
                    cursor1.addEntity(new ConstantIdentityKey("NQH9"));
                }
            }
        } finally {
            cursor1.close();
            cursor2.close();
        }
    }

    @Test
    public void     test() throws Exception {

        TickCursor cursor = getTickDB().getStream("Daily").createCursor(new SelectionOptions());
        cursor.addEntity(new ConstantIdentityKey("W-N59"));
        cursor.reset(Long.MIN_VALUE);

        while (cursor.next());

        cursor.removeEntity(new ConstantIdentityKey("W-N59"));
        //cursor.addEntities(new IdentityKey[0], 0, 0);

        IdentityKey[] keys = new IdentityKey[] {
                new ConstantIdentityKey("W-U59"),
                new ConstantIdentityKey("S-U59"),
                new ConstantIdentityKey("C-U59"),
                new ConstantIdentityKey("W-N59"),
        };

        long time = GMT.parseDateTime("1959-08-04 00:00:00").getTime();
        cursor.setTimeForNewSubscriptions(time);

        //cursor.removeEntities(new IdentityKey[0], 0, 0);
        cursor.addEntities(keys, 0, keys.length);

        cursor.next();

        cursor.removeEntity(new ConstantIdentityKey("W-N59"));

        cursor.next();

        time = GMT.parseDateTime("1959-09-03 00:00:00").getTime();
        cursor.setTimeForNewSubscriptions(time);
        cursor.addEntity(new ConstantIdentityKey("S-X59"));

        boolean contains = false;
        while (cursor.next()) {
            if (!contains)
                contains = "S-X59".equals(cursor.getMessage().getSymbol());
            
            //System.out.println(cursor.getMessage());
        }

        try {
            assertTrue("Not contains S-X59", contains);
        } finally {
            cursor.close();
        }
    }

    @Test
    public void     testDuplicates() throws Exception {

        TickCursor cursor = getTickDB().getStream("Daily").createCursor(new SelectionOptions());
        cursor.addEntity(new ConstantIdentityKey("W-N59"));
        cursor.reset(Long.MIN_VALUE);

        while (cursor.next());

        cursor.removeEntity(new ConstantIdentityKey("W-N59"));

        IdentityKey[] keys = new IdentityKey[] {
                new ConstantIdentityKey("W-U59"),
                new ConstantIdentityKey("S-U59"),
                new ConstantIdentityKey("C-U59"),
                new ConstantIdentityKey("S-X59"),
        };

        long time = GMT.parseDateTime("1959-08-04 00:00:00").getTime();
        cursor.setTimeForNewSubscriptions(time);

        cursor.addEntities(keys, 0, 1);
        cursor.next();
        cursor.addEntities(keys, 1, 1);
        cursor.next();
        cursor.addEntities(keys, 2, 1);
        cursor.next();
        cursor.addEntities(keys, 3, 1);
        cursor.next();

        cursor.removeEntity(new ConstantIdentityKey("S-X59"));
        time = GMT.parseDateTime("1959-09-03 00:00:00").getTime();
        cursor.next();
        cursor.setTimeForNewSubscriptions(time);
        cursor.addEntity(new ConstantIdentityKey("S-X59"));

        HashSet<String> messages = new HashSet<String>();

        try {
            while (cursor.next()) {
                String actual = cursor.getMessage().toString();
                if (!messages.contains(actual))
                    messages.add(actual);
                else  {
                    assertTrue("Got same message: " + actual, false);
                }
            }
        } finally {
            cursor.close();
        }
    }

    @Test
    public void testRestart() throws ParseException {
        
        IdentityKey[] keys = new IdentityKey[] {
                new ConstantIdentityKey("VZ UN Equity"),
                new ConstantIdentityKey("T UN Equity")
        };

        long time = GMT.parseDateTime("2000-01-01 00:00:00").getTime();
        TickCursor cursor1 = getTickDB().getStream("daily_bars").createCursor(new SelectionOptions());
        cursor1.addEntities(keys, 0, keys.length);
        cursor1.reset(time);

        ArrayList<String> list = new ArrayList<String>();
        
        while (cursor1.next())
            list.add(cursor1.getMessage().toString());
        cursor1.close();

        getTickDB().close();
        getTickDB().open(false);

        TickCursor cursor2 = getTickDB().getStream("daily_bars").createCursor(new SelectionOptions());
        cursor2.addEntities(keys, 0, keys.length);
        cursor2.reset(time);

        int index = 0;
        while (cursor2.next())
            assertEquals(list.get(index++), cursor2.getMessage().toString());
        cursor2.close();
    }

    @Test
    public void newTest() throws ParseException {

        IdentityKey[] keys = new IdentityKey[] {
                new ConstantIdentityKey("IMO CT Equity"),
                new ConstantIdentityKey("TLM CT Equity"),
                new ConstantIdentityKey("AGU CT Equity"),
                new ConstantIdentityKey("RCI/B CT Equity"),
                new ConstantIdentityKey("MRU/A CT Equity"),
        };

        IdentityKey[] remove = new IdentityKey[] {
                new ConstantIdentityKey("TLM CT Equity"),
                new ConstantIdentityKey("MRU/A CT Equity"),
        };

        
        DXTickDB tickDB = getTickDB();

        long time = 915667200000L; //GMT.parseDateTime("2000-01-01 00:00:00").getTime();
        TickCursor cursor = tickDB.getStream("Daily Bars").createCursor(new SelectionOptions());
        cursor.reset(time);
        cursor.setTimeForNewSubscriptions(time);
        cursor.addEntities(keys, 0, 3);

        while (cursor.getMessage() == null || cursor.getMessage().getTimeStampMs() <= 946771200000L) {
            cursor.next();
        }

        cursor.removeEntity(new ConstantIdentityKey("AGU CT Equity"));
        cursor.setTimeForNewSubscriptions(946771200000L);
        cursor.addEntities(keys, 2, 3);

        while (cursor.getMessage().getTimeStampMs() <= 959904000000L && cursor.next()) {
            //System.out.println(cursor.getMessage());
        }

        cursor.setTimeForNewSubscriptions(959904000000L);
        cursor.removeEntities(remove, 0, remove.length);
        cursor.addEntities(keys, 1, 1);

        HashSet<String> messages = new HashSet<String>();
        time = 959904000000L;
        while (cursor.next()) {
            InstrumentMessage msg = cursor.getMessage();
            assert msg.getTimeStampMs() >= time;
            time = cursor.getMessage().getTimeStampMs();

            assert !messages.contains(msg.toString());
            messages.add(msg.toString());
        }

        cursor.close();
    }

//    public void dump() {
//        TickDBClient client = new TickDBClient("pc4", 41243);
//        client.open(true);
//
//        DXTickStream stream = client.getStream("Daily");
//        long[] range = new long[] {Long.MIN_VALUE, Long.MAX_VALUE};
//
//        IdentityKey[] keys = new IdentityKey[] {
//                new ConstantIdentityKey("W-U59"),
//                new ConstantIdentityKey("S-U59"),
//                new ConstantIdentityKey("C-U59"),
//                new ConstantIdentityKey("W-N59"),
//                new ConstantIdentityKey("S-X59"),
//                new ConstantIdentityKey("C-Z59"),
//                new ConstantIdentityKey("SMZ59"),
//                new ConstantIdentityKey("W-Z59"),
//                new ConstantIdentityKey("S-F60"),
//                new ConstantIdentityKey("SMF60"),
//        };
//
//        dumpStream(stream, keys, range);
//
//        client.close();
//    }

    public void dumpStream(DXTickStream stream, IdentityKey[] ids, long[] range) {
        
        DXTickStream copy = getTickDB().createStream(stream.getKey(), stream.getStreamOptions());
        TickLoader loader = copy.createLoader(new LoadingOptions(true));

        TickCursor cursor = stream.createCursor(new SelectionOptions(true, false));
        cursor.addEntities(ids, 0, ids.length);
        cursor.reset(range[0]);

        try {
            long time = range[0];
            while (cursor.next() && time <= range[1]) {
                loader.send(cursor.getMessage());
                time = cursor.getMessage().getTimeStampMs();
            }
        } finally {
            loader.close();
            cursor.close();
        }
    }
}
