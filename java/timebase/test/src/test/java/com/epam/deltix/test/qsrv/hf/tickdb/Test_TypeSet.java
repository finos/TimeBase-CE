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
import com.epam.deltix.qsrv.hf.pub.md.NonStaticDataField;
import com.epam.deltix.qsrv.hf.pub.md.RecordClassDescriptor;
import com.epam.deltix.qsrv.hf.pub.md.VarcharDataType;
import com.epam.deltix.qsrv.hf.tickdb.pub.*;
import com.epam.deltix.qsrv.test.messages.BarMessage;
import com.epam.deltix.util.time.TimeKeeper;
import org.junit.Test;

import static org.junit.Assert.assertTrue;

import org.junit.experimental.categories.Category;
import com.epam.deltix.util.JUnitCategories.TickDBFast;

/**
 *
 */
@Category(TickDBFast.class)
public class Test_TypeSet extends TDBTestBase {

    public Test_TypeSet() {
        super(true, true);
    }

    public static class MyMessage extends BarMessage {
        public MyMessage() {
        }

        public String ratio;
    }

    @Test
    public void testSelect() {

        DXTickDB tickDb = getTickDb();

        StreamOptions options = new StreamOptions (StreamScope.DURABLE, "bars", null, 0);
        RecordClassDescriptor bar = StreamConfigurationHelper.mkUniversalBarMessageDescriptor();
        RecordClassDescriptor extened = new RecordClassDescriptor(MyMessage.class.getName(), "extended" , false, bar,
                new NonStaticDataField("ratio", "ratio",
                        new VarcharDataType(VarcharDataType.ENCODING_INLINE_VARSIZE, true, true)));

        options.setPolymorphic(bar, extened);

        DXTickStream stream = tickDb.createStream("extended", options);

        TickLoader loader = stream.createLoader();

        BarMessage msg = new MyMessage();
        msg.setTimeStampMs(TimeKeeper.currentTime);
        ((MyMessage)msg).ratio = "1.0";
        msg.setOpen(10.1);
        msg.setClose(10.1);
        loader.send(msg);

        msg = new BarMessage();
        msg.setTimeStampMs(TimeKeeper.currentTime + 1);
        msg.setOpen(10.1);
        msg.setClose(0.11);
        loader.send(msg);

        loader.close();
        
        TickCursor cursor = stream.select(Long.MIN_VALUE, null);

        try {
            assertTrue(cursor.next());
            assertTrue(cursor.getMessage() instanceof MyMessage);
            assertTrue(cursor.next());
            assertTrue(cursor.getMessage() instanceof BarMessage);
        } finally {
            cursor.close();
        }


    }
}
