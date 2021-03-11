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

        ObjectArrayList<ClassDescriptorInfo> previousState = new ObjectArrayList<>();
        deltix.timebase.messages.schema.RecordClassDescriptor sourceDescriptor = new com.epam.deltix.timebase.messages.schema.RecordClassDescriptor();
        ObjectArrayList<DataFieldInfo> sourceDescriptorFields = new ObjectArrayList<>();

        deltix.timebase.messages.schema.VarcharDataType varcharDataType = new com.epam.deltix.timebase.messages.schema.VarcharDataType();
        varcharDataType.setEncodingType(-1000);
        varcharDataType.setEncoding("UTF8");
        varcharDataType.setLength(0);
        varcharDataType.setIsMultiline(false);
        varcharDataType.setIsNullable(false);

        deltix.timebase.messages.schema.DataField previousFieldState = new com.epam.deltix.timebase.messages.schema.NonStaticDataField();
        previousFieldState.setTitle("title");
        previousFieldState.setName("field");
        previousFieldState.setDataType(varcharDataType);

        sourceDescriptorFields.add(previousFieldState);

        sourceDescriptor.setTitle("title");
        sourceDescriptor.setName("name");
        sourceDescriptor.setDataFields(sourceDescriptorFields);
        sourceDescriptor.setIsAbstract(false);
        sourceDescriptor.setIsContentClass(true);

        previousState.add(sourceDescriptor);

        schemaChangeMessage.setPreviousState(previousState);

        ObjectArrayList<ClassDescriptorInfo> newState = new ObjectArrayList<>();
        deltix.timebase.messages.schema.RecordClassDescriptor targetDescriptor = new com.epam.deltix.timebase.messages.schema.RecordClassDescriptor();
        targetDescriptor.setName("name");
        targetDescriptor.setTitle("title");
        targetDescriptor.setIsAbstract(false);
        targetDescriptor.setIsContentClass(true);

        ObjectArrayList<DataFieldInfo> targetDescriptorFields = new ObjectArrayList<>();

        deltix.timebase.messages.schema.DataField renamedField = new com.epam.deltix.timebase.messages.schema.NonStaticDataField();
        renamedField.setTitle("title");
        renamedField.setName("field2");
        renamedField.setDataType(varcharDataType);

        targetDescriptorFields.add(renamedField);

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

        deltix.qsrv.hf.pub.md.DataField newFieldState = new com.epam.deltix.qsrv.hf.pub.md.NonStaticDataField( "field2", "title", new com.epam.deltix.qsrv.hf.pub.md.VarcharDataType("UTF8", false, false));

        deltix.qsrv.hf.pub.md.RecordClassDescriptor targetDescriptor = new com.epam.deltix.qsrv.hf.pub.md.RecordClassDescriptor( "guid2", "name", "title", false, null, newFieldState);

        deltix.qsrv.hf.pub.md.DataField oldFieldState = new com.epam.deltix.qsrv.hf.pub.md.NonStaticDataField( "field", "title", new com.epam.deltix.qsrv.hf.pub.md.VarcharDataType("UTF8", false, false));

        deltix.qsrv.hf.pub.md.RecordClassDescriptor sourceDescriptor = new com.epam.deltix.qsrv.hf.pub.md.RecordClassDescriptor( "guid1", "name", "title", false, null, oldFieldState);

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
                    (deltix.qsrv.hf.pub.md.RecordClassDescriptor) actual.getClassDescriptors()[i],
                    (deltix.qsrv.hf.pub.md.RecordClassDescriptor) expected.getClassDescriptors()[i]);
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

    private void assertRecordClassDescriptors(deltix.qsrv.hf.pub.md.RecordClassDescriptor actual, deltix.qsrv.hf.pub.md.RecordClassDescriptor expected) {
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
