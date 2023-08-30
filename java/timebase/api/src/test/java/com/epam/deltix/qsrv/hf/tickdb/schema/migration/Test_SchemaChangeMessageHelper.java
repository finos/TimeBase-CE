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

import com.epam.deltix.qsrv.hf.pub.md.RecordClassSet;
import com.epam.deltix.qsrv.hf.tickdb.schema.*;
import com.epam.deltix.timebase.messages.schema.*;
import com.epam.deltix.util.collections.generated.ObjectArrayList;
import org.junit.Test;

import java.util.ArrayList;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class Test_SchemaChangeMessageHelper {

    @Test
    public void testGetStreamMetaDataChange() {
        SchemaChangeMessage schemaChangeMessage = getSchemaChangeMessage();

        StreamMetaDataChange streamMetaDataChange = SchemaChangeMessageHelper.getStreamMetaDataChange(schemaChangeMessage);

        assertStreamMetaDataChanges(streamMetaDataChange, getExpectedStreamMetaDataChange());
    }

    private SchemaChangeMessage getSchemaChangeMessage() {
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

        Field previousFieldState = new NonStaticField();
        previousFieldState.setTitle("title");
        previousFieldState.setName("field");
        previousFieldState.setType(varcharDataType);

        sourceDescriptorFields.add(previousFieldState);

        sourceDescriptor.setTitle("title");
        sourceDescriptor.setName("name");
        sourceDescriptor.setFields(sourceDescriptorFields);
        sourceDescriptor.setIsAbstract(false);
        sourceDescriptor.setIsContentClass(true);
        sourceDescriptor.setIsContentClass(false);

        previousState.add(sourceDescriptor);

        schemaChangeMessage.setPreviousState(previousState);

        ObjectArrayList<UniqueDescriptor> newState = new ObjectArrayList<>();
        TypeDescriptor targetDescriptor = new TypeDescriptor();
        targetDescriptor.setName("name");
        targetDescriptor.setTitle("title");
        targetDescriptor.setIsAbstract(false);
        targetDescriptor.setIsContentClass(true);
        targetDescriptor.setIsContentClass(false);

        ObjectArrayList<Field> targetDescriptorFields = new ObjectArrayList<>();

        Field renamedField = new NonStaticField();
        renamedField.setTitle("title");
        renamedField.setName("field2");
        renamedField.setType(varcharDataType);

        targetDescriptorFields.add(renamedField);

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
        fieldAction.setPreviousState(previousFieldState);
        fieldAction.setNewState(renamedField);
        fieldAction.setChangeTypes(SchemaFieldChangeType.RENAME);

        fieldChanges.add(fieldAction);

        alterAction.setFieldChangeActions(fieldChanges);

        changes.add(alterAction);

        schemaChangeMessage.setDescriptorChangeActions(changes);

        return schemaChangeMessage;
    }

    private StreamMetaDataChange getExpectedStreamMetaDataChange() {
        StreamMetaDataChange streamMetaDataChange = new StreamMetaDataChange();

        com.epam.deltix.qsrv.hf.pub.md.DataField newFieldState = new com.epam.deltix.qsrv.hf.pub.md.NonStaticDataField( "field2", "title", new com.epam.deltix.qsrv.hf.pub.md.VarcharDataType("UTF8", false, false));

        com.epam.deltix.qsrv.hf.pub.md.RecordClassDescriptor targetDescriptor = new com.epam.deltix.qsrv.hf.pub.md.RecordClassDescriptor( "guid2", "name", "title", false, null, newFieldState);

        com.epam.deltix.qsrv.hf.pub.md.DataField oldFieldState = new com.epam.deltix.qsrv.hf.pub.md.NonStaticDataField( "field", "title", new com.epam.deltix.qsrv.hf.pub.md.VarcharDataType("UTF8", false, false));

        com.epam.deltix.qsrv.hf.pub.md.RecordClassDescriptor sourceDescriptor = new com.epam.deltix.qsrv.hf.pub.md.RecordClassDescriptor( "guid1", "name", "title", false, null, oldFieldState);

        RecordClassSet targetClassSet = new RecordClassSet();
        targetClassSet.setClassDescriptors(targetDescriptor);
        targetClassSet.addContentClasses(targetDescriptor);

        RecordClassSet sourceClassSet = new RecordClassSet();
        sourceClassSet.setClassDescriptors(sourceDescriptor);
        sourceClassSet.addContentClasses(sourceDescriptor);

        streamMetaDataChange.setMetaData(targetClassSet);
        streamMetaDataChange.setSource(sourceClassSet);

        AbstractFieldChange fieldChange = new FieldChange(oldFieldState, newFieldState, FieldAttribute.Name);
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

    private void assertStreamMetaDataChanges(StreamMetaDataChange actual, StreamMetaDataChange expected) {
        assertRecordClassSets(actual.getMetaData(), expected.getMetaData());
        assertRecordClassSets(actual.getSource(), expected.getSource());
        assertChanges(actual.changes, expected.changes);
    }

    private void assertRecordClassSets(RecordClassSet actual, RecordClassSet expected) {
        assertThat(actual.getClassDescriptors().length, is(expected.getClassDescriptors().length));
        for (int i = 0; i < actual.getClassDescriptors().length; i++) {
            assertRecordClassDescriptors(
                    (com.epam.deltix.qsrv.hf.pub.md.RecordClassDescriptor) actual.getClassDescriptors()[i],
                    (com.epam.deltix.qsrv.hf.pub.md.RecordClassDescriptor) expected.getClassDescriptors()[i]);
        }
        assertThat(actual.getNumTopTypes(), is(expected.getNumTopTypes()));
        for (int i = 0; i < actual.getContentClasses().length; i++) {
            assertRecordClassDescriptors(
                    actual.getContentClasses()[i],
                    expected.getContentClasses()[i]);
        }
    }

    private void assertChanges(ArrayList<ClassDescriptorChange> actual, ArrayList<ClassDescriptorChange> expected) {
        assertThat(actual.size(), is(expected.size()));
        for (int i = 0; i < actual.size(); i++) {
            assertThat(actual.get(i).getChanges().length, is(expected.get(i).getChanges().length));
            for (int j = 0; j < actual.get(i).getChanges().length; j++) {
                assertThat(actual.get(i).getChanges()[j].toString(), is(expected.get(i).getChanges()[j].toString()));
            }
        }
    }

    private void assertRecordClassDescriptors(com.epam.deltix.qsrv.hf.pub.md.RecordClassDescriptor actual, com.epam.deltix.qsrv.hf.pub.md.RecordClassDescriptor expected) {
        assertThat(actual.getFields().length, is(expected.getFields().length));
        for (int i = 0; i < actual.getFields().length; i++) {
            assertThat(actual.getFields()[i].getType().getCode(), is(expected.getFields()[i].getType().getCode()));
            assertThat(actual.getFields()[i].getName(), is(expected.getFields()[i].getName()));
            assertThat(actual.getFields()[i].getTitle(), is(expected.getFields()[i].getTitle()));
        }
        assertThat(actual.getName(), is(expected.getName()));
        assertThat(actual.getDescription(), is(expected.getDescription()));
        assertThat(actual.getParent(), is(expected.getParent()));
        assertThat(actual.getTitle(), is(expected.getTitle()));
    }
}