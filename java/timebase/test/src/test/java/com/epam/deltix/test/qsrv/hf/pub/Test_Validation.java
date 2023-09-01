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
package com.epam.deltix.test.qsrv.hf.pub;

import com.epam.deltix.qsrv.hf.pub.md.*;
import com.epam.deltix.qsrv.hf.tickdb.StreamConfigurationHelper;
import com.epam.deltix.qsrv.hf.tickdb.TDBRunner;
import com.epam.deltix.qsrv.test.messages.BarMessage;
import com.epam.deltix.test.qsrv.hf.tickdb.TDBRunnerBase;
import com.epam.deltix.qsrv.hf.tickdb.pub.*;

import com.epam.deltix.test.qsrv.hf.tickdb.server.ServerRunner;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertTrue;

import org.junit.experimental.categories.Category;
import com.epam.deltix.util.JUnitCategories.TickDBFast;

@Category(TickDBFast.class)
public class Test_Validation extends TDBRunnerBase {

    private static TDBRunner runner;

    @BeforeClass
    public static void start() throws Throwable {
        runner = new ServerRunner(true, true);
        runner.startup();
    }

    @AfterClass
    public static void stop() throws Throwable {
        runner.shutdown();
        runner = null;
    }

    @Test
    public void testPoly() {

        RecordClassDescriptor marketMsgDescriptor =
                StreamConfigurationHelper.mkMarketMessageDescriptor(null);

        RecordClassDescriptor bar = StreamConfigurationHelper.mkUniversalBarMessageDescriptor();


        String name = "aaaa";
        DataField[] fields =  new DataField[] {
                new NonStaticDataField("close", "Close", new IntegerDataType(IntegerDataType.ENCODING_INT32, false, 0, 10)),
                new NonStaticDataField ("open", "Open", new FloatDataType(FloatDataType.ENCODING_FIXED_FLOAT, true)),
                new NonStaticDataField ("high", "High", new FloatDataType (FloatDataType.ENCODING_FIXED_FLOAT, true)),
                new NonStaticDataField ("low", "Low", new FloatDataType (FloatDataType.ENCODING_FIXED_FLOAT, true)),
                new NonStaticDataField ("volume", "Volume", new FloatDataType (FloatDataType.ENCODING_FIXED_FLOAT, true))
        };

        RecordClassDescriptor unbound =  new RecordClassDescriptor (
                BarMessage.CLASS_NAME, name, false,
                marketMsgDescriptor,
                fields
        );

        StreamOptions options = StreamOptions.polymorphic(StreamScope.DURABLE, "poly", "poly", 0, bar, unbound);

        DXTickStream stream = runner.getServerDb().createStream("poly", options);

        try (TickLoader loader = stream.createLoader()) {
            loader.send(new BarMessage());
            assertTrue("Exception expected", false);
        } catch (IllegalArgumentException e) {
            // valid case
        }
    }

    @Test
    public void testFixed() {

        RecordClassDescriptor marketMsgDescriptor =
                StreamConfigurationHelper.mkMarketMessageDescriptor(null);

        String name = "aaaa";
        DataField[] fields =  new DataField[] {
                new NonStaticDataField("close", "Close", new IntegerDataType(IntegerDataType.ENCODING_INT32, false, 0, 10)),
                new NonStaticDataField ("open", "Open", new FloatDataType(FloatDataType.ENCODING_FIXED_FLOAT, true)),
                new NonStaticDataField ("high", "High", new FloatDataType (FloatDataType.ENCODING_FIXED_FLOAT, true)),
                new NonStaticDataField ("low", "Low", new FloatDataType (FloatDataType.ENCODING_FIXED_FLOAT, true)),
                new NonStaticDataField ("volume", "Volume", new FloatDataType (FloatDataType.ENCODING_FIXED_FLOAT, true))
        };

        RecordClassDescriptor unbound =  new RecordClassDescriptor (
                BarMessage.CLASS_NAME, name, false,
                marketMsgDescriptor,
                fields
        );

        StreamOptions options = StreamOptions.fixedType(StreamScope.DURABLE, "fixed", "fixed", 0, unbound);

        DXTickStream stream = runner.getServerDb().createStream("fixed", options);

        try (TickLoader loader = stream.createLoader()) {
            loader.send(new BarMessage());
            assertTrue("Exception expected", false);
        } catch (IllegalArgumentException e) {
            // valid case
        }

    }
}