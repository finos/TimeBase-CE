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
import com.epam.deltix.qsrv.hf.tickdb.TickDBUtil;
import com.epam.deltix.qsrv.test.messages.BarMessage;
import com.epam.deltix.streaming.MessageChannel;
import com.epam.deltix.qsrv.hf.tickdb.pub.query.SubscriptionChangeListener;
import com.epam.deltix.timebase.messages.InstrumentMessage;

import org.junit.Test;
import org.junit.Assert;
import org.junit.Before;
import org.junit.After;
import static org.junit.Assert.assertEquals;
import com.epam.deltix.qsrv.hf.tickdb.pub.*;
import com.epam.deltix.qsrv.hf.pub.md.RecordClassDescriptor;
import com.epam.deltix.qsrv.hf.pub.*;
import com.epam.deltix.util.csvx.CSVXReader;
import com.epam.deltix.util.lang.Util;
import com.epam.deltix.util.io.Home;
import com.epam.deltix.util.io.IOUtil;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.File;
import java.io.Reader;
import java.text.SimpleDateFormat;
import java.text.ParseException;
import java.util.*;

import org.junit.experimental.categories.Category;
import com.epam.deltix.util.JUnitCategories.TickDBFast;

/**
 * User: BazylevD
 * Date: Apr 15, 2009
 * Time: 7:23:25 PM
 */
@Category(TickDBFast.class)
public class Test_TickDBLoader {
    private static final SimpleDateFormat DF = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss.S");
    private static final SimpleDateFormat DF2 = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.S");

    private final TickDBUtil util = new TickDBUtil();
    protected DXTickDB db = null;
    private final List<String> errors = new ArrayList<String>();

    private static final String ABORT_ETALON = "com.epam.deltix.qsrv.hf.tickdb.pub.OutOfSequenceMessageException: [S1] Message EURCHF:FX 2008-11-12 01:59:59.999 GMT is out of sequence (< 2008-11-12 02:00:00.000)";

    private static final String[] WHOLE_CHAIN_MESSAGES_ETALON = {        
        "com.epam.deltix.qsrv.hf.tickdb.pub.BumpUpMessageException: [S1] Message GBPUSD:FX 2008-11-12 01:58:01.000 GMT is bumped up to 2008-11-12 01:58:02.0 GMT",
        "com.epam.deltix.qsrv.hf.tickdb.pub.SkipMessageException: [S1] Message USDCHF:FX 2008-11-12 01:56:01.000 GMT is skipped"
    };

    private static final String[] WHOLE_CHAIN_SYMBOL_LEVEL_MESSAGES_ETALON = {
        "com.epam.deltix.qsrv.hf.tickdb.pub.BumpUpMessageException: [S1] Message USDJPY:FX 2008-11-12 01:58:02.0 GMT is bumped up to 2008-11-12 02:00:00.0 GMT",
        "com.epam.deltix.qsrv.hf.tickdb.pub.BumpUpMessageException: [S1] Message USDJPY:FX 2008-11-12 01:58:01.0 GMT is bumped up to 2008-11-12 02:00:00.0 GMT",
        "com.epam.deltix.qsrv.hf.tickdb.pub.BumpUpMessageException: [S1] Message EURJPY:FX 2008-11-12 01:58:02.0 GMT is bumped up to 2008-11-12 02:00:00.0 GMT",
        "com.epam.deltix.qsrv.hf.tickdb.pub.BumpUpMessageException: [S1] Message EURJPY:FX 2008-11-12 01:58:01.0 GMT is bumped up to 2008-11-12 02:00:00.0 GMT",
        "com.epam.deltix.qsrv.hf.tickdb.pub.SkipMessageException: [S1] Message EURJPY:FX 2008-11-12 01:56:01.0 GMT is skipped",
        "com.epam.deltix.qsrv.hf.tickdb.pub.SkipMessageException: [S1] Message USDCHF:FX 2008-11-12 01:56:01.0 GMT is skipped"
    };

    private static final String[] WHOLE_CHAIN_ONE_SYMBOL_MESSAGES_ETALON = {
        "com.epam.deltix.qsrv.hf.tickdb.pub.BumpUpMessageException: [S1] Message AUDUSD:FX 2008-11-12 01:58:02.0 GMT is bumped up to 2008-11-12 02:00:00.2 GMT",
        "com.epam.deltix.qsrv.hf.tickdb.pub.BumpUpMessageException: [S1] Message AUDUSD:FX 2008-11-12 01:58:01.0 GMT is bumped up to 2008-11-12 02:00:00.2 GMT",
        "com.epam.deltix.qsrv.hf.tickdb.pub.SkipMessageException: [S1] Message AUDUSD:FX 2008-11-12 01:56:01.0 GMT is skipped"
    };

    private static final String[] WHOLE_CHAIN_ABORT_MESSAGES_ETALON = {
        WHOLE_CHAIN_MESSAGES_ETALON[0],
        WHOLE_CHAIN_MESSAGES_ETALON[1],
        //WHOLE_CHAIN_MESSAGES_ETALON[2],
        "com.epam.deltix.qsrv.hf.tickdb.pub.OutOfSequenceMessageException: [S1] Message EURCHF:FX 2008-11-12 04:50:00.0 GMT is out of sequence (< 2008-11-12 05:00:00.0)"
    };

    static {
        DF.setTimeZone(TimeZone.getTimeZone("GMT"));
        DF2.setTimeZone(TimeZone.getTimeZone("GMT"));
    }

    @Before
    public void         setUp () {
        db = TickDBFactory.create(TDBRunner.getTemporaryLocation());
        db.format ();
    }

    @After
    public void         tearDown () {
        db.close();
    }

    private DXTickStream        createStream(int df) {
        DXTickStream s1 = db.createStream ("S1", null, null, df);
        final RecordClassDescriptor cd = StreamConfigurationHelper.mkUniversalBarMessageDescriptor();
        s1.setFixedType (cd);
        
        return s1;
    }

    @Test
    public void testOk() throws IOException, ParseException {
        createAndLoad(new LoadingOptions(false), 1, "loader.ok.csv");
        assertEquals(0, errors.size());
    }

    @Test
    public void testNullOptions() throws IOException, ParseException {
        createAndLoad(null, 1, "loader.ok.csv");
        assertEquals(0, errors.size());
    }

    @Test
    public void testAbort() throws IOException, ParseException {
        LoadingOptions options = new LoadingOptions(false);
        options.addErrorAction(LoadingError.class, LoadingOptions.ErrorAction.NotifyAndContinue);
        createAndLoad(options, 1, "loader.abort.csv");

        if (System.getProperty(TickDBFactory.VERSION_PROPERTY).startsWith("5")) {
            // 5.0 Does not support error reporting. TODO: Confirm
            assertEquals(0, errors.size());
        } else {
            // 4.3 Specific
            assertEquals(1, errors.size());
            assertEquals(ABORT_ETALON, errors.get(0));
        }
    }

    //@Test TODO: rewrite test for single entity
    public void testWholeChain() throws IOException, ParseException, InterruptedException {
        // 1min, 2min, 4min

        DXTickStream s1 = createStream(1);
        LoadingOptions options = new LoadingOptions(false); //, 60000, 120000, 240000);
        options.addErrorAction(LoadingError.class, LoadingOptions.ErrorAction.NotifyAndContinue);

        final TickLoader loader = s1.createLoader (options);
        loader.addEventListener(new LoadingErrorListenerImpl());
//        MessageChannel<InstrumentMessage> channel = FiltersConfigurator.newBumpUpSorter(
//                FiltersConfigurator.newSkipSorter(loader, 240000), 120000);
//        channel = FiltersConfigurator.newBufferedSorter(channel, 60000);

        MessageChannel<InstrumentMessage> channel =
                FiltersConfigurator.create(loader, 60000, 120000, 240000);

        try {
            loadCSVFile("loader.chain.csv", channel);
        } finally {
            channel.close();
        }

        Assert.assertArrayEquals(WHOLE_CHAIN_MESSAGES_ETALON, errors.toArray());

        TickCursor cursor = TickCursorFactory.create(db.listStreams()[0], 0);
        final String output = util.toString(cursor);
        Util.close(cursor);

        //final String output = util.select(0, db.listStreams()[0], null, false);
        //TickDBUtil.dump2File(TickDBUtil.USER_HOME + "loader.chain.txt", output);
        final String etalon = readFromResource("loader.chain.txt");
        Assert.assertEquals("Data log is not the same as etalon", etalon, output);
    }

    private String readFromResource(String name) throws IOException, InterruptedException {
        try (Reader rd = IOUtil.openResourceAsReader (getClass().getPackageName() + "/" + name)) {
            return IOUtil.readFromReader(rd);
        }
    }

    @Test
    public void testWholeChainMax() throws IOException, ParseException, InterruptedException {
        // 1min, 2min, 4min
        DXTickStream s1 = createStream(0);
        LoadingOptions options = new LoadingOptions(false); //, 60000, 120000, 240000);
        options.addErrorAction(LoadingError.class, LoadingOptions.ErrorAction.NotifyAndContinue);

        final TickLoader loader = s1.createLoader (options);
        loader.addEventListener(new LoadingErrorListenerImpl());
//        MessageChannel<InstrumentMessage> channel = FiltersConfigurator.newBumpUpSorter(
//                FiltersConfigurator.newSkipSorter(loader, 240000), 120000);
//        channel = FiltersConfigurator.newBufferedSorter(channel, 60000);

        MessageChannel<InstrumentMessage> channel =
                FiltersConfigurator.create(loader, 60000, 120000, 240000);

        try {
            loadCSVFile("loader.chain.csv", channel);
        } finally {
            channel.close();
        }
        Assert.assertEquals(0, errors.size());

//        final String output = util.select(0, db.listStreams()[0], null, false);
//        final String etalon = IOUtil.readTextFile (new File (DIR, "loader.chain.txt"));
//        Assert.assertEquals("Data log is not the same as etalon", etalon, output);
    }
    
    public void testWholeChainSymbolLevel() throws IOException, ParseException {
        // 1min, 2min, 4min
        // 1min, 2min, 4min
        DXTickStream s1 = createStream(0);
        LoadingOptions options = new LoadingOptions(false); //, 60000, 120000, 240000);
        options.addErrorAction(LoadingError.class, LoadingOptions.ErrorAction.NotifyAndContinue);

        final TickLoader loader = s1.createLoader (options);
        loader.addEventListener(new LoadingErrorListenerImpl());
        MessageChannel<InstrumentMessage> channel =
                FiltersConfigurator.create(loader, 60000, 120000, 240000);

        try {
            loadCSVFile("loader.chain_BySym.csv", channel);
        } finally {
            channel.close();
        }
       
        Assert.assertArrayEquals(WHOLE_CHAIN_SYMBOL_LEVEL_MESSAGES_ETALON, errors.toArray());

        InstrumentMessage[] actual = select2Array(0, db.listStreams()[0], false);
        compare("loader.chain_BySym.txt", actual);
    }

    //@Test FIXME: enable after supporting loading filters
//    public void testWholeChain1S() throws IOException, ParseException, InterruptedException {
//        // 1min, 2min, 4min
//        LoadingOptions options = new LoadingOptions(false, 60000, 120000, 240000);
//        options.addErrorAction(LoadingError.class, LoadingOptions.ErrorAction.NotifyAndContinue);
//        createAndLoad(options, 1, "loader.chain_1s.csv");
//        Assert.assertArrayEquals(WHOLE_CHAIN_ONE_SYMBOL_MESSAGES_ETALON, errors.toArray());
//
//        final String output = util.select(0, db.listStreams()[0], null, false);
//        //TickDBUtil.dump2File(TickDBUtil.USER_HOME + "loader.chain_1s.txt", output);
//        final String etalon = IOUtil.readTextFile (new File (DIR, "loader.chain_1s.txt"));
//        Assert.assertEquals("Data log is not the same as etalon", etalon, output);
//    }

    //@Test FIXME: enable after supporting loading filters
//    public void testWholeChainAbort() throws IOException, ParseException {
//        // 1min, 2min, 4min
//        LoadingOptions options = new LoadingOptions(false, 60000, 120000, 240000);
//        options.addErrorAction(LoadingError.class, LoadingOptions.ErrorAction.NotifyAndContinue);
//        createAndLoad(options, 1, "loader.chain_abort.csv");
//        Assert.assertArrayEquals(WHOLE_CHAIN_ABORT_MESSAGES_ETALON, errors.toArray());
//    }

    private void createAndLoad(LoadingOptions options, int distributionFactor, String fileName) throws IOException, ParseException {
        DXTickStream s1 = db.createStream ("S1", null, null, distributionFactor);
        final RecordClassDescriptor cd = StreamConfigurationHelper.mkUniversalBarMessageDescriptor();
        s1.setFixedType (cd);

        final TickLoader loader = s1.createLoader (options);
        loader.addEventListener(new LoadingErrorListenerImpl());
        try {
            loadCSVFile(fileName, loader);
        } finally {
            loader.close();
        }
    }

    private static void loadCSVFile(String fileName, MessageChannel<InstrumentMessage> loader)
            throws IOException, ParseException
    {
        CSVXReader csv = CSVXReader.openResource(Test_TickDBLoader.class, fileName);
        csv.nextLine(); // headers

        final BarMessage bar = new BarMessage();
        while (csv.nextLine()) {
            int idx = 0;
            bar.setSymbol(csv.getString(idx++, true));
            bar.setTimeStampMs(DF.parse(csv.getString(idx++, true)).getTime());
            bar.setExchangeId(ExchangeCodec.codeToLong(csv.getString(idx++)));
            bar.setCurrencyCode((short) csv.getInt(idx++));
            bar.setOpen(csv.getFloat(idx++));
            bar.setHigh(csv.getFloat(idx++));
            bar.setLow(csv.getFloat(idx++));
            bar.setClose(csv.getFloat(idx++));
            idx++; //bar.barSize = csv.getInt(idx++);
            bar.setVolume(csv.getFloat(idx));
            loader.send(bar);
        }
        csv.close();
    }

    // BarMessage,FX,NZDUSD,2008-11-12 01:59:01.000,1226455141000,0,999,0.57,0.57,0.57,0.57,77.00
    private static void loadEtalonFile(String fileName, TickLoader loader) throws IOException, ParseException {
        CSVXReader csv = CSVXReader.openResource(Test_TickDBLoader.class, fileName);
        //csv.nextLine(); // headers

        final BarMessage bar = new BarMessage();
        //bar.barSize = 3600000;
        while (csv.nextLine()) {
            int idx = 1;
            bar.setSymbol(csv.getString(idx++, true));
            bar.setTimeStampMs(DF2.parse(csv.getString(idx++, true)).getTime());
            idx++;
            bar.setCurrencyCode((short) csv.getInt(idx++));
            bar.setExchangeId(ExchangeCodec.codeToLong(csv.getString(idx++)));
            bar.setOpen(csv.getFloat(idx++));
            bar.setHigh(csv.getFloat(idx++));
            bar.setLow(csv.getFloat(idx++));
            bar.setClose(csv.getFloat(idx++));
            bar.setVolume(csv.getFloat(idx));
            loader.send(bar);
        }
        csv.close();
    }

    private static void compare(String etalonFile, InstrumentMessage[] actual) throws IOException, ParseException {
        final List<BarMessage> messages = new ArrayList<BarMessage>();
        final TickLoader loader = new TickLoader<InstrumentMessage>() {
            public WritableTickStream getTargetStream () {
                return (null);
            }


            @Override
            public void flush() throws IOException {
                
            }

            @Override
            public void removeUnique(InstrumentMessage msg) {
                
            }

            public void send(InstrumentMessage msg) {
                messages.add((BarMessage) msg.clone());
            }

            public void close() {
            }

            public void addEventListener(LoadingErrorListener listener) {
            }

            public void removeEventListener(LoadingErrorListener listener) {
            }

            @Override
            public void addSubscriptionListener(SubscriptionChangeListener listener) {
            }

            @Override
            public void removeSubscriptionListener(SubscriptionChangeListener listener) {
            }
        };

        try {
            loadEtalonFile(etalonFile, loader);
        } finally {
            loader.close();
        }

        final BarMessage[] expected = messages.toArray(new BarMessage[messages.size()]);
        sort(expected);
        sort(actual);

        //dump(actual, TickDBUtil.USER_HOME + "actual.txt");
        //dump(expected, TickDBUtil.USER_HOME + "expected.txt");
        assertEquals("Array's length is different.", expected.length, actual.length);
        for (int i = 0; i < expected.length; i++) {            
            assertEquals("element " + i, expected[i].toString(), actual[i].toString());
        }
    }

    // do only local sorting for the messages with the same timestamp
    private static void sort(InstrumentMessage[] messages) {
        int idx1, idx2;
        idx1 = idx2 = 0;
        while (idx1 < messages.length) {
            long ts = messages[idx1].getTimeStampMs();
            for (int i = idx1; i < messages.length; i++) {
                InstrumentMessage message = messages[i];
                if (message.getTimeStampMs() != ts) {
                    idx2 = i;
                    break;
                }
            }
            if (idx2 - idx1 > 1)
                Arrays.sort(messages, idx1, idx2, new Comparator<InstrumentMessage>() {
                    public int compare(InstrumentMessage o1, InstrumentMessage o2) {
                        assert Util.compare(o1.getTimeStampMs(), o2.getTimeStampMs()) == 0;
                        return Util.compare(o1.getSymbol(), o2.getSymbol(), true);
                    }
                });
            idx1 = idx2;
            idx2 = messages.length;
        }
    }

    private static InstrumentMessage[] select2Array(long time, TickStream stream, boolean reversed) {
        SelectionOptions options = new SelectionOptions(false, false, reversed);
        TickCursor cursor = stream.select(time, options);

        if (cursor == null) {
            return null;
        } else {
            List<InstrumentMessage> messages = new ArrayList<InstrumentMessage>();
            while (cursor.next()) {
                InstrumentMessage msg = cursor.getMessage();
                messages.add(msg.clone());
            }
            cursor.close();
            return messages.toArray(new InstrumentMessage[messages.size()]);
        }
    }
/*
    @Test
    public void dump() throws IOException, ParseException {
        TickLoader loader = new TickLoader() {
            public void saveChanges() {
            }

            public void send(InstrumentMessage msg) {
                util.print(msg, System.out);
            }

            public void close() {
            }
        };
        loadFile(DIR + "loader.chain_BySym.csv", loader);
    }

    private void dump(InstrumentMessage[] messages, String fileName) throws IOException {
        FileOutputStream fos = new FileOutputStream(fileName);
        dump(messages, new PrintStream(fos));
        fos.close();
    }

    private void dump(InstrumentMessage[] messages, PrintStream out) {
        out = out != null ? out : System.out;
        for (InstrumentMessage msg : messages)
            util.print(msg, out);
    }
*/

    private final class LoadingErrorListenerImpl implements LoadingErrorListener {

        public void saveChanges() {
        }

        public void onError(LoadingError e) {
            synchronized (errors) {
                errors.add(e.toString());
            }
        }
    }
}
