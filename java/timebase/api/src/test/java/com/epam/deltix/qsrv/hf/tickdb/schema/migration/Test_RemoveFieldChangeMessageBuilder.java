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

import com.epam.deltix.qsrv.hf.pub.md.DataField;
import com.epam.deltix.qsrv.hf.pub.md.NonStaticDataField;
import com.epam.deltix.qsrv.hf.pub.md.RecordClassDescriptor;
import com.epam.deltix.qsrv.hf.pub.md.VarcharDataType;
import com.epam.deltix.qsrv.hf.pub.md.*;
import com.epam.deltix.qsrv.hf.tickdb.schema.AbstractFieldChange;
import com.epam.deltix.qsrv.hf.tickdb.schema.ClassDescriptorChange;
import com.epam.deltix.qsrv.hf.tickdb.schema.DeleteFieldChange;
import com.epam.deltix.qsrv.hf.tickdb.schema.StreamMetaDataChange;
import com.epam.deltix.timebase.messages.schema.*;
import com.epam.deltix.util.collections.generated.ObjectArrayList;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class Test_RemoveFieldChangeMessageBuilder {

    private SchemaChangeMessageBuilder schemaChangeMessageBuilder = new SchemaChangeMessageBuilder();

    @Test
    public void testRemoveFieldMigration() {
        StreamMetaDataChange streamMetaDataChange = getStreamMetaDataChange();

        SchemaChangeMessage actualSchemaChangeMessage = schemaChangeMessageBuilder.build(streamMetaDataChange, "event", 0l);

        assertThat(actualSchemaChangeMessage, is(getExpectedSchemaChangeMessage()));
    }

    private StreamMetaDataChange getStreamMetaDataChange() {
        StreamMetaDataChange streamMetaDataChange = new StreamMetaDataChange();

        DataField targetField1 = new NonStaticDataField(
                "field",
                "title",
                new VarcharDataType("UTF8", false, false)
        );

        RecordClassDescriptor targetDescriptor = new RecordClassDescriptor(
                "guid2",
                "name",
                "title",
                false,
                null,
                targetField1
        );

        DataField sourceField1 = new NonStaticDataField(
                "field",
                "title",
                new VarcharDataType("UTF8", false, false)
        );

        DataField sourceField2 = new NonStaticDataField(
                "field2",
                "title",
                new VarcharDataType("UTF8", false, false)
        );

        RecordClassDescriptor sourceDescriptor = new RecordClassDescriptor(
                "guid1",
                "name",
                "title",
                false,
                null,
                sourceField1,
                sourceField2
        );

        RecordClassSet targetClassSet = new RecordClassSet();
        targetClassSet.setClassDescriptors(targetDescriptor);

        RecordClassSet sourceClassSet = new RecordClassSet();
        sourceClassSet.setClassDescriptors(sourceDescriptor);

        streamMetaDataChange.setMetaData(targetClassSet);
        streamMetaDataChange.setSource(sourceClassSet);

        AbstractFieldChange fieldChange = new DeleteFieldChange(sourceField2);
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
        TypeDescriptor sourceDescriptor = new TypeDescriptor();
        ObjectArrayList<Field> sourceDescriptorFields = new ObjectArrayList<>();

        VarcharFieldType varcharDataType = new VarcharFieldType();
        varcharDataType.setEncodingType(-1000);
        varcharDataType.setEncoding("UTF8");
        varcharDataType.setLength(0);
        varcharDataType.setIsMultiline(false);
        varcharDataType.setIsNullable(false);

        Field expectedField1 = new NonStaticField();
        expectedField1.setTitle("title");
        expectedField1.setName("field");
        expectedField1.setType(varcharDataType);

        Field deletedField = new NonStaticField();
        deletedField.setTitle("title");
        deletedField.setName("field2");
        deletedField.setType(varcharDataType);

        sourceDescriptorFields.addAll(Arrays.asList(expectedField1, deletedField));

        sourceDescriptor.setTitle("title");
        sourceDescriptor.setName("name");
        sourceDescriptor.setFields(sourceDescriptorFields);
        sourceDescriptor.setIsAbstract(false);
        sourceDescriptor.setIsContentClass(false);

        previousState.add(sourceDescriptor);

        schemaChangeMessage.setPreviousState(previousState);

        ObjectArrayList<UniqueDescriptor> newState = new ObjectArrayList<>();
        TypeDescriptor targetDescriptor = new TypeDescriptor();
        targetDescriptor.setName("name");
        targetDescriptor.setTitle("title");
        targetDescriptor.setIsAbstract(false);
        targetDescriptor.setIsContentClass(false);

        ObjectArrayList<Field> targetDescriptorFields = new ObjectArrayList<>();

        targetDescriptorFields.add(expectedField1);

        targetDescriptor.setFields(targetDescriptorFields);
        newState.add(targetDescriptor);
        schemaChangeMessage.setNewState(newState);

        ObjectArrayList<SchemaDescriptorChangeAction> changes = new ObjectArrayList<>();
        SchemaDescriptorChangeAction alterAction = new SchemaDescriptorChangeAction();
        alterAction.setNewState(targetDescriptor);
        alterAction.setChangeTypes(SchemaDescriptorChangeType.FIELDS_CHANGE);
        alterAction.setPreviousState(sourceDescriptor);

        ObjectArrayList<SchemaFieldChangeAction> fieldChanges = new ObjectArrayList<>();
        SchemaFieldChangeAction fieldAction = new SchemaFieldChangeAction();
        fieldAction.setPreviousState(deletedField);
        fieldAction.setNewState(null);
        fieldAction.setChangeTypes(SchemaFieldChangeType.DELETE);

        fieldChanges.add(fieldAction);

        alterAction.setFieldChangeActions(fieldChanges);

        changes.add(alterAction);

        schemaChangeMessage.setDescriptorChangeActions(changes);

        return schemaChangeMessage;
    }
}