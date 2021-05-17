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

import com.epam.deltix.qsrv.hf.tickdb.pub.*;
import com.epam.deltix.qsrv.hf.pub.md.*;
import com.epam.deltix.util.JUnitCategories;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertEquals;

@Category(JUnitCategories.TickDBFast.class)
public class Test_StreamMetaData extends TDBTestBase {

    public Test_StreamMetaData() {
        super(true);
    }

    @Test
    public void Test1() {
        DXTickDB tdb = getTickDb();

        IntegerDataType type1 = new IntegerDataType(IntegerDataType.ENCODING_INT32, true, 1, 10);
        FloatDataType type2 = new FloatDataType(FloatDataType.ENCODING_FIXED_FLOAT, true, 1f, 100f);

        final String            name = "Test";
        NonStaticDataField close = new NonStaticDataField("close", "Close", type1);
        close.setDescription("<HTML></HTML>");

        RecordClassDescriptor d2 =  new RecordClassDescriptor (
            name, name, false,
            null,
                close,
            new NonStaticDataField("open", "Open", type2)
        );

        tdb.createStream("Test1", StreamOptions.fixedType(StreamScope.DURABLE, "Test1", null, 0, d2));
        tdb.close();

        tdb.open(false);
        
        DXTickStream stream = tdb.getStream("Test1");
        DataField closeField = stream.getFixedType().getField("close");
        assertEquals(close.getDescription(), closeField.getDescription());
        
        IntegerDataType closeType = (IntegerDataType) closeField.getType();
        FloatDataType openType = (FloatDataType) stream.getFixedType().getField("open").getType();
        
        assertTrue(closeType.getMin().longValue() == 1);
        assertTrue(openType.getMin() instanceof Float);
        assertTrue(openType.getMin().floatValue() == 1);

    }
}
