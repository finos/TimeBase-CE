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
package com.epam.deltix.test.qsrv.hf.tickdb.schema;

import com.sun.xml.bind.marshaller.NamespacePrefixMapper;
import com.sun.xml.bind.v2.WellKnownNamespace;
import com.epam.deltix.qsrv.hf.tickdb.StreamConfigurationHelper;
import com.epam.deltix.qsrv.hf.tickdb.schema.*;
import com.epam.deltix.qsrv.test.messages.BarMessage;
import com.epam.deltix.qsrv.test.messages.BestBidOfferMessage;
import com.epam.deltix.qsrv.test.messages.TradeMessage;
import com.epam.deltix.streaming.MessageSource;
import com.epam.deltix.data.stream.MessageSourceMultiplexer;
import com.epam.deltix.qsrv.hf.pub.*;
import com.epam.deltix.qsrv.hf.pub.md.*;
import com.epam.deltix.qsrv.hf.stream.MessageFileHeader;
import com.epam.deltix.qsrv.hf.stream.Protocol;
import com.epam.deltix.qsrv.hf.tickdb.TDBRunner;
import com.epam.deltix.qsrv.hf.tickdb.impl.TickDBJAXBContext;
import com.epam.deltix.qsrv.hf.tickdb.pub.*;
import com.epam.deltix.qsrv.hf.tickdb.pub.task.SchemaChangeTask;
import com.epam.deltix.qsrv.hf.tickdb.pub.task.SchemaUpdateTask;
import com.epam.deltix.qsrv.hf.tickdb.pub.task.TransformationContext;
import com.epam.deltix.qsrv.hf.tickdb.ui.tbshell.TickDBShell;
import com.epam.deltix.qsrv.testsetup.TickDBCreator;
import com.epam.deltix.test.qsrv.hf.tickdb.server.ServerRunner;
import com.epam.deltix.timebase.messages.IdentityKey;
import com.epam.deltix.timebase.messages.InstrumentMessage;
import com.epam.deltix.util.JUnitCategories;
import com.epam.deltix.util.collections.ArraysComparator;
import com.epam.deltix.util.collections.generated.ObjectArrayList;
import com.epam.deltix.util.io.Home;
import com.epam.deltix.util.lang.Util;
import junit.framework.Assert;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import javax.xml.XMLConstants;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

@Category(JUnitCategories.TickDBFast.class)
public class Test_SchemaConverter {
    private final RawMessageHelper rawMessageHelper = new RawMessageHelper();

    private static TDBRunner runner;

    @BeforeClass
    public static void start() throws Throwable {
        runner = new ServerRunner(true, true);
        runner.startup();

        TickDBCreator.createBarsStream (runner.getTickDb(), TickDBCreator.BARS_STREAM_KEY);
    }

    @AfterClass
    public static void stop() throws Throwable {
        runner.shutdown();
        runner = null;
    }

    public DXTickDB getTickDb() {
        return runner.getTickDb();
    }

    @Test
    public void Test_EncodingChange() {
        DXTickDB tdb = getTickDb();

        DXTickStream source = createBarsStream(
                tdb, "Test_EncodingChange.s", "", null,
                new TDBRunner.BarsGenerator(new GregorianCalendar(2009, 1, 1), (int) BarMessage.BAR_MINUTE, 100));

        RecordClassDescriptor d2 = StreamConfigurationHelper.mkBarMessageDescriptor(null,
            "", null, FloatDataType.ENCODING_FIXED_FLOAT, FloatDataType.ENCODING_FIXED_FLOAT);

        DXTickStream target = tdb.createStream("Test_EncodingChange.t",
                StreamOptions.fixedType(StreamScope.DURABLE, "Test_EncodingChange.t", null, 0, d2));

        MetaDataChange change = getChanges(target, source);

        TickCursor cursor = source.select(Long.MIN_VALUE, new SelectionOptions(true, false));
        TickLoader loader = target.createLoader(new LoadingOptions(true));

        try {
            new SchemaConverter(change).convert(cursor, loader);
            
        } finally {
            loader.close();
            cursor.close();
        }

        assertEquals(target.getFixedType().getGuid(), d2.getGuid());

        //rawCompareStreams(source, target, null, null);
    }

    @Test
    public void testNetUnmarshall() throws Exception {

        SchemaUpdateTask task = new SchemaUpdateTask();
        String xml = "<SchemaUpdateTask xmlns=\"http://xml.deltixlab.com/internal/quantserver/3.0\">\n" +
                "  <polymorphic>false</polymorphic>\n" +
                "  <background>false</background>\n" +
                "  <schema/>" +
                "  <defaults />\n" +
                "  <mappings>\n" +
                "    <item>\n" +
                "      <name>test:name</name>\n" +
                "      <value>test:value</value>\n" +
                "    </item>\n" +
                "  </mappings>\n" +
                "</SchemaUpdateTask>";

        Unmarshaller u = TransformationContext.createUnmarshaller(task);

        NamespacePrefixMapper namespacePrefixMapper = new com.sun.xml.bind.marshaller.NamespacePrefixMapper() {

            private Map<String, String> prefixes;

            {
                prefixes = new HashMap<>(3);
                prefixes.put(XMLConstants.XML_NS_URI, XMLConstants.XML_NS_PREFIX);
                prefixes.put(XMLConstants.W3C_XML_SCHEMA_INSTANCE_NS_URI, "xsi");
                prefixes.put(XMLConstants.W3C_XML_SCHEMA_NS_URI, "xs");
                prefixes.put(WellKnownNamespace.XML_MIME_URI, "xmime");
            }

            @Override
            public String getPreferredPrefix(String namespaceUri, String suggestion,
                                             boolean requirePrefix) {
                String prefix = suggestion == null ? prefixes.get(namespaceUri)
                        : suggestion;
                return prefix == null ? XMLConstants.DEFAULT_NS_PREFIX : prefix;
            }

        };

        //u.setProperty("com.sun.xml.bind.namespacePrefixMapper", namespacePrefixMapper);

        SchemaUpdateTask result = (SchemaUpdateTask) u.unmarshal(new StringReader(xml));

        assertEquals("test:value", result.getMapping("test:name"));
    }

    @Test
    public void testMarshall() throws Exception {

        SchemaUpdateTask task = new SchemaUpdateTask();
        task.schema = "empty";
        task.background = false;
        task.addMapping( "map", "value");
        task.addMapping( "map1", "value1");
        task.addDefault( "default", "value");
        task.addDefault( "default1", "value1");

        StringWriter taskWriter = new StringWriter();

        Marshaller marshaller = TransformationContext.createMarshaller(task);
        marshaller.marshal(task, taskWriter);

        System.out.println(taskWriter.toString());
    }

    @Test
    public void testTask() throws JAXBException {
        SchemaUpdateTask task = new SchemaUpdateTask();

        RecordClassDescriptor d2 = StreamConfigurationHelper.mkBarMessageDescriptor(null,
                "", null, FloatDataType.ENCODING_FIXED_FLOAT, FloatDataType.ENCODING_FIXED_FLOAT);

        RecordClassSet set = new RecordClassSet();
        set.addContentClasses(d2);

        StringWriter writer = new StringWriter();
        TickDBJAXBContext.createMarshaller().marshal(set, writer);
        task.schema = writer.getBuffer().toString();
        task.background = false;
        task.addMapping( "map", "value");
        task.addMapping( "map1", "value1");

        getTickDb().getStream(TickDBCreator.BARS_STREAM_KEY).execute(task);
    }

    @Test
    public void Test_CreateFieldChange() {
        DXTickDB tdb = getTickDb();

        DXTickStream source = TickDBCreator.createBarsStream(tdb, "Test_CreateFieldChange.s");

        RecordClassDescriptor d2 = StreamConfigurationHelper.mkBarMessageDescriptor(null,
                null, 840, "DECIMAL(4)", "DECIMAL(0)");

        DXTickStream target = tdb.createStream("Test_CreateFieldChange.t",
                StreamOptions.fixedType(StreamScope.DURABLE, "Test_CreateFieldChange.t", null, 0, d2));

        MetaDataChange change = getChanges(target, source);
        AbstractFieldChange[] changes = change.getChange(null, d2).getFieldChanges(null, d2.getField("exchangeId"));
        ((FieldModifierChange)changes[0]).setInitialValue("122");

        TickCursor cursor = source.select(0, new SelectionOptions(true, false));
        TickLoader loader = target.createLoader(new LoadingOptions(true));

        try {
            new SchemaConverter(change).convert(cursor, loader);
        } finally {
            loader.close();
            cursor.close();
        }

        TickCursor cursor1 = null;
        TickCursor cursor2 = null;
        try {
            cursor1 = TickCursorFactory.create(target, 0, new SelectionOptions(false, false),
                    "AAPL");

            cursor2 = TickCursorFactory.create(source, 0, new SelectionOptions(false, false),
                    "AAPL");

            while (true) {
                if (cursor1.next() && cursor2.next())
                    checkEquals(cursor2.getMessage(), cursor1.getMessage());
                else
                    break;
            }
        } finally {
            Util.close(cursor1);
            Util.close(cursor2);
        }
    }

    @Test
    public void Test_CreateFieldChange2() {
        DXTickDB tdb = getTickDb();

        DXTickStream source = TickDBCreator.createBarsStream(tdb, "Test_CreateFieldChange2");

        RecordClassDescriptor d2 = StreamConfigurationHelper.mkBarMessageDescriptor(null,
                "", 840, "DECIMAL(4)", "DECIMAL(0)");

        DXTickStream target = tdb.createStream("Test_CreateFieldChange2.t",
                StreamOptions.fixedType(StreamScope.DURABLE, "Test_CreateFieldChange2.t", null, 0, d2));

        MetaDataChange change = getChanges(target, source);
        AbstractFieldChange[] changes = change.getChange(null, d2).getFieldChanges(null, d2.getField("currencyCode"));
        ((FieldModifierChange)changes[0]).setInitialValue(null);

        org.junit.Assert.assertEquals(SchemaChange.Impact.DataConvert, change.getChangeImpact());

        TickCursor cursor = source.select(0, new SelectionOptions(true, false));
        TickLoader loader = target.createLoader(new LoadingOptions(true));

        try {
            new SchemaConverter(change).convert(cursor, loader);
        } finally {
            loader.close();
            cursor.close();
        }

        TickCursor cursor1 = null;
        TickCursor cursor2 = null;
        try {
            cursor1 = TickCursorFactory.create(target, 0, new SelectionOptions(false, false),
                    "ORCL");

            cursor2 = TickCursorFactory.create(source, 0, new SelectionOptions(false, false),
                    "ORCL");

            while (true) {
                if (cursor1.next() && cursor2.next())
                    checkEquals(cursor1.getMessage(), cursor2.getMessage());
                else
                    break;
            }
        } finally {
            Util.close(cursor1);
            Util.close(cursor2);
        }
    }

    @Test
    public void Test_CreateFieldChange1() {
        DXTickDB tdb = getTickDb();

        DXTickStream source = TickDBCreator.createBarsStream(tdb, "Test_CreateFieldChange1");

        RecordClassDescriptor d2 = StreamConfigurationHelper.mkBarMessageDescriptor(null,
                null, 840, "DECIMAL(4)", "DECIMAL(0)");

        DXTickStream target = tdb.createStream("Test_CreateFieldChange1.t",
                StreamOptions.fixedType(StreamScope.DURABLE, "Test_CreateFieldChange1.t", null, 0, d2));

        MetaDataChange change = getChanges(target, source);
        AbstractFieldChange[] changes = change.getChange(null, d2).getFieldChanges(null, d2.getField("exchangeId"));
        ((FieldModifierChange)changes[0]).setInitialValue(null);

        TickCursor cursor = source.select(0, new SelectionOptions(true, false));
        TickLoader loader = target.createLoader(new LoadingOptions(true));

        try {
            new SchemaConverter(change).convert(cursor, loader);
        } finally {
            loader.close();
            cursor.close();
        }

        TickCursor cursor1 = null;
        TickCursor cursor2 = null;
        try {
            cursor1 = TickCursorFactory.create(target, 0, new SelectionOptions(false, false),
                    "MSFT");

            cursor2 = TickCursorFactory.create(source, 0, new SelectionOptions(false, false),
                    "MSFT");

            while (true) {
                if (cursor1.next() && cursor2.next())
                    checkEquals(cursor1.getMessage(), cursor2.getMessage());
                else
                    break;
            }
        } finally {
            Util.close(cursor1);
            Util.close(cursor2);
        }
    }


    @Test
    public void Test_CreateFieldChange3() {
        DXTickDB tdb = getTickDb();

        DXTickStream source = TickDBCreator.createBarsStream(tdb, "Test_CreateFieldChange3");

        RecordClassDescriptor d2 = StreamConfigurationHelper.mkBarMessageDescriptor(null,
                null, 840, "DECIMAL(4)", "DECIMAL(0)");

        DXTickStream target = tdb.createStream("Test_CreateFieldChange3.t",
                StreamOptions.fixedType(StreamScope.DURABLE, "Test_CreateFieldChange3.t", null, 0, d2));

        MetaDataChange change = getChanges(target, source);
        AbstractFieldChange[] changes = change.getChange(null, d2).getFieldChanges(null, d2.getField("exchangeId"));
        ((FieldModifierChange)changes[0]).setInitialValue("NYSE");

        TickCursor cursor = source.select(0, new SelectionOptions(true, false));
        TickLoader loader = target.createLoader(new LoadingOptions(true));

        try {
            new SchemaConverter(change).convert(cursor, loader);
        } finally {
            loader.close();
            cursor.close();
        }

        TickCursor cursor1 = null;
        TickCursor cursor2 = null;
        try {
            cursor1 = TickCursorFactory.create(target, 0, new SelectionOptions(false, false),
                    "AAPL");

            cursor2 = TickCursorFactory.create(source, 0, new SelectionOptions(false, false),
                    "AAPL");

            while (true) {
                if (cursor1.next() && cursor2.next()) {
                    BarMessage converted = (BarMessage) cursor1.getMessage();
                    checkEquals(cursor2.getMessage(), converted);

                    assertEquals("NYSE", ExchangeCodec.longToCode(converted.getExchangeId()));
                }
                else {
                    break;
                }
            }
        } finally {
            Util.close(cursor1);
            Util.close(cursor2);
        }
    }


    @Test
    public void Test_SeveralStreams() {
        DXTickDB tdb = getTickDb();

        TickDBCreator.createBarsStream(tdb, "Test_SeveralStreams.s1");
        TickDBCreator.createBarsStream(tdb, "Test_SeveralStreams.s2");
        DXTickStream[] sources = new DXTickStream[] {
                tdb.getStream("Test_SeveralStreams.s1"),
                tdb.getStream("Test_SeveralStreams.s2")
        };

        RecordClassDescriptor d2 = StreamConfigurationHelper.mkBarMessageDescriptor(null,
                "", 840, FloatDataType.ENCODING_FIXED_FLOAT, FloatDataType.ENCODING_FIXED_FLOAT);

        DXTickStream target = tdb.createStream("Test_SeveralStreams.t",
                StreamOptions.fixedType(StreamScope.DURABLE, "Test_SeveralStreams.t", null, 0, d2));

        MetaDataChange change = getChanges(target, sources);
//        AbstractFieldChange[] changes = change.getClassDescriptorChange(null, d2).getFieldChanges(null, d2.getField("exchangeCode"));
//        ((FieldModifierChange)changes[0]).setInitialValue("122");

        SchemaConverter converter = new SchemaConverter(change);

        MessageSourceMultiplexer<InstrumentMessage> source = new MessageSourceMultiplexer<InstrumentMessage>();
        for (DXTickStream stream : sources) {
            TickCursor cursor = stream.select(0, new SelectionOptions(true, false));
            source.add(cursor);            
        }

        TickLoader loader = target.createLoader(new LoadingOptions(true));

        try {
            converter.convert(source, loader);
        } finally {
            loader.close();
            source.close();
        }

        TickCursor cursor = target.select(0, new SelectionOptions(true, false));
        assertTrue(cursor.next());
        cursor.close();
    }

    @Test
    public void Test_Polymorphic() {
        DXTickDB tdb = getTickDb();
        
        RecordClassDescriptor marketMsgDescriptor =
            StreamConfigurationHelper.mkMarketMessageDescriptor (840);

        RecordClassDescriptor rd1 = StreamConfigurationHelper.mkBBOMessageDescriptor(marketMsgDescriptor,
        true, "", 840, FloatDataType.ENCODING_FIXED_DOUBLE, FloatDataType.ENCODING_FIXED_DOUBLE);
        RecordClassDescriptor rd2 = StreamConfigurationHelper.mkTradeMessageDescriptor(marketMsgDescriptor,
	            "", 840, FloatDataType.ENCODING_FIXED_DOUBLE, FloatDataType.ENCODING_FIXED_DOUBLE);

        MessageSourceMultiplexer<InstrumentMessage> dataSource =
                new MessageSourceMultiplexer<InstrumentMessage>();
        dataSource.add(new TDBRunner.TradesGenerator(new GregorianCalendar(2009, 1, 1), (int) BarMessage.BAR_MINUTE, 10));
        dataSource.add(new TDBRunner.BBOGenerator(new GregorianCalendar(2009, 1, 1), (int) BarMessage.BAR_MINUTE, 10));

        DXTickStream source = createStream(tdb,
                StreamOptions.polymorphic(StreamScope.DURABLE, "111", "111", 0, rd1, rd2), dataSource);

        marketMsgDescriptor =
            StreamConfigurationHelper.mkMarketMessageDescriptor (null);

        RecordClassDescriptor rd3 = StreamConfigurationHelper.mkBBOMessageDescriptor(marketMsgDescriptor,
                true, null, null, FloatDataType.ENCODING_FIXED_DOUBLE, FloatDataType.ENCODING_FIXED_DOUBLE);
        RecordClassDescriptor rd4 = StreamConfigurationHelper.mkTradeMessageDescriptor(marketMsgDescriptor,
	            null, null, FloatDataType.ENCODING_FIXED_DOUBLE, FloatDataType.ENCODING_FIXED_DOUBLE);

        DXTickStream target = tdb.createStream("222",
                StreamOptions.polymorphic(StreamScope.DURABLE, "222", null, 0, rd3, rd4));

        // convert messages
        MetaDataChange change = getChanges(target, source);
        ClassDescriptorChange rd3c = change.getChange(null, rd3);

        AbstractFieldChange[] changes = rd3c.getFieldChanges(null, rd3.getField("currencyCode"));
        ((FieldModifierChange)changes[0]).setInitialValue("555");

        changes = rd3c.getFieldChanges(null, rd3.getField("bidExchangeId"));
        ((FieldModifierChange)changes[0]).setInitialValue(null);
        changes = rd3c.getFieldChanges(null, rd3.getField("offerExchangeId"));
        ((FieldModifierChange)changes[0]).setInitialValue(null);

        ClassDescriptorChange rd4c = change.getChange(null, rd4);
        changes = rd4c.getFieldChanges(null, rd4.getField("exchangeId"));
        ((FieldModifierChange)changes[0]).setInitialValue("22");

        changes = rd4c.getFieldChanges(null, rd4.getField("currencyCode"));
        ((FieldModifierChange)changes[0]).setInitialValue("555");

        SchemaConverter converter = new SchemaConverter(change);

        TickCursor cursor = source.select(0, new SelectionOptions(true, false));
        TickLoader loader = target.createLoader(new LoadingOptions(true));

        try {
            converter.convert(cursor, loader);
        } finally {
            Util.close(loader);
            Util.close(cursor);
        }

        TickCursor cursor1 = null;
        TickCursor cursor2 = null;
        try {
            cursor1 = target.select(0, new SelectionOptions(false, false));
            cursor2 = source.select(0, new SelectionOptions(false, false));

            while (true) {
                if (cursor1.next() && cursor2.next())
                    checkEquals(cursor1.getMessage(), cursor2.getMessage());
                else
                    break;
            }
        } finally {
            Util.close(cursor1);
            Util.close(cursor2);
        }
    }

    @Test
    public void Test_SeveralTypes() {
        DXTickDB tdb = getTickDb();

        RecordClassSet s1 = new RecordClassSet();
        RecordClassDescriptor marketMsgDescriptor =
            StreamConfigurationHelper.mkMarketMessageDescriptor (9);

        RecordClassDescriptor rd1 = StreamConfigurationHelper.mkBarMessageDescriptor(marketMsgDescriptor,
                "1", 9, FloatDataType.ENCODING_FIXED_DOUBLE, FloatDataType.ENCODING_FIXED_DOUBLE);
        RecordClassDescriptor rd2 = StreamConfigurationHelper.mkTradeMessageDescriptor(marketMsgDescriptor,
	            "1", 9, FloatDataType.ENCODING_FIXED_DOUBLE, FloatDataType.ENCODING_FIXED_DOUBLE);
        s1.addContentClasses(rd1, rd2);

        MessageSourceMultiplexer<InstrumentMessage> dataSource =
                new MessageSourceMultiplexer<InstrumentMessage>();
        dataSource.add(new TDBRunner.TradesGenerator(new GregorianCalendar(2009, 1, 1), (int) BarMessage.BAR_MINUTE, 10));
        dataSource.add(new TDBRunner.BarsGenerator(new GregorianCalendar(2009, 1, 1), (int) BarMessage.BAR_MINUTE, 10));

        DXTickStream source = createStream(tdb,
                StreamOptions.polymorphic(StreamScope.DURABLE, "Test_SeveralTypes",
                        "Test_SeveralTypes", 0, s1.getTopTypes()), dataSource);

        RecordClassDescriptor d2 = StreamConfigurationHelper.mkBarMessageDescriptor(null,
                "", 840, FloatDataType.ENCODING_FIXED_FLOAT, FloatDataType.ENCODING_FIXED_FLOAT);

        DXTickStream target = tdb.createStream("Test_SeveralTypes.t",
                StreamOptions.fixedType(StreamScope.DURABLE, "Test_SeveralTypes.t", null, 0, d2));

        // convert messages
        MetaDataChange change = getChanges(target, source);

        SchemaConverter converter = new SchemaConverter(change);

        TickCursor cursor = source.select(0, new SelectionOptions(true, false));
        TickLoader loader = target.createLoader(new LoadingOptions(true));

        try {
            converter.convert(cursor, loader);
        } finally {
            Util.close(loader);
            Util.close(cursor);            
        }

        cursor = target.select(0, new SelectionOptions(true, false));
        int count = 0;
        while (cursor.next())
            count++;
        assertTrue(count == 10);
        cursor.close();
    }

    @Test
    public void testCDOrderChange() {
        DXTickDB tdb = getTickDb();
        
        RecordClassDescriptor marketMsgDescriptor =
            StreamConfigurationHelper.mkMarketMessageDescriptor (9);

        RecordClassDescriptor rd1 = StreamConfigurationHelper.mkBarMessageDescriptor(marketMsgDescriptor,
                "1", 9, FloatDataType.ENCODING_FIXED_DOUBLE, FloatDataType.ENCODING_FIXED_DOUBLE);
        RecordClassDescriptor rd2 = StreamConfigurationHelper.mkTradeMessageDescriptor(marketMsgDescriptor,
	            "1", 9, FloatDataType.ENCODING_FIXED_DOUBLE, FloatDataType.ENCODING_FIXED_DOUBLE);

        MessageSourceMultiplexer<InstrumentMessage> dataSource =
                new MessageSourceMultiplexer<InstrumentMessage>();
        dataSource.add(new TDBRunner.TradesGenerator(new GregorianCalendar(2009, 1, 1), (int) BarMessage.BAR_MINUTE, 10));
        dataSource.add(new TDBRunner.BarsGenerator(new GregorianCalendar(2009, 1, 1), (int) BarMessage.BAR_MINUTE, 10));

        StreamOptions options = new StreamOptions(StreamScope.DURABLE, "testCDOrderChange.111", null, 0);
        options.setPolymorphic(rd1, rd2);
        DXTickStream source = createStream(tdb, options, dataSource);

        marketMsgDescriptor = StreamConfigurationHelper.mkMarketMessageDescriptor (9);
        RecordClassDescriptor rd11 = StreamConfigurationHelper.mkBarMessageDescriptor(marketMsgDescriptor,
                "1", 9, FloatDataType.ENCODING_FIXED_DOUBLE, FloatDataType.ENCODING_FIXED_DOUBLE);
        RecordClassDescriptor rd12 = StreamConfigurationHelper.mkTradeMessageDescriptor(marketMsgDescriptor,
	            "1", 9, FloatDataType.ENCODING_FIXED_DOUBLE, FloatDataType.ENCODING_FIXED_DOUBLE);

        options = new StreamOptions(StreamScope.DURABLE, "testCDOrderChange.222", null, 0);
        options.setPolymorphic(rd12, rd11);
        DXTickStream target = tdb.createStream(options.name, options);

        // convert messages
        MetaDataChange change = getChanges(target, source);

        assertEquals(SchemaChange.Impact.DataConvert, change.getChangeImpact());

        SchemaConverter converter = new SchemaConverter(change);

        TickCursor cursor = source.select(0, new SelectionOptions(true, false));
        TickLoader loader = target.createLoader(new LoadingOptions(true));

        try {
            converter.convert(cursor, loader);
        } finally {
            Util.close(loader);
            Util.close(cursor);
        }

        TickCursor cursor1 = null;
        TickCursor cursor2 = null;
        try {
            cursor1 = target.select(0, new SelectionOptions(false, false));
            cursor2 = source.select(0, new SelectionOptions(false, false));

            while  (cursor2.next()) {
                assertTrue(cursor1.next());
                checkEquals(cursor2.getMessage(), cursor1.getMessage());
            }
        } finally {
            Util.close(cursor1);
            Util.close(cursor2);
        }
    }

    public void Test_Ignore() {
        DXTickDB tdb = getTickDb();

        DXTickStream source = TickDBCreator.createBarsStream(tdb, "Test_SchemaConverter.Test_Ignore.src");

        RecordClassDescriptor marketMsgDescriptor =
            StreamConfigurationHelper.mkMarketMessageDescriptor(null);

        final String            name = BarMessage.class.getName ();
        final DataField []      fields = {                
            new NonStaticDataField ("close", "Close", new IntegerDataType (IntegerDataType.ENCODING_INT32, false, 0, 10)),
            new NonStaticDataField ("open", "Open", new FloatDataType (FloatDataType.ENCODING_FIXED_FLOAT, true)),
            new NonStaticDataField ("high", "High", new FloatDataType (FloatDataType.ENCODING_FIXED_FLOAT, true)),
            new NonStaticDataField ("low", "Low", new FloatDataType (FloatDataType.ENCODING_FIXED_FLOAT, true)),
            new NonStaticDataField ("volume", "Volume", new FloatDataType (FloatDataType.ENCODING_FIXED_FLOAT, true))
        };
        
        RecordClassDescriptor d2 =  new RecordClassDescriptor (
            name, name, false,
            marketMsgDescriptor,
            fields
        );

        DXTickStream target = tdb.createStream("Test_SchemaConverter.Test_Ignore.t",
                StreamOptions.fixedType(StreamScope.DURABLE, "Test_SchemaConverter.Test_Ignore.t", null, 0, d2));

        MetaDataChange change = getChanges(target, source);
        AbstractFieldChange[] changes = change.getChange(null, d2).getFieldChanges(null, d2.getField("close"));
        ((FieldTypeChange)changes[0]).setIgnoreErrors();

        TickCursor cursor = source.select(0, new SelectionOptions(true, false));
        TickLoader loader = target.createLoader(new LoadingOptions(true));

        try {
            new SchemaConverter(change).convert(cursor, loader);
        } finally {
            loader.close();
            cursor.close();
        }

        cursor = target.select(0, new SelectionOptions(true, false));
        assertTrue(!cursor.next());
        cursor.close();
    }

    @Test
    public void Test_OutOfRange() {
        DXTickDB tdb = getTickDb();

        DXTickStream source = TickDBCreator.createBarsStream(tdb, "Test_SchemaConverter.Test_OutOfRange.src");
        RecordClassDescriptor marketMsgDescriptor =
            StreamConfigurationHelper.mkMarketMessageDescriptor(null);

        final String            name = BarMessage.class.getName ();
        final DataField []      fields = {
            new NonStaticDataField ("close", "Close", new IntegerDataType (IntegerDataType.ENCODING_INT32, true, 0, 10)),
            new NonStaticDataField ("open", "Open", new FloatDataType (FloatDataType.ENCODING_FIXED_FLOAT, true)),
            new NonStaticDataField ("high", "High", new FloatDataType (FloatDataType.ENCODING_FIXED_FLOAT, true)),
            new NonStaticDataField ("low", "Low", new FloatDataType (FloatDataType.ENCODING_FIXED_FLOAT, true)),
            new NonStaticDataField ("volume", "Volume", new FloatDataType (FloatDataType.ENCODING_FIXED_FLOAT, true))
        };

        RecordClassDescriptor d2 =  new RecordClassDescriptor (
            name, name, false,
            marketMsgDescriptor,
            fields
        );

        DXTickStream target = tdb.createStream("Test_SchemaConverter.Test_OutOfRange.t",
                StreamOptions.fixedType(StreamScope.DURABLE, "Test_SchemaConverter.Test_OutOfRange.t", null, 0, d2));

        MetaDataChange change = getChanges(target, source);
        AbstractFieldChange[] changes = change.getChange(null, d2).getFieldChanges(null, d2.getField("close"));
        ((FieldTypeChange)changes[0]).setDefaultValue("0");
        
        assertTrue(((FieldTypeChange)changes[0]).isDefaultValueRequired());

        TickCursor cursor = source.select(0, new SelectionOptions(true, false));
        TickLoader loader = target.createLoader(new LoadingOptions(true));

        try {
            new SchemaConverter(change).convert(cursor, loader);
        } finally {
            loader.close();
            cursor.close();
        }

        cursor = target.select(0, new SelectionOptions(true, false));
        assertTrue(cursor.next());        
        assertTrue(cursor.getMessage().toString().contains("close:0"));
        cursor.close();
    }

    public static MetaDataChange getChanges(DXTickStream target, DXTickStream ... source) {
        SimpleClassSet in = new SimpleClassSet ();
        RecordClassSet out = new RecordClassSet ();
        MetaDataChange.ContentType inType;
        MetaDataChange.ContentType outType;

        if (source.length > 1) {
            inType = MetaDataChange.ContentType.Mixed;
            
            for (DXTickStream stream : source)
                in.addContentClasses(StreamConfigurationHelper.getClassDescriptors(stream));
        }
        else if (source[0].isFixedType()) {
            inType = MetaDataChange.ContentType.Fixed;
            in.addContentClasses(source[0].getFixedType ());
        } else {
            inType = MetaDataChange.ContentType.Polymorphic;
            in.addContentClasses(source[0].getPolymorphicDescriptors ());
        }

        if (target.isFixedType ()) {
            outType = MetaDataChange.ContentType.Fixed;
            out.addContentClasses (target.getFixedType ());
        } else {
            outType = MetaDataChange.ContentType.Polymorphic;
            out.addContentClasses (target.getPolymorphicDescriptors ());
        }

        return SchemaAnalyzer.DEFAULT.getChanges (in, inType, out, outType);
    }


    public static DXTickStream createBarsStream(DXTickDB tdb,
                                     String name,
                                     String exchangeCode,
                                     Integer currencyCode, 
                                     MessageSource source) {

        RecordClassDescriptor rd = StreamConfigurationHelper.mkBarMessageDescriptor(null,
                exchangeCode, currencyCode, FloatDataType.ENCODING_SCALE_AUTO, FloatDataType.ENCODING_SCALE_AUTO);

        DXTickStream            stream = tdb.createStream(name,
                StreamOptions.fixedType(StreamScope.DURABLE, name, name, 0, rd));
        TickLoader      loader = stream.createLoader ();

        try {
             while (source.next()) {
                loader.send((InstrumentMessage) source.getMessage());
             }
        } finally {
            Util.close(loader);
        }

        return stream;
    }

    public static DXTickStream createStream(DXTickDB tdb,
                                     StreamOptions options,
                                     MessageSource source) {

        DXTickStream            stream = tdb.createStream(options.name, options);

        TickLoader      loader = stream.createLoader ();

        try {
             while (source.next()) {
                loader.send((InstrumentMessage) source.getMessage());
             }
        } finally {
            Util.close(loader);
        }

        return stream;
    }

    @Test
    public void Test_NoChanges() {
        DXTickDB tdb = getTickDb();

        RecordClassDescriptor d = StreamConfigurationHelper.mkTradeMessageDescriptor(null,
                "", null, FloatDataType.ENCODING_SCALE_AUTO, FloatDataType.ENCODING_SCALE_AUTO);

        StreamOptions options = StreamOptions.fixedType(StreamScope.DURABLE, "Test_SchemaConverter.Test_NoChanges.1", null, 0, d);
        DXTickStream source = createStream(tdb, options,
                new TDBRunner.TradesGenerator(new GregorianCalendar(2009, 1, 1), (int) BarMessage.BAR_MINUTE, 100));

        d = StreamConfigurationHelper.mkTradeMessageDescriptor(null,
                "", null, FloatDataType.ENCODING_SCALE_AUTO, FloatDataType.ENCODING_SCALE_AUTO);

        DXTickStream target = tdb.createStream("Test_SchemaConverter.Test_NoChanges.2",
                StreamOptions.fixedType(StreamScope.DURABLE, "Test_SchemaConverter.Test_NoChanges.2", null, 0, d));

        MetaDataChange change = getChanges(target, source);
        assertEquals(SchemaChange.Impact.None, change.getChangeImpact());
        assertTrue(change.changes.size() == 0);

        TickCursor cursor = source.select(0, new SelectionOptions(true, false));
        TickLoader loader = target.createLoader(new LoadingOptions(true));

        try {
            new SchemaConverter(change).convert(cursor, loader);

        } finally {
            loader.close();
            cursor.close();
        }

        TickCursor cursor1 = null;
        TickCursor cursor2 = null;
        try {
            cursor1 = target.select(0, new SelectionOptions(false, false));
            cursor2 = source.select(0, new SelectionOptions(false, false));

            while (true) {
                if (cursor1.next() && cursor2.next())
                    assertTrue(Util.xequals(cursor1.getMessage().toString(), cursor2.getMessage().toString()));
                else
                    break;
            }
        } finally {
            Util.close(cursor1);
            Util.close(cursor2);
        }
    }

    @Test
    public void Test_NoChangesDecimals() {
        DXTickDB tdb = getTickDb();

        RecordClassDescriptor d = StreamConfigurationHelper.mkTradeMessageDescriptor(null,
                "", null, FloatDataType.ENCODING_DECIMAL64, FloatDataType.ENCODING_DECIMAL64);

        StreamOptions options = StreamOptions.fixedType(StreamScope.DURABLE, "Test_SchemaConverter.Test_NoChanges.D1", null, 0, d);
        DXTickStream source = createStream(tdb, options,
                new TDBRunner.TradesGenerator(new GregorianCalendar(2009, 1, 1), (int) BarMessage.BAR_MINUTE, 100));

        d = StreamConfigurationHelper.mkTradeMessageDescriptor(null,
                "", null, FloatDataType.ENCODING_DECIMAL64, FloatDataType.ENCODING_DECIMAL64);

        DXTickStream target = tdb.createStream("Test_SchemaConverter.Test_NoChanges.D2",
                StreamOptions.fixedType(StreamScope.DURABLE, "Test_SchemaConverter.Test_NoChanges.D2", null, 0, d));

        MetaDataChange change = getChanges(target, source);
        assertEquals(SchemaChange.Impact.None, change.getChangeImpact());
        assertTrue(change.changes.size() == 0);

        TickCursor cursor = source.select(0, new SelectionOptions(true, false));
        TickLoader loader = target.createLoader(new LoadingOptions(true));

        try {
            new SchemaConverter(change).convert(cursor, loader);

        } finally {
            loader.close();
            cursor.close();
        }

        TickCursor cursor1 = null;
        TickCursor cursor2 = null;
        try {
            cursor1 = target.select(0, new SelectionOptions(false, false));
            cursor2 = source.select(0, new SelectionOptions(false, false));

            while (true) {
                if (cursor1.next() && cursor2.next())
                    assertTrue(Util.xequals(cursor1.getMessage().toString(), cursor2.getMessage().toString()));
                else
                    break;
            }
        } finally {
            Util.close(cursor1);
            Util.close(cursor2);
        }
    }

    @Test
    public void Test_StreamConvert() throws Throwable {

        DXTickDB tdb = runner.getServerDb();

        DXTickStream source = TickDBCreator.createBarsStream(tdb, "Test_StreamConvert");
        source.enableVersioning();

        DXTickStream etalon = TickDBCreator.createBarsStream(tdb, "Test_StreamConvert.src");

        RecordClassDescriptor d2 = StreamConfigurationHelper.mkBarMessageDescriptor(null,
                "",
                null,
                FloatDataType.ENCODING_FIXED_DOUBLE,
                FloatDataType.ENCODING_FIXED_DOUBLE);

        covert(source, d2, etalon);

        assertEquals(source.getFixedType().getGuid(), d2.getGuid());

        RecordClassDescriptor d3 = StreamConfigurationHelper.mkBarMessageDescriptor(null,
                "",
                0,
                FloatDataType.ENCODING_FIXED_DOUBLE,
                FloatDataType.ENCODING_FIXED_DOUBLE);

        Thread.sleep(1000);

        covert(source, d3, etalon);

        assertEquals(source.getFixedType().getGuid(), d3.getGuid());
    }

    @Test
    public void Test_StreamConvert1() throws Throwable {

        DXTickDB tdb = runner.getServerDb();

        DXTickStream source = TickDBCreator.createBarsStream(tdb, "Test_StreamConvert1");
        source.enableVersioning();

        source.truncate(source.getTimeRange()[1]);
        DXTickStream etalon = TickDBCreator.createBarsStream(tdb, "Test_StreamConvert1.src");
        etalon.truncate(etalon.getTimeRange()[1]);

        RecordClassDescriptor d2 = StreamConfigurationHelper.mkBarMessageDescriptor(null,
                "",
                null,
                FloatDataType.ENCODING_FIXED_DOUBLE,
                FloatDataType.ENCODING_FIXED_DOUBLE);

        covert(source, d2, etalon);

        assertEquals(source.getFixedType().getGuid(), d2.getGuid());

        RecordClassDescriptor d3 = StreamConfigurationHelper.mkBarMessageDescriptor(null,
                "",
                0,
                FloatDataType.ENCODING_FIXED_DOUBLE,
                FloatDataType.ENCODING_FIXED_DOUBLE);

        Thread.sleep(1000);

        covert(source, d3, etalon);

        assertEquals(source.getFixedType().getGuid(), d3.getGuid());
    }

    public static void covert(DXTickStream source, RecordClassDescriptor rcd, DXTickStream etalon) throws Throwable {

        RecordClassSet in = new RecordClassSet ();
        in.addContentClasses(source.getFixedType());
        RecordClassSet out = new RecordClassSet ();
        out.addContentClasses(rcd);

        StreamMetaDataChange change = SchemaAnalyzer.DEFAULT.getChanges
                (in, MetaDataChange.ContentType.Fixed,  out, MetaDataChange.ContentType.Fixed);
        source.execute(new SchemaChangeTask(change));

        boolean complete = false;
        while (!complete) {
            BackgroundProcessInfo process = source.getBackgroundProcess();
            complete = process != null && process.isFinished();
            Thread.sleep(300);

            if (process != null && process.error != null)
                throw process.error;
        }

        TickCursor cursor1 = etalon.select(0, new SelectionOptions(false, false));
        TickCursor cursor2 = source.select(0, new SelectionOptions(false, false));

        while (true) {
            boolean oneNext = cursor1.next();
            boolean secondNext = cursor2.next();

            assertEquals(oneNext, secondNext);

            if (oneNext && secondNext)
                checkEquals(cursor1.getMessage(), cursor2.getMessage());
            else
                break;
        }
        Util.close(cursor1);
        Util.close(cursor2);
    }

    @Test
    public void testNewCD() throws InterruptedException {        
        DXTickStream source = generateTickStream("testNewCD");        

        RecordClassSet in = new RecordClassSet ();
        in.addContentClasses(source.getPolymorphicDescriptors());

        RecordClassDescriptor descriptor = StreamConfigurationHelper.mkEventMessageDescriptor();
        
        RecordClassSet out = new RecordClassSet ();
        out.addContentClasses(source.getPolymorphicDescriptors());
        out.addContentClasses(descriptor);

        StreamMetaDataChange change = SchemaAnalyzer.DEFAULT.getChanges
                (in, MetaDataChange.ContentType.Polymorphic, out, MetaDataChange.ContentType.Polymorphic);
        assert change.getChangeImpact() == SchemaChange.Impact.None;
        
        source.execute(new SchemaChangeTask(change));

        boolean complete = false;
        while (!complete) {
            BackgroundProcessInfo process = source.getBackgroundProcess();
            complete = process != null && process.isFinished();
            Thread.sleep(100);
        }
    }
    
    public DXTickStream generateTickStream(String name) {
        DXTickDB tdb = getTickDb();

        RecordClassDescriptor marketMsgDescriptor =
                StreamConfigurationHelper.mkMarketMessageDescriptor (840);

        RecordClassDescriptor rd1 = StreamConfigurationHelper.mkBBOMessageDescriptor(marketMsgDescriptor,
            true, "", 840, FloatDataType.ENCODING_FIXED_DOUBLE, FloatDataType.ENCODING_FIXED_DOUBLE);
        RecordClassDescriptor rd2 = StreamConfigurationHelper.mkTradeMessageDescriptor(marketMsgDescriptor,
            "", 840, FloatDataType.ENCODING_FIXED_DOUBLE, FloatDataType.ENCODING_FIXED_DOUBLE);
        RecordClassDescriptor rd3 = StreamConfigurationHelper.mkBarMessageDescriptor(marketMsgDescriptor,
            "", 840, FloatDataType.ENCODING_FIXED_DOUBLE, FloatDataType.ENCODING_FIXED_DOUBLE);

        MessageSourceMultiplexer<InstrumentMessage> dataSource = new MessageSourceMultiplexer<InstrumentMessage>();

        int count = 10000;
        String[] tickers = new String[] {"ES1", "ES2", "ES3", "ES4"};
        dataSource.add(
                new TDBRunner.TradesGenerator(new GregorianCalendar(2009, 1, 1), (int) BarMessage.BAR_MINUTE, count, tickers));
        dataSource.add(new TDBRunner.BBOGenerator(new GregorianCalendar(2009, 1, 1), (int) BarMessage.BAR_MINUTE, count, tickers));
        dataSource.add(new TDBRunner.BarsGenerator(new GregorianCalendar(2009, 1, 1), (int) BarMessage.BAR_MINUTE, count, tickers));

        return createStream(tdb,
                StreamOptions.polymorphic(StreamScope.DURABLE, name, name, 0, rd1, rd2, rd3), dataSource);        
    }

    private static final String ENUMS_STREAM_KEY = "testEnumsStream";

    private DXTickStream createEnumsTestStream(RecordClassDescriptor rcd) {
        StreamOptions streamOptions = new StreamOptions(StreamScope.DURABLE, ENUMS_STREAM_KEY, ENUMS_STREAM_KEY,0);
        streamOptions.setFixedType(rcd);
        return getTickDb().createStream(ENUMS_STREAM_KEY, streamOptions);
    }

    private DXTickStream getEnumsTestStream() {
        return getTickDb().getStream(ENUMS_STREAM_KEY);
    }

    private void deleteEnumsTestStream() {
        getTickDb().getStream(ENUMS_STREAM_KEY).delete();
    }

    private TickCursor createEnumsTestStreamCursor() {
        return getTickDb().select(Long.MIN_VALUE, new SelectionOptions(true, false), getEnumsTestStream());
    }

    @Test
    public void testEnumsAddValue() {
        String enumName = "TestEnum";
        String rcdName = "TestDescriptor";
        String fieldName = "field";
        String[] values1 = {"VALUE1", "VALUE2"};
        String[] values2 = {"VALUE1", "VALUE2", "VALUE3"};
        EnumDataType edt1 = createTestEnumDT(true, enumName, values1);
        EnumDataType edt2 = createTestEnumDT(true, enumName, values2);

        RecordClassDescriptor rcd1 = createTestEnumRCD(rcdName, fieldName, edt1);
        RecordClassDescriptor rcd2 = createTestEnumRCD(rcdName, fieldName, edt2);

        ObjectArrayList<RawMessage> messages = createMessages(rcd1, fieldName, values1);

        MetaDataChange change = getChange(rcd1, rcd2);
        assertTrue(change.isAcceptable());
        SchemaConverter converter = new SchemaConverter(change);

        for (int i = 0; i < messages.size(); i++) {
            RawMessage converted = converter.convert(messages.get(i));
            assertNotNull(converted);
            Map<String, Object> map = rawMessageHelper.getValues(converted);
            assertEquals(map.get(fieldName), values1[i]);
        }
    }

    @Test
    public void testEnumsAddValueServer() {
        String enumName = "TestEnum";
        String rcdName = "TestDescriptor";
        String fieldName = "field";
        String[] values1 = {"VALUE1", "VALUE2"};
        String[] values2 = {"VALUE1", "VALUE2", "VALUE3"};
        EnumDataType edt1 = createTestEnumDT(true, enumName, values1);
        EnumDataType edt2 = createTestEnumDT(true, enumName, values2);

        RecordClassDescriptor rcd1 = createTestEnumRCD(rcdName, fieldName, edt1);
        RecordClassDescriptor rcd2 = createTestEnumRCD(rcdName, fieldName, edt2);

        DXTickStream stream = createEnumsTestStream(rcd1);
        try {
            ObjectArrayList<RawMessage> messages = createMessages(rcd1, fieldName, values1);
            try (TickLoader loader = stream.createLoader(new LoadingOptions(true))) {
                messages.forEach(loader::send);
            }

            StreamMetaDataChange change = getChange(rcd1, rcd2);
            assertTrue(change.isAcceptable());

            SchemaChangeTask task = new SchemaChangeTask(change);

            task.setBackground(false);
            stream.execute(task);

            try (TickCursor cursor = createEnumsTestStreamCursor()) {
                for (int i = 0; i < messages.size(); i++) {
                    cursor.next();
                    RawMessage converted = (RawMessage) cursor.getMessage();
                    System.out.println(converted);
                    System.out.println(messages.get(i));
                    assertNotNull(converted);
                    Map<String, Object> map = rawMessageHelper.getValues(converted);
                    assertEquals(map.get(fieldName), values1[i]);
                    System.out.println(values1[i]);
                }
            }
        } finally {
            deleteEnumsTestStream();
        }
    }

    @Test
    public void testEnumsInnerAddValue() {
        String enumName = "TestEnum";
        String rcdName = "TestDescriptor";
        String innerRcd = "InnerTestDescriptor";
        String fieldName = "field";
        String enumFieldName = "enumField";
        String[] values1 = {"VALUE1", "VALUE2"};
        String[] values2 = {"VALUE1", "VALUE2", "VALUE3"};
        EnumDataType edt1 = createTestEnumDT(true, enumName, values1);
        EnumDataType edt2 = createTestEnumDT(true, enumName, values2);

        RecordClassDescriptor rcd1 = createTestInnerEnumRCD(rcdName, fieldName, innerRcd, enumFieldName, edt1);
        RecordClassDescriptor rcd2 = createTestInnerEnumRCD(rcdName, fieldName, innerRcd, enumFieldName, edt2);

        ObjectArrayList<RawMessage> messages = createMessagesInnerEnum(rcd1, fieldName, innerRcd, enumFieldName, values1);

        MetaDataChange change = getChange(rcd1, rcd2);
        assertTrue(change.isAcceptable());
        SchemaConverter converter = new SchemaConverter(change);

        for (int i = 0; i < messages.size(); i++) {
            RawMessage converted = converter.convert(messages.get(i));
            assertNotNull(converted);
            Map<String, Object> map = rawMessageHelper.getValues(converted);
            assertEquals(((Map<String, Object>) map.get(fieldName)).get(enumFieldName), values1[i]);
        }
    }

    @Test
    public void testEnumsRemoveValue() {
        String enumName = "TestEnum";
        String rcdName = "TestDescriptor";
        String fieldName = "field";
        String[] values1 = {"VALUE1", "VALUE2", "VALUE3"};
        String[] values2 = {"VALUE1", "VALUE2"};
        EnumDataType edt1 = createTestEnumDT(true, enumName, values1);
        EnumDataType edt2 = createTestEnumDT(true, enumName, values2);

        RecordClassDescriptor rcd1 = createTestEnumRCD(rcdName, fieldName, edt1);
        RecordClassDescriptor rcd2 = createTestEnumRCD(rcdName, fieldName, edt2);

        ObjectArrayList<RawMessage> messages = createMessages(rcd1, fieldName, values1);

        MetaDataChange change = getChange(rcd1, rcd2);
        assertTrue(change.isAcceptable());
        SchemaConverter converter = new SchemaConverter(change);

        for (int i = 0; i < messages.size() - 1; i++) {
            RawMessage converted = converter.convert(messages.get(i));
            assertNotNull(converted);
            Map<String, Object> map = rawMessageHelper.getValues(converted);
            assertEquals(map.get(fieldName), values1[i]);
        }
        RawMessage converted = converter.convert(messages.get(messages.size() - 1));
        assertNotNull(converted);
        Map<String, Object> map = rawMessageHelper.getValues(converted);
        assertNull(map.get(fieldName));
    }

    @Test
    public void testEnumsRemoveValueServer() {
        String enumName = "TestEnum";
        String rcdName = "TestDescriptor";
        String fieldName = "field";
        String[] values1 = {"VALUE1", "VALUE2", "VALUE3"};
        String[] values2 = {"VALUE1", "VALUE2"};
        EnumDataType edt1 = createTestEnumDT(true, enumName, values1);
        EnumDataType edt2 = createTestEnumDT(true, enumName, values2);

        RecordClassDescriptor rcd1 = createTestEnumRCD(rcdName, fieldName, edt1);
        RecordClassDescriptor rcd2 = createTestEnumRCD(rcdName, fieldName, edt2);

        DXTickStream stream = createEnumsTestStream(rcd1);
        try {
            ObjectArrayList<RawMessage> messages = createMessages(rcd1, fieldName, values1);
            try (TickLoader loader = stream.createLoader(new LoadingOptions(true))) {
                messages.forEach(loader::send);
            }

            StreamMetaDataChange change = getChange(rcd1, rcd2);
            assertTrue(change.isAcceptable());

            SchemaChangeTask task = new SchemaChangeTask(change);

            task.setBackground(false);
            stream.execute(task);

            try (TickCursor cursor = createEnumsTestStreamCursor()) {
                for (int i = 0; i < messages.size() - 1; i++) {
                    cursor.next();
                    RawMessage converted = (RawMessage) cursor.getMessage();
                    assertNotNull(converted);
                    Map<String, Object> map = rawMessageHelper.getValues(converted);
                    assertEquals(map.get(fieldName), values1[i]);
                }
                cursor.next();
                RawMessage converted = (RawMessage) cursor.getMessage();
                assertNotNull(converted);
                Map<String, Object> map = rawMessageHelper.getValues(converted);
                assertNull(map.get(fieldName));
            }
        } finally {
            deleteEnumsTestStream();
        }
    }

    @Test
    public void testEnumsInnerRemoveValue() {
        String enumName = "TestEnum";
        String rcdName = "TestDescriptor";
        String innerRcd = "InnerTestDescriptor";
        String fieldName = "field";
        String enumFieldName = "enumField";
        String[] values1 = {"VALUE1", "VALUE2", "VALUE3"};
        String[] values2 = {"VALUE1", "VALUE2"};
        EnumDataType edt1 = createTestEnumDT(true, enumName, values1);
        EnumDataType edt2 = createTestEnumDT(true, enumName, values2);

        RecordClassDescriptor rcd1 = createTestInnerEnumRCD(rcdName, fieldName, innerRcd, enumFieldName, edt1);
        RecordClassDescriptor rcd2 = createTestInnerEnumRCD(rcdName, fieldName, innerRcd, enumFieldName, edt2);

        ObjectArrayList<RawMessage> messages = createMessagesInnerEnum(rcd1, fieldName, innerRcd, enumFieldName, values1);

        MetaDataChange change = getChange(rcd1, rcd2);
        assertTrue(change.isAcceptable());
        SchemaConverter converter = new SchemaConverter(change);

        for (int i = 0; i < messages.size() - 1; i++) {
            RawMessage converted = converter.convert(messages.get(i));
            assertNotNull(converted);
            Map<String, Object> map = rawMessageHelper.getValues(converted);
            assertEquals(((Map<String, Object>) map.get(fieldName)).get(enumFieldName), values1[i]);
        }
        RawMessage converted = converter.convert(messages.get(messages.size() - 1));
        assertNotNull(converted);
        Map<String, Object> map = rawMessageHelper.getValues(converted);
        assertNull(((Map<String, Object>) map.get(fieldName)).get(enumFieldName));
    }

    @Test
    public void testEnumRemoveNonNullable() {
        String enumName = "TestEnum";
        String rcdName = "TestDescriptor";
        String fieldName = "field";
        String[] values1 = {"VALUE1", "VALUE2", "VALUE3", "VALUE4"};
        String[] values2 = {"VALUE1", "VALUE2", "VALUE3"};
        EnumDataType edt1 = createTestEnumDT(false, enumName, values1);
        EnumDataType edt2 = createTestEnumDT(false, enumName, values2);

        RecordClassDescriptor rcd1 = createTestEnumRCD(rcdName, fieldName, edt1);
        RecordClassDescriptor rcd2 = createTestEnumRCD(rcdName, fieldName, edt2);

        MetaDataChange change = getChange(rcd1, rcd2);
        assertFalse(change.isAcceptable());
    }

    /**
     * Rename test
     */
    @Test
    public void testEnumsRenameValue() {
        String enumName = "TestEnum";
        String rcdName = "TestDescriptor";
        String fieldName = "field";
        String[] values1 = {"VALUE1", "VALUE2", "VALUE3", "VALUE4"};
        String[] values2 = {"VALUE4", "VALUE3", "VALUE2", "VALUE1", "VALUE5"};
        EnumDataType edt1 = createTestEnumDT(true, enumName, values1);
        EnumDataType edt2 = createTestEnumDT(true, enumName, values2);

        RecordClassDescriptor rcd1 = createTestEnumRCD(rcdName, fieldName, edt1);
        RecordClassDescriptor rcd2 = createTestEnumRCD(rcdName, fieldName, edt2);

        ObjectArrayList<RawMessage> messages = createMessages(rcd1, fieldName, values1);

        MetaDataChange change = getChange(rcd1, rcd2);
        assertTrue(change.isAcceptable());
        SchemaConverter converter = new SchemaConverter(change);

        for (int i = 0; i < messages.size(); i++) {
            RawMessage converted = converter.convert(messages.get(i));
            assertNotNull(converted);
            Map<String, Object> map = rawMessageHelper.getValues(converted);
            assertEquals(map.get(fieldName), values2[i]);
        }
    }

    @Test
    public void testEnumsRenameValueServer() {
        String enumName = "TestEnum";
        String rcdName = "TestDescriptor";
        String fieldName = "field";
        String[] values1 = {"VALUE1", "VALUE2", "VALUE3", "VALUE4"};
        String[] values2 = {"VALUE4", "VALUE3", "VALUE2", "VALUE1", "VALUE5"};
        EnumDataType edt1 = createTestEnumDT(true, enumName, values1);
        EnumDataType edt2 = createTestEnumDT(true, enumName, values2);

        RecordClassDescriptor rcd1 = createTestEnumRCD(rcdName, fieldName, edt1);
        RecordClassDescriptor rcd2 = createTestEnumRCD(rcdName, fieldName, edt2);

        DXTickStream stream = createEnumsTestStream(rcd1);
        try {
            ObjectArrayList<RawMessage> messages = createMessages(rcd1, fieldName, values1);
            try (TickLoader loader = stream.createLoader(new LoadingOptions(true))) {
                messages.forEach(loader::send);
            }

            StreamMetaDataChange change = getChange(rcd1, rcd2);
            assertTrue(change.isAcceptable());

            SchemaChangeTask task = new SchemaChangeTask(change);

            task.setBackground(false);
            stream.execute(task);

            try (TickCursor cursor = createEnumsTestStreamCursor()) {
                for (int i = 0; i < messages.size(); i++) {
                    cursor.next();
                    RawMessage converted = (RawMessage) cursor.getMessage();
                    assertNotNull(converted);
                    Map<String, Object> map = rawMessageHelper.getValues(converted);
                    assertEquals(map.get(fieldName), values2[i]);
                }
            }
        } finally {
            deleteEnumsTestStream();
        }
    }

    @Test
    public void testEnumsInnerRenameValue() {
        String enumName = "TestEnum";
        String rcdName = "TestDescriptor";
        String innerRcd = "InnerTestDescriptor";
        String fieldName = "field";
        String enumFieldName = "enumField";
        String[] values1 = {"VALUE1", "VALUE2", "VALUE3", "VALUE4"};
        String[] values2 = {"VALUE4", "VALUE3", "VALUE2", "VALUE1", "VALUE5"};
        EnumDataType edt1 = createTestEnumDT(true, enumName, values1);
        EnumDataType edt2 = createTestEnumDT(true, enumName, values2);

        RecordClassDescriptor rcd1 = createTestInnerEnumRCD(rcdName, fieldName, innerRcd, enumFieldName, edt1);
        RecordClassDescriptor rcd2 = createTestInnerEnumRCD(rcdName, fieldName, innerRcd, enumFieldName, edt2);

        ObjectArrayList<RawMessage> messages = createMessagesInnerEnum(rcd1, fieldName, innerRcd, enumFieldName, values1);

        MetaDataChange change = getChange(rcd1, rcd2);
        assertTrue(change.isAcceptable());
        SchemaConverter converter = new SchemaConverter(change);

        for (int i = 0; i < messages.size(); i++) {
            RawMessage converted = converter.convert(messages.get(i));
            assertNotNull(converted);
            Map<String, Object> map = rawMessageHelper.getValues(converted);
            assertEquals(((Map<String, Object>) map.get(fieldName)).get(enumFieldName), values2[i]);
        }
    }

    @Test
    public void testEnumsAddFieldNullable() {
        String enumName = "TestEnum";
        String rcdName = "TestDescriptor";
        String fieldName = "field";
        String[] values = {"VALUE1", "VALUE2", "VALUE3", "VALUE4"};
        EnumDataType edt = createTestEnumDT(true, enumName, values);

        RecordClassDescriptor rcd1 = new RecordClassDescriptor(rcdName, rcdName, false, null);
        RecordClassDescriptor rcd2 = createTestEnumRCD(rcdName, fieldName, edt);

        ObjectArrayList<RawMessage> messages = createMessages(rcd1, fieldName, values);

        MetaDataChange change = getChange(rcd1, rcd2);
        assertTrue(change.isAcceptable());
        SchemaConverter converter = new SchemaConverter(change);

        for (int i = 0; i < messages.size(); i++) {
            RawMessage converted = converter.convert(messages.get(i));
            assertNotNull(converted);
            Map<String, Object> map = rawMessageHelper.getValues(converted);
            assertNull(map.get(fieldName));
        }
    }

    @Test
    public void testEnumsRemoveField() {
        testEnumsRemoveField(false);
        testEnumsRemoveField(true);
    }

    private void testEnumsRemoveField(boolean nullable) {
        String enumName = "TestEnum";
        String rcdName = "TestDescriptor";
        String fieldName = "field";
        String[] values = {"VALUE1", "VALUE2", "VALUE3", "VALUE4"};
        EnumDataType edt = createTestEnumDT(nullable, enumName, values);

        RecordClassDescriptor rcd1 = createTestEnumRCD(rcdName, fieldName, edt);
        RecordClassDescriptor rcd2 = new RecordClassDescriptor(rcdName, rcdName, false, null);

        ObjectArrayList<RawMessage> messages = createMessages(rcd1, fieldName, values);

        MetaDataChange change = getChange(rcd1, rcd2);
        assertTrue(change.isAcceptable());
        SchemaConverter converter = new SchemaConverter(change);

        for (int i = 0; i < messages.size(); i++) {
            RawMessage converted = converter.convert(messages.get(i));
            assertNotNull(converted);
            Map<String, Object> map = rawMessageHelper.getValues(converted);
            assertNull(map.get(fieldName));
        }
    }

    private ObjectArrayList<RawMessage> createMessages(RecordClassDescriptor rcd, String fieldName, String[] values) {

        ObjectArrayList<RawMessage> list = new ObjectArrayList<>(values.length);
        Map<String, Object> map = new HashMap<>(1);
        for (String value : values) {
            map.put(fieldName, value);
            RawMessage raw = new RawMessage(rcd);
            raw.setSymbol("TEST_ENUMS");
            rawMessageHelper.setValues(raw, map);
            list.add(raw);
        }
        return list;
    }

    private ObjectArrayList<RawMessage> createMessagesInnerEnum(RecordClassDescriptor rcd, String fieldName, String innerTypeName, String enumFieldName, String[] values) {
        ObjectArrayList<RawMessage> list = new ObjectArrayList<>(values.length);
        Map<String, Object> map = new HashMap<>(1);
        Map<String, Object> innerMap = new HashMap<>(1);
        innerMap.put("type", innerTypeName);
        map.put(fieldName, innerMap);
        for (String value : values) {
            innerMap.put(enumFieldName, value);
            RawMessage raw = new RawMessage(rcd);
            raw.setSymbol("TEST_ENUMS");
            rawMessageHelper.setValues(raw, map);
            list.add(raw);
        }
        return list;
    }

    public StreamMetaDataChange getChange(RecordClassDescriptor rcd1, RecordClassDescriptor rcd2) {
        RecordClassSet rcs1 = new RecordClassSet(new RecordClassDescriptor[]{rcd1});
        RecordClassSet rcs2 = new RecordClassSet(new RecordClassDescriptor[]{rcd2});
        SchemaAnalyzer analyzer = new SchemaAnalyzer(new SchemaMapping());
        return analyzer.getChanges(rcs1, MetaDataChange.ContentType.Fixed, rcs2, MetaDataChange.ContentType.Fixed);
    }

    private RecordClassDescriptor createTestEnumRCD(String name, String fieldName, EnumDataType type) {
        return new RecordClassDescriptor(name, name, false, null, new NonStaticDataField(fieldName, fieldName, type));
    }

    private RecordClassDescriptor createTestInnerEnumRCD(String name, String objectFieldName, String objectClassName, String enumFieldName, EnumDataType type) {
        RecordClassDescriptor objectRcd = new RecordClassDescriptor(objectClassName, objectClassName, false,
                null, new NonStaticDataField(enumFieldName, enumFieldName, type));
        ClassDataType dataType = new ClassDataType(false, objectRcd);
        return new RecordClassDescriptor(name, name, false, null, new NonStaticDataField(objectFieldName, objectFieldName, dataType));
    }

    private EnumDataType createTestEnumDT(boolean nullable, String name, String ... values) {
        return new EnumDataType(nullable, new EnumClassDescriptor(name, name, values));
    }

    public static void rawCompareStreams(DXTickStream source, DXTickStream target,
                                      String[] types, IdentityKey[] ids) {
        TickCursor cursor1 = null;
        TickCursor cursor2 = null;
        try {

            SelectionOptions options = new SelectionOptions(true, false);

            options.ordered = true;

            cursor1 = source.select(Long.MIN_VALUE, options, types, ids);
            cursor2 = target.select(Long.MIN_VALUE, options, types, ids);

            while (true) {
                if (cursor1.next())
                    Assert.assertTrue("Target cursor has no message, but source has " + cursor1.getMessage(), cursor2.next());
                else
                    break;

                assertEquals(cursor1.getMessage().toString(), cursor2.getMessage().toString());
            }
        } finally {
            Util.close(cursor1);
            Util.close(cursor2);
        }
    }

    public static void compareStreams(DXTickStream source, DXTickStream target,
                                      String[] types, IdentityKey[] ids) {
        TickCursor cursor1 = null;
        TickCursor cursor2 = null;
        try {

            SelectionOptions options = new SelectionOptions(false, false);
            options.channelQOS = ChannelQualityOfService.MAX_THROUGHPUT;

            options.ordered = true;

            cursor1 = source.select(Long.MIN_VALUE, options, types, ids != null && ids.length == 0 ? null : ids);
            cursor2 = target.select(Long.MIN_VALUE, options, types, ids != null && ids.length == 0 ? null : ids);

            while (true) {
                if (cursor1.next())
                    Assert.assertTrue("Target cursor has no message, but source has " + cursor1.getMessage(), cursor2.next());
                else
                    break;

                checkEquals(cursor1.getMessage(), cursor2.getMessage());
            }
        } finally {
            Util.close(cursor1);
            Util.close(cursor2);
        }
    }

    public SchemaConverter      createConverter (DXTickStream dest, DXTickStream... src) {
        final RecordClassSet out = new RecordClassSet ();
        out.addContentClasses(StreamConfigurationHelper.getClassDescriptors(dest));
        MetaDataChange.ContentType outType = dest.isFixedType() ?
                MetaDataChange.ContentType.Fixed : MetaDataChange.ContentType.Polymorphic;

        final SimpleClassSet in = new SimpleClassSet ();
        for (DXTickStream t : src)
            in.addContentClasses(StreamConfigurationHelper.getClassDescriptors(t));

        MetaDataChange.ContentType inType = src.length > 1 ? MetaDataChange.ContentType.Mixed :
                (src[0].isFixedType() ? MetaDataChange.ContentType.Fixed : MetaDataChange.ContentType.Polymorphic );

        final SchemaConverter converter = new SchemaConverter (
                SchemaAnalyzer.DEFAULT.getChanges(in, inType, out, outType));

        if (!converter.canConvert()) {
            System.out.println ("Source and destination streams in not compatible.");
            return (null);
        }

        return (converter);
    }

    public static void checkEquals(InstrumentMessage msg1, InstrumentMessage msg2)
    {
        assertEquals(msg1.getTimeStampMs(), msg2.getTimeStampMs());
        assertEquals(msg1.getSymbol().toString(), msg2.getSymbol().toString());

        if (msg1 instanceof BestBidOfferMessage && msg2 instanceof BestBidOfferMessage) {
            BestBidOfferMessage bbo1 = (BestBidOfferMessage) msg1;
            BestBidOfferMessage bbo2 = (BestBidOfferMessage) msg2;

            assertEquals(bbo1.getBidPrice(), bbo2.getBidPrice(), 0.00001);
            assertEquals(bbo1.getBidSize(), bbo2.getBidSize(), 0.00001);
            assertEquals(bbo1.getOfferPrice(), bbo2.getOfferPrice(), 0.00001);
            assertEquals(bbo1.getOfferSize(), bbo2.getOfferSize(), 0.00001);
        }
        else if (msg1 instanceof TradeMessage && msg2 instanceof TradeMessage) {
            TradeMessage trade1 = (TradeMessage) msg1;
            TradeMessage trade2 = (TradeMessage) msg2;

            assertEquals(trade1.getPrice(), trade2.getPrice(), 0.00001);
            assertEquals(trade1.getSize(), trade2.getSize(), 0.00001);
        }
        else if (msg1 instanceof BarMessage && msg2 instanceof BarMessage) {
            BarMessage bar1 = (BarMessage) msg1;
            BarMessage bar2 = (BarMessage) msg2;
            
            assertEquals(bar1.getOpen(), bar2.getOpen(), 0.00001);
            assertEquals(bar1.getClose(), bar2.getClose(), 0.00001);
            assertEquals(bar1.getHigh(), bar2.getHigh(), 0.00001);
            assertEquals(bar1.getLow(), bar2.getLow(), 0.00001);
            assertEquals(bar1.getVolume(), bar2.getVolume(), 0.00001);
        }
        else {
            assertEquals(msg1.toString(), msg2.toString());
        }
    }
}
