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
package com.epam.deltix.test.qsrv.hf.pub.md;

import com.epam.deltix.qsrv.hf.pub.md.EnumClassDescriptor;
import com.epam.deltix.qsrv.hf.pub.md.EnumValue;
import com.epam.deltix.qsrv.hf.pub.md.Introspector;
import com.epam.deltix.util.JUnitCategories;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

@Category(JUnitCategories.TickDBFast.class)
public class EnumIntrospectionTest {

    @Test
    public void testOld() throws Introspector.IntrospectionException {
        Introspector introspector = Introspector.createOldIntrospector();
        EnumClassDescriptor ecd = introspector.introspectEnumClass(CMEEventType.class);
        for (int i = 0; i < ecd.getValues().length; i++) {
            assertEquals(ecd.getValues()[i].value, i);
            assertNotEquals(ecd.getValues()[i].value, CMEEventType.valueOf(ecd.getValues()[i].symbol));
        }
    }

    @Test
    public void testNew() throws Introspector.IntrospectionException {
        Introspector introspector = Introspector.createEmptyMessageIntrospector();
        EnumClassDescriptor ecd = introspector.introspectEnumClass(CMEEventType.class);
        int i = 0;
        for (EnumValue value : ecd.getValues()) {
            assertEquals(value.value, CMEEventType.valueOf(value.symbol).getNumber());
            assertNotEquals(value.value, i++);
        }
    }

}