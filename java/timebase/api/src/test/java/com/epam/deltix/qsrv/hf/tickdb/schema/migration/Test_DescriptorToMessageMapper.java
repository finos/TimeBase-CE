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
package com.epam.deltix.qsrv.hf.tickdb.schema.migration;

import com.epam.deltix.qsrv.hf.pub.md.*;
import com.epam.deltix.timebase.messages.schema.*;
import com.epam.deltix.util.collections.generated.ObjectArrayList;
import org.junit.Test;

import static com.epam.deltix.qsrv.hf.pub.md.VarcharDataType.INLINE_VARSIZE;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

//TODO add more tests
public class Test_DescriptorToMessageMapper {

    @Test
    public void testMapping() {
        DataField field1 = new NonStaticDataField(
                "field1",
                "field1_title",
                new VarcharDataType("UTF8", false, false)
        );
        DataField field2 = new StaticDataField(
                "field2",
                "field2_title",
                new VarcharDataType("UTF8", false, false),
                "default_value"
        );
        RecordClassDescriptor descriptor = new RecordClassDescriptor(
                "guid",
                "name",
                "title",
                false,
                null,
                field2,
                field1
        );

        TypeDescriptor actualDescriptorMessage = DescriptorToMessageMapper.map(descriptor);

        ObjectArrayList<Field> expectedFields = new ObjectArrayList<>();

        VarcharFieldType varcharDataType = new VarcharFieldType();
        varcharDataType.setEncoding("UTF8");
        varcharDataType.setIsNullable(false);
        varcharDataType.setIsMultiline(false);
        varcharDataType.setLength(0);
        varcharDataType.setEncodingType(INLINE_VARSIZE);

        NonStaticField expectedField1 = Builder.createNonStatic("field1_title", "field1", varcharDataType);

        StaticField expectedField2 = new StaticField();
        expectedField2.setName("field2");
        expectedField2.setTitle("field2_title");
        expectedField2.setType(varcharDataType);
        expectedField2.setStaticValue("default_value");

        expectedFields.add(expectedField2);
        expectedFields.add(expectedField1);

        TypeDescriptor expectedDescriptorMessage = Builder.createDescriptor("title", "name", expectedFields);

        assertThat(actualDescriptorMessage, is(expectedDescriptorMessage));
    }
}