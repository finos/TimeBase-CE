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

import com.epam.deltix.qsrv.hf.pub.md.*;
import com.epam.deltix.qsrv.hf.pub.md.DataField;
import com.epam.deltix.qsrv.hf.pub.md.NonStaticDataField;
import com.epam.deltix.qsrv.hf.pub.md.RecordClassDescriptor;
import com.epam.deltix.qsrv.hf.pub.md.StaticDataField;
import com.epam.deltix.qsrv.hf.pub.md.VarcharDataType;
import com.epam.deltix.qsrv.hf.tickdb.schema.StreamMetaDataChange;
import com.epam.deltix.timebase.messages.schema.*;
import com.epam.deltix.util.collections.generated.ObjectArrayList;
import org.junit.Test;

import java.util.Arrays;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class Test_RemoveDescriptorChangeMessageBuilder {

    private SchemaChangeMessageBuilder builder = new SchemaChangeMessageBuilder();

    @Test
    public void testRemoveDescriptorMigration() {
        StreamMetaDataChange streamMetaDataChange = getStreamMetaDataChange();

        SchemaChangeMessage actualSchemaChangeMessage = builder.build(streamMetaDataChange, "event", 0l);

        assertThat(actualSchemaChangeMessage, is(getExpectedSchemaChangeMessage()));
    }

    private StreamMetaDataChange getStreamMetaDataChange() {
        StreamMetaDataChange streamMetaDataChange = new StreamMetaDataChange();

        DataField sourceField1 = new NonStaticDataField(
                "source_field1",
                "title",
                new VarcharDataType("UTF8", false, false)
        );
        RecordClassDescriptor sourceDescriptor1 = new RecordClassDescriptor(
                "guid2",
                "descriptor1",
                "title",
                false,
                null,
                sourceField1
        );

        DataField sourceField2 = new NonStaticDataField(
                "source_field2",
                "title",
                new VarcharDataType("UTF8", false, false)
        );
        DataField sourceField3 = new StaticDataField(
                "source_field3",
                "title",
                new VarcharDataType("UTF8", false, false),
                "default_value"
        );
        RecordClassDescriptor sourceDescriptor2 = new RecordClassDescriptor(
                "guid1",
                "descriptor2",
                "title",
                false,
                null,
                sourceField2,
                sourceField3
        );

        RecordClassSet targetClassSet = new RecordClassSet();
        targetClassSet.setClassDescriptors(sourceDescriptor1);

        RecordClassSet sourceClassSet = new RecordClassSet();
        // TODO: possible may cause an error of assertion. RecordClassSet uses HashMap as ClassDescriptor's container.
        sourceClassSet.setClassDescriptors(sourceDescriptor2, sourceDescriptor1);

        streamMetaDataChange.setMetaData(targetClassSet);
        streamMetaDataChange.setSource(sourceClassSet);

        return streamMetaDataChange;
    }

    private SchemaChangeMessage getExpectedSchemaChangeMessage() {
        SchemaChangeMessage schemaChangeMessage = new SchemaChangeMessage();
        schemaChangeMessage.setTimeStampMs(0);
        schemaChangeMessage.setSymbol("event");

        ObjectArrayList<UniqueDescriptor> previousState = new ObjectArrayList<>();
        TypeDescriptor descriptor2 = new TypeDescriptor();
        ObjectArrayList<Field> sourceDescriptor2Fields = new ObjectArrayList<>();

        VarcharFieldType varcharDataType = new VarcharFieldType();
        varcharDataType.setEncodingType(-1000);
        varcharDataType.setEncoding("UTF8");
        varcharDataType.setLength(0);
        varcharDataType.setIsMultiline(false);
        varcharDataType.setIsNullable(false);

        Field sourceField2 = new NonStaticField();
        sourceField2.setTitle("title");
        sourceField2.setName("source_field2");
        sourceField2.setType(varcharDataType);

        StaticField sourceField3 = new StaticField();
        sourceField3.setTitle("title");
        sourceField3.setName("source_field3");
        sourceField3.setStaticValue("default_value");
        sourceField3.setType(varcharDataType);

        sourceDescriptor2Fields.addAll(Arrays.asList(sourceField2, sourceField3));

        descriptor2.setTitle("title");
        descriptor2.setName("descriptor2");
        descriptor2.setFields(sourceDescriptor2Fields);
        descriptor2.setIsAbstract(false);

        TypeDescriptor descriptor1 = new TypeDescriptor();
        descriptor1.setName("descriptor1");
        descriptor1.setTitle("title");
        descriptor1.setIsAbstract(false);

        ObjectArrayList<Field> descriptor1Fields = new ObjectArrayList<>();

        Field sourceField1 = new NonStaticField();
        sourceField1.setType(varcharDataType);
        sourceField1.setName("source_field1");
        sourceField1.setTitle("title");

        descriptor1Fields.add(sourceField1);

        descriptor1.setFields(descriptor1Fields);

        previousState.addAll(Arrays.asList(descriptor1, descriptor2));

        schemaChangeMessage.setPreviousState(previousState);

        ObjectArrayList<UniqueDescriptor> newState = new ObjectArrayList<>();
        newState.add(descriptor1);
        schemaChangeMessage.setNewState(newState);

        ObjectArrayList<SchemaDescriptorChangeAction> changes = new ObjectArrayList<>();
        SchemaDescriptorChangeAction removeAction = new SchemaDescriptorChangeAction();
        removeAction.setNewState(null);
        removeAction.setChangeTypes(SchemaDescriptorChangeType.DELETE);
        removeAction.setPreviousState(descriptor2);

        changes.add(removeAction);

        schemaChangeMessage.setDescriptorChangeActions(changes);

        return schemaChangeMessage;
    }
}
