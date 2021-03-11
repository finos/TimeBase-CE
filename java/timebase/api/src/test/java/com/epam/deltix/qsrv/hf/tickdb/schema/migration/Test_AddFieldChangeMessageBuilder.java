package com.epam.deltix.qsrv.hf.tickdb.schema.migration;

import com.epam.deltix.qsrv.hf.pub.md.*;
import com.epam.deltix.qsrv.hf.pub.md.DataField;
import com.epam.deltix.qsrv.hf.pub.md.NonStaticDataField;
import com.epam.deltix.qsrv.hf.pub.md.RecordClassDescriptor;
import com.epam.deltix.qsrv.hf.pub.md.VarcharDataType;
import com.epam.deltix.qsrv.hf.tickdb.schema.AbstractFieldChange;
import com.epam.deltix.qsrv.hf.tickdb.schema.ClassDescriptorChange;
import com.epam.deltix.qsrv.hf.tickdb.schema.CreateFieldChange;
import com.epam.deltix.qsrv.hf.tickdb.schema.StreamMetaDataChange;
import com.epam.deltix.timebase.messages.schema.*;
import com.epam.deltix.util.collections.generated.ObjectArrayList;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class Test_AddFieldChangeMessageBuilder {

    private SchemaChangeMessageBuilder schemaChangeMessageBuilder = new SchemaChangeMessageBuilder();

    @Test
    public void testAddNewFieldMigration() {
        StreamMetaDataChange streamMetadataChange = getStreamMetadataChange();

        SchemaChangeMessage actualSchemaChangeMessage = schemaChangeMessageBuilder.build(streamMetadataChange, "event", 0l);

        assertThat(actualSchemaChangeMessage, is(getExpectedSchemaChangeMessage()));
    }

    private StreamMetaDataChange getStreamMetadataChange() {
        StreamMetaDataChange streamMetaDataChange = new StreamMetaDataChange();

        DataField targetField1 = new NonStaticDataField(
                "field",
                "title",
                new VarcharDataType("UTF8", false, false)
        );

        DataField targetField2 = new NonStaticDataField(
                "field2",
                "title",
                new VarcharDataType("UTF8", false, false)
        );

        RecordClassDescriptor targetDescriptor = new RecordClassDescriptor(
                "guid2",
                "name",
                "title",
                false,
                null,
                targetField1,
                targetField2
        );

        DataField sourceField1 = new NonStaticDataField(
                "field",
                "title",
                new VarcharDataType("UTF8", false, false)
        );

        RecordClassDescriptor sourceDescriptor = new RecordClassDescriptor(
                "guid1",
                "name",
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

        AbstractFieldChange fieldChange = new CreateFieldChange(targetField2, false);
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

        ObjectArrayList<ClassDescriptorInfo> previousState = new ObjectArrayList<>();
        deltix.timebase.messages.schema.RecordClassDescriptor sourceDescriptor = new com.epam.deltix.timebase.messages.schema.RecordClassDescriptor();
        ObjectArrayList<DataFieldInfo> sourceDescriptorFields = new ObjectArrayList<>();

        deltix.timebase.messages.schema.VarcharDataType varcharDataType = new com.epam.deltix.timebase.messages.schema.VarcharDataType();
        varcharDataType.setEncodingType(-1000);
        varcharDataType.setEncoding("UTF8");
        varcharDataType.setLength(0);
        varcharDataType.setIsMultiline(false);
        varcharDataType.setIsNullable(false);

        deltix.timebase.messages.schema.DataField sourceField = new com.epam.deltix.timebase.messages.schema.NonStaticDataField();
        sourceField.setTitle("title");
        sourceField.setName("field");
        sourceField.setDataType(varcharDataType);

        sourceDescriptorFields.add(sourceField);

        sourceDescriptor.setTitle("title");
        sourceDescriptor.setName("name");
        sourceDescriptor.setDataFields(sourceDescriptorFields);
        sourceDescriptor.setIsAbstract(false);

        previousState.add(sourceDescriptor);

        schemaChangeMessage.setPreviousState(previousState);

        ObjectArrayList<ClassDescriptorInfo> newState = new ObjectArrayList<>();
        deltix.timebase.messages.schema.RecordClassDescriptor targetDescriptor = new com.epam.deltix.timebase.messages.schema.RecordClassDescriptor();
        targetDescriptor.setName("name");
        targetDescriptor.setTitle("title");
        targetDescriptor.setIsAbstract(false);

        ObjectArrayList<DataFieldInfo> targetDescriptorFields = new ObjectArrayList<>();

        deltix.timebase.messages.schema.DataField newField = new com.epam.deltix.timebase.messages.schema.NonStaticDataField();
        newField.setTitle("title");
        newField.setName("field2");
        newField.setDataType(varcharDataType);

        targetDescriptorFields.addAll(Arrays.asList(sourceField, newField));

        targetDescriptor.setDataFields(targetDescriptorFields);
        newState.add(targetDescriptor);
        schemaChangeMessage.setNewState(newState);

        ObjectArrayList<SchemaDescriptorChangeActionInfo> changes = new ObjectArrayList<>();
        SchemaDescriptorChangeAction alterAction = new SchemaDescriptorChangeAction();
        alterAction.setNewState(targetDescriptor);
        alterAction.setChangeTypes(SchemaDescriptorChangeType.FIELDS_CHANGE);
        alterAction.setPreviousState(sourceDescriptor);

        ObjectArrayList<SchemaFieldChangeActionInfo> fieldChanges = new ObjectArrayList<>();
        SchemaFieldChangeAction fieldAction = new SchemaFieldChangeAction();
        fieldAction.setPreviousState(null);
        fieldAction.setNewState(newField);
        fieldAction.setChangeTypes(SchemaFieldChangeType.ADD);

        fieldChanges.add(fieldAction);

        alterAction.setFieldChangeActions(fieldChanges);

        changes.add(alterAction);

        schemaChangeMessage.setDescriptorChangeActions(changes);

        return schemaChangeMessage;
    }
}
