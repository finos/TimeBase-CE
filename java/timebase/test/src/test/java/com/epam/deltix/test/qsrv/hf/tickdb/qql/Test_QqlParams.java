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

package com.epam.deltix.test.qsrv.hf.tickdb.qql;

import com.epam.deltix.qsrv.hf.pub.ChannelQualityOfService;
import com.epam.deltix.qsrv.hf.pub.MappingTypeLoader;
import com.epam.deltix.qsrv.hf.pub.md.Introspector;
import com.epam.deltix.qsrv.hf.tickdb.TDBRunner;
import com.epam.deltix.qsrv.hf.tickdb.comm.server.TomcatServer;
import com.epam.deltix.qsrv.hf.tickdb.pub.*;
import com.epam.deltix.qsrv.hf.tickdb.pub.query.InstrumentMessageSource;
import com.epam.deltix.qsrv.hf.tickdb.pub.query.Parameter;
import com.epam.deltix.qsrv.test.messages.TradeMessage;
import com.epam.deltix.test.qsrv.hf.tickdb.TDBRunnerBase;
import com.epam.deltix.test.qsrv.hf.tickdb.qql.messages.QQLTypeLoader;
import com.epam.deltix.test.qsrv.hf.tickdb.qql.messages.SimpleTradeMessage;
import com.epam.deltix.util.JUnitCategories;
import com.epam.deltix.util.io.Home;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@Category(JUnitCategories.TickDBQQL.class)
public class Test_QqlParams extends TDBRunnerBase {

    private static TDBRunner runner;
    private static DXTickDB db;
    private static boolean external;
    private static boolean remote;

    @BeforeClass
    public static void start() throws Throwable {
        external = Boolean.parseBoolean(
            System.getProperty("runner.external", "false")
        );
        if (external) {
            db = openDb("dxtick://localhost:8101");
        } else {
            remote = Boolean.parseBoolean(
                System.getProperty("runner.remote", "true")
            );
            db = createDb(remote);
        }

        boolean loadData = Boolean.parseBoolean(
            System.getProperty("qql.test.loadData", "true")
        );
        if (loadData) {
            loadAllData();
        }
    }

    private static DXTickDB openDb(String url) {
        return TickDBFactory.openFromUrl(url, false);
    }

    private static DXTickDB createDb(boolean remote) throws Exception {
        runner = new TDBRunner(remote, false, Home.getPath("temp/qql_params_test/timebase"), new TomcatServer());
        runner.startup();
        return runner.getTickDb();
    }

    @AfterClass
    public static void stop() throws Throwable {
        if (runner != null) {
            runner.shutdown();
            runner = null;
        } else {
            db.close();
        }
    }

    @Test
    public void Test_timestampSmoke() {
        String query1 = "select price, size type t from trades where (timestamp > $ts0 and timestamp < $ts1)";
        List<SimpleTradeMessage> fetch1 = fetch(() -> query1, new long[]{100, 200});
        Assert.assertEquals(99, fetch1.size());
        Assert.assertEquals(101, fetch1.get(0).getTimeStampMs());
        Assert.assertEquals(111, fetch1.get(0).getSize(), 0.00001);
        Assert.assertEquals(199, fetch1.get(98).getTimeStampMs());
        Assert.assertEquals(209, fetch1.get(98).getSize(), 0.00001);

        List<SimpleTradeMessage> fetch2 = fetch(() -> query1, new long[]{1000, 2000});
        Assert.assertEquals(999, fetch2.size());
        Assert.assertEquals(1001, fetch2.get(0).getTimeStampMs());
        Assert.assertEquals(1011, fetch2.get(0).getSize(), 0.00001);
        Assert.assertEquals(1999, fetch2.get(998).getTimeStampMs());
        Assert.assertEquals(2009, fetch2.get(998).getSize(), 0.00001);

        String query2 = "select price, size type t from trades where (timestamp between $ts0 and $ts1)";
        List<SimpleTradeMessage> fetch3 = fetch(() -> query2, new long[]{55, 555});
        Assert.assertEquals(501, fetch3.size());
        Assert.assertEquals(55, fetch3.get(0).getTimeStampMs());
        Assert.assertEquals(65, fetch3.get(0).getSize(), 0.00001);
        Assert.assertEquals(555, fetch3.get(500).getTimeStampMs());
        Assert.assertEquals(565, fetch3.get(500).getSize(), 0.00001);

        List<SimpleTradeMessage> fetch4 = fetch(() -> query2, new long[]{5555, 6666});
        Assert.assertEquals(1112, fetch4.size());
        Assert.assertEquals(5555, fetch4.get(0).getTimeStampMs());
        Assert.assertEquals(5565, fetch4.get(0).getSize(), 0.00001);
        Assert.assertEquals(6666, fetch4.get(1111).getTimeStampMs());
        Assert.assertEquals(6676, fetch4.get(1111).getSize(), 0.00001);
    }

    @Test
    public void Test_timestampParams() {
        timestampTestSuite(
            "select count{}() as 'count', last{}(price) as 'price', last{}(size) as 'size' type t from trades "
        );
    }

    @Test
    public void Test_reverseTimestampParams() {
        timestampTestSuite(
            "select count{}() as 'count', last{}(price) as 'price', last{}(size) as 'size' type t from reverse(trades) "
        );
    }

    @Test
    public void Test_symbolsSmoke() {
        String query1 = "select price, size type t from trades where (symbol in ($s0, $s1))";

        List<SimpleTradeMessage> fetch1 = fetch(() -> query1, new long[0], "S1", "S2");
        Assert.assertEquals(0, checkSymbols(fetch1, "S1", "S2").size());

        List<SimpleTradeMessage> fetch2 = fetch(() -> query1, new long[0], "S3", "S7");
        Assert.assertEquals(0, checkSymbols(fetch2, "S3", "S7").size());

        List<SimpleTradeMessage> fetch3 = fetch(() -> query1, new long[0], "S4", "S4");
        Assert.assertEquals(0, checkSymbols(fetch3, "S4").size());

        String query2 = "select price, size type t from trades where symbol == $s0";

        List<SimpleTradeMessage> fetch4 = fetch(() -> query2, new long[0], "S1");
        Assert.assertEquals(0, checkSymbols(fetch4, "S1").size());

        List<SimpleTradeMessage> fetch5 = fetch(() -> query2, new long[0], "S6");
        Assert.assertEquals(0, checkSymbols(fetch5, "S6").size());

        List<SimpleTradeMessage> fetch6 = fetch(() -> "select price, size type t from trades where symbol == 'S5'", new long[0]);
        Assert.assertEquals(0, checkSymbols(fetch6, "S5").size());
    }

    @Test
    public void Test_symbols() {
        String query1 = "select running price, size, count{}() as count type t from trades where (symbol in (%s, %s, %s)) ";
        check(query1, "S1", "S2", "S3");
        check(query1, "S2", "S1", "S3");
        check(query1, "S6", "S6", "S6");
        check(query1, "S3", "S4", "S5");
        check(query1, "S1", "S7", "S6");
        check(query1, "S1", "S7", "S6");
        check(query1, "S5", "S4", "S1");
        check(query1, "SS", "SV", "SZ");

        String query2 = "select running price, size, count{}() as count type t from reverse(trades) where (symbol == %s or symbol == %s or symbol == %s) ";
        check(query2, "S1", "S2", "S3");
        check(query2, "S2", "S1", "S3");
        check(query2, "S6", "S6", "S6");
        check(query2, "S3", "S4", "S5");
        check(query2, "S1", "S7", "S6");
        check(query2, "S1", "S7", "S6");
        check(query2, "S5", "S4", "S1");
        check(query2, "SS", "SV", "SZ");
    }

    @Test
    public void Test_misc() {
        String query1 = "select running price, size, count{}() as count type t from trades " +
            "where (timestamp between %s and %s and symbol in (%s, %s, %s)) ";
        check(query1, new long[]{10, 20}, "S1", "S2", "S3");
        check(query1, new long[]{0, 10000}, "S2", "S1", "S3");
        for (int i = 0; i < 2; ++i) {
            int ts = (int) (Math.random() * 10000);
            int ts1 = (int) (Math.random() * 1000);
            check(query1, new long[]{ts, ts + ts1}, "S3", "S4", "S5");
        }
        for (int i = 0; i < 2; ++i) {
            int ts = (int) (Math.random() * 10000);
            int ts1 = (int) (Math.random() * 1000);
            check(query1, new long[]{ts, ts + ts1}, "S5", "S4", "S1");
        }
    }

    @Test
    public void Test_misc2() {
        String query1 = "select running price, size, count{}() as count type t from trades " +
            "where (timestamp between $ts0 and 1000 and timestamp < 900 and (symbol in ('S5', $s0, $s1) or symbol == 'S6'))";

        List<SimpleTradeMessage> fetch1 = fetch(() -> query1, new long[]{500}, "S2", "S4");
        Assert.assertEquals(229, fetch1.size());
        Assert.assertEquals(500, fetch1.get(0).getTimeStampMs());
        Assert.assertEquals(510, fetch1.get(0).getSize(), 0.0001);
        Assert.assertEquals(899, fetch1.get(228).getTimeStampMs());
        Assert.assertEquals(909, fetch1.get(228).getSize(), 0.0001);
        Assert.assertEquals(0, checkSymbols(fetch1, "S2", "S4", "S5", "S6").size());


        String query2 = "select running price, size, count{}() as count type t from reverse(trades) " +
            "where (timestamp between 500 and $ts0 and timestamp < $ts1 and (symbol == 'S6' or symbol in ($s0, 'S5', $s1)))";

        List<SimpleTradeMessage> fetch2 = fetch(() -> query2, new long[]{1000, 900}, "S2", "S4");
        Assert.assertEquals(229, fetch2.size());
        Assert.assertEquals(500, fetch2.get(228).getTimeStampMs());
        Assert.assertEquals(510, fetch2.get(228).getSize(), 0.0001);
        Assert.assertEquals(899, fetch2.get(0).getTimeStampMs());
        Assert.assertEquals(909, fetch2.get(0).getSize(), 0.0001);
        Assert.assertEquals(0, checkSymbols(fetch2, "S2", "S4", "S5", "S6").size());

        String query3 = "select running price, size, count{}() as count type t from trades " +
            "where (timestamp between 500 and $ts0 or (timestamp < $ts1 and (symbol == 'S6' or symbol in ($s0, 'S5', $s1))))";

        List<SimpleTradeMessage> fetch3 = fetch(() -> query3, new long[]{1000, 900}, "S2", "S4");
        Assert.assertEquals(786, fetch3.size());
        Assert.assertEquals(1, fetch3.get(0).getTimeStampMs());
        Assert.assertEquals(11, fetch3.get(0).getSize(), 0.0001);
        Assert.assertEquals(1000, fetch3.get(785).getTimeStampMs());
        Assert.assertEquals(1010, fetch3.get(785).getSize(), 0.0001);

        List<SimpleTradeMessage> fetch31 = fetch3.stream().filter(f -> f.getTimeStampMs() < 500).collect(Collectors.toList());
        Assert.assertEquals(0, checkSymbols(fetch31, "S2", "S4", "S5", "S6").size());

        List<SimpleTradeMessage> fetch32 = fetch3.stream().filter(f -> f.getTimeStampMs() > 500).collect(Collectors.toList());
        Assert.assertEquals(0, checkSymbols(fetch32, "S1", "S2", "S3", "S4", "S5", "S6", "S7").size());


        String query4 = "select running price, size, count{}() as count type t from trades " +
            "where (timestamp between 500 and $ts0 or (symbol == 'S6' or symbol in ($s0, 'S5', $s1)))";
        List<SimpleTradeMessage> fetch4 = fetch(() -> query4, new long[]{900}, "S2", "S4");
        List<SimpleTradeMessage> fetch41 = fetch4.stream().filter(f -> f.getTimeStampMs() < 500 || f.getTimeStampMs() > 900)
            .collect(Collectors.toList());
        List<SimpleTradeMessage> fetch42 = fetch4.stream().filter(f -> f.getTimeStampMs() >= 500 || f.getTimeStampMs() <= 900)
            .collect(Collectors.toList());
        Assert.assertEquals(0, checkSymbols(fetch41, "S2", "S4", "S5", "S6").size());
        Assert.assertEquals(0, checkSymbols(fetch42, "S1", "S2", "S3", "S4", "S5", "S6", "S7").size());
    }

    private void timestampTestSuite(String baseQuery) {
        checkStartTime(baseQuery);
        checkEndTime(baseQuery);
        checkStartEndTime(baseQuery);
        checkBetween(baseQuery);
    }

    private void checkBetween(String baseQuery) {
        String query = baseQuery + "where (timestamp between %s and %s) ";
        check(query, 10, 20);
        check(query, 0, 10000);
        check(query, 0, 0);
        check(query, 9999, 9999);
        check(query, 1, 1);
        check(query, 10, 10);
        check(query, 10000, 10000);
        check(query, -1, -1);
        for (int i = 0; i < 5; ++i) {
            int ts = (int) (Math.random() * 10000);
            int ts1 = (int) (Math.random() * 1000);
            check(query, ts, ts + ts1);
        }
    }

    private void checkStartTime(String baseQuery) {
        String query1 = baseQuery + "where (timestamp > %s) ";
        String query2 = baseQuery + "where (timestamp >= %s) ";
        check(query1, 10);
        check(query2, 10);
        check(query1, 9999);
        check(query2, 9999);
        check(query1, 10000);
        check(query2, 10000);
        check(query1, 0);
        check(query2, 0);
        check(query1, 1);
        check(query2, 1);
        check(query1, -1);
        check(query2, -1);
        check(query1, 100000);
        check(query2, 100000);
        for (int i = 0; i < 5; ++i) {
            int ts = (int) (Math.random() * 10000);
            check(query1, ts);
            check(query2, ts);
        }
    }

    private void checkEndTime(String baseQuery) {
        String query1 = baseQuery + "where (timestamp < %s) ";
        String query2 = baseQuery + "where (timestamp <= %s) ";
        check(query1, 10);
        check(query2, 10);
        check(query1, 9999);
        check(query2, 9999);
        check(query1, 10000);
        check(query2, 10000);
        check(query1, 0);
        check(query2, 0);
        check(query1, 1);
        check(query2, 1);
        check(query1, -1);
        check(query2, -1);
        check(query1, 100000);
        check(query2, 100000);
        for (int i = 0; i < 5; ++i) {
            int ts = (int) (Math.random() * 10000);
            check(query1, ts);
            check(query2, ts);
        }
    }

    private void checkStartEndTime(String baseQuery) {
        String query1 = baseQuery + "where (timestamp > %s and timestamp < %s) ";
        String query2 = baseQuery + "where (timestamp > %s and timestamp <= %s) ";
        String query3 = baseQuery + "where (timestamp >= %s and timestamp <= %s) ";
        String query4 = baseQuery + "where (timestamp >= %s and timestamp < %s) ";
        check(query1, 10, 20);
        check(query2, 10, 20);
        check(query3, 10, 20);
        check(query4, 10, 20);
        check(query1, 9999, 10000);
        check(query2, 9999, 10000);
        check(query3, 9999, 10000);
        check(query4, 9999, 10000);
        check(query1, 0, 0);
        check(query2, 0, 0);
        check(query3, 0, 0);
        check(query4, 0, 0);
        check(query1, 0, 9999);
        check(query2, 0, 9999);
        check(query3, 0, 9999);
        check(query4, 0, 9999);
        for (int i = 0; i < 5; ++i) {
            int ts = (int) (Math.random() * 10000);
            int ts1 = (int) (Math.random() * 1000);
            check(query1, ts, ts + ts1);
            check(query2, ts, ts + ts1);
            check(query3, ts, ts + ts1);
            check(query4, ts, ts + ts1);
        }
    }

    private void check(String query, long... ts) {
        check(query, ts, new String[0]);
    }

    private void check(String query, String... sym) {
        check(query, new long[0], sym);
    }

    private void check(String query, long[] ts, String... sym) {
        List<SimpleTradeMessage> fetch1 = fetch(() -> String.format(query, toString(ts, sym)), ts, sym);
        List<SimpleTradeMessage> fetch2 = fetch(() -> String.format(query, toString(ts, sym)) + "or false", ts, sym);
        List<SimpleTradeMessage> fetch3 = fetch(() -> String.format(query, parameterNames(ts, sym)) , ts, sym);
        Assert.assertEquals(fetch1.size(), fetch2.size());
        Assert.assertEquals(fetch1.size(), fetch3.size());
        if (fetch1.isEmpty()) {
            System.out.println("Empty result");
        } else {
            for (int i = 0; i < fetch1.size(); ++i) {
                if (i == 0) {
                    System.out.println("Result: " + fetch1.get(i));
                }
                Assert.assertEquals(fetch1.get(i), fetch2.get(i));
                Assert.assertEquals(fetch1.get(i), fetch3.get(i));
            }
        }
        if (sym.length > 0) {
            checkSymbols(fetch1, sym);
            checkSymbols(fetch2, sym);
            List<String> notFoundSymbols = checkSymbols(fetch3, sym);
            for (String symbol : notFoundSymbols) {
                System.out.println("Not found symbol: " + symbol);
            }
        }
    }

    private List<String> checkSymbols(List<SimpleTradeMessage> messages, String... sym) {
        Set<String> symbols = new HashSet<>(Arrays.asList(sym));
        Set<String> notFoundSymbols = new HashSet<>(Arrays.asList(sym));
        for (SimpleTradeMessage message : messages) {
            Assert.assertTrue(
                "Symbol not found: " + message.getSymbol(),
                symbols.contains(message.getSymbol().toString())
            );
            notFoundSymbols.remove(message.getSymbol().toString());
        }

        return new ArrayList<>(notFoundSymbols);
    }

    private List<SimpleTradeMessage> fetch(Supplier<String> queryProvider, long[] ts, String... sym) {
        List<SimpleTradeMessage> result = new ArrayList<>();
        String query = queryProvider.get();
        System.out.println("QUERY: " + query);
        try (InstrumentMessageSource cursor = db.executeQuery(query, selectionOptions(), parameters(ts, sym))) {
            while (cursor.next()) {
                result.add((SimpleTradeMessage) cursor.getMessage().clone());
            }
        }

        return result;
    }

    private SelectionOptions selectionOptions() {
        MappingTypeLoader loader = new QQLTypeLoader.QueryTypeLoader(null);
        loader.bind("T", SimpleTradeMessage.class);
        SelectionOptions options = new SelectionOptions();
        options.raw = false;
        options.typeLoader = loader;
        options.channelQOS = ChannelQualityOfService.MIN_INIT_TIME;
        return options;
    }

    private String[] toString(long[] ts, String... sym) {
        List<String> result = new ArrayList<>();
        result.addAll(Arrays.stream(ts).mapToObj(String::valueOf).collect(Collectors.toList()));
        result.addAll(Arrays.stream(sym).map(s -> "'" + s + "'").collect(Collectors.toList()));
        return result.toArray(new String[0]);
    }

    private String[] parameterNames(long[] ts, String... sym) {
        return Arrays.stream(parameters(ts, sym)).map(p -> p.name).toArray(String[]::new);
    }

    private Parameter[] parameters(long[] ts, String... sym) {
        List<Parameter> result = new ArrayList<>();
        for (int i = 0; i < ts.length; ++i) {
            result.add(Parameter.INTEGER("$ts" + i, ts[i]));
        }
        for (int i = 0; i < sym.length; ++i) {
            result.add(Parameter.VARCHAR("$s" + i, sym[i]));
        }

        return result.toArray(new Parameter[0]);
    }

    // --------- GENERATE AND LOAD DATA
    private static void loadAllData() throws Exception {
        loadTrades(db, "trades", true);
    }

    private static void loadTrades(DXTickDB db, String streamKey, boolean createStream) {
        if (createStream) {
            DXTickStream stream1 = db.getStream(streamKey);
            if (stream1 != null) {
                stream1.delete();
            }
        }

        try {
            StreamOptions options = new StreamOptions(StreamScope.DURABLE, streamKey, streamKey, 0);
            options.setFixedType(Introspector.createCustomIntrospector().introspectMemberClass(
                "", TradeMessage.class
            ));
            DXTickStream stream = db.createStream(streamKey, options);
            try (TickLoader loader = stream.createLoader()) {
                String[] symbols = new String[] { "S1", "S2", "S3", "S4", "S5", "S6", "S7" };
                TradeMessage tradeMessage = new TradeMessage();
                for (int i = 0; i < 10000; i++) {
                    tradeMessage.setTimeStampMs(i);
                    tradeMessage.setSymbol(symbols[i % symbols.length]);
                    tradeMessage.setPrice(i);
                    tradeMessage.setSize(i + 10);
                    loader.send(tradeMessage);
                }
            }
        } catch (Introspector.IntrospectionException e) {
            throw new RuntimeException(e);
        }
    }

}
