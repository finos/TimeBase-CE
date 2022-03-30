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
package com.epam.deltix.qsrv.hf.tickdb.schema.migration;

import com.epam.deltix.qsrv.hf.pub.md.RecordClassSet;
import com.epam.deltix.qsrv.hf.tickdb.schema.AbstractFieldChange;
import com.epam.deltix.qsrv.hf.tickdb.schema.ClassDescriptorChange;
import com.epam.deltix.qsrv.hf.tickdb.schema.FieldTypeChange;
import com.epam.deltix.qsrv.hf.tickdb.schema.StreamMetaDataChange;
import com.epam.deltix.timebase.messages.schema.*;
import com.epam.deltix.util.collections.generated.ObjectArrayList;
import org.junit.Test;

import java.util.ArrayList;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class Test_ConvertFieldChangeMessageBuilder {

    private SchemaChangeMessageBuilder schemaChangeMessageBuilder = new SchemaChangeMessageBuilder();

    @Test
    public void testConvertFieldDataTypeMigration() {
        StreamMetaDataChange streamMetaDataChange = getStreamMetaDataChange();

        SchemaChangeMessage actualSchemaChangeMessage = schemaChangeMessageBuilder.build(streamMetaDataChange, "event", 0l);

        assertThat(actualSchemaChangeMessage, is(getExpectedSchemaChangeMessage()));
    }

    private StreamMetaDataChange getStreamMetaDataChange() {
        StreamMetaDataChange streamMetaDataChange = new StreamMetaDataChange();

        com.epam.deltix.qsrv.hf.pub.md.DataField newField = new com.epam.deltix.qsrv.hf.pub.md.NonStaticDataField( "field", "title", new com.epam.deltix.qsrv.hf.pub.md.IntegerDataType("INT64", false));

        com.epam.deltix.qsrv.hf.pub.md.RecordClassDescriptor targetDescriptor = new com.epam.deltix.qsrv.hf.pub.md.RecordClassDescriptor( "guid2", "name", "title", false, null, newField);

        com.epam.deltix.qsrv.hf.pub.md.DataField oldField = new com.epam.deltix.qsrv.hf.pub.md.NonStaticDataField( "field", "title", new com.epam.deltix.qsrv.hf.pub.md.IntegerDataType("INT32", false));

        com.epam.deltix.qsrv.hf.pub.md.RecordClassDescriptor sourceDescriptor = new com.epam.deltix.qsrv.hf.pub.md.RecordClassDescriptor( "guid1", "name", "title", false, null, oldField);

        RecordClassSet targetClassSet = new RecordClassSet();
        targetClassSet.setClassDescriptors(targetDescriptor);

        RecordClassSet sourceClassSet = new RecordClassSet();
        sourceClassSet.setClassDescriptors(sourceDescriptor);

        streamMetaDataChange.setMetaData(targetClassSet);
        streamMetaDataChange.setSource(sourceClassSet);

        FieldTypeChange fieldChange = new FieldTypeChange(oldField, newField);
        ClassDescriptorChange classDescriptorChange = new ClassDescriptorChange(
                sourceDescriptor,
                targetDescriptor,
                new AbstractFieldChange[]{fieldChange}
        );

        ArrayList<ClassDescriptorChange> changes = streamMetaDataChange.changes;
        if (changes == null) {
            streamMetaDataChange.changes = new ArrayList<>();
        }
        streamMetaDataChange.changes.add(classDescriptorChange);


        return streamMetaDataChange;
    }

    private SchemaChangeMessage getExpectedSchemaChangeMessage() {
        SchemaChangeMessage schemaChangeMessage = new SchemaChangeMessage();
        schemaChangeMessage.setTimeStampMs(0);
        schemaChangeMessage.setSymbol("event");

        ObjectArrayList<UniqueDescriptor> previousState = new ObjectArrayList<>();

        IntegerFieldType int32DataType = new IntegerFieldType();
        int32DataType.setEncoding("INT32");
        int32DataType.setIsNullable(false);

        IntegerFieldType int64DataType = new IntegerFieldType();
        int64DataType.setEncoding("INT64");
        int64DataType.setIsNullable(false);

        Field previousFieldState = Builder.createNonStatic("title", "field", int32DataType);
        TypeDescriptor sourceDescriptor = Builder.createDescriptor("title", "name", previousFieldState);
        previousState.add(sourceDescriptor);

        schemaChangeMessage.setPreviousState(previousState);

        ObjectArrayList<UniqueDescriptor> newState = new ObjectArrayList<>();

        Field targetField = Builder.createNonStatic("title", "field", int64DataType);
        TypeDescriptor targetDescriptor = Builder.createDescriptor("title", "name", targetField);
        newState.add(targetDescriptor);
        schemaChangeMessage.setNewState(newState);

        ObjectArrayList<SchemaDescriptorChangeAction> changes = new ObjectArrayList<>();
        SchemaDescriptorChangeAction alterAction = new SchemaDescriptorChangeAction();
        alterAction.setNewState(targetDescriptor);
        alterAction.setChangeTypes(SchemaDescriptorChangeType.FIELDS_CHANGE);
        alterAction.setPreviousState(sourceDescriptor);

        ObjectArrayList<SchemaFieldChangeAction> fieldChanges = new ObjectArrayList<>();
        SchemaFieldChangeAction fieldAction = new SchemaFieldChangeAction();
        fieldAction.setPreviousState(previousFieldState);
        fieldAction.setNewState(targetField);
        fieldAction.setChangeTypes(SchemaFieldChangeType.DATA_TYPE_CHANGE);
        SchemaFieldDataTransformation dataTransformation = new SchemaFieldDataTransformation();
        dataTransformation.setTransformationType(SchemaFieldDataTransformationType.CONVERT_DATA);
        fieldAction.setDataTransformation(dataTransformation);

        fieldChanges.add(fieldAction);

        alterAction.setFieldChangeActions(fieldChanges);

        changes.add(alterAction);

        schemaChangeMessage.setDescriptorChangeActions(changes);

        return schemaChangeMessage;
    }
}
