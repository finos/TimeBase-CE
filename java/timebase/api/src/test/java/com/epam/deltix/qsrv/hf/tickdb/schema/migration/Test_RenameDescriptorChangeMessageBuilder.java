package com.epam.deltix.qsrv.hf.tickdb.schema.migration;

import com.epam.deltix.qsrv.hf.pub.md.DataField;
import com.epam.deltix.qsrv.hf.pub.md.NonStaticDataField;
import com.epam.deltix.qsrv.hf.pub.md.RecordClassDescriptor;
import com.epam.deltix.qsrv.hf.pub.md.VarcharDataType;
import com.epam.deltix.qsrv.hf.pub.md.*;
import com.epam.deltix.qsrv.hf.tickdb.schema.StreamMetaDataChange;
import com.epam.deltix.timebase.messages.schema.*;
import com.epam.deltix.util.collections.generated.ObjectArrayList;
import org.junit.Test;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class Test_RenameDescriptorChangeMessageBuilder {

    private SchemaChangeMessageBuilder schemaChangeMessageBuilder = new SchemaChangeMessageBuilder();

    @Test
    public void testRenameDescriptorMigration() {
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
                "targetName",
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

        RecordClassDescriptor sourceDescriptor = new RecordClassDescriptor(
                "guid1",
                "sourceName",
                "title",
                false,
                null,
                sourceField1
        );

        RecordClassSet targetClassSet = new RecordClassSet();
        targetClassSet.setClassDescriptors(targetDescriptor);

        RecordClassSet sourceClassSet = new RecordClassSet();
        sourceClassSet.setClassDescriptors(sourceDescriptor);

        streamMetaDataChange.setMetaData(targetClassSet);
        streamMetaDataChange.setSource(sourceClassSet);

        return streamMetaDataChange;
    }

    private SchemaChangeMessage getExpectedSchemaChangeMessage() {
        SchemaChangeMessage schemaChangeMessage = new SchemaChangeMessage();
        schemaChangeMessage.setTimeStampMs(0);
        schemaChangeMessage.setSymbol("event");

        ObjectArrayList<ClassDescriptorInfo> previousState = new ObjectArrayList<>();
        com.epam.deltix.timebase.messages.schema.RecordClassDescriptor sourceDescriptor = new com.epam.deltix.timebase.messages.schema.RecordClassDescriptor();
        ObjectArrayList<DataFieldInfo> sourceDescriptorFields = new ObjectArrayList<>();

        com.epam.deltix.timebase.messages.schema.VarcharDataType varcharDataType = new com.epam.deltix.timebase.messages.schema.VarcharDataType();
        varcharDataType.setEncodingType(-1000);
        varcharDataType.setEncoding("UTF8");
        varcharDataType.setLength(0);
        varcharDataType.setIsMultiline(false);
        varcharDataType.setIsNullable(false);

        com.epam.deltix.timebase.messages.schema.DataField expectedField1 = new com.epam.deltix.timebase.messages.schema.NonStaticDataField();
        expectedField1.setTitle("title");
        expectedField1.setName("field");
        expectedField1.setDataType(varcharDataType);

        sourceDescriptorFields.add(expectedField1);

        sourceDescriptor.setTitle("title");
        sourceDescriptor.setName("sourceName");
        sourceDescriptor.setDataFields(sourceDescriptorFields);
        sourceDescriptor.setIsAbstract(false);

        previousState.add(sourceDescriptor);

        schemaChangeMessage.setPreviousState(previousState);

        ObjectArrayList<ClassDescriptorInfo> newState = new ObjectArrayList<>();
        com.epam.deltix.timebase.messages.schema.RecordClassDescriptor targetDescriptor = new com.epam.deltix.timebase.messages.schema.RecordClassDescriptor();
        targetDescriptor.setName("targetName");
        targetDescriptor.setTitle("title");
        targetDescriptor.setIsAbstract(false);

        ObjectArrayList<DataFieldInfo> targetDescriptorFields = new ObjectArrayList<>();
        targetDescriptorFields.add(expectedField1);

        targetDescriptor.setDataFields(targetDescriptorFields);
        newState.add(targetDescriptor);
        schemaChangeMessage.setNewState(newState);

        ObjectArrayList<SchemaDescriptorChangeActionInfo> changes = new ObjectArrayList<>();
        SchemaDescriptorChangeAction renameAction = new SchemaDescriptorChangeAction();
        renameAction.setNewState(targetDescriptor);
        renameAction.setChangeTypes(SchemaDescriptorChangeType.RENAME);
        renameAction.setPreviousState(sourceDescriptor);

        changes.add(renameAction);

        schemaChangeMessage.setDescriptorChangeActions(changes);

        return schemaChangeMessage;
    }
}
