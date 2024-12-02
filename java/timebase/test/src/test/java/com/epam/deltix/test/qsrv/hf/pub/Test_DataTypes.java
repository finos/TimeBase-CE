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
import com.epam.deltix.qsrv.hf.pub.util.SchemaMerger;
import com.epam.deltix.qsrv.testsetup.FloatMessage;
import com.epam.deltix.util.JUnitCategories;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import static com.epam.deltix.qsrv.hf.pub.md.DateTimeDataType.ENCODING_MILLISECONDS;
import static com.epam.deltix.qsrv.hf.pub.md.DateTimeDataType.ENCODING_NANOSECONDS;
import static org.junit.Assert.assertEquals;

/**
 *
 */
@Category(JUnitCategories.TickDBCodecs.class)
public class Test_DataTypes {
    public void testParsePositive (DataType dt, String ... goods) {
        for (String value : goods) {
            dt.parse (value);
        }       
    }
    
    public void testParseNegative (DataType dt, String ... bads) {
        for (String value : bads) {
            try {
                dt.parse (value);
                
                throw new AssertionError (
                    "No exception thrown by " + dt + 
                    " while parsing illegal '" + value + "'"
                );
            } catch (IllegalArgumentException e) {    
                // expected.
            }
        }
    }
    
    @Test
    public void testValidateINT64() {
        DataType    dt = new IntegerDataType(IntegerDataType.ENCODING_INT64, false, 1, 100);
        
        testParsePositive (dt, "1", "99", "100");
        testParseNegative (dt, "200", "0", "-1", "-12340", null);
    }

    @Test
    public void testDateTime() {
        DataType    dt = new DateTimeDataType(false, ENCODING_MILLISECONDS);

        testParsePositive (dt, "2024-01-14 06:12:42.10", "2024-01-14 06:12:42.111", "2024-01-14 06:12:42.0", "2024-01-14 06:12:42");
        checkParsePositive (dt, "2024-01-14 06:12:42.10", "2024-01-14 06:12:42.111", "2024-01-14 06:12:42.0", "2024-01-14 06:12:42.9");
        testParseNegative (dt, "2024-01-14", "0", "-1", "-12340", null);

        dt = new DateTimeDataType(false, ENCODING_NANOSECONDS);
        testParsePositive (dt, "2024-01-14 06:12:42.101212000", "2024-01-14 06:12:42.111123440", "2024-01-14 06:12:42.100", "2024-23-14 06:12:42");
        checkParsePositive (dt, "2024-01-14 06:12:42.101212000", "2024-01-14 06:12:42.111123440", "2024-01-14 06:12:42.100", "2024-01-14 06:12:42.000");
        testParseNegative (dt, "0", "-1", "-12340", "2024-01-14 06:12:42.11112344121212");
    }

    public void checkParsePositive (DataType dt, String ... goods) {
        for (String value : goods) {
            Object parsed = dt.parse(value);
            assertEquals(value, dt.toString(parsed));
        }
    }

    @Test
    public void testValidateFloat() {        
        for (String encoding : FloatDataType.ENCODING) {
            DataType    dt = new FloatDataType(encoding, false, 1.01, 100.25);
        
            testParsePositive (dt, "1.02", "1.01", "100", "100.25");
            testParseNegative (dt, "200", "0", "-1", "-12340", "1.0099", "1", "100.251", null);                    
        }
    }

    @Test
    public void testClone() throws Introspector.IntrospectionException {
        Introspector it = Introspector.createEmptyMessageIntrospector();
        RecordClassDescriptor rcd = it.introspectRecordClass(FloatMessage.class);

        ArrayDataType type = new ArrayDataType(true, new ClassDataType(true, rcd));
        DataType type1 = SchemaMerger.createNullableType(type);
        DataType type2 = type.clone();

        System.out.println(type1);
        System.out.println(type2);
    }
}
