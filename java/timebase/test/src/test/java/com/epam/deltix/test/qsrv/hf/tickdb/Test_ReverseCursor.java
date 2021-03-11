package com.epam.deltix.test.qsrv.hf.tickdb;

import com.epam.deltix.qsrv.hf.pub.md.*;
import com.epam.deltix.qsrv.hf.tickdb.StreamConfigurationHelper;
import com.epam.deltix.qsrv.hf.tickdb.TDBRunner;
import com.epam.deltix.qsrv.hf.tickdb.TickDBUtil;
import com.epam.deltix.qsrv.test.messages.BestBidOfferMessage;
import com.epam.deltix.qsrv.test.messages.MarketMessage;
import com.epam.deltix.timebase.messages.ConstantIdentityKey;
import com.epam.deltix.timebase.messages.IdentityKey;
import com.epam.deltix.timebase.messages.InstrumentMessage;

import org.junit.*;
import com.epam.deltix.qsrv.hf.tickdb.pub.*;
import com.epam.deltix.qsrv.hf.pub.*;
import com.epam.deltix.qsrv.hf.tickdb.ui.tbshell.TickDBShell;
import com.epam.deltix.util.io.Home;
import com.epam.deltix.util.io.IOUtil;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

import org.junit.experimental.categories.Category;
import com.epam.deltix.util.JUnitCategories.TickDBFast;

/**
 * User: BazylevD
 * Date: Feb 17, 2009
 * Time: 9:41:24 PM
 */
@Category(TickDBFast.class)
public class Test_ReverseCursor {

    private final static String LOCATION = TDBRunner.getTemporaryLocation();

    protected static int distribution_factor = 1;
    private static File DIR = Home.getFile("testdata", "qsrv", "hf", "tickdb");

    private final TickDBUtil util = new TickDBUtil();

    private DXTickDB db = null;


    private Callback callback;

    static RecordClassDescriptor     mkMarketMessageDescriptor ()
    {
        final String            name = MarketMessage.class.getName ();
        DataField[] fields = new DataField[2];

        fields[0] = new NonStaticDataField(
                "currencyCode", "Currency Code",
                new IntegerDataType (IntegerDataType.ENCODING_INT16, true), null);

        fields[1] = new StaticDataField (
                "sequenceNumber", "Sequence Number",
                new IntegerDataType (IntegerDataType.ENCODING_INT64, true), null);

        return (new RecordClassDescriptor (name, name, true, null, fields));
    }

    @BeforeClass
    public static void createDb() throws Throwable {
        createDb(LOCATION, "0");
    }

    @Before
    public void setUp() throws Throwable {
        callback = null;
        db = TickDBFactory.create(LOCATION);
        db.open(true);
    }

    private static void createDb(String path, String factor) throws Throwable {
        if (!Boolean.getBoolean("quiet"))
            System.out.println("distribution_factor=" + distribution_factor);
        
        DXTickDB db = TickDBFactory.create(path);
        db.format();
        DXTickStream trades = db.createStream("trade", "trade", null, distribution_factor);

        RecordClassDescriptor market = mkMarketMessageDescriptor();
        RecordClassDescriptor trade = StreamConfigurationHelper.mkTradeMessageDescriptor(market, null, null, FloatDataType.ENCODING_SCALE_AUTO, FloatDataType.ENCODING_SCALE_AUTO);
        trades.setFixedType(trade);
        TickDBShell.loadMessageFile(new File(DIR, "trade.qsmsg"), trades);

        DXTickStream poly = db.createStream("poli", "poli", null, distribution_factor);

        RecordClassDescriptor bbo = StreamConfigurationHelper.mkBBOMessageDescriptor(market, false, null, null, FloatDataType.ENCODING_SCALE_AUTO, FloatDataType.ENCODING_SCALE_AUTO);

        poly.setPolymorphic(trade, bbo);
        //StreamConfigurationHelper.setTradeOrBBO(poly, true, null, null);
        TickDBShell.loadMessageFile(new File(DIR, "poly.qsmsg"), poly);
        db.close();
    }

    @After
    public void cleanup() {
        if (db != null) {
            db.close();
        }
    }

/*    @Test
    public void loadMulti() throws IOException {
        DXTickDB db = TickDBFactory.create("C:\\temp\\tickdb.1.x");
        db.open(false);
        WritableTickStream stream = db.getStream("poli");

        MessageReader reader1 = new MessageReader("C:\\temp\\poly_tradeV2.qsmsg");
        MessageReader reader2 = new MessageReader("C:\\temp\\poly_bboV2.qsmsg");
        TickLoader writer = stream.createLoader(new LoadingOptions(false));

        int milli=0;
        while (reader1.next ()) {
            InstrumentMessage msg = reader1.getMessage();
            if (msg.timestamp % 10 == 3) {
                milli = 0;
                msg.timestamp -= 3;
            } else {
                milli++;
                msg.timestamp += milli - msg.timestamp % 10;
            }
            writer.send (msg);
            msg.instrumentType = InstrumentType.FUTURE;
            milli++;
            msg.timestamp++;
            writer.send (msg);
            if(reader2.next ()) {
                msg = reader2.getMessage ();

                milli++;
                msg.timestamp += milli - msg.timestamp % 10;

                writer.send (msg);
                msg.instrumentType = InstrumentType.FUTURE;

                milli++;
                msg.timestamp++;

                writer.send (msg);
            }
        }

        reader1.close();
        reader2.close();
        writer.close ();
        db.close();
    }
*/
    @Test
    public void testTimeInMiddle() throws IOException, InterruptedException {
        // 2009-01-01 17:00:00.000   1230829200000
        final long time = 1230829200009L;
        select(time, db.getStream("trade"), null, null, "timeInMiddle.txt");
    }

    @Test
    public void testTimeAfterEndtime() throws IOException, InterruptedException {
        // 2009-01-01 21:00:00.000   123084360000
        final long time = 1230843600000L;
        select(time, db.getStream("trade"), null, null, "timeAfterEndtime.txt");
    }

    @Test
    public void testTimeBeforeEndtime() throws IOException, InterruptedException {
        // 2008-12-01 00:00:00.000   1228089600000
        final long time = 1228089600000L;
        select(time, db.getStream("trade"), null, null, "timeBeforeEndtime.txt");
    }

    @Test
    public void testSymbolFiltering() throws IOException, InterruptedException {
        // 2009-01-01 17:00:00.000   1230829200000
        final long time = 1230829200009L;
        
        IdentityKey[] ids = new IdentityKey[] {
            new ConstantIdentityKey("MSFT")
        };
        select(time, db.getStream("trade"), null, ids, "MSFT.txt");

        ids = new IdentityKey[] {
            new ConstantIdentityKey("MSFT"),
            new ConstantIdentityKey("CL1")
        };
        select(time, db.getStream("trade"), null, ids, "MSFT_CL1.txt");
    }

    @Test
    public void testMsgTypeFiltering() throws IOException, InterruptedException {
        // 2009-01-01 17:00:00.000   1230829200000
        final long time = 1230829200015L;
        final TickStream stream = db.getStream("poli");

        IdentityKey[] ids = new IdentityKey[] {
            new ConstantIdentityKey("MSFT"),
            new ConstantIdentityKey("CL1")
        };

        select(time, stream, null, ids, "poli_2s.txt");

        select(time, stream, new String[] {BestBidOfferMessage.class.getName()}, ids, "bbo_2s.txt");
    }

    @Test
    public void testFilterChange() throws IOException, InterruptedException {
        // 2009-01-01 17:00:00.000   1230829200000
        final long time = 1230829200015L;
        final TickStream stream = db.getStream("poli");

        IdentityKey[] ids = new IdentityKey[] {
            new ConstantIdentityKey("MSFT"),
            new ConstantIdentityKey("CL1")
        };

        callback = new Callback() {
            private boolean done = false;

            public void callback(InstrumentMessage msg, TickCursor cursor) {
                // 2009-01-01 15:00:00.000 1230822000000
                if (!done && msg.getTimeStampMs() <= 1230822000000L) {
//                    FeedFilter filter = FeedFilter.createRestricted();
//                    filter.addSymbol("MSFT");
//                    filter.addSymbol("CL1");
//                    filter.addMessageType(MarketMessageType.TYPE_BBO);
//                    cursor.setFilter(filter);
                    cursor.setTypes(BestBidOfferMessage.class.getName());
                    done = true;
                }
            }
        };

        select(time, stream, null, ids, "filterChange.txt");
    }

    private void select(long time, TickStream stream, String[] types, IdentityKey[] filter, String etalonFile)
        throws IOException, InterruptedException
    {
        //stream = stream != null ? stream : db.getStream("trade");
        SelectionOptions options = new SelectionOptions(false, false, true);
        StringBuilder out = new StringBuilder();
        try (TickCursor cursor = stream.select(time, options, types, filter)) {
            printCursor(cursor, out);
        }

        //TickDBUtil.dump2File(TickDBUtil.USER_HOME + etalonFile, os.toByteArray());
        String[] lines = IOUtil.readLinesFromTextFile (new File (DIR, etalonFile));
        String etalon = String.join(System.lineSeparator(), lines);

        Assert.assertEquals("Data log is not the same as etalon", etalon, out.toString());
    }

//    //@Test
//    public void testForwardSelect() {
//        DXTickStream stream = db.listStreams()[0];
//
//        TickCursor cursor = TickCursorFactory.create(stream, 0);
//        printCursor(cursor, new PrintWriter (System.out, true));
//        cursor.close();
//    }

    private void printCursor(TickCursor cur, StringBuilder out) {
        if (cur == null) {
            System.out.println("NO DATA");
        } else {
            while (cur.next()) {
                InstrumentMessage msg = cur.getMessage();
                if (callback != null)
                    callback.callback(msg, cur);

                util.print(msg, out);
            }
        }
    }

    private interface Callback {
        void callback(InstrumentMessage msg, TickCursor cursor);
    }

}
