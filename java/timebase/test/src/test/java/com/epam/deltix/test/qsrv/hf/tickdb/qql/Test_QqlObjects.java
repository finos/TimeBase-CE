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

package com.epam.deltix.test.qsrv.hf.tickdb.qql;

import com.epam.deltix.qsrv.hf.pub.NullValueException;
import com.epam.deltix.qsrv.hf.pub.RawMessage;
import com.epam.deltix.qsrv.hf.pub.TypeLoaderImpl;
import com.epam.deltix.qsrv.hf.pub.codec.InterpretingCodecMetaFactory;
import com.epam.deltix.qsrv.hf.pub.codec.NonStaticFieldInfo;
import com.epam.deltix.qsrv.hf.pub.codec.UnboundDecoder;
import com.epam.deltix.qsrv.hf.pub.md.DateTimeDataType;
import com.epam.deltix.qsrv.hf.stream.MessageReader2;
import com.epam.deltix.qsrv.hf.tickdb.Generator;
import com.epam.deltix.qsrv.hf.tickdb.PerFieldRandomGenerator;
import com.epam.deltix.qsrv.hf.tickdb.TestMessagesHelper;
import com.epam.deltix.qsrv.hf.tickdb.pub.DXTickDB;
import com.epam.deltix.qsrv.hf.tickdb.pub.SelectionOptions;
import com.epam.deltix.qsrv.hf.pub.md.Introspector;
import com.epam.deltix.qsrv.hf.pub.md.RecordClassDescriptor;
import com.epam.deltix.qsrv.hf.tickdb.TDBRunner;
import com.epam.deltix.qsrv.hf.tickdb.comm.server.TomcatServer;
import com.epam.deltix.qsrv.hf.tickdb.pub.*;
import com.epam.deltix.qsrv.hf.tickdb.pub.query.InstrumentMessageSource;
import com.epam.deltix.qsrv.test.messages.AllTypesMessage;
import com.epam.deltix.qsrv.test.messages.BarMessage;
import com.epam.deltix.qsrv.util.json.JSONRawMessagePrinter;
import com.epam.deltix.test.qsrv.hf.tickdb.TDBRunnerBase;
import com.epam.deltix.test.qsrv.hf.tickdb.qql.messages.*;
import com.epam.deltix.test.qsrv.hf.tickdb.qql.messages.entries.*;
import com.epam.deltix.test.qsrv.hf.tickdb.qql.messages.entries.Package;
import com.epam.deltix.test.qsrv.hf.tickdb.qql.messages.orders.*;
import com.epam.deltix.timebase.messages.InstrumentMessage;
import com.epam.deltix.util.JUnitCategories;
import com.epam.deltix.util.collections.generated.*;
import com.epam.deltix.util.io.Home;
import com.epam.deltix.util.io.IOUtil;
import com.epam.deltix.util.memory.MemoryDataInput;
import com.epam.deltix.util.time.Periodicity;
import org.apache.commons.math3.util.Pair;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Category(JUnitCategories.TickDBQQL.class)
public class Test_QqlObjects extends TDBRunnerBase {

    private static final boolean GENERATE_EXPECTED = Boolean.getBoolean("Test_QqlObjects.generateResults");
    private static final String RUNNING_QQL_LABEL = "Running QQL: ";
    private static final String END_TEST_LABEL = "!END";
    private static final String EXPECTED_PATH = "java/timebase/test/src/test/resources/qql/arrays/";
    private static final String EXPECTED_GENERATE_PATH = EXPECTED_PATH;

    private static TDBRunner runner;
    private static DXTickDB db;
    private static boolean remote;

    private static class MappingInfo {
        private Class<?> cls;
        private String name;

        private boolean json;

        public MappingInfo(Class<?> cls) {
            this(cls, null);
        }

        public MappingInfo(Class<?> cls, String name) {
            this.cls = cls;
            this.name = name;
        }

        public MappingInfo() {
            this.json = true;
        }
    }

    private static Pair<String, MappingInfo>[] QQL_OBJECTS = new Pair[] {
            QUERY("select * from orders"),

            QUERY("select order as res1 from orders", QueryResults.OrderQuery.class),
            QUERY("select \"deltix.orders.OrderEvent\":order as res1 from orders", QueryResults.OrderQuery.class),
            QUERY("select \"deltix.orders.OrderEvent\":order.id as res1 from orders", QueryResults.IdQuery.class),
            QUERY("select order.sequence as res1 from orders", QueryResults.FloatQuery.class),

            QUERY("select order.info as res1 from orders", QueryResults.OrderInfoQuery.class),
            QUERY("select order.info as res1 from orders where order.info is deltix.orders.MarketOrderInfo", QueryResults.OrderInfoQuery.class),
            QUERY("select order.info as res1 from orders where order is deltix.orders.MarketOrder", QueryResults.OrderInfoQuery.class),
            QUERY("select order.info as res1 from orders where order.info is deltix.orders.LimitOrderInfo", QueryResults.OrderInfoQuery.class),
            QUERY("select order.info as res1 from orders where order is deltix.orders.LimitOrder", QueryResults.OrderInfoQuery.class),

            QUERY("select order.info.executedInfo.avgPrice as res1 from orders", QueryResults.FloatQuery.class),
            QUERY("select order.info.executedInfo as res1 from orders", QueryResults.ExecutedInfoQuery.class),
            QUERY("select order.info.executedInfo as res1 from orders " +
                    "where order.info.executedInfo is deltix.orders.ExecutedLimitOrderInfoA", QueryResults.ExecutedInfoQuery.class),
            QUERY("select order.info.executedInfo as res1 from orders " +
                    "where order.info.executedInfo is deltix.orders.ExecutedLimitOrderInfoB", QueryResults.ExecutedInfoQuery.class),
            QUERY("select order.info.executedInfo as res1 from orders " +
                    "where order.info.executedInfo is deltix.orders.ExecutedMarketOrderInfo", QueryResults.ExecutedInfoQuery.class),
            QUERY("select ((order.info as deltix.orders.LimitOrderInfo).executedInfo as deltix.orders.ExecutedLimitOrderInfoA).infoIdA as res1 from orders",
                    QueryResults.IntegerQuery.class),
            QUERY("select ((order.info as deltix.orders.LimitOrderInfo).executedInfo as deltix.orders.ExecutedLimitOrderInfoB).infoIdB as res1 from orders",
                    QueryResults.IntegerQuery.class),
            QUERY("select ((order.info as deltix.orders.MarketOrderInfo).executedInfo as deltix.orders.ExecutedMarketOrderInfo).infoId as res1 from orders",
                    QueryResults.IntegerQuery.class),

            QUERY("select * from orders where order is deltix.orders.LimitOrder"),
            QUERY("select * from orders where order is deltix.orders.MarketOrder"),

            QUERY("select order.execution as res1 from orders", QueryResults.ExecutionQuery.class),
            QUERY("select order.execution.info.price as res1 from orders", QueryResults.FloatQuery.class),
            QUERY("select order.execution.id.external.id as res1 from orders", QueryResults.VarcharQuery.class),
            QUERY("select (order as deltix.orders.MarketOrder).execution.id.external.id as res1 from orders", QueryResults.VarcharQuery.class), // redundant <deltix.orders.MarketOrder>
            QUERY("select order.id.source as res1, order.id.correlationId as res2 from orders", QueryResults.VarcharIntegerQuery.class),

            QUERY("select order.info.size as res1 from orders", QueryResults.FloatQuery.class),
            QUERY("select order.info.price as res1 from orders", QueryResults.FloatQuery.class),
            QUERY("select (order.info as deltix.orders.LimitOrderInfo).price as res1 from orders", QueryResults.FloatQuery.class),
            QUERY("select (order.info as deltix.orders.MarketOrderInfo).size as res1 from orders", QueryResults.FloatQuery.class),

            QUERY("select (order.info.price + 3) as res1, (order.info.size - 10) as res2 from orders", QueryResults.DoubleDoubleQuery.class),
            QUERY("select order.info.price as res1 from orders where order.info.price > 101", QueryResults.FloatQuery.class),

            QUERY("select ((order.info as deltix.orders.LimitOrderInfo).price + (order.info as deltix.orders.LimitOrderInfo).size) as res1 from orders " +
                    "where (order.info as deltix.orders.LimitOrderInfo) is not null", QueryResults.FloatQuery.class),
            QUERY("select (order as deltix.orders.MarketOrder).info as res1, (order as deltix.orders.LimitOrder).info as res2 from orders", QueryResults.OrderInfoOrderInfoQuery.class),
            QUERY("select ((order.info as deltix.orders.MarketOrderInfo).executedInfo as deltix.orders.ExecutedMarketOrderInfo).customInfo as res1 from orders",
                    QueryResults.LongArrayQuery.class),
            QUERY("select ((order.info as deltix.orders.LimitOrderInfo).price + (order.info as deltix.orders.LimitOrderInfo).size) as res1 from orders " +
                    "where order.info is deltix.orders.LimitOrderInfo")
    };

    private static Pair<String, MappingInfo>[] QQL_ARRAYS = new Pair[] {

            // list obj -> value
            QUERY("select entries.price as res1 from packages", QueryResults.FloatArrayQuery.class),
            QUERY("select entries.size as res1 from packages", QueryResults.FloatArrayQuery.class),
            QUERY("select entries.level as res1 from packages", QueryResults.IntegerArrayQuery.class),
            QUERY("select entries.?level as res1 from packages", QueryResults.IntegerArrayQuery.class),
            QUERY("select entries.exchange as res1 from packages", QueryResults.VarcharArrayQuery.class),
            QUERY("select entries.?exchange as res1 from packages", QueryResults.VarcharArrayQuery.class),
            QUERY("select entries.attributes as res1 from packages", QueryResults.AttributesArrayQuery.class),
            QUERY("select entries.price as res1, entries.size as res2, entries.level as res3, entries.exchange as res4 from packages",
                    QueryResults.FloatFloatIntegerVarcharArrayQuery.class),
            QUERY("select entries.?price as res1, entries.?size as res2, entries.?level as res3, entries.?exchange as res4 from packages",
                    QueryResults.FloatFloatIntegerVarcharArrayQuery.class),
            // list obj -> list obj -> value
            QUERY("select entries.attributes.value as res1 from packages", QueryResults.VarcharArrayQuery.class),
            // list obj -> list obj -> list obj -> value
            QUERY("select entries.attributes.extendedAttributes.id as res1 from packages",
                    QueryResults.IntegerArrayQuery.class),
            QUERY("select entries.attributes.extendedAttributes.keys as res1 from packages",
                    QueryResults.IntegerArrayQuery.class),
            // list obj -> list obj -> list obj -> value array
            QUERY("select entries.attributes.extendedAttributes.values as res1 from packages",
                    QueryResults.VarcharArrayQuery.class),
            // list obj -> list obj -> value
            QUERY("select entries.attributes.attributeId.id as res1 from packages", QueryResults.IntegerArrayQuery.class),
            // obj -> obj list
            QUERY("select order.executions as res1 from orders", QueryResults.ExecutionsArrayQuery.class),
            QUERY("select order.customTags as res1 from orders", QueryResults.VarcharArrayQuery.class),
            // obj -> obj list -> value array
            QUERY("select order.executions.customTags as res1 from orders", QueryResults.VarcharArrayQuery.class),
            // obj -> obj list -> obj -> value
            QUERY("select order.executions.info.price as res1 from orders where order is deltix.orders.LimitOrder", QueryResults.FloatArrayQuery.class),
            QUERY("select order.executions.info.price as res1, order.executions.info as res2, order.executions as res3, order as res4 from orders " +
                    "where order is deltix.orders.LimitOrder", QueryResults.FloatExecutionsInfoArrayQuery.class),
            // obj -> obj list -> obj list -> value
            QUERY("select order.executions.attributes.value as res1 from orders", QueryResults.VarcharArrayQuery.class),
            // obj -> obj list -> obj list -> value
            QUERY("select order.executions.attributes.attributeId.id as res1 from orders", QueryResults.IntegerArrayQuery.class),
            // obj -> obj list -> obj list -> obj list -> value
            QUERY("select order.executions.attributes.extendedAttributes.id as res1 from orders " +
                    "where order is deltix.orders.LimitOrder", QueryResults.IntegerArrayQuery.class),
            // obj -> obj list -> obj list -> obj list -> value list
            QUERY("select order.executions.attributes.extendedAttributes.keys as res1 from orders " +
                    "where order is deltix.orders.LimitOrder", QueryResults.IntegerArrayQuery.class),
            // obj -> obj list -> obj list -> obj list -> value array
            QUERY("select order.executions.attributes.extendedAttributes.values as res1 from orders " +
                    "where order is deltix.orders.LimitOrder", QueryResults.VarcharArrayQuery.class),

            QUERY("select order.executions.attributes.extendedAttributes.keys as res1, " +
                    "order.executions as res2, " +
                    "order.executions.attributes as res3 from orders", QueryResults.IntegerExecutionsAttributesArrayQuery.class),

            // obj -> obj -> value array
            QUERY("select order.execution.customTags as res1 from orders", QueryResults.VarcharArrayQuery.class),
            // obj -> obj -> obj array -> value
            QUERY("select order.execution.attributes.value as res1 from orders", QueryResults.VarcharArrayQuery.class),
            // obj -> obj -> obj array -> obj -> value
            QUERY("select order.execution.attributes.attributeId.id as res1 from orders", QueryResults.IntegerArrayQuery.class),
            // obj -> obj -> obj array -> obj -> value
            QUERY("select order.execution.attributes.extendedAttributes.id as res1 from orders", QueryResults.IntegerArrayQuery.class),
            // obj -> obj -> obj array -> obj -> value array
            QUERY("select order.execution.attributes.extendedAttributes.keys as res1 from orders", QueryResults.IntegerArrayQuery.class),
            // obj -> obj -> obj array -> obj array -> value
            QUERY("select order.execution.attributes.extendedAttributes.values as res1 from orders", QueryResults.VarcharArrayQuery.class),

            // polymorphic obj -> obj -> polymorphic obj array -> value
            QUERY("select order.info.executedInfoHistory.totalQuantity as res1 from orders", QueryResults.FloatArrayQuery.class),
            QUERY("select order.info.executedInfoHistory as res1 from orders", QueryResults.ExecutedInfoHistoryQuery.class),

            QUERY("select (entries.attributes as array(deltix.CustomAttribute)).key as res1 from packages", QueryResults.VarcharArrayQuery.class),
            QUERY("select (entries.attributes as array(deltix.FixAttribute)).key as res1 from packages", QueryResults.IntegerArrayQuery.class),

            // arithmetic
            QUERY("select ((entries.price + 3) * 10) as res1 from packages", QueryResults.FloatArrayQuery.class),

            QUERY_RAW("select this.aaa, this.ooo, this.oooaaa from subclasses"),
    };

    private static Pair<String, MappingInfo>[] QQL_ALL_TYPES = new Pair[] {

            // list
            QUERY("select booleanList as res1 from alltypes", QueryResults.BoolArrayQuery.class),
            QUERY("select byteList as res1 from alltypes", QueryResults.ByteArrayQuery.class),
            QUERY("select shortList as res1 from alltypes", QueryResults.ShortArrayQuery.class),
            QUERY("select intList as res1 from alltypes", QueryResults.IntegerArrayQuery.class),
            QUERY("select longList as res1 from alltypes", QueryResults.LongArrayQuery.class),
            QUERY("select floatList as res1 from alltypes", QueryResults.FloatArrayQuery.class),
            QUERY("select doubleList as res1 from alltypes", QueryResults.DoubleArrayQuery.class),
            QUERY("select decimalList as res1 from alltypes", QueryResults.DecimalArrayQuery.class),
            QUERY("select asciiTextList as res1 from alltypes", QueryResults.VarcharArrayQuery.class),
            QUERY("select alphanumericList as res1 from alltypes", QueryResults.AlphanumericArrayQuery.class),

            // obj -> list
            QUERY("select lists.nestedBooleanList as res1 from alltypes", QueryResults.BoolArrayQuery.class),
            QUERY("select lists.nestedByteList as res1 from alltypes", QueryResults.ByteArrayQuery.class),
            QUERY("select lists.nestedShortList as res1 from alltypes", QueryResults.ShortArrayQuery.class),
            QUERY("select lists.nestedIntList as res1 from alltypes", QueryResults.IntegerArrayQuery.class),
            QUERY("select lists.nestedLongList as res1 from alltypes", QueryResults.LongArrayQuery.class),
            QUERY("select lists.nestedFloatList as res1 from alltypes", QueryResults.FloatArrayQuery.class),
            QUERY("select lists.nestedDoubleList as res1 from alltypes", QueryResults.DoubleArrayQuery.class),
            QUERY("select lists.nestedDecimalList as res1 from alltypes", QueryResults.DecimalArrayQuery.class),
            QUERY("select lists.nestedAsciiTextList as res1 from alltypes", QueryResults.VarcharArrayQuery.class),
            QUERY("select lists.nestedAlphanumericList as res1 from alltypes", QueryResults.AlphanumericArrayQuery.class),

            // obj list -> value
            QUERY("select objectsList.boolField as res1 from alltypes", QueryResults.BoolArrayQuery.class),
            QUERY("select objectsList.byteField as res1 from alltypes", QueryResults.ByteArrayQuery.class),
            QUERY("select objectsList.shortField as res1 from alltypes", QueryResults.ShortArrayQuery.class),
            QUERY("select objectsList.intField as res1 from alltypes", QueryResults.IntegerArrayQuery.class),
            QUERY("select objectsList.longField as res1 from alltypes", QueryResults.LongArrayQuery.class),
            QUERY("select objectsList.floatField as res1 from alltypes", QueryResults.FloatArrayQuery.class),
            QUERY("select objectsList.doubleField as res1 from alltypes", QueryResults.DoubleArrayQuery.class),
            QUERY("select objectsList.decimalField as res1 from alltypes", QueryResults.DecimalArrayQuery.class),
            QUERY("select objectsList.asciiTextField as res1 from alltypes", QueryResults.VarcharArrayQuery.class),
            QUERY("select objectsList.textAlphaNumericField as res1 from alltypes", QueryResults.AlphanumericArrayQuery.class),

            // obj list -> value array
            QUERY("select listOfLists.nestedBooleanList as res1 from alltypes", QueryResults.BoolArrayQuery.class),
//        QUERY("select listOfLists.nestedByteList as res1 from alltypes", QueryResults.ByteArrayQuery.class),
            QUERY("select listOfLists.nestedShortList as res1 from alltypes", QueryResults.ShortArrayQuery.class),
            QUERY("select listOfLists.nestedIntList as res1 from alltypes", QueryResults.IntegerArrayQuery.class),
            QUERY("select listOfLists.nestedLongList as res1 from alltypes", QueryResults.LongArrayQuery.class),
            QUERY("select listOfLists.nestedFloatList as res1 from alltypes", QueryResults.FloatArrayQuery.class),
            QUERY("select listOfLists.nestedDoubleList as res1 from alltypes", QueryResults.DoubleArrayQuery.class),
            QUERY("select listOfLists.nestedDecimalList as res1 from alltypes", QueryResults.DecimalArrayQuery.class),
            QUERY("select listOfLists.nestedAsciiTextList as res1 from alltypes", QueryResults.VarcharArrayQuery.class),
            QUERY("select listOfLists.nestedAlphanumericList as res1 from alltypes", QueryResults.AlphanumericArrayQuery.class),

            // obj list -> obj list -> value
            QUERY("select listOfLists.nestedObjectsList.boolField as res1 from alltypes", QueryResults.BoolArrayQuery.class),
            QUERY("select listOfLists.nestedObjectsList.byteField as res1 from alltypes", QueryResults.ByteArrayQuery.class),
            QUERY("select listOfLists.nestedObjectsList.shortField as res1 from alltypes", QueryResults.ShortArrayQuery.class),
            QUERY("select listOfLists.nestedObjectsList.intField as res1 from alltypes", QueryResults.IntegerArrayQuery.class),
            QUERY("select listOfLists.nestedObjectsList.longField as res1 from alltypes", QueryResults.LongArrayQuery.class),
            QUERY("select listOfLists.nestedObjectsList.floatField as res1 from alltypes", QueryResults.FloatArrayQuery.class),
            QUERY("select listOfLists.nestedObjectsList.doubleField as res1 from alltypes", QueryResults.DoubleArrayQuery.class),
            QUERY("select listOfLists.nestedObjectsList.decimalField as res1 from alltypes", QueryResults.DecimalArrayQuery.class),
            QUERY("select listOfLists.nestedObjectsList.asciiTextField as res1 from alltypes", QueryResults.VarcharArrayQuery.class),
            QUERY("select listOfLists.nestedObjectsList.textAlphaNumericField as res1 from alltypes", QueryResults.AlphanumericArrayQuery.class),

    };

    private static Pair<String, MappingInfo>[] QQL_ALL_PREDICATES = new Pair[] {
            QUERY("select entries.price as res1 from packages", QueryResults.FloatArrayQuery.class),
            QUERY("select entries[this is deltix.entries.L1Entry] as res1 from packages", QueryResults.EntryArrayQuery.class),
            QUERY("select entries[this is deltix.entries.L2Entry] as res1 from packages", QueryResults.EntryArrayQuery.class),
            QUERY("select entries[this is deltix.entries.TradeEntry] as res1 from packages", QueryResults.EntryArrayQuery.class),
            QUERY("select entries.price[0] as res1 from packages", QueryResults.FloatQuery.class),
            QUERY("select entries.price[2] as res1 from packages", QueryResults.FloatQuery.class),
            QUERY("select entries.price[5] as res1 from packages", QueryResults.FloatQuery.class),
            QUERY("select entries.price[10] as res1 from packages", QueryResults.FloatQuery.class),
            QUERY("select entries.price[-10] as res1 from packages", QueryResults.FloatQuery.class),
            QUERY("select entries.price[this < 2050] as res1 from packages", QueryResults.FloatArrayQuery.class),
            QUERY("select entries.price[this < 2050 and this >= 1910] as res1 from packages", QueryResults.FloatArrayQuery.class),
            QUERY("select entries.exchange[this == 'GDAX'] as res1 from packages", QueryResults.VarcharArrayQuery.class),
            QUERY("select entries[1].price as res1, " +
                    "entries[3].size as res2, " +
                    "entries[0].exchange as res3, " +
                    "entries[5].level as res4, " +
                    "entries[10].price as res5 from packages", QueryResults.FloatFloatVarcharIntegerFloatQuery.class),
            QUERY("select entries[-1].price as res1, " +
                    "entries[-3].size as res2, " +
                    "entries[-1].exchange as res3, " +
                    "entries[-5].level as res4, " +
                    "entries[-10].price as res5 from packages", QueryResults.FloatFloatVarcharIntegerFloatQuery.class),
            QUERY("select entries[this is deltix.entries.TradeEntry].price as res1, " +
                    "entries[this is deltix.entries.TradeEntry].size as res2 from packages", QueryResults.FloatFloatArrayQuery.class),
            QUERY("select entries[this is deltix.entries.L2Entry].level as res1 from packages", QueryResults.IntegerArrayQuery.class),
            QUERY("select entries[this.price > 2000 and size <= 30000] as res1 from packages", QueryResults.EntryArrayQuery.class),
            QUERY("select entries[price > 2000] as res1 from packages", QueryResults.EntryArrayQuery.class),
            QUERY("select entries[this.price > 2000] as res1 from packages", QueryResults.EntryArrayQuery.class),
            QUERY("select entries[this.price > 2000].price as res1 from packages", QueryResults.FloatArrayQuery.class),
            QUERY("select entries[entries.price > 2000].price as res1 from packages", QueryResults.FloatArrayQuery.class),
            QUERY("select entries.attributes[attributeId.id > 7300].attributeId.id as res1 from packages", QueryResults.IntegerArrayQuery.class),
            QUERY("select entries.attributes[size(entries.price) - 2].attributeId.id as res1 from packages", QueryResults.IntegerQuery.class),

            QUERY("select entries.attributes[size(this.extendedAttributes.keys) > 1].attributeId.id as res1 from packages",
                    QueryResults.IntegerArrayQuery.class),

            QUERY("select entries[all(attributes.attributeId.id > 7300)] as res1 from packages",
                    QueryResults.EntryArrayQuery.class),
            QUERY("select entries[this is deltix.entries.TradeEntry].attributes[attributeId.id > 7300].extendedAttributes.keys[this < 541000] as res1 from packages",
                    QueryResults.IntegerArrayQuery.class),

            QUERY("select entries.attributes[entries.attributes.attributeId.id > 7300].attributeId.id as res1 from packages",
                    QueryResults.IntegerArrayQuery.class),
            QUERY("select (entries.attributes as array(deltix.FixAttribute)).key as res1 from packages",
                    QueryResults.IntegerArrayQuery.class),
            QUERY("select (entries.attributes[(this as deltix.FixAttribute).key > 5100] as array(deltix.FixAttribute)).key as res1 from packages",
                    QueryResults.IntegerArrayQuery.class),

            QUERY("select (entries[price > 2000])[price <= 3000] as res1 from packages", QueryResults.EntryArrayQuery.class),
            QUERY("select entries[size(attributes[all(extendedAttributes.keys > 300000)]) > 0].attributes as res1 from packages",
                    QueryResults.AttributesArrayQuery.class),
            QUERY("select entries[all((this.attributes as array(deltix.FixAttribute)).key > 5300)].attributes as res1 from packages", QueryResults.AttributesArrayQuery.class),
            QUERY("select entries.attributes[this is deltix.FixAttribute] as res1 from packages " +
                    "where any((entries.attributes as array(deltix.FixAttribute)).key < 5300)", QueryResults.AttributesArrayQuery.class),

            QUERY("select entries[size(attributes.extendedAttributes[id > 3000000]) > 0].attributes.extendedAttributes.id as res1 from packages",
                    QueryResults.IntegerArrayQuery.class),
            QUERY("select entries.attributes[this is deltix.FixAttribute] as res1 from packages where any((entries.attributes as array(deltix.FixAttribute)).key < 5300)",
                    QueryResults.AttributesArrayQuery.class),

            QUERY("select entries[entries.level] as res1 from packages", QueryResults.EntryArrayQuery.class),
            QUERY("select entries[entries.level + 5] as res1 from packages", QueryResults.EntryArrayQuery.class),
            QUERY("select entries[entries.level - 7] as res1 from packages", QueryResults.EntryArrayQuery.class),

            QUERY("select entries[position() > 3] as res1 from packages", QueryResults.EntryArrayQuery.class),
            QUERY("select entries[position() == last()] as res1 from packages", QueryResults.EntryArrayQuery.class),

            QUERY("select entries[1:3] as res1 from packages", QueryResults.EntryArrayQuery.class),
            QUERY("select entries[1:6:2] as res1 from packages", QueryResults.EntryArrayQuery.class),
            QUERY("select entries[6:0:-1] as res1 from packages", QueryResults.EntryArrayQuery.class),
            QUERY("select entries[::] as res1 from packages", QueryResults.EntryArrayQuery.class),
            QUERY("select entries[::-1] as res1 from packages", QueryResults.EntryArrayQuery.class),
            QUERY("select entries[size(entries)::-1] as res1 from packages", QueryResults.EntryArrayQuery.class),
    };

    private static Pair<String, MappingInfo>[] QQL_ALL_ARRAY_JOIN = new Pair[]{
            QUERY("select entries as res1 from packages array join entries", QueryResults.EntryQuery.class),
            QUERY("select entry as res1 from packages array join entries as entry", QueryResults.EntryQuery.class),
            QUERY("select entry.price as res1, entry.size as res2 from packages array join entries as entry", QueryResults.FloatFloatQuery.class),
            QUERY("select entry as res1 from packages array join entries as entry " +
                    "where entry is deltix.entries.L1Entry", QueryResults.EntryQuery.class),
            QUERY("select entry as res1 from packages array join entries as entry " +
                    "where entry.price > 3000", QueryResults.EntryQuery.class),
            QUERY("select entry as res1, num as res2 from packages " +
                    "array join entries as entry, enumerate(entries) as num", QueryResults.EntryIntegerQuery.class),

            QUERY("select entry as res1, order as res2 from stream1 " +
                    "array join entries as entry", QueryResults.EntryOrderQuery.class),
            QUERY("select entry as res1, order as res2 from stream1 " +
                    "left array join entries as entry", QueryResults.EntryOrderQuery.class),

            QUERY("select * from packages array join entries",
                    QueryResults.EntriesPackageTypeQuery.class),

            QUERY("select * from packages array join entries as entry",
                    QueryResults.EntriesPackageTypeEntryQuery.class),

            QUERY("select * from alltypes array join booleanList as A"),

            QUERY("select * from packages array join [1, 2, 3, 4, 5, 6, 7, 8, 9]"),

            QUERY("select running entry, num, count{}() from packages " +
                    "array join entries as entry, enumerate(entries) as num"),

    };

    private static Pair<String, MappingInfo>[] QQL_ALL_GROUP_BY = new Pair[] {
            QUERY("select packageType, count{}() from packages group by packageType"),
            QUERY("select * from packages group by packageType"),
            QUERY("select infoIdA, avg{}(totalQuantity) from infoA group by (infoIdA % 3)"),
            QUERY("select * from infoA group by (infoIdA % 3)"),
            QUERY("select infoIdA, avg{}(totalQuantity) from infoA where totalQuantity > 2 and customInfo < 20 group by (infoIdA % 3)"),
            QUERY("select packageType, entries[this is L2Entry].level[0] as s from packages group by s"),
            QUERY("select packageType, s from packages group by entries[this is L2Entry].level[0] as s"),
            QUERY("select entry.level, count{}() from packages array join entries as entry group by entry.level"),
            QUERY("select entry.exchange, count{}() from packages array join entries as entry group by entry.exchange"),

            QUERY("select running entry.level, count{}() from packages array join entries as entry group by entry.level"),
            QUERY("select entry.level as level, count{}() from packages array join entries as entry where level > 5 group by level"),
            QUERY("select running entry.level as level, count{}() from packages array join entries as entry where level > 5 group by level"),

            QUERY("select boolField, byteField, count{}() from alltypesrand group by boolField, byteField"),
            QUERY("select intField, count{}() from alltypesrand group by intField, symbol, byteField"),
            QUERY("select asciiTextField, count{}() from alltypesrand group by asciiTextField"),
            QUERY("select boolNullableField, count{}() from alltypesrand group by boolNullableField"),
            QUERY("select textAlphanumericField, count{}() from alltypesrand group by textAlphanumericField"),
            QUERY("select max{}(decimalField), min{}(decimalField), count{}() from alltypesrand over time(5m) group by symbol"),
            QUERY("select max{}(decimalField), min{}(decimalField), boolNullableField, count{}() from alltypesrand over every time(20m) group by boolNullableField, symbol"),
            QUERY("select max{}(decimalField), min{}(decimalField), boolNullableField, byteField, count{}() from alltypesrand over time(20m) group by boolNullableField, byteField"),
    };

    private static Pair<String, MappingInfo>[] QQL_ALL_MISC = new Pair[] {
            // object prediacates
            QUERY("select order[this is deltix.orders.MarketOrder] as res1 from orders", QueryResults.OrderQuery.class),
            QUERY("select entry[price > 4000].price as res1 from packages array join entries as entry", QueryResults.FloatQuery.class),
            QUERY("select price[this > 2000] as res1 from packages array join entries.price as price", QueryResults.FloatQuery.class),

            // object.* operator
            QUERY("select ((order.info as deltix.orders.LimitOrderInfo) as info).* from orders"),
            QUERY("select order.execution.info.*, order.execution.info.commissionInfo.* from orders where order.execution is not null"),
            QUERY("select entry.* from packages array join entries as entry"),
            QUERY("select entries[0].* from packages"),
            QUERY("select entries.attributes[(this as deltix.FixAttribute).key > 5100][0].* from packages"),

            QUERY("select running this.*, count{}() type \"deltix.MyBarMessage\" from bar1minExtended"),

            QUERY("select entries as res0, " +
                    "entries.level as res1, entries.price as res2, entries.?price as res3, entries.?level as res4, " +
                    "entries[entries.level > 0] as res5, entries[entries.?level > 0] as res6 " +
                    "from packages", QueryResults.TestQueryResult1.class),

            QUERY("select * type \"deltix.MyBarMessage\" from bar1minExtended " +
                    "union " +
                    "select * type \"deltix.MyBarMessage\" from bar1min"),
            QUERY("select " +
                    "trade.price as \"price\", " +
                    "trade.size as \"size\" " +
                    "type \"deltix.timebase.api.messages.BestBidOfferTradeMessage\" " +
                    "from packages " +
                    "array join entries[this is TradeEntry] as trade " +
                    "UNION " +
                    "select " +
                    "bbo[side == \"deltix.entries.QuoteSide\":BUY].price as \"offerPrice\", " +
                    "bbo[side == \"deltix.entries.QuoteSide\":BUY].size as \"offerSize\", " +
                    "bbo[side == \"deltix.entries.QuoteSide\":SELL].price as \"bidPrice\", " +
                    "bbo[side == \"deltix.entries.QuoteSide\":SELL].size as \"bidSize\" " +
                    "type \"deltix.timebase.api.messages.BestBidOfferTradeMessage\" " +
                    "from packages " +
                    "array join (entries as array(L1Entry))[this is not null] as bbo"),
            QUERY("select " +
                    "trade.price as \"price\", " +
                    "trade.size as \"size\" " +
                    "type \"deltix.timebase.api.messages.TradeMessage\" " +
                    "from packages " +
                    "array join entries[this is TradeEntry] as trade " +
                    "UNION " +
                    "select " +
                    "bbo[side == \"deltix.entries.QuoteSide\":BUY].price as \"offerPrice\", " +
                    "bbo[side == \"deltix.entries.QuoteSide\":BUY].size as \"offerSize\", " +
                    "bbo[side == \"deltix.entries.QuoteSide\":SELL].price as \"bidPrice\", " +
                    "bbo[side == \"deltix.entries.QuoteSide\":SELL].size as \"bidSize\" " +
                    "type \"deltix.timebase.api.messages.BestBidOfferMessage\" " +
                    "from packages " +
                    "array join (entries as array(L1Entry))[this is not null] as bbo"),
            QUERY("(select " +
                    "trade.price as \"price\", " +
                    "trade.size as \"size\" " +
                    "type \"deltix.timebase.api.messages.TradeMessage\" " +
                    "from packages " +
                    "array join entries[this is TradeEntry] as trade " +
                    "UNION " +
                    "select " +
                    "bbo[side == \"deltix.entries.QuoteSide\":BUY].price as \"offerPrice\", " +
                    "bbo[side == \"deltix.entries.QuoteSide\":BUY].size as \"offerSize\", " +
                    "bbo[side == \"deltix.entries.QuoteSide\":SELL].price as \"bidPrice\", " +
                    "bbo[side == \"deltix.entries.QuoteSide\":SELL].size as \"bidSize\" " +
                    "type \"deltix.timebase.api.messages.BestBidOfferMessage\" " +
                    "from packages " +
                    "array join (entries as array(L1Entry))[this is not null] as bbo) " +
                    "limit 3 offset 5"),

            QUERY("select * from packages limit 20"),
            QUERY("select * from packages limit 4"),
            QUERY("select * from packages limit 3, 5"),
            QUERY("select * from packages limit 4 offset 2"),
            QUERY("select * from packages limit 1"),
            QUERY("select * from packages limit 10, 1"),
            QUERY("select * from packages limit 9, 1"),
            QUERY("select * from packages limit 9, 0"),
            QUERY("select * from packages limit 9, 0"),
            QUERY("select * type \"deltix.MyBarMessage\" from bar1minExtended limit 3, 4 " +
                    "union " +
                    "select * type \"deltix.MyBarMessage\" from bar1min"),
            QUERY("(select * type \"deltix.MyBarMessage\" from bar1minExtended " +
                    "union " +
                    "select * type \"deltix.MyBarMessage\" from bar1min) " +
                    "limit 3, 4"),

            // primitive casts
            QUERY("select (byteList as array(INT8))[0], (byteList as array(INT16))[0], (byteList as array(INT32))[0], " +
                    "(byteList as array(INT64))[0], (byteList as array(DECIMAL))[0], (byteList as array(FLOAT32))[0], " +
                    "(byteList as array(FLOAT64))[0], (byteList as array(CHAR))[0], (byteList as array(boolean))[0] from alltypes"),
            QUERY("select (shortList as array(INT8))[0], (shortList as array(INT16))[0], (shortList as array(INT32))[0], " +
                    "(shortList as array(INT64))[0], (shortList as array(DECIMAL))[0], (shortList as array(FLOAT32))[0], " +
                    "(shortList as array(FLOAT64))[0], (shortList as array(CHAR))[0], (shortList as array(boolean))[0] from alltypes"),
            QUERY("select (intList as array(INT8))[0], (intList as array(INT16))[0], (intList as array(INT32))[0], " +
                    "(intList as array(INT64))[0], (intList as array(DECIMAL))[0], (intList as array(FLOAT32))[0], " +
                    "(intList as array(FLOAT64))[0], (intList as array(CHAR))[0], (intList as array(boolean))[0] from alltypes"),
            QUERY("select (longList as array(INT8))[0], (longList as array(INT16))[0], (longList as array(INT32))[0], " +
                    "(longList as array(INT64))[0], (byteList as array(DECIMAL))[0], (longList as array(FLOAT32))[0], " +
                    "(longList as array(FLOAT64))[0], (longList as array(CHAR))[0], (longList as array(boolean))[0] from alltypes"),
            QUERY("select (floatList as array(INT8))[0], (floatList as array(INT16))[0], (floatList as array(INT32))[0], " +
                    "(floatList as array(INT64))[0], (floatList as array(DECIMAL))[0], (floatList as array(FLOAT32))[0], " +
                    "(floatList as array(FLOAT64))[0], (floatList as array(CHAR))[0], (floatList as array(boolean))[0] from alltypes"),
            QUERY("select (doubleList as array(INT8))[0], (doubleList as array(INT16))[0], (doubleList as array(INT32))[0], " +
                    "(doubleList as array(INT64))[0], (doubleList as array(DECIMAL))[0], (doubleList as array(FLOAT32))[0], " +
                    "(doubleList as array(FLOAT64))[0], (doubleList as array(CHAR))[0], (doubleList as array(boolean))[0] from alltypes"),
            QUERY("select (decimalList as array(INT8))[0], (decimalList as array(INT16))[0], (decimalList as array(INT32))[0], " +
                    "(decimalList as array(INT64))[0], (decimalList as array(DECIMAL))[0], (decimalList as array(FLOAT32))[0], " +
                    "(decimalList as array(FLOAT64))[0], (decimalList as array(CHAR))[0], (decimalList as array(boolean))[0] from alltypes"),

            QUERY("select byteField as INT8, byteField as INT16, byteField as INT32, byteField as INT64, " +
                    "byteField as DECIMAL, byteField as FLOAT32, byteField as FLOAT64, byteField as char, byteField as boolean from alltypes"),
            QUERY("select shortField as INT8, shortField as INT16, shortField as INT32, shortField as INT64, " +
                    "shortField as DECIMAL, shortField as FLOAT32, shortField as FLOAT64, shortField as char, shortField as boolean from alltypes"),
            QUERY("select intField as INT8, intField as INT16, intField as INT32, intField as INT64, " +
                    "intField as DECIMAL, intField as FLOAT32, intField as FLOAT64, intField as char, intField as boolean from alltypes"),
            QUERY("select longField as INT8, longField as INT16, longField as INT32, longField as INT64, " +
                    "longField as DECIMAL, longField as FLOAT32, longField as FLOAT64, longField as char, longField as boolean from alltypes"),
            QUERY("select floatField as INT8, floatField as INT16, floatField as INT32, floatField as INT64, " +
                    "floatField as DECIMAL, floatField as FLOAT32, floatField as FLOAT64, floatField as char, floatField as boolean from alltypes"),
            QUERY("select doubleField as INT8, doubleField as INT16, doubleField as INT32, doubleField as INT64, " +
                    "doubleField as DECIMAL, doubleField as FLOAT32, doubleField as FLOAT64, doubleField as char, doubleField as boolean from alltypes"),
            QUERY("select decimalField as INT8, decimalField as INT16, decimalField as INT32, decimalField as INT64, " +
                    "decimalField as DECIMAL, decimalField as FLOAT32, decimalField as FLOAT64, decimalField as char, decimalField as boolean from alltypes"),

            // if and case statements
            QUERY_RAW("select [1,2,3] if true else [4,5,6]"),
            QUERY_RAW("select 'odd' if n % 2 == 0 else 'even' array join [1, 2, 3, 4, 5, 6] as n"),
            QUERY_RAW("select 'odd' if n % 2 == 0 array join [1, 2, 3, 4, 5, 6] as n"),
            QUERY_RAW("select s if s == '2' else null array join ['1','2','3','4'] as s"),
            QUERY_RAW("select byteField, '>50' if byteField > 50 else '<50' from alltypesrand"),
            QUERY_RAW("select byteField if boolField else byteNullableField from alltypesrand"),
            QUERY_RAW("select 'few' if n < 3 else 'many', n array join [0, 1, 2, 3, 4] as n"),
            QUERY_RAW("select entries.level if size(entries.level) > 0 else [99,99] as array(int16) from kraken"),
            QUERY_RAW("select entries.level if size(entries.level) > 0 from kraken"),
            QUERY_RAW("with this.entries[this is L1Entry][0] as trade " +
                    "select max{}(trade.price) as high, " +
                    "min{}(trade.price) as low, " +
                    "first{}(trade.price) as open, " +
                    "last{}(trade.price) as close, " +
                    "'HIGH' if high > 22593 else 'LOW' " +
                    "from (binance union kraken union bitfinex) " +
                    "over time(5s) " +
                    "where trade != null and symbol == 'BTC/USD'"),
            QUERY_RAW("with this.entries[this is L1Entry][0] as trade, " +
                    "(22593 as decimal if trade.price > 22593 else trade.price) as price " +
                    "select max{}(price) as high, " +
                    "min{}(price) as low, " +
                    "first{}(price) as open, " +
                    "last{}(price) as close " +
                    "from (binance union kraken union bitfinex) " +
                    "over time(5s) " +
                    "where trade != null and symbol == 'BTC/USD'"),
            QUERY_RAW("select case n " +
                    "when 0 then 'zero' " +
                    "when 1 then 'one' " +
                    "when 2 then 'two' " +
                    "else 'many' " +
                    "end, n " +
                    "array join [0, 1, 2, 3, 4] as n"),
            QUERY_RAW("select case s " +
                    "when 'zero' then 0 " +
                    "when 'one' then 1 " +
                    "when 'two' then 2 " +
                    "else 999 " +
                    "end, s " +
                    "array join ['zero', 'one', 'two', 'three', 'four'] as s"),
            QUERY_RAW("select case n + 1 " +
                    "when n then 'zero' " +
                    "when n + 1 then 'one' " +
                    "when n + 2 then 'two' " +
                    "else 'many' " +
                    "end, n + 1 " +
                    "array join [0, 1, 2, 3, 4] as n"),
            QUERY_RAW("select case " +
                    "when n == 0 then 'zero' " +
                    "when n == 1 then 'one' " +
                    "when n == 2 then 'two' " +
                    "else 'many' " +
                    "end, n " +
                    "array join [0, 1, 2, 3, 4] as n"),
            QUERY_RAW("select " +
                    "case when n < 3 then 'few' else 'many' end " +
                    "array join [0, 1, 2, 3, 4] as n"),
            QUERY_RAW("with " +
                    "(case size(entries) " +
                    "when 1 then 'one' " +
                    "when 2 then 'two' " +
                    "when 3 then 'three' " +
                    "else 'many' " +
                    "end) as t " +
                    "select t, count{}() " +
                    "from kraken " +
                    "group by t"),
            QUERY_RAW("select case size(entries) " +
                    "when 1 then entries[this is L1Entry] " +
                    "when 2 then entries[this is L2EntryUpdate] " +
                    "when 3 then entries[this is L2EntryNew] " +
                    "else entries " +
                    "end, size(entries), entries " +
                    "from kraken"),


            QUERY_RAW("select case " +
                    "when size(entries) == 1 then entries[this is L1Entry] " +
                    "when size(entries) == 2 then entries[this is L2EntryUpdate] " +
                    "else entries " +
                    "end, size(entries), entries " +
                    "from kraken"),
            QUERY_RAW("select case when n < 3 then 'few' else 'many' end, n array join [0, 1, 2, 3, 4] as n"),
            QUERY_RAW("with " +
                    "this.entries[this is L1Entry][0] as trade, " +
                    "(case when trade.price > 22593 then 22593 as decimal else trade.price end) as price " +
                    "select " +
                    "max{}(price) as high, " +
                    "min{}(price) as low, " +
                    "first{}(price) as open, " +
                    "last{}(price) as close " +
                    "from (binance union kraken union bitfinex) " +
                    "over time(5s) " +
                    "where trade != null and symbol == 'BTC/USD'"),

            QUERY_RAW("WITH entries[this is L1Entry] as 'entries', " +
                    "sum(entries.price * entries.size) / sum(entries.size) as 'avgPrice', " +
                    "bollinger{}(avgPrice) as 'bollinger' " +
                    "SELECT " +
                    "avgPrice, " +
                    "sma{timePeriod: 1h}(avgPrice) as 'sma-1h', " +
                    "cma{}(avgPrice) as 'cma', " +
                    "ema{period: 14}(avgPrice) as 'ema-14', " +
                    "bollinger.upperBand, " +
                    "bollinger.middleBand, " +
                    "bollinger.lowerBand " +
                    "FROM KRAKEN " +
                    "OVER TIME(5s) " +
                    "WHERE symbol == 'BTC/USD' AND notEmpty(entries)"),

//            QUERY_RAW("select " +
//                    "orderbook{maxDepth: 10}(this.packageType, this.entries) as entries, " +
//                    "PERIODICAL_SNAPSHOT as packageType " +
//                    "type \"deltix.timebase.api.messages.universal.PackageHeader\" " +
//                    "from KRAKEN " +
//                    "over time(5s) " +
//                    "where symbol == 'BTC/USD'"),
//
//            QUERY_RAW("with " +
//                    "orderbook{maxDepth: 10}(this.packageType, this.entries) as book " +
//                    "select " +
//                    "book as entries, book.price, book.size, book.exchangeId, " +
//                    "PERIODICAL_SNAPSHOT as packageType " +
//                    "type \"deltix.timebase.api.messages.universal.PackageHeader\" " +
//                    "from (BITFINEX union BINANCE union KRAKEN) " +
//                    "over time(1s) " +
//                    "where symbol == 'BTC/USDT'"),
//
//            QUERY_RAW("with " +
//                    "orderbook{maxDepth: 15}(this.packageType, this.entries) as book " +
//                    "select " +
//                    "book as entries, " +
//                    "PERIODICAL_SNAPSHOT as packageType " +
//                    "type \"deltix.timebase.api.messages.universal.PackageHeader\" " +
//                    "from (BITFINEX union BINANCE union KRAKEN) " +
//                    "over time(1s) " +
//                    "where symbol == 'BTC/USDT'"),

            QUERY_RAW("select lastNotNull{}(entries[this is L2EntryNew]) from BITFINEX " +
                    "over count(10) " +
                    "where symbol == 'BTC/USDT' limit 10"),
            QUERY_RAW("select lastNotNull{}(entries[this is L2EntryNew][0]) from BITFINEX " +
                    "over count(10) " +
                    "where symbol == 'BTC/USDT' limit 10"),

            QUERY_RAW("select entries.exchangeId == 'KRAKEN', " +
                    "entries[exchangeId == 'KRAKEN'].exchangeId, " +
                    "entries.exchangeId[this == 'KRAKEN'], " +
                    "entries.exchangeId == 'BINANCE', " +
                    "entries[exchangeId == 'BINANCE'].exchangeId, " +
                    "entries.exchangeId[this == 'BINANCE'] from KRAKEN"),

//            QUERY_RAW("with " +
//                    "orderbook{maxDepth: 15}(this.packageType, this.entries) as book " +
//                    "select " +
//                    "book.exchangeId, book.exchangeId == 'KRAKEN', book.exchangeId == 'BINANCE', " +
//                    "book[exchangeId == 'KRAKEN'].exchangeId, book[exchangeId == 'BINANCE'].exchangeId, " +
//                    "book.exchangeId[this == 'KRAKEN'], book.exchangeId[this == 'BINANCE'] " +
//                    "from (BITFINEX union BINANCE union KRAKEN) " +
//                    "over time(1s) " +
//                    "where symbol == 'BTC/USDT'"),

            QUERY_RAW("select * from kraken " +
                    "where symbol == 'BTC/USDT' and timestamp between '2023-01-25 12:00:00.000'd and '2023-01-25 12:00:00.100'd "),
            QUERY_RAW("select * from kraken " +
                    "where timestamp between '2023-01-25 12:00:00.000'd and '2023-01-25 12:00:00.100'd and symbol == 'BTC/USDT'"),

            QUERY_RAW("with entry as L1Entry as l1, " +
                    "entry as L2EntryNew as l2, " +
                    "entry as L2EntryUpdate as l2u " +
                    "select " +
                    "record l1.price field price, l1.size field size, l1.side field side type L1Entry when entry is L1Entry " +
                    "record l2.price field price, l2.size field size, l2.side field side type L2EntryNew when entry is L2EntryNew " +
                    "record l2u.price field price, l2u.size field size, l2u.side field side type L2EntryUpdate when entry is L2EntryUpdate " +
                    "from kraken " +
                    "array join entries as entry " +
                    "limit 20"),

            QUERY_RAW("select " +
                    "record boolField, byteField, asciiTextField type t1 when boolField " +
                    "record boolField, doubleField, enumField type t2 when not boolField " +
                    "from alltypesrand limit 10"),

            QUERY_RAW("select " +
                    "record doubleField type t1 when boolField " +
                    "record doubleField type t2 when not boolField " +
                    "from alltypesrand limit 10"),

            QUERY_RAW("select " +
                    "record boolField, byteField type t1 when boolField " +
                    "record boolField, doubleField type t2 when not boolField " +
                    "from alltypesrand " +
                    "group by boolField"),

            QUERY_RAW("with " +
                    "entry as TradeEntry as trade, entry as L1Entry as bbo " +
                    "select " +
                    "record trade.price field \"price\", trade.size field \"size\" type \"deltix.timebase.api.messages.TradeMessage\" when entry is TradeEntry " +
                    "record " +
                    "bbo[side == \"deltix.entries.QuoteSide\":BUY].price as \"offerPrice\", " +
                    "bbo[side == \"deltix.entries.QuoteSide\":BUY].size as \"offerSize\", " +
                    "bbo[side == \"deltix.entries.QuoteSide\":SELL].price as \"bidPrice\", " +
                    "bbo[side == \"deltix.entries.QuoteSide\":SELL].size as \"bidSize\" " +
                    "type \"deltix.timebase.api.messages.BestBidOfferMessage\" " +
                    "when entry is L1Entry " +
                    "from packages array join entries as entry " +
                    "limit 20"),

            QUERY_RAW("select " +
                    "record doubleField as d1, boolField as b1 type t1 when d1 > 500 " +
                    "record doubleField as d2, boolField as b2 type t2 when d2 < 500 " +
                    "from alltypesrand " +
                    "union select " +
                    "record doubleField as d1, boolField as b1 type t1 when d1 > 500 " +
                    "record doubleField as d2, boolField as b2 type t2 when d2 < 500 " +
                    "from alltypesrand"),

            QUERY_RAW("select " +
                    "record doubleField as d1 type t1 when d1 > 500 " +
                    "record doubleField as d2, boolField as b2 type t2 when d2 < 500 " +
                    "from alltypesrand " +
                    "union select " +
                    "record doubleField as d1, boolField as b1 type t1 when d1 > 500 " +
                    "record doubleField as d2 type t2 when d2 < 500 " +
                    "from alltypesrand"),

            QUERY_RAW("select " +
                    "record doubleField as d2, boolField as b2 type t2 when d2 < 500 " +
                    "from alltypesrand " +
                    "union select " +
                    "record doubleField as d1, boolField as b1 type t1 when d1 > 500 " +
                    "from alltypesrand")

    };

    private static Pair<String, MappingInfo>[] QQL_ALL_UNIONS = new Pair[] {
            QUERY_RAW("select * from (binance union bitfinex union kraken) limit 10 offset 30"),
            QUERY_RAW("select this.entries from (binance union bitfinex union kraken) limit 10 offset 30"),
            QUERY_RAW("select this.entries[this is L1Entry][0].* from (binance union bitfinex union kraken) " +
                    "where this.entries[this is L1Entry][0] != null limit 10 offset 30"),
            QUERY_RAW("select entry.* from (binance union bitfinex union kraken) " +
                    "array join  this.entries[this is L2EntryNew] as entry " +
                    "where packageType == PERIODICAL_SNAPSHOT and entry.level == 5 " +
                    "limit 10 offset 5"),
            QUERY_RAW("with this.entries[this is L1Entry] as bbo " +
                    "select bbo_entry from (binance union bitfinex union kraken) " +
                    "array join bbo as bbo_entry " +
                    "limit 10 offset 30"),
            QUERY_RAW("select BinancePackageHeader:entries as e1, KrakenPackageHeader:entries as e2 " +
                    "from (binance union bitfinex union kraken) where e1 != null or e2 != null"),
            QUERY_RAW("select l2.level, count{}() from (binance union bitfinex union kraken) " +
                    "array join this.entries[this is L2EntryNew] as l2 " +
                    "where symbol == 'BTC/USD' and l2.level < 5 " +
                    "group by l2.level"),
            QUERY_RAW("with this.entries[this is L1Entry] as array(L1Entry) as bbo " +
                    "select bbo_entry type t from (binance union bitfinex) " +
                    "array join bbo as bbo_entry " +
                    "limit 10 offset 20 " +
                    "union " +
                    "with this.entries[this is L1Entry] as array(L1Entry) as bbo " +
                    "select bbo[0] as bbo_entry " +
                    "type t " +
                    "from kraken " +
                    "where size(bbo) > 0 " +
                    "limit 10 offset 30")
    };


    public static Pair<String, MappingInfo> QUERY(String qql) {
        return QUERY(qql, null);
    }

    public static Pair<String, MappingInfo> QUERY(String qql, Class<?> type) {
        return Pair.create(qql, new MappingInfo(type));
    }

    public static Pair<String, MappingInfo> QUERY_RAW(String qql) {
        return Pair.create(qql, new MappingInfo());
    }

    public static Pair<String, MappingInfo> QUERY(String qql, Class<?> type, String name) {
        return Pair.create(qql, new MappingInfo(type, name));
    }

    private static class TestResult {
        private final String query;
        private final List<String> results = new ArrayList<>();

        public TestResult(String query) {
            this.query = query.trim();
        }

        public void append(String result) {
            if (result != null) {
                results.add(result.trim());
            }
        }
    }

    @BeforeClass
    public static void start() throws Throwable {
        remote = Boolean.parseBoolean(
                System.getProperty("runner.remote", "true")
        );
        db = createDb(remote);

        boolean loadData = Boolean.parseBoolean(
                System.getProperty("qql.test.loadData", "true")
        );
        if (loadData) {
            loadAllData();
        }
    }

    private static DXTickDB createDb(boolean remote) throws Exception {
        runner = new TDBRunner(remote, false, Home.getPath("temp/qql_objects_test/timebase"), new TomcatServer());
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
    public void Test_AllObjects() throws Exception {
        if (!remote) {
            throw new RuntimeException("Test is available only in remote mode: use -Drunner.remote=true");
        }

        if (GENERATE_EXPECTED) {
            qqlWriteExpected(db, QQL_OBJECTS, Home.getPath(EXPECTED_GENERATE_PATH + "objects.txt"));
        } else {
            Assert.assertTrue(
                    qql(db, QQL_OBJECTS, readExpectedResults(Home.getPath(EXPECTED_PATH + "objects.txt")))
            );
        }
    }

    @Test
    public void Test_AllArrays() throws Exception {
        if (!remote) {
            throw new RuntimeException("Test is available only in remote mode: use -Drunner.remote=true");
        }

        if (GENERATE_EXPECTED) {
            qqlWriteExpected(db, QQL_ARRAYS, Home.getPath(EXPECTED_GENERATE_PATH + "slicing.txt"));
        } else {
            Assert.assertTrue(
                    qql(db, QQL_ARRAYS, readExpectedResults(Home.getPath(EXPECTED_PATH + "slicing.txt")))
            );
        }
    }

    @Test
    public void Test_AllArraysAllTypes() throws Exception {
        if (!remote) {
            throw new RuntimeException("Test is available only in remote mode: use -Drunner.remote=true");
        }

        if (GENERATE_EXPECTED) {
            qqlWriteExpected(db, QQL_ALL_TYPES, Home.getPath(EXPECTED_GENERATE_PATH + "slicing_alltypes.txt"));
        } else {
            Assert.assertTrue(
                    qql(db, QQL_ALL_TYPES, readExpectedResults(Home.getPath(EXPECTED_PATH + "slicing_alltypes.txt")))
            );
        }
    }

    @Test
    public void Test_AllPredicates() throws Exception {
        if (!remote) {
            throw new RuntimeException("Test is available only in remote mode: use -Drunner.remote=true");
        }

        if (GENERATE_EXPECTED) {
            qqlWriteExpected(db, QQL_ALL_PREDICATES, Home.getPath(EXPECTED_GENERATE_PATH + "predicates.txt"));
        } else {
            Assert.assertTrue(
                    qql(db, QQL_ALL_PREDICATES, readExpectedResults(Home.getPath(EXPECTED_PATH + "predicates.txt")))
            );
        }
    }

    @Test
    public void Test_AllArrayJoin() throws Exception {
        if (!remote) {
            throw new RuntimeException("Test is available only in remote mode: use -Drunner.remote=true");
        }

        if (GENERATE_EXPECTED) {
            qqlWriteExpected(db, QQL_ALL_ARRAY_JOIN, Home.getPath(EXPECTED_GENERATE_PATH + "array_join.txt"));
        } else {
            Assert.assertTrue(
                    qql(db, QQL_ALL_ARRAY_JOIN, readExpectedResults(Home.getPath(EXPECTED_PATH + "array_join.txt")))
            );
        }
    }

    @Test
    public void Test_AllGroupBy() throws Exception {
        if (!remote) {
            throw new RuntimeException("Test is available only in remote mode: use -Drunner.remote=true");
        }

        if (GENERATE_EXPECTED) {
            qqlWriteExpected(db, QQL_ALL_GROUP_BY, Home.getPath(EXPECTED_GENERATE_PATH + "groupby.txt"));
        } else {
            Assert.assertTrue(
                    qql(db, QQL_ALL_GROUP_BY, readExpectedResults(Home.getPath(EXPECTED_PATH + "groupby.txt")))
            );
        }
    }

    @Test
    public void Test_AllMisc() throws Exception {
        if (!remote) {
            throw new RuntimeException("Test is available only in remote mode: use -Drunner.remote=true");
        }

        if (GENERATE_EXPECTED) {
            qqlWriteExpected(db, QQL_ALL_MISC, Home.getPath(EXPECTED_GENERATE_PATH + "misc.txt"));
        } else {
            Assert.assertTrue(
                    qql(db, QQL_ALL_MISC, readExpectedResults(Home.getPath(EXPECTED_PATH + "misc.txt")))
            );
        }
    }

    @Test
    public void Test_Unions() throws Exception {
        if (!remote) {
            throw new RuntimeException("Test is available only in remote mode: use -Drunner.remote=true");
        }

        if (GENERATE_EXPECTED) {
            qqlWriteExpected(db, QQL_ALL_UNIONS, Home.getPath(EXPECTED_GENERATE_PATH + "unions.txt"));
        } else {
            Assert.assertTrue(
                    qql(db, QQL_ALL_UNIONS, readExpectedResults(Home.getPath(EXPECTED_PATH + "unions.txt")))
            );
        }
    }

    @Test
    public void Test_ArrayChange() throws Exception {
        if (remote) {
            System.err.println("Test is available only in embedded mode: use -Drunner.remote=false");
            return;
        }

        qqlReadAll(db, "select entry.level, count{}(), timestamp " +
                "from kraken " +
                "array join entries as entry " +
                "group by symbol, timestamp, entry.level");

    }

    @Test
    public void Test_Filter() throws Exception {
        if (remote) {
            System.err.println("Test is available only in embedded mode: use -Drunner.remote=false");
            return;
        }

//        DXTickStream stream1 = db.getStream("securities_adia");
//        TickCursor cursor = stream1.select(Long.MIN_VALUE, new SelectionOptions(true, false));
//        ArrayFilter filter = new ArrayFilter();
//        filter.types = stream1.getTypes();
//        filter.inputTypes = stream1.getTypes();
//        filter.outputTypes = filter.inputTypes;
//        InstrumentMessageSource source = filter.ex(cursor);
//        int maxCount = 20;
//        while (source.next()) {
////            System.out.println("MSG: " + source.getMessage());
//            if (--maxCount <= 0) {
//                break;
//            }
//        }
    }

    public static void main1(String[] args) throws Exception {
        db = createDb(remote);
        importStream(db, "BINANCE", "testdata/tickdb/misc/qql/BINANCE.30s.qsmsg.gz");
        loadPackagesData(db, "packages", true);
        loadAllTypesData(db, "alltypes", true);
//        DXTickDB db = TickDBFactory.openFromUrl("dxtick://localhost:8102", false);

        try {
            InstrumentMessageSource source = db.executeQuery(
                    "select " +
                            "record doubleField type t1 when boolField " +
                            "record doubleField type t2 when not boolField " +
                            "from alltypes limit 10"
            );
            for (int i = 0; i < 500; ++i) {
                source.next();
                System.out.println(source.getMessage());
            }
        } catch (Throwable t) {
            t.printStackTrace();
        }

//        loadSubclassedData(db, "subclasses", true);
    }

    // --------- RUN TESTS
    private static void qql(DXTickDB db, String[] qqls) throws Exception {
        for (int i = 0; i < qqls.length; ++i) {
            qql(db, qqls[i]);
        }
    }

    private static void qql(DXTickDB db, String qql) throws Exception {
        System.out.println(RUNNING_QQL_LABEL + qql);

        SelectionOptions options = new SelectionOptions();
        options.typeLoader = QQLTypeLoader.createTypeLoader();
        try (InstrumentMessageSource source = db.executeQuery(qql, options)) {
            int maxCount = 200;
            while (source.next()) {
                System.out.println(source.getMessage());
                if (--maxCount <= 0) {
                    break;
                }
            }
        }
    }

    private static void qqlReadAll(DXTickDB db, String qql) throws Exception {
        System.out.println(RUNNING_QQL_LABEL + qql);

        SelectionOptions options = new SelectionOptions();
        options.typeLoader = QQLTypeLoader.createTypeLoader();
        try (InstrumentMessageSource source = db.executeQuery(qql, options)) {
            int maxCount = 0;
            while (source.next()) {
                maxCount++;
            }
            System.out.println("Read count: " + maxCount);
        }
    }

    private static boolean qql(DXTickDB db, Pair<String, MappingInfo>[] qqls, Map<String, TestResult> results) {
        int errors = 0;
        for (int i = 0; i < qqls.length; ++i) {
            TestResult actual = qql(db, qqls[i].getFirst(), qqls[i].getSecond());
            TestResult expected = results.get(actual.query);
            if (expected == null) {
                System.err.println("WARNING: expected results not found for query: " + actual.query);
                errors++;
            } else {
                if (!compare(expected, actual)) {
                    errors++;
                    System.out.println("Test ERROR");
                } else {
                    System.out.println("Test OK");
                }
            }
        }

        System.out.println("Tests OK: " + (qqls.length - errors) + "; Errors: " + errors);

        return errors == 0;
    }

    private static void qqlWriteExpected(DXTickDB db, Pair<String, MappingInfo>[] qqls, String outputFile) {
        Paths.get(outputFile).getParent().toFile().mkdirs();
        try (FileWriter writer = new FileWriter(outputFile)) {
            for (int i = 0; i < qqls.length; ++i) {
                TestResult result = qql(db, qqls[i].getFirst(), qqls[i].getSecond());
                writer.write(RUNNING_QQL_LABEL + result.query + "\n");
                for (String resultLine : result.results) {
                    writer.write(resultLine + "\n");
                }
                writer.write(END_TEST_LABEL + "\n");
            }
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }

    private static boolean compare(TestResult expected, TestResult actual) {
        for (int i = 0; i < expected.results.size() && i < actual.results.size(); ++i) {
            String expectedLine = expected.results.get(i);
            String actualLine = actual.results.get(i);

            if (!expectedLine.trim().equals(actualLine.trim())) {
                System.err.println("QUERY: " + actual.query);
                System.err.println("ACTUAL: " + actualLine);
                System.err.println("EXPECTED: " + expectedLine);

                return false;
            }
        }

        if (expected.results.size() != actual.results.size()) {
            System.err.println("ACTUAL SIZE: " + actual.results.size());
            System.err.println("EXPECTED SIZE: " + expected.results.size());

            return false;
        }

        return true;
    }

    private static TestResult qql(DXTickDB db, String qql, MappingInfo mappingInfo) {
        TestResult result = new TestResult(qql);

        System.out.println(RUNNING_QQL_LABEL + qql);

        JSONRawMessagePrinter printer = new JSONRawMessagePrinter();

        try {
            SelectionOptions options = new SelectionOptions();
            if (mappingInfo != null && mappingInfo.cls != null) {
                QQLTypeLoader.QueryTypeLoader typeLoader = QQLTypeLoader.createTypeLoader(mappingInfo.cls);
                typeLoader.bind(mappingInfo.name, mappingInfo.cls);

                options.typeLoader = typeLoader;
                options.raw = false;
            } else {
                options.raw = true;
            }

            StringBuilder sb = new StringBuilder();

            try (InstrumentMessageSource source = db.executeQuery(qql, options)) {
                int maxCount = 20;

                while (source.next()) {
                    InstrumentMessage msg = source.getMessage();

                    String message;
                    if (msg instanceof RawMessage) {
                        message = mappingInfo.json ? toJsonString(printer, (RawMessage) msg) : toString((RawMessage) msg, true);
                    } else {
                        message = msg.toString();
                    }

                    String[] results = message.split("\n");
                    for (int i = 0; i < results.length; ++i) {
                        result.append(results[i]);
                    }
                    System.out.println(message);
                    if (--maxCount <= 0) {
                        break;
                    }
                }
            }
        } catch (Throwable t) {
            result.append(t.getMessage());
            System.out.println(t.getMessage());
        }

        System.out.println(END_TEST_LABEL);

        return result;
    }

    private static Map<String, TestResult> readExpectedResults(String path) throws IOException {
        Map<String, TestResult> results = new HashMap<>();
        List<String> lines = Files.lines(Paths.get(path)).collect(Collectors.toList());

        TestResult result = null;
        for (String line : lines) {
            if (line.startsWith(RUNNING_QQL_LABEL)) {
                result = new TestResult(line.substring(RUNNING_QQL_LABEL.length()));
                results.put(result.query, result);
            } else if (result != null) {
                if (line.equalsIgnoreCase(END_TEST_LABEL)) {
                    result = null;
                } else {
                    result.append(line);
                }
            }
        }

        return results;
    }

    // --------- GENERATE AND LOAD DATA
    private static void loadAllData() throws Exception {
        loadAllTypesData(db, "alltypes", true);
        loadAllTypesRandom(db, "alltypesrand", true);
        loadData(db, "stream1", true);
        loadPackagesData(db, "packages", true);
        loadOrdersData(db, "orders", true);
        loadExecutedInfoA(db, "infoA", true);
        loadExecutedInfoB(db, "infoB", true);
        loadBars(db, "bar1min", true);
        loadBars2(db, "bar1minExtended", true);
        loadSubclassedData(db, "subclasses", true);
        importStream(db, "BINANCE", "BINANCE.30s.qsmsg.gz");
        importStream(db, "BITFINEX", "BITFINEX.30s.qsmsg.gz");
        importStream(db, "KRAKEN", "KRAKEN.30s.qsmsg.gz");
    }

    private static void importStream(DXTickDB db, String streamKey, String file) throws IOException {
        DXTickStream stream = db.getStream(streamKey);
        if (stream != null) {
            stream.delete();
        }

        InputStream inputStream = IOUtil.openResourceAsStream("com/epam/deltix/qqltest/" + file);

        MessageReader2 reader = new MessageReader2(inputStream, 1, file.endsWith(".gz"), 1 << 20, null);
        StreamOptions options = new StreamOptions();
        options.setPolymorphic(reader.getTypes());
        stream = db.createStream(streamKey, options);
        try (TickLoader loader = stream.createLoader(new LoadingOptions(true))) {
            while (reader.next()) {
                loader.send(reader.getMessage());
            }
        }
    }

    private static void loadAllTypesData(DXTickDB db, String streamKey, boolean createStream) {
        if (createStream) {
            DXTickStream stream1 = db.getStream(streamKey);
            if (stream1 != null) {
                stream1.delete();
            }
        }

        TestMessagesHelper helper = new TestMessagesHelper(
//            Generator.createRandom(1234, 5, 10)
                Generator.createFixed(10, 20)
        );
        DXTickStream stream = helper.createStream(db, streamKey);
        try (TickLoader loader = stream.createLoader()) {
            for (int i = 0; i < 10; i++) {
                AllTypesMessage message = helper.nextMessage();
                message.setTimeStampMs(0);
                loader.send(message);
            }
        }
    }

    private static void loadAllTypesRandom(DXTickDB db, String streamKey, boolean createStream) {
        if (createStream) {
            DXTickStream stream1 = db.getStream(streamKey);
            if (stream1 != null) {
                stream1.delete();
            }
        }

        TestMessagesHelper helper = new TestMessagesHelper(new PerFieldRandomGenerator(1234, 10, 10));
        DXTickStream stream = helper.createStream(db, streamKey);

        LocalDateTime startTime = LocalDateTime.of(2021, 1, 1, 0, 0, 0);
        LocalDateTime endTime = startTime.plusDays(1);
        helper.loadMessages(
                stream,
                startTime.toEpochSecond(ZoneOffset.UTC) * 1000,
                endTime.toEpochSecond(ZoneOffset.UTC) * 1000,
                TimeUnit.MINUTES.toMillis(1),
                "S1", "S2", "S3"
        );
    }

    private static void loadPackagesData(DXTickDB db, String streamKey, boolean createStream) throws Exception {
        if (createStream) {
            DXTickStream stream1 = db.getStream(streamKey);
            if (stream1 != null) {
                stream1.delete();
            }
        }

        DXTickStream stream = db.getStream(streamKey);
        if (stream == null) {
            StreamOptions options = new StreamOptions();
            Introspector introspector = Introspector.createEmptyMessageIntrospector();
            RecordClassDescriptor[] rcds = new RecordClassDescriptor[] {
                    introspector.introspectRecordClass(Package.class)
            };

            options.setPolymorphic(rcds);
            stream = db.createStream(streamKey, options);
        }

        stream.truncate(Long.MIN_VALUE);

        LoadingOptions options = new LoadingOptions();
        options.typeLoader = QQLTypeLoader.createTypeLoader();
        try (TickLoader loader = stream.createLoader(options)) {
            LocalDateTime dateTime = LocalDateTime.of(2021, 1, 1, 9, 0);
            for (int i = 0; i < 10; ++i) {
                loader.send(generatePackage(dateTime, "DLTX", i + 1));
            }
        }
    }

    private static void loadOrdersData(DXTickDB db, String streamKey, boolean createStream) throws Exception {
        if (createStream) {
            DXTickStream stream1 = db.getStream(streamKey);
            if (stream1 != null) {
                stream1.delete();
            }
        }

        DXTickStream stream = db.getStream(streamKey);
        if (stream == null) {
            StreamOptions options = new StreamOptions();
            Introspector introspector = Introspector.createEmptyMessageIntrospector();
            RecordClassDescriptor[] rcds = new RecordClassDescriptor[] {
                    introspector.introspectRecordClass(OrderEvent.class)
            };

            options.setPolymorphic(rcds);
            stream = db.createStream(streamKey, options);
        }

        stream.truncate(Long.MIN_VALUE);

        LoadingOptions options = new LoadingOptions();
        options.typeLoader = QQLTypeLoader.createTypeLoader();
        try (TickLoader loader = stream.createLoader(options)) {
            LocalDateTime dateTime = LocalDateTime.of(2021, 1, 1, 9, 0);
            for (int i = 0; i < 5; ++i) {
                dateTime = dateTime.plusSeconds(1);
                OrderEvent marketOrder = generateMarketOrder(dateTime, "DLTX", i);
                loader.send(marketOrder);

                OrderEvent limitOrder = generateLimitOrder(dateTime, "DLTX", i);
                loader.send(limitOrder);
            }
        }
    }

    private static void loadExecutedInfoA(DXTickDB db, String streamKey, boolean createStream) throws Exception {
        if (createStream) {
            DXTickStream stream1 = db.getStream(streamKey);
            if (stream1 != null) {
                stream1.delete();
            }
        }

        DXTickStream stream = db.getStream(streamKey);
        if (stream == null) {
            StreamOptions options = new StreamOptions();
            Introspector introspector = Introspector.createEmptyMessageIntrospector();
            RecordClassDescriptor[] rcds = new RecordClassDescriptor[] {
                    introspector.introspectRecordClass(ExecutedLimitOrderInfoA.class)
            };

            options.setPolymorphic(rcds);
            stream = db.createStream(streamKey, options);
        }

        stream.truncate(Long.MIN_VALUE);

        LoadingOptions options = new LoadingOptions();
        options.typeLoader = QQLTypeLoader.createTypeLoader();
        try (TickLoader loader = stream.createLoader(options)) {
            LocalDateTime dateTime = LocalDateTime.of(2021, 1, 1, 9, 0);
            for (int i = 0; i < 20; ++i) {
                dateTime = dateTime.plusSeconds(1);

                ExecutedLimitOrderInfoA infoA = new ExecutedLimitOrderInfoA();
                infoA.setTimeStampMs(dateTime.toEpochSecond(ZoneOffset.UTC));
                infoA.setSymbol("AAAA");
                //infoA.setInstrumentType(InstrumentType.FX);
                infoA.setAvgPrice(1.111f * i);
                infoA.setTotalQuantity(2.222f * i);
                infoA.setCustomInfo(3.333f * i);
                infoA.setInfoIdA(4 * i);

                loader.send(infoA);
            }
        }
    }


    private static void loadExecutedInfoB(DXTickDB db, String streamKey, boolean createStream) throws Exception {
        if (createStream) {
            DXTickStream stream1 = db.getStream(streamKey);
            if (stream1 != null) {
                stream1.delete();
            }
        }

        DXTickStream stream = db.getStream(streamKey);
        if (stream == null) {
            StreamOptions options = new StreamOptions();
            Introspector introspector = Introspector.createEmptyMessageIntrospector();
            RecordClassDescriptor[] rcds = new RecordClassDescriptor[] {
                    introspector.introspectRecordClass(ExecutedLimitOrderInfoB.class)
            };

            options.setPolymorphic(rcds);
            stream = db.createStream(streamKey, options);
        }

        stream.truncate(Long.MIN_VALUE);

        LoadingOptions options = new LoadingOptions();
        options.typeLoader = QQLTypeLoader.createTypeLoader();
        try (TickLoader loader = stream.createLoader(options)) {
            LocalDateTime dateTime = LocalDateTime.of(2021, 1, 1, 9, 0);
            for (int i = 0; i < 20; ++i) {
                dateTime = dateTime.plusSeconds(1);

                ExecutedLimitOrderInfoB infoB = new ExecutedLimitOrderInfoB();
                infoB.setTimeStampMs(dateTime.toEpochSecond(ZoneOffset.UTC));
                infoB.setSymbol("BBBB");
                infoB.setAvgPrice(5.555f * i);
                infoB.setTotalQuantity(6.666f * i);
                infoB.setInfoIdB(8 * i);

                loader.send(infoB);
            }
        }
    }

    private static void loadBars(DXTickDB db, String streamKey, boolean createStream) throws Exception {
        if (createStream) {
            DXTickStream stream1 = db.getStream(streamKey);
            if (stream1 != null) {
                stream1.delete();
            }
        }

        DXTickStream stream = db.getStream(streamKey);
        if (stream == null) {
            InstrumentMessageSource query = db.executeQuery("CREATE DURABLE STREAM \"" + streamKey + "\" (\n" +
                    "    CLASS \"deltix.timebase.api.messages.MarketMessage\" (        \n" +
                    "        \"sequenceNumber\" 'Sequence Number' INTEGER\n" +
                    "    )\n" +
                    "        AUXILIARY\n" +
                    "        NOT INSTANTIABLE;\n" +
                    "    CLASS \"com.epam.deltix.qsrv.test.messages.BarMessage\" UNDER \"deltix.timebase.api.messages.MarketMessage\" (\n" +
                    "        \"exchangeId\" 'Exchange Code' VARCHAR ALPHANUMERIC (10),\n" +
                    "        \"close\" 'Close' FLOAT DECIMAL,\n" +
                    "        \"open\" 'Open' FLOAT DECIMAL RELATIVE TO \"close\",\n" +
                    "        \"high\" 'High' FLOAT DECIMAL RELATIVE TO \"close\",\n" +
                    "        \"low\" 'Low' FLOAT DECIMAL RELATIVE TO \"close\",\n" +
                    "        \"volume\" 'Volume' FLOAT\n" +
                    "    );\n" +
                    ")\n" +
                    "OPTIONS (FIXEDTYPE; PERIODICITY = '1I'; HIGHAVAILABILITY = FALSE; UNIQUE = FALSE; STORAGEVERSION = '5')");
//            StreamOptions options = new StreamOptions();
//            Introspector introspector = Introspector.createEmptyMessageIntrospector();
//            RecordClassDescriptor[] rcds = new RecordClassDescriptor[] {
//                    introspector.introspectRecordClass(BarMessage.class)
//            };
//
//            options.setPolymorphic(rcds);
//            options.periodicity = Periodicity.parse("1I");
//            stream = db.createStream(streamKey, options);
            if (query.next())
                stream = db.getStream(streamKey);
        }


        stream.truncate(Long.MIN_VALUE);

        LoadingOptions options = new LoadingOptions();
        options.typeLoader = QQLTypeLoader.createTypeLoader();
        try (TickLoader loader = stream.createLoader(options)) {
            LocalDateTime dateTime = LocalDateTime.of(2021, 1, 1, 9, 0);
            for (int i = 0; i < 20; ++i) {
                dateTime = dateTime.plusMinutes(1);

                BarMessage bar = new BarMessage();
                bar.setTimeStampMs(dateTime.toEpochSecond(ZoneOffset.UTC));
                bar.setOpen(1.111f * i);
                bar.setClose(2.222f * i);
                bar.setHigh(3.333f * i);
                bar.setLow(3.333f * i);
                bar.setVolume(4.444 * i);

                loader.send(bar);
            }
        }
    }

    private static void loadBars2(DXTickDB db, String streamKey, boolean createStream) throws Exception {
        if (createStream) {
            DXTickStream stream1 = db.getStream(streamKey);
            if (stream1 != null) {
                stream1.delete();
            }
        }

        DXTickStream stream = db.getStream(streamKey);
        if (stream == null) {
//            StreamOptions options = new StreamOptions();
//            Introspector introspector = Introspector.createEmptyMessageIntrospector();
//            RecordClassDescriptor[] rcds = new RecordClassDescriptor[] {
//                    introspector.introspectRecordClass(BarMessageExtended.class)
//            };
//
//            options.setPolymorphic(rcds);
//            options.periodicity = Periodicity.parse("1I");
//            stream = db.createStream(streamKey, options);

            InstrumentMessageSource query = db.executeQuery("CREATE DURABLE STREAM \"" + streamKey + "\" (\n" +
                    "    CLASS \"deltix.timebase.api.messages.MarketMessage\" (        \n" +
                    "        \"sequenceNumber\" 'Sequence Number' INTEGER\n" +
                    "    )\n" +
                    "        AUXILIARY\n" +
                    "        NOT INSTANTIABLE;\n" +
                    "    CLASS \"BarMessageExtended\" UNDER \"deltix.timebase.api.messages.MarketMessage\" (\n" +
                    "        \"exchangeId\" 'Exchange Code' VARCHAR ALPHANUMERIC (10),\n" +
                    "        \"close\" 'Close' FLOAT DECIMAL,\n" +
                    "        \"open\" 'Open' FLOAT DECIMAL RELATIVE TO \"close\",\n" +
                    "        \"high\" 'High' FLOAT DECIMAL RELATIVE TO \"close\",\n" +
                    "        \"low\" 'Low' FLOAT DECIMAL RELATIVE TO \"close\",\n" +
                    "        \"volume\" 'Volume' FLOAT,\n" +
                    "        \"customValue\" FLOAT IEEE32\n" +
                    "    );\n" +
                    ")\n" +
                    "OPTIONS (FIXEDTYPE; PERIODICITY = '1I'; HIGHAVAILABILITY = FALSE; UNIQUE = FALSE; STORAGEVERSION = '5')");

            if (query.next())
                stream = db.getStream(streamKey);

        }

        stream.truncate(Long.MIN_VALUE);

        LoadingOptions options = new LoadingOptions();
        options.typeLoader = TypeLoader.TYPE_LOADER;
        try (TickLoader loader = stream.createLoader(options)) {
            LocalDateTime dateTime = LocalDateTime.of(2021, 1, 1, 9, 0);
            for (int i = 0; i < 20; ++i) {
                dateTime = dateTime.plusMinutes(1);

                BarMessageExtended bar = new BarMessageExtended();
                bar.setTimeStampMs(dateTime.toEpochSecond(ZoneOffset.UTC));
                bar.setOpen(1.111f * i);
                bar.setClose(2.222f * i);
                bar.setHigh(3.333f * i);
                bar.setLow(3.333f * i);
                bar.setVolume(4.444 * i);
                bar.setCustomValue(11.1111f * i);

                loader.send(bar);
            }
        }
    }

    private static void loadSubclassedData(DXTickDB db, String streamKey, boolean createStream) throws Introspector.IntrospectionException {
        if (createStream) {
            DXTickStream stream = db.getStream(streamKey);
            if (stream != null) {
                stream.delete();
            }
        }

        DXTickStream stream = db.getStream(streamKey);
        if (stream == null) {
            StreamOptions options = new StreamOptions();
            Introspector introspector = Introspector.createEmptyMessageIntrospector();
            RecordClassDescriptor[] rcds = new RecordClassDescriptor[] {
                    introspector.introspectRecordClass(Test1Msg.class),
                    introspector.introspectRecordClass(Test2Msg.class)
            };

            options.setPolymorphic(rcds);
            stream = db.createStream(streamKey, options);
        }

        LoadingOptions options = new LoadingOptions();
        options.typeLoader = QQLTypeLoader.createTypeLoader();
        try (TickLoader loader = stream.createLoader(options)) {
            LocalDateTime dateTime = LocalDateTime.of(2021, 1, 1, 9, 0);
            for (int i = 0; i < 20; ++i) {
                dateTime = dateTime.plusMinutes(1);

                Test1Msg test1 = new Test1Msg();
                test1.setTimeStampMs(dateTime.toEpochSecond(ZoneOffset.UTC));
                test1.setAaa(11.1 + i);
                test1.setOoo(getSubclass((i % 2) + 1, 10 + i));
                test1.setOooaaa(getSubclasses2(5, 1, 2, 20 + i));
                loader.send(test1);

                Test2Msg test2 = new Test2Msg();
                test2.setTimeStampMs(dateTime.toEpochSecond(ZoneOffset.UTC));
                test2.setAaa(22.2 + i);
                test2.setOoo(getSubclass((i % 2) + 2, 30 + i));
                test2.setOooaaa(getSubclasses2(5, 2, 3, 40 + i));
                loader.send(test2);
            }
        }
    }

    private static SubclassMsg getSubclass(int n, int size) {
        switch (n) {
            case 1: return new Subclass1Msg(size, size + 1, size + 2);
            case 2: return new Subclass2Msg(size, size + 2, size + 4);
            case 3: return new Subclass3Msg(size, size + 3, size + 6);
            default:
                return new Subclass1Msg(size, size + 1, size + 1);
        }
    }

    private static ObjectArrayList<SubclassMsg> getSubclasses(int count, int n, int size) {
        ObjectArrayList<SubclassMsg> subclasses = new ObjectArrayList<>();
        for (int i = 0; i < count; ++i) {
            subclasses.add(getSubclass(n, 100 + size));
        }

        return subclasses;
    }

    private static ObjectArrayList<SubclassMsg> getSubclasses2(int count, int n1, int n2, int size) {
        ObjectArrayList<SubclassMsg> subclasses = getSubclasses(count, n1, size);
        subclasses.addAll(getSubclasses(count, n2, size));
        return subclasses;
    }

    private static void loadData(DXTickDB db, String streamKey, boolean createStream) throws Exception {
        if (createStream) {
            DXTickStream stream1 = db.getStream(streamKey);
            if (stream1 != null) {
                stream1.delete();
            }
        }

        DXTickStream stream = db.getStream(streamKey);
        if (stream == null) {
            StreamOptions options = new StreamOptions();
            Introspector introspector = Introspector.createEmptyMessageIntrospector();
            RecordClassDescriptor[] rcds = new RecordClassDescriptor[] {
                    introspector.introspectRecordClass(Package.class),
                    introspector.introspectRecordClass(OrderEvent.class)
            };

            options.setPolymorphic(rcds);
            stream = db.createStream(streamKey, options);
        }

        stream.truncate(Long.MIN_VALUE);

        LoadingOptions options = new LoadingOptions();
        options.typeLoader = QQLTypeLoader.createTypeLoader();
        try (TickLoader loader = stream.createLoader(options)) {
            LocalDateTime dateTime = LocalDateTime.of(2021, 1, 1, 9, 0);
            for (int i = 0; i < 3; ++i) {
                loader.send(generatePackage(dateTime, "DLTX", i + 1));
            }

            for (int i = 0; i < 5; ++i) {
                dateTime = dateTime.plusSeconds(1);
                OrderEvent marketOrder = generateMarketOrder(dateTime, "DLTX", i);
                loader.send(marketOrder);

                OrderEvent limitOrder = generateLimitOrder(dateTime, "DLTX", i);
                loader.send(limitOrder);
            }
        }
    }

    private static OrderEvent generateMarketOrder(LocalDateTime dateTime, String symbol, int n) {
        OrderEvent message = new OrderEvent();
        message.setTimeStampMs(dateTime.toEpochSecond(ZoneOffset.UTC) * 1000);
        message.setSymbol(symbol);

        MarketOrder marketOrder = new MarketOrder();
        marketOrder.setSequence(n * 1.111f);
        marketOrder.setId(generateId(n, "MO_SOURCE"));

        marketOrder.setExecution(generateExecution(n));

        MarketOrderInfo marketOrderInfo = new MarketOrderInfo();
        marketOrderInfo.setSize(10000 + n);
        marketOrderInfo.setSide(AggressorSide.values()[n % 2]);
        marketOrderInfo.setUserId("USER#" + (100 + n));
        marketOrderInfo.setExecutedInfo(generateExecutedMarketOrderInfo(n));
        marketOrderInfo.setExecutedInfoHistory(generateExecutedInfoCArray(5, n));
        marketOrder.setInfo(marketOrderInfo);

        message.setOrder(marketOrder);

        return message;
    }

    private static OrderEvent generateLimitOrder(LocalDateTime dateTime, String symbol, int n) {
        OrderEvent message = new OrderEvent();
        message.setTimeStampMs(dateTime.toEpochSecond(ZoneOffset.UTC) * 1000);
        message.setSymbol(symbol);

        LimitOrder limitOrder = new LimitOrder();
        limitOrder.setSequence(n * 2.222f);
        limitOrder.setId(generateId(n, "LO_SOURCE"));

        ObjectArrayList<Execution> executions = new ObjectArrayList<>();
        for (int i = 0; i < 3; ++i) {
            executions.add(generateExecution(n * 10 + i)); // 3 executions
        }
        limitOrder.setExecutions(executions);

        LimitOrderInfo limitOrderInfo = new LimitOrderInfo();
        limitOrderInfo.setPrice(100 + n);
        limitOrderInfo.setSize(1000 + n);
        limitOrderInfo.setSide(AggressorSide.values()[n % 2]);

        limitOrderInfo.setUserId(100 + n);
        limitOrderInfo.setExecutedInfo(generateExecutedLimitOrderInfoAB(n));
        limitOrderInfo.setExecutedInfoHistory(generateExecutedInfoABHistory(5, n));
        limitOrder.setInfo(limitOrderInfo);

        limitOrder.setCustomTags(generateSomeVarchars("TAG#", 5, n));

        message.setOrder(limitOrder);

        return message;
    }

    private static ObjectArrayList<ExecutedInfo> generateExecutedInfoABHistory(int count, int n) {
        ObjectArrayList<ExecutedInfo> executedInfos = new ObjectArrayList<>();
        for (int i = 0; i < count; ++i) {
            executedInfos.add(generateExecutedLimitOrderInfoAB(n + i));
        }

        return executedInfos;
    }

    private static ObjectArrayList<ExecutedMarketOrderInfo> generateExecutedInfoCArray(int count, int n) {
        ObjectArrayList<ExecutedMarketOrderInfo> executedInfos = new ObjectArrayList<>();
        for (int i = 0; i < count; ++i) {
            executedInfos.add(generateExecutedMarketOrderInfo(n + i));
        }

        return executedInfos;
    }

    private static ExecutedInfo generateExecutedLimitOrderInfoAB(int n) {
        if (n % 2 == 0) {
            ExecutedLimitOrderInfoA executedInfo = new ExecutedLimitOrderInfoA();
            executedInfo.setAvgPrice(12345 + n);
            executedInfo.setTotalQuantity(12345 + n);
            executedInfo.setInfoIdA(100 + n);
            executedInfo.setCustomInfo(5000 + n);
            return executedInfo;
        } else {
            ExecutedLimitOrderInfoB executedInfo = new ExecutedLimitOrderInfoB();
            executedInfo.setAvgPrice(23456 + n);
            executedInfo.setTotalQuantity(23456 + n);
            executedInfo.setInfoIdB(5500 + n);
            return executedInfo;
        }
    }

    private static ExecutedMarketOrderInfo generateExecutedMarketOrderInfo(int n) {
        ExecutedMarketOrderInfo executedInfo = new ExecutedMarketOrderInfo();
        executedInfo.setAvgPrice(11111 + n);
        executedInfo.setTotalQuantity(22222 + n);
        executedInfo.setCustomInfo(generateSomeLongs(15, (300 + n)));
        executedInfo.setInfoId(6000 + n);
        return executedInfo;
    }

    private static Execution generateExecution(int n) {
        Execution execution = new Execution();
        execution.setId(generateId(n, "EX_SOURCE"));

        ExecutionInfo executionInfo = new ExecutionInfo();
        executionInfo.setPrice(100 + n);
        executionInfo.setSize(10000 + n);
        executionInfo.setSide(AggressorSide.values()[n % 2]);
        executionInfo.setCommissionInfo(generateCommissionInfo(n));
        execution.setInfo(executionInfo);

        execution.setAttributes(generateAttributes(5, n));
        execution.setCustomTags(generateSomeVarchars("EXECUTION TAG", 7, n));

        return execution;
    }

    private static CommissionInfo generateCommissionInfo(int n) {
        CommissionInfo commissionInfo = new CommissionInfo();
        commissionInfo.setCommission(n * 333.3f);
        commissionInfo.setCurrency(n % 2 == 0 ? "USD" : "JPY");

        return commissionInfo;
    }

    private static ObjectArrayList<Attribute> generateAttributes(int count, int n) {
        ObjectArrayList<Attribute> attributes = new ObjectArrayList<>();
        for (int i = 0; i < count; ++i) {
            attributes.add(generateAttribute(n * 10 + i));
        }

        return attributes;
    }

    private static Attribute generateAttribute(int n) {
        if (n % 2 == 0) {
            FixAttribute attribute = new FixAttribute();
            attribute.setKey(5000 + n);
            if (n % 4 != 0 && n > 0) {
                attribute.setValue("Value #" + (6000 + n));
            }
            AttributeId attributeId = new AttributeId();
            attributeId.setId(7000 + n);
            attribute.setAttributeId(attributeId);
            attribute.setExtended(generateExtendedAttribute(100 * n));
            attribute.setExtendedAttributes(generateExtendedAttributes(3, n));

            return attribute;
        } else {
            CustomAttribute attribute = new CustomAttribute();
            attribute.setKey("Key #" + (5000 + n));
            attribute.setValue("Value #" + (6000 + n));
            AttributeId attributeId = new AttributeId();
            attributeId.setId(8000 + n);
            attribute.setAttributeId(attributeId);
            if (n % 3 != 0 && n > 0) {
                attribute.setExtended(generateExtendedAttribute(200 * n));
                attribute.setExtendedAttributes(generateExtendedAttributes(3, n));
            }

            return attribute;
        }
    }

    private static ObjectArrayList<ExtendedAttribute> generateExtendedAttributes(int count, int n) {
        ObjectArrayList<ExtendedAttribute> attributes = new ObjectArrayList<>();
        for (int i = 0; i < count; ++i) {
            attributes.add(generateExtendedAttribute(n * 100 + i));
        }

        return attributes;
    }

    private static ExtendedAttribute generateExtendedAttribute(int n) {
        ExtendedAttribute attribute = new ExtendedAttribute();
        attribute.setId(n * 111);
        attribute.setKeys(generateSomeInts(12, n * 3));
        attribute.setValues(generateSomeVarchars(7, n));

        return attribute;
    }

    private static Id generateId(int n, String source) {
        Id id = new Id();
        id.setSource(source);
        id.setCorrelationId(10000 + n);

        ExternalId externalId = new ExternalId();
        externalId.setId("ID#" + n);
        id.setExternal(externalId);

        return id;
    }

    private static FloatArrayList generateSomeFloats(int count, int n) {
        FloatArrayList someFloats = new FloatArrayList();
        for (int i = 0; i < count; ++i) {
            someFloats.add(n * 3.3f + i);
        }

        return someFloats;
    }

    private static LongArrayList generateSomeLongs(int count, int n) {
        LongArrayList someLongs = new LongArrayList();
        for (int i = 0; i < count; ++i) {
            someLongs.add(n * 3 + i);
        }

        return someLongs;
    }

    private static IntegerArrayList generateSomeInts(int count, int n) {
        IntegerArrayList someInts = new IntegerArrayList();
        for (int i = 0; i < count; ++i) {
            someInts.add(n * 6 + i);
        }

        return someInts;
    }

    private static ObjectArrayList<CharSequence> generateSomeVarchars(int count, int n) {
        return generateSomeVarchars("Value", count, n);
    }

    private static ObjectArrayList<CharSequence> generateSomeVarchars(String prefix, int count, int n) {
        ObjectArrayList<CharSequence> someFloats = new ObjectArrayList<>();
        for (int i = 0; i < count; ++i) {
            someFloats.add(prefix + " " + (n * 3.3f + i));
        }

        return someFloats;
    }

    private static Package generatePackage(LocalDateTime dateTime, String symbol, int n) {
        Package message = new Package();
        message.setTimeStampMs(dateTime.toEpochSecond(ZoneOffset.UTC) * 1000);
        message.setSymbol(symbol);
        message.setPackageType(n % 4 == 0 ? PackageType.VENDOR_SNAPSHOT : PackageType.INCREMENTAL_UPDATE);

        ObjectArrayList<PackageEntry> entries = new ObjectArrayList<>();

        if (message.getPackageType() == PackageType.INCREMENTAL_UPDATE) {
            int count = 2;
            if (n % 3 == 0) {
                count = 3;
            } else if (n % 7 == 0) {
                count = 0;
            }
            // 2 trades
            for (int j = 0; j < count; ++j) {
                int k = j % 2 == 0 ? 1 : -1;
                TradeEntry trade = new TradeEntry();
                trade.setExchange("BINANCE");
                trade.setPrice(2000 + k * (n * 10 + j));
                trade.setSize(20000 + k * (n * 10 + j));
                trade.setSide(k > 0 ? AggressorSide.BUY : AggressorSide.SELL);
                if (n % 5 != 0) {
                    trade.setAttributes(generateAttributes(count + 2, n * 10 + j));
                }

                entries.add(trade);
            }

            L1Entry l1Entry = new L1Entry();
            l1Entry.setExchange("BINANCE");
            l1Entry.setPrice(2000 + n * 10);
            l1Entry.setSize(20000 + n * 10);
            l1Entry.setSide(QuoteSide.BUY);
            entries.add(l1Entry);

            l1Entry = new L1Entry();
            l1Entry.setExchange("BINANCE");
            l1Entry.setPrice(2000 - n * 11 - 1);
            l1Entry.setSize(20000 - n * 11 - 1);
            l1Entry.setSide(QuoteSide.SELL);
            entries.add(l1Entry);

            for (int j = 0; j < count; ++j) {
                int k = j % 2 == 0 ? j : -j;
                L2Entry l2Entry = new L2Entry();
                l2Entry.setExchange("GDAX");
                l2Entry.setPrice(5000 + k * (n * 10 + j));
                l2Entry.setSize(50000 + k * (n * 10 + j));
                l2Entry.setSide(k > 0 ? QuoteSide.BUY : QuoteSide.SELL);
                l2Entry.setLevel((j + n) % 10);

                entries.add(l2Entry);
            }
        } else {
            for (int j = 0; j < 10; ++j) {
                L2Entry l2Entry = new L2Entry();
                l2Entry.setExchange("GDAX");
                l2Entry.setPrice(5000 + n * 10 + j);
                l2Entry.setSize(50000 + n * 10 + j);
                l2Entry.setSide(QuoteSide.BUY);
                l2Entry.setLevel(j);

                entries.add(l2Entry);
            }
            for (int j = 0; j < 10; ++j) {
                L2Entry l2Entry = new L2Entry();
                l2Entry.setExchange("GDAX");
                l2Entry.setPrice(5000 - n * 10 - j);
                l2Entry.setSize(50000 - n * 10 - j);
                l2Entry.setSide(QuoteSide.SELL);
                l2Entry.setLevel(j);

                entries.add(l2Entry);
            }
        }

        message.setEntries(entries);

        return message;
    }

    public static String               toString (RawMessage msg, boolean printNulls) {

        StringBuilder   sb = new StringBuilder ();

        if (msg.type != null && !msg.type.getName().startsWith("QUERY")) {
            sb.append (msg.type.getName ());
            sb.append (",");
        }

        sb.append (msg.getSymbol());
        sb.append (",");
        if (msg.getTimeStampMs() == DateTimeDataType.NULL)
            sb.append ("<null>");
        else
            sb.append (msg.getTimeString());

        MemoryDataInput in = new MemoryDataInput(msg.data, msg.offset, msg.length);
        UnboundDecoder decoder =
                InterpretingCodecMetaFactory.INSTANCE.createFixedUnboundDecoderFactory (msg.type).create ();

        decoder.beginRead (in);

        int position = 0;

        while (decoder.nextField ()) {
            NonStaticFieldInfo df = decoder.getField ();
            position = sb.length();

            sb.append (",");
            sb.append (df.getName ());
            sb.append (":");
            try {
                if (printNulls)
                    sb.append (decoder.getString ());
                else if (decoder.isNull())
                    sb.setLength(position);
                else
                    sb.append (decoder.getString ());
            } catch (NullValueException e) {
                if (printNulls)
                    sb.append ("<null>");
                else
                    sb.setLength(position);
            }
        }

        return (sb.toString ());
    }

    private static String toJsonString(JSONRawMessagePrinter printer, RawMessage msg) {
        StringBuilder sb = new StringBuilder();
        printer.append(msg, sb);
        return sb.toString();
    }


}
