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
package com.epam.deltix.qsrv.hf.tickdb.pub;

import com.epam.deltix.qsrv.hf.pub.RawMessage;
import com.epam.deltix.qsrv.hf.pub.md.DataField;
import com.epam.deltix.qsrv.hf.pub.md.NonStaticDataField;
import com.epam.deltix.qsrv.hf.pub.md.RecordClassDescriptor;
import com.epam.deltix.qsrv.hf.pub.md.VarcharDataType;
import com.epam.deltix.qsrv.hf.tickdb.StreamConfigurationHelper;
import org.junit.Test;

import java.util.*;

import static org.junit.Assert.*;

/**
 * @author Daniil Yarmalkevich
 * Date: 6/19/2019
 */
public class RawMessageHelperTest {

    @Test
    public void filterStringTest() {
        RawMessageHelper helper = new RawMessageHelper();
        Map<String, Object> values = new HashMap<>();
        RecordClassDescriptor desc1 = createDesc1();
        RecordClassDescriptor desc2 = createDesc2();

        String a = "a", b = "b", c = "c", d = "d", e = "e", f = "f", g = "g", h = "h";

        RawMessage msg1 = new RawMessage();
        msg1.type = desc1;
        msg1.setSymbol("USD");
        values.put(a, "hello");
        values.put(b, "goodbye");
        values.put(c, "foo");
        values.put(d, "test");
        helper.setValues(msg1, values);
        values.clear();

        RawMessage msg2 = new RawMessage();
        msg2.type = desc1;
        msg2.setSymbol("BTC");
        values.put(a, "goodbye");
        values.put(b, "goodbye");
        values.put(c, "fee");
        values.put(d, "test");
        helper.setValues(msg2, values);
        values.clear();

        RawMessage msg3 = new RawMessage();
        msg3.type = desc2;
        msg2.setSymbol("USD");
        values.put(d, "test");
        values.put(g, "hello");
        helper.setValues(msg3, values);
        values.clear();

        RawMessage msg4 = new RawMessage();
        msg4.type = desc1;
        msg4.setSymbol("ETH");
        values.put(a, "hello");
        values.put(b, "goodbye");
        values.put(c, "foo");
        values.put(d, "test");
        helper.setValues(msg4, values);
        values.clear();

        Map<String, List<String>> params = new HashMap<>();
        params.put(a, Arrays.asList("hello", "goodbye"));
        params.put(b, Collections.singletonList("goodbye"));
        params.put(c, Collections.singletonList("foo"));
        params.put(d, Collections.singletonList("test"));
        params.put("symbol", Arrays.asList("USD", "BTC"));

        assertTrue(helper.isAccepted(msg1, params));
        assertFalse(helper.isAccepted(msg2, params));
        assertFalse(helper.isAccepted(msg3, params));
        assertFalse(helper.isAccepted(msg4, params));
    }


    @Test
    public void testTypeFilter() {
        RawMessageHelper helper = new RawMessageHelper();
        RawMessage message = new RawMessage();
        RecordClassDescriptor rcd = StreamConfigurationHelper.mkUniversalBarMessageDescriptor();
        message.type = rcd;
        String typeName = rcd.getName();
        String shortTypeName = typeName.substring(typeName.lastIndexOf('.') + 1);
        Map<String, List<String>> params1 = new HashMap<>();
        params1.put("type", Collections.singletonList(typeName));
        Map<String, List<String>> params2 = new HashMap<>();
        params2.put("type", Collections.singletonList(shortTypeName));
        Map<String, List<String>> params3 = new HashMap<>();
        Map<String, List<String>> params4 = new HashMap<>();
        params4.put("type", Collections.singletonList("sampleName"));

        assertTrue(helper.isTypeAccepted(message, params1));
        assertTrue(helper.isTypeAccepted(message, params2));
        assertTrue(helper.isTypeAccepted(message, params3));
        assertFalse(helper.isTypeAccepted(message, params4));
    }

    private RecordClassDescriptor createDesc1() {
        DataField[] fields = new DataField[]{
                createVarcharDataField("a"),
                createVarcharDataField("b"),
                createVarcharDataField("c"),
                createVarcharDataField("d"),
                createVarcharDataField("e")
        };
        return new RecordClassDescriptor("Sample1", "Sample1", false, null, fields);
    }

    private RecordClassDescriptor createDesc2() {
        DataField[] fields = new DataField[]{
                createVarcharDataField("d"),
                createVarcharDataField("e"),
                createVarcharDataField("f"),
                createVarcharDataField("g"),
                createVarcharDataField("h"),
        };
        return new RecordClassDescriptor("Sample2", "Sample2", false, null, fields);
    }

    private NonStaticDataField createVarcharDataField(String name) {
        return new NonStaticDataField(name, name, new VarcharDataType("UTF8", true, true));
    }

}