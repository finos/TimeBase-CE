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

import com.epam.deltix.qsrv.hf.pub.md.EnumClassDescriptor;
import com.epam.deltix.qsrv.hf.pub.md.RecordClassSet;
import com.epam.deltix.qsrv.hf.tickdb.schema.StreamMetaDataChange;
import com.epam.deltix.timebase.messages.schema.*;
import com.epam.deltix.util.collections.generated.ObjectArrayList;
import org.junit.Test;

import java.util.Arrays;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class Test_RenameEnumDescriptorChangeMessageBuilder {

    private SchemaChangeMessageBuilder builder = new SchemaChangeMessageBuilder();

    @Test
    public void testRemoveEnumDescriptorMigration() {
        StreamMetaDataChange streamMetaDataChange = getStreamMetaDataChange();

        SchemaChangeMessage actualSchemaChangeMessage = builder.build(streamMetaDataChange, "event", 0l);

        assertThat(actualSchemaChangeMessage, is(getExpectedSchemaChangeMessage()));
    }

    private StreamMetaDataChange getStreamMetaDataChange() {
        StreamMetaDataChange streamMetaDataChange = new StreamMetaDataChange();

        EnumClassDescriptor sourceEnumDescriptor = new EnumClassDescriptor(
                "sourceName",
                "title",
                "value1", "value2"
        );

        EnumClassDescriptor targetEnumDescriptor = new EnumClassDescriptor(
                "targetName",
                "title",
                "value1", "value2"
        );

        RecordClassSet targetClassSet = new RecordClassSet();
        targetClassSet.setClassDescriptors(targetEnumDescriptor);

        RecordClassSet sourceClassSet = new RecordClassSet();
        // TODO: possible may cause an error of assertion. RecordClassSet uses HashMap as ClassDescriptor's container.
        sourceClassSet.setClassDescriptors(sourceEnumDescriptor);

        streamMetaDataChange.setMetaData(targetClassSet);
        streamMetaDataChange.setSource(sourceClassSet);

        return streamMetaDataChange;
    }

    private SchemaChangeMessage getExpectedSchemaChangeMessage() {
        SchemaChangeMessage schemaChangeMessage = new SchemaChangeMessage();
        schemaChangeMessage.setTimeStampMs(0);
        schemaChangeMessage.setSymbol("event");

        ObjectArrayList<UniqueDescriptor> previousState = new ObjectArrayList<>();
        EnumDescriptor sourceEnumDescriptor = new EnumDescriptor();
        sourceEnumDescriptor.setName("sourceName");
        sourceEnumDescriptor.setTitle("title");

        ObjectArrayList<EnumConstant> enumValues = new ObjectArrayList<>();

        EnumConstant enumValue1 = new EnumConstant();
        enumValue1.setSymbol("value1");
        enumValue1.setValue((short) 0);

        EnumConstant enumValue2 = new EnumConstant();
        enumValue2.setSymbol("value2");
        enumValue2.setValue((short) 1);

        enumValues.addAll(Arrays.asList(enumValue1, enumValue2));

        sourceEnumDescriptor.setValues(enumValues);

        EnumDescriptor targetEnumDescriptor = new EnumDescriptor();
        targetEnumDescriptor.setName("targetName");
        targetEnumDescriptor.setTitle("title");
        targetEnumDescriptor.setValues(enumValues);

        previousState.add(sourceEnumDescriptor);

        schemaChangeMessage.setPreviousState(previousState);

        ObjectArrayList<UniqueDescriptor> newState = new ObjectArrayList<>();
        newState.add(targetEnumDescriptor);
        schemaChangeMessage.setNewState(newState);

        ObjectArrayList<SchemaDescriptorChangeAction> changes = new ObjectArrayList<>();
        SchemaDescriptorChangeAction renameAction = new SchemaDescriptorChangeAction();
        renameAction.setNewState(targetEnumDescriptor);
        renameAction.setChangeTypes(SchemaDescriptorChangeType.RENAME);
        renameAction.setPreviousState(sourceEnumDescriptor);

        changes.add(renameAction);

        schemaChangeMessage.setDescriptorChangeActions(changes);

        return schemaChangeMessage;
    }
}
