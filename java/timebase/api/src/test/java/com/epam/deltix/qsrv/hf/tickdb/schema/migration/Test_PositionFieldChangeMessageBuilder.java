package com.epam.deltix.qsrv.hf.tickdb.schema.migration;

import com.epam.deltix.qsrv.hf.pub.md.RecordClassSet;
import com.epam.deltix.qsrv.hf.tickdb.schema.AbstractFieldChange;
import com.epam.deltix.qsrv.hf.tickdb.schema.ClassDescriptorChange;
import com.epam.deltix.qsrv.hf.tickdb.schema.FieldPositionChange;
import com.epam.deltix.qsrv.hf.tickdb.schema.StreamMetaDataChange;
import com.epam.deltix.timebase.messages.schema.*;
import com.epam.deltix.util.collections.generated.ObjectArrayList;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class Test_PositionFieldChangeMessageBuilder {

    private SchemaChangeMessageBuilder schemaChangeMessageBuilder = new SchemaChangeMessageBuilder();

    @Test
    public void testChangeFieldPositionMigration() {
        StreamMetaDataChange streamMetaDataChange = getStreamMetaDataChange();

        SchemaChangeMessage actualSchemaChangeMessage = schemaChangeMessageBuilder.build(streamMetaDataChange, "event", 0l);

        assertThat(actualSchemaChangeMessage, is(getExpectedSchemaChangeMessage()));
    }

    private StreamMetaDataChange getStreamMetaDataChange() {
        StreamMetaDataChange streamMetaDataChange = new StreamMetaDataChange();

        com.epam.deltix.qsrv.hf.pub.md.DataField newFieldState1 = new com.epam.deltix.qsrv.hf.pub.md.NonStaticDataField( "field", "title", new com.epam.deltix.qsrv.hf.pub.md.VarcharDataType("UTF8", false, false));

        com.epam.deltix.qsrv.hf.pub.md.DataField newFieldState2 = new com.epam.deltix.qsrv.hf.pub.md.NonStaticDataField( "field2", "title", new com.epam.deltix.qsrv.hf.pub.md.VarcharDataType("UTF8", false, false));

        com.epam.deltix.qsrv.hf.pub.md.RecordClassDescriptor targetDescriptor = new com.epam.deltix.qsrv.hf.pub.md.RecordClassDescriptor( "guid2", "name", "title", false, null, newFieldState1, newFieldState2);

        com.epam.deltix.qsrv.hf.pub.md.DataField oldFieldState1 = new com.epam.deltix.qsrv.hf.pub.md.NonStaticDataField( "field", "title", new com.epam.deltix.qsrv.hf.pub.md.VarcharDataType("UTF8", false, false));

        com.epam.deltix.qsrv.hf.pub.md.DataField oldFieldState2 = new com.epam.deltix.qsrv.hf.pub.md.NonStaticDataField( "field2", "title", new com.epam.deltix.qsrv.hf.pub.md.VarcharDataType("UTF8", false, false));

        com.epam.deltix.qsrv.hf.pub.md.RecordClassDescriptor sourceDescriptor = new com.epam.deltix.qsrv.hf.pub.md.RecordClassDescriptor( "guid1", "name", "title", false, null, oldFieldState2, oldFieldState1);

        RecordClassSet targetClassSet = new RecordClassSet();
        targetClassSet.setClassDescriptors(targetDescriptor);

        RecordClassSet sourceClassSet = new RecordClassSet();
        sourceClassSet.setClassDescriptors(sourceDescriptor);

        streamMetaDataChange.setMetaData(targetClassSet);
        streamMetaDataChange.setSource(sourceClassSet);

        AbstractFieldChange fieldChange1 = new FieldPositionChange(oldFieldState2, newFieldState1);
        AbstractFieldChange fieldChange2 = new FieldPositionChange(oldFieldState1, newFieldState2);
        ClassDescriptorChange classDescriptorChange = new ClassDescriptorChange(
                sourceDescriptor,
                targetDescriptor,
                new AbstractFieldChange[]{fieldChange1, fieldChange2}
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
        com.epam.deltix.timebase.messages.schema.RecordClassDescriptor sourceDescriptor = new com.epam.deltix.timebase.messages.schema.RecordClassDescriptor();
        ObjectArrayList<DataFieldInfo> sourceDescriptorFields = new ObjectArrayList<>();

        com.epam.deltix.timebase.messages.schema.VarcharDataType varcharDataType = new com.epam.deltix.timebase.messages.schema.VarcharDataType();
        varcharDataType.setEncodingType(-1000);
        varcharDataType.setEncoding("UTF8");
        varcharDataType.setLength(0);
        varcharDataType.setIsMultiline(false);
        varcharDataType.setIsNullable(false);

        com.epam.deltix.timebase.messages.schema.DataField previousFieldState1 = new com.epam.deltix.timebase.messages.schema.NonStaticDataField();
        previousFieldState1.setTitle("title");
        previousFieldState1.setName("field");
        previousFieldState1.setDataType(varcharDataType);

        com.epam.deltix.timebase.messages.schema.DataField previousFieldState2 = new com.epam.deltix.timebase.messages.schema.NonStaticDataField();
        previousFieldState2.setTitle("title");
        previousFieldState2.setName("field2");
        previousFieldState2.setDataType(varcharDataType);

        sourceDescriptorFields.addAll(Arrays.asList(previousFieldState2, previousFieldState1));

        sourceDescriptor.setTitle("title");
        sourceDescriptor.setName("name");
        sourceDescriptor.setDataFields(sourceDescriptorFields);
        sourceDescriptor.setIsAbstract(false);

        previousState.add(sourceDescriptor);

        schemaChangeMessage.setPreviousState(previousState);

        ObjectArrayList<ClassDescriptorInfo> newState = new ObjectArrayList<>();
        com.epam.deltix.timebase.messages.schema.RecordClassDescriptor targetDescriptor = new com.epam.deltix.timebase.messages.schema.RecordClassDescriptor();
        targetDescriptor.setName("name");
        targetDescriptor.setTitle("title");
        targetDescriptor.setIsAbstract(false);

        ObjectArrayList<DataFieldInfo> targetDescriptorFields = new ObjectArrayList<>();

        com.epam.deltix.timebase.messages.schema.DataField targetField1 = new com.epam.deltix.timebase.messages.schema.NonStaticDataField();
        targetField1.setTitle("title");
        targetField1.setName("field");
        targetField1.setDataType(varcharDataType);

        com.epam.deltix.timebase.messages.schema.DataField targetField2 = new com.epam.deltix.timebase.messages.schema.NonStaticDataField();
        targetField2.setTitle("title");
        targetField2.setName("field2");
        targetField2.setDataType(varcharDataType);

        targetDescriptorFields.addAll(Arrays.asList(targetField1, targetField2));

        targetDescriptor.setDataFields(targetDescriptorFields);
        newState.add(targetDescriptor);
        schemaChangeMessage.setNewState(newState);

        ObjectArrayList<SchemaDescriptorChangeActionInfo> changes = new ObjectArrayList<>();
        SchemaDescriptorChangeAction alterAction = new SchemaDescriptorChangeAction();
        alterAction.setNewState(targetDescriptor);
        alterAction.setChangeTypes(SchemaDescriptorChangeType.FIELDS_CHANGE);
        alterAction.setPreviousState(sourceDescriptor);

        ObjectArrayList<SchemaFieldChangeActionInfo> fieldChanges = new ObjectArrayList<>();

        SchemaFieldChangeAction ordinalChangeAction1 = new SchemaFieldChangeAction();
        ordinalChangeAction1.setPreviousState(previousFieldState2);
        ordinalChangeAction1.setNewState(targetField1);
        ordinalChangeAction1.setChangeTypes(SchemaFieldChangeType.ORDINAL_CHANGE);

        SchemaFieldChangeAction ordinalChangeAction2 = new SchemaFieldChangeAction();
        ordinalChangeAction2.setPreviousState(previousFieldState1);
        ordinalChangeAction2.setNewState(targetField2);
        ordinalChangeAction2.setChangeTypes(SchemaFieldChangeType.ORDINAL_CHANGE);

        fieldChanges.addAll(Arrays.asList(ordinalChangeAction1, ordinalChangeAction2));

        alterAction.setFieldChangeActions(fieldChanges);

        changes.add(alterAction);

        schemaChangeMessage.setDescriptorChangeActions(changes);

        return schemaChangeMessage;
    }
}
