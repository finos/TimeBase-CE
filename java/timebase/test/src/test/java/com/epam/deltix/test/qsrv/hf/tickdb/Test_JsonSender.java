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

import com.google.gson.JsonArray;
import com.google.gson.JsonParser;
import com.epam.deltix.qsrv.hf.pub.ChannelQualityOfService;
import com.epam.deltix.qsrv.hf.tickdb.TDBRunner;
import com.epam.deltix.test.qsrv.hf.tickdb.server.ServerRunner;
import com.epam.deltix.timebase.messages.InstrumentMessage;
import com.epam.deltix.qsrv.hf.pub.RawMessage;
import com.epam.deltix.qsrv.hf.tickdb.pub.DXTickDB;
import com.epam.deltix.qsrv.hf.tickdb.pub.DXTickStream;
import com.epam.deltix.qsrv.hf.tickdb.pub.SelectionOptions;
import com.epam.deltix.qsrv.hf.tickdb.pub.TickCursor;
import com.epam.deltix.test.qsrv.hf.tickdb.testframework.TestAllTypesStreamCreator;
import com.epam.deltix.qsrv.util.json.DataEncoding;
import com.epam.deltix.qsrv.util.json.JSONHelper;
import com.epam.deltix.qsrv.util.json.JSONRawMessagePrinter;
import com.epam.deltix.qsrv.util.json.PrintType;
import com.epam.deltix.util.JUnitCategories.TickDBFast;
import com.epam.deltix.util.collections.generated.ObjectArrayList;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.util.HashSet;

import static org.junit.Assert.assertEquals;

/**
 * @author Daniil Yarmalkevich
 * Date: 11/6/2019
 */
@Category(TickDBFast.class)
public class Test_JsonSender {

    private static TDBRunner runner;

    @BeforeClass
    public static void start() throws Throwable {
        runner = new ServerRunner(true, false);
        runner.startup();
    }

    @AfterClass
    public static void stop() throws Throwable {
        runner.shutdown();
        runner = null;
    }

    @Test
    public void testAllTypesStream() {
        DXTickDB db = runner.getServerDb();

        TestAllTypesStreamCreator creator = new TestAllTypesStreamCreator(db);
        creator.createStream();

        DXTickStream stream = db.getStream(TestAllTypesStreamCreator.STREAM_KEY);

        creator.loadTestData(ChannelQualityOfService.MAX_THROUGHPUT);

        ObjectArrayList<InstrumentMessage> nativeMessages = new ObjectArrayList<>();

        try (TickCursor cursor = db.select(Long.MIN_VALUE, new SelectionOptions(false, false), stream)) {
            while (cursor.next()) {
                nativeMessages.add(cursor.getMessage().clone());
            }
        }

        SelectionOptions options = new SelectionOptions(true, false);
        JSONRawMessagePrinter printer = new JSONRawMessagePrinter(false, true, DataEncoding.STANDARD,
                false, true, PrintType.FULL);
        StringBuilder arrayBuilder = new StringBuilder();
        arrayBuilder.append('[');
        HashSet<String> messages = new HashSet<>();
        ObjectArrayList<String> messagesList = new ObjectArrayList<>();
        ObjectArrayList<String> jsonMessagesList = new ObjectArrayList<>();
        StringBuilder temp = new StringBuilder();
        try (TickCursor cursor = db.select(Long.MIN_VALUE, options, stream)) {
            while (cursor.next()) {
                RawMessage raw = (RawMessage) cursor.getMessage();
                messages.add(raw.toString());
                messagesList.add(raw.toString());
                printer.append(raw, arrayBuilder);
                printer.append(raw, temp);
                jsonMessagesList.add(temp.toString());
                temp.setLength(0);
                arrayBuilder.append(",");
            }
        } finally {
            arrayBuilder.setLength(arrayBuilder.length() - 1);
            arrayBuilder.append(']');
        }
        String array = arrayBuilder.toString();
//        System.out.println(array);
        JsonArray jsonArray = new JsonParser().parse(array).getAsJsonArray();
        assertEquals(messages.size(), jsonArray.size());

        stream.clear();

        JSONHelper.parseAndLoad(array, stream);

        int count = 0;
        try (TickCursor cursor = db.select(Long.MIN_VALUE, new SelectionOptions(false, false), stream)) {
            while (cursor.next()) {
                assertEquals(nativeMessages.get(count), cursor.getMessage().clone());
                count++;
            }
        }

//        System.out.println(messages.size());
        count = 0;
        try (TickCursor cursor = db.select(Long.MIN_VALUE, options, stream)) {
            while (cursor.next()) {
                RawMessage raw = (RawMessage) cursor.getMessage();
//                System.out.printf("%d, ", count);
                printer.append(raw, temp);
//                assertTrue(String.format("%d %d %s", count, messages.size(), raw.toString()), messages.contains(raw.toString()));
                assertEquals(String.format("%d, \n%s, \n%s, \n%s\n", count, raw.toString(), messagesList.get(count), jsonMessagesList.get(count)),
                        raw.toString(),
                        messagesList.get(count));
                assertEquals(String.format("%d, \n%s, \n%s, \n%s\n", count, raw.toString(), messagesList.get(count), jsonMessagesList.get(count)),
                        temp.toString(),
                        jsonMessagesList.get(count));
                count++;
                temp.setLength(0);
            }
        }
    }
}