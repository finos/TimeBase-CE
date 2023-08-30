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
import com.epam.deltix.qsrv.hf.tickdb.schema.StaticFieldChange;
import com.epam.deltix.qsrv.hf.tickdb.schema.StreamMetaDataChange;
import com.epam.deltix.timebase.messages.schema.*;
import com.epam.deltix.util.collections.generated.ObjectArrayList;
import org.hamcrest.MatcherAssert;
import org.junit.Test;

import java.util.ArrayList;

import static org.hamcrest.core.Is.is;

public class Test_StaticValueChangeMessageBuilder {

    private SchemaChangeMessageBuilder schemaChangeMessageBuilder = new SchemaChangeMessageBuilder();

    @Test
    public void testChangeStaticValueMigration() {
        StreamMetaDataChange streamMetaDataChange = getStreamMetaDataChange();

        SchemaChangeMessage actualSchemaChangeMessage = schemaChangeMessageBuilder.build(streamMetaDataChange, "event", 0l);

        MatcherAssert.assertThat(actualSchemaChangeMessage, is(getExpectedSchemaChangeMessage()));
    }

    private StreamMetaDataChange getStreamMetaDataChange() {
        StreamMetaDataChange streamMetaDataChange = new StreamMetaDataChange();

        StaticDataField newFieldState = new StaticDataField( "field", "title", new VarcharDataType("UTF8", false, false), "new_default_value");

        RecordClassDescriptor targetDescriptor = new RecordClassDescriptor( "guid2", "name", "title", false, null, newFieldState);

        StaticDataField oldFieldState = new StaticDataField( "field", "title", new VarcharDataType("UTF8", false, false), "old_default_value");

        RecordClassDescriptor sourceDescriptor = new RecordClassDescriptor( "guid1", "name", "title", false, null, oldFieldState);

        RecordClassSet targetClassSet = new RecordClassSet();
        targetClassSet.setClassDescriptors(targetDescriptor);

        RecordClassSet sourceClassSet = new RecordClassSet();
        sourceClassSet.setClassDescriptors(sourceDescriptor);

        streamMetaDataChange.setMetaData(targetClassSet);
        streamMetaDataChange.setSource(sourceClassSet);

        AbstractFieldChange fieldChange = new StaticFieldChange(oldFieldState, newFieldState);
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

        StaticField previousFieldState = new StaticField();
        previousFieldState.setTitle("title");
        previousFieldState.setName("field");
        previousFieldState.setType(varcharDataType);
        previousFieldState.setStaticValue("old_default_value");

        sourceDescriptorFields.add(previousFieldState);

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

        StaticField newFieldState = new StaticField();
        newFieldState.setTitle("title");
        newFieldState.setName("field");
        newFieldState.setType(varcharDataType);
        newFieldState.setStaticValue("new_default_value");

        targetDescriptorFields.add(newFieldState);

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
        fieldAction.setNewState(newFieldState);
        fieldAction.setChangeTypes(SchemaFieldChangeType.STATIC_VALUE_CHANGE);

        fieldChanges.add(fieldAction);

        alterAction.setFieldChangeActions(fieldChanges);

        changes.add(alterAction);

        schemaChangeMessage.setDescriptorChangeActions(changes);

        return schemaChangeMessage;
    }
}