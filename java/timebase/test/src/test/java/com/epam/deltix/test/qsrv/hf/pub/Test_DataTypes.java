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

import com.epam.deltix.qsrv.hf.pub.md.DataType;
import com.epam.deltix.qsrv.hf.pub.md.FloatDataType;
import com.epam.deltix.qsrv.hf.pub.md.IntegerDataType;
import com.epam.deltix.util.JUnitCategories;
import org.junit.Test;
import org.junit.experimental.categories.Category;

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
    public void testValidateFloat() {        
        for (String encoding : FloatDataType.ENCODING) {
            DataType    dt = new FloatDataType(encoding, false, 1.01, 100.25);
        
            testParsePositive (dt, "1.02", "1.01", "100", "100.25");
            testParseNegative (dt, "200", "0", "-1", "-12340", "1.0099", "1", "100.251", null);                    
        }
    }
}