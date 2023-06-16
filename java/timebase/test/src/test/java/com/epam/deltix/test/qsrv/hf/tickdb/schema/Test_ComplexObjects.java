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

import com.epam.deltix.qsrv.hf.pub.md.*;
import com.epam.deltix.test.qsrv.hf.tickdb.TDBRunnerBase;
import com.epam.deltix.qsrv.hf.tickdb.pub.*;
import com.epam.deltix.qsrv.hf.tickdb.schema.MetaDataChange;
import com.epam.deltix.qsrv.hf.tickdb.schema.SchemaConverter;
import com.epam.deltix.util.JUnitCategories;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@Category(JUnitCategories.TickDBFast.class)
public class Test_ComplexObjects extends TDBRunnerBase {

    @Test
    public void Test_ArrayChange() {
        DXTickDB tdb = getTickDb();

        final RecordClassDescriptor CUSTOM_ARRAY_CLASS =
                new RecordClassDescriptor (
                        "MyArrayClass",
                        "Custom Type with Array fields",
                        false,
                        null,
                        new NonStaticDataField(
                                "prices",
                                "Prices (FLOAT)",
                                new ArrayDataType(true, new FloatDataType(FloatDataType.ENCODING_FIXED_DOUBLE, true))
                        ),
                        new NonStaticDataField(
                                "sizes",
                                "Sizes (INT)",
                                new ArrayDataType(true, new IntegerDataType(IntegerDataType.ENCODING_INT16, true))
                        )
                );

        DXTickStream source = tdb.createStream("Test_ArrayChange.s",
                StreamOptions.fixedType(StreamScope.DURABLE, "Test_ArrayChange.s", null, 0, CUSTOM_ARRAY_CLASS));

        ArrayFieldSample.writeIntoStream(source);

        final RecordClassDescriptor CHANGED_ARRAY_CLASS =
                new RecordClassDescriptor (
                        "MyArrayClass",
                        "Custom Type with Array fields",
                        false,
                        null,
                        new NonStaticDataField(
                                "prices",
                                "Prices (FLOAT)",
                                new ArrayDataType(true, new FloatDataType(FloatDataType.ENCODING_FIXED_DOUBLE, true))
                        ),
                        new NonStaticDataField(
                                "sizes",
                                "Sizes (INT)",
                                new ArrayDataType(true, new IntegerDataType(IntegerDataType.ENCODING_INT64, true))
                        ),
                        new NonStaticDataField(
                                "description",
                                "Description",
                                new VarcharDataType( VarcharDataType.ENCODING_INLINE_VARSIZE, true, false)
                        )
                );

        DXTickStream target = tdb.createStream("Test_ArrayChange.t",
                StreamOptions.fixedType(StreamScope.DURABLE, "Test_ArrayChange.t", null, 0, CHANGED_ARRAY_CLASS));

        MetaDataChange change = Test_SchemaConverter.getChanges(target, source);

        TickCursor cursor = source.select(0, new SelectionOptions(true, false));
        TickLoader loader = target.createLoader(new LoadingOptions(true));

        try {
            new SchemaConverter(change).convert(cursor, loader);

        } finally {
            loader.close();
            cursor.close();
        }

        assertEquals(target.getFixedType().getGuid(), CHANGED_ARRAY_CLASS.getGuid());

        //rawCompareStreams(source, target, null, null);

        cursor = target.select(0, new SelectionOptions(true, false));

        assertTrue(cursor.next());

        cursor.close();
    }

    @Test
    public void Test_ArrayChange1() {
        DXTickDB tdb = getTickDb();

        final RecordClassDescriptor CUSTOM_ARRAY_CLASS =
                new RecordClassDescriptor (
                        "MyArrayClass",
                        "Custom Type with Array fields",
                        false,
                        null,
                        new NonStaticDataField(
                                "prices",
                                "Prices (FLOAT)",
                                new ArrayDataType(true, new FloatDataType(FloatDataType.ENCODING_FIXED_DOUBLE, true))
                        ),
                        new NonStaticDataField(
                                "sizes",
                                "Sizes (INT)",
                                new ArrayDataType(true, new IntegerDataType(IntegerDataType.ENCODING_INT16, true))
                        )
                );

        DXTickStream source = tdb.createStream("Test_ArrayChange1.s",
                StreamOptions.fixedType(StreamScope.DURABLE, "Test_ArrayChange1.s", null, 0, CUSTOM_ARRAY_CLASS));

        ArrayFieldSample.writeIntoStream(source);

        final RecordClassDescriptor CHANGED_ARRAY_CLASS =
                new RecordClassDescriptor (
                        "MyArrayClass",
                        "Custom Type with Array fields",
                        false,
                        null,
                        new NonStaticDataField(
                                "prices",
                                "Prices (FLOAT)",
                                new ArrayDataType(true, new FloatDataType(FloatDataType.ENCODING_SCALE_AUTO, true))
                        ),
                        new NonStaticDataField(
                                "sizes",
                                "Sizes (INT)",
                                new ArrayDataType(true, new IntegerDataType(IntegerDataType.ENCODING_INT64, true))
                        )
                );

        DXTickStream target = tdb.createStream("Test_ArrayChange1.t",
                StreamOptions.fixedType(StreamScope.DURABLE, "Test_ArrayChange1.t", null, 0, CHANGED_ARRAY_CLASS));

        MetaDataChange change = Test_SchemaConverter.getChanges(target, source);

        TickCursor cursor = source.select(0, new SelectionOptions(true, false));
        TickLoader loader = target.createLoader(new LoadingOptions(true));

        try {
            new SchemaConverter(change).convert(cursor, loader);

        } finally {
            loader.close();
            cursor.close();
        }

        assertEquals(target.getFixedType().getGuid(), CHANGED_ARRAY_CLASS.getGuid());

        Test_SchemaConverter.rawCompareStreams(source, target, null, null);
    }

    @Test
    public void Test_ObjectChange() {
        DXTickDB tdb = getTickDb();

        DXTickStream source = ObjectFieldSample.createSampleStream(tdb);
        ObjectFieldSample.writeIntoStreamUnbound(source);

        DataField[] fields = ObjectFieldSample.CUSTOM_OBJECT_CLASS.getFields();
        DataField[] newFields = new DataField[fields.length + 1];
        System.arraycopy(fields, 0, newFields, 0, fields.length);
        newFields[fields.length] = new NonStaticDataField(
                "description",
                "Description",
                new VarcharDataType( VarcharDataType.ENCODING_INLINE_VARSIZE, true, false)
        );

        final RecordClassDescriptor CHANGED_OBJECT_CLASS =
                new RecordClassDescriptor (
                        ObjectFieldSample.CUSTOM_OBJECT_CLASS.getName(),
                        ObjectFieldSample.CUSTOM_OBJECT_CLASS.getDescription(),
                        false,
                        null,
                        newFields
                );

        DXTickStream target = tdb.createStream("Test_ObjectChange.t",
                StreamOptions.fixedType(StreamScope.DURABLE, "Test_ObjectChange.t", null, 0, CHANGED_OBJECT_CLASS));

        MetaDataChange change = Test_SchemaConverter.getChanges(target, source);

        TickCursor cursor = source.select(0, new SelectionOptions(true, false));
        TickLoader loader = target.createLoader(new LoadingOptions(true));

        try {
            new SchemaConverter(change).convert(cursor, loader);

        } finally {
            loader.close();
            cursor.close();
        }

        assertEquals(target.getFixedType().getGuid(), CHANGED_OBJECT_CLASS.getGuid());

//        rawCompareStreams(source, target, null, null);

        try (TickCursor c = target.select(0, new SelectionOptions(true, false)))
        {
            assertTrue(c.next());

            assertTrue(cursor.getMessage().toString() != null);

            while (c.next()) {
                System.out.println(cursor.getMessage());
            }
        }
    }

}