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

public class Test_DropRecordChangeMessageBuilder {

    private final SchemaChangeMessageBuilder schemaChangeMessageBuilder = new SchemaChangeMessageBuilder();

    @Test
    public void testDropRecordMigration() {
        StreamMetaDataChange streamMetaDataChange = getStreamMetaDataChange();

        SchemaChangeMessage actualSchemaChangeMessage = schemaChangeMessageBuilder.build(streamMetaDataChange, "event", 0l);

        assertThat(actualSchemaChangeMessage, is(getExpectedSchemaChangeMessage()));
    }

    private StreamMetaDataChange getStreamMetaDataChange() {
        StreamMetaDataChange streamMetaDataChange = new StreamMetaDataChange();

        DataField newFieldState = new NonStaticDataField( "field", "title", new IntegerDataType("INT32", false));
        RecordClassDescriptor targetDescriptor = new RecordClassDescriptor( "guid2", "name", "title", false, null, newFieldState);

        DataField oldFieldState = new NonStaticDataField( "field", "title", new IntegerDataType("INT64", false));
        RecordClassDescriptor sourceDescriptor = new RecordClassDescriptor( "guid1", "name", "title", false, null, oldFieldState);

        RecordClassSet targetClassSet = new RecordClassSet();
        targetClassSet.setClassDescriptors(targetDescriptor);

        RecordClassSet sourceClassSet = new RecordClassSet();
        sourceClassSet.setClassDescriptors(sourceDescriptor);

        streamMetaDataChange.setMetaData(targetClassSet);
        streamMetaDataChange.setSource(sourceClassSet);

        FieldTypeChange fieldChange = new FieldTypeChange(oldFieldState, newFieldState);
        fieldChange.setIgnoreErrors();
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

        IntegerFieldType int64DataType = new IntegerFieldType();
        int64DataType.setEncoding("INT64");
        int64DataType.setIsNullable(false);

        NonStaticField previousFieldState = Builder.createNonStatic("title", "field", int64DataType);
        TypeDescriptor sourceDescriptor = Builder.createDescriptor("title", "name", previousFieldState);
        previousState.add(sourceDescriptor);

        schemaChangeMessage.setPreviousState(previousState);

        ObjectArrayList<UniqueDescriptor> newState = new ObjectArrayList<>();

        IntegerFieldType int32DataType = new IntegerFieldType();
        int32DataType.setEncoding("INT32");
        int32DataType.setIsNullable(false);

        ObjectArrayList<Field> targetDescriptorFields = new ObjectArrayList<>();
        NonStaticField targetField = Builder.createNonStatic("title", "field", int32DataType);
        targetDescriptorFields.add(targetField);
        
        TypeDescriptor targetDescriptor = Builder.createDescriptor("title", "name", targetDescriptorFields);

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
        dataTransformation.setTransformationType(SchemaFieldDataTransformationType.SET_DEFAULT);
        fieldAction.setDataTransformation(dataTransformation);

        fieldChanges.add(fieldAction);

        alterAction.setFieldChangeActions(fieldChanges);

        changes.add(alterAction);

        schemaChangeMessage.setDescriptorChangeActions(changes);

        return schemaChangeMessage;
    }
}