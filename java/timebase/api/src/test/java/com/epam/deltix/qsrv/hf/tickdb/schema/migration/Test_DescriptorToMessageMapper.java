package com.epam.deltix.qsrv.hf.tickdb.schema.migration;

import com.epam.deltix.qsrv.hf.pub.md.*;
import com.epam.deltix.timebase.messages.schema.DataFieldInfo;
import com.epam.deltix.util.collections.generated.ObjectArrayList;
import org.junit.Test;

import static com.epam.deltix.qsrv.hf.pub.md.VarcharDataType.INLINE_VARSIZE;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

//TODO add more tests
public class Test_DescriptorToMessageMapper {

    @Test
    public void testMapping() {
        DataField field1 = new NonStaticDataField(
                "field1",
                "field1_title",
                new VarcharDataType("UTF8", false, false)
        );
        DataField field2 = new StaticDataField(
                "field2",
                "field2_title",
                new VarcharDataType("UTF8", false, false),
                "default_value"
        );
        RecordClassDescriptor descriptor = new RecordClassDescriptor(
                "guid",
                "name",
                "title",
                false,
                null,
                field2,
                field1
        );

        deltix.timebase.messages.schema.RecordClassDescriptor actualDescriptorMessage = DescriptorToMessageMapper.map(descriptor);

        deltix.timebase.messages.schema.RecordClassDescriptor expectedDescriptorMessage = new com.epam.deltix.timebase.messages.schema.RecordClassDescriptor();
        expectedDescriptorMessage.setName("name");
        expectedDescriptorMessage.setTitle("title");
        expectedDescriptorMessage.setIsAbstract(false);

        ObjectArrayList<DataFieldInfo> expectedFields = new ObjectArrayList<>();

        deltix.timebase.messages.schema.VarcharDataType varcharDataType = new com.epam.deltix.timebase.messages.schema.VarcharDataType();
        varcharDataType.setEncoding("UTF8");
        varcharDataType.setIsNullable(false);
        varcharDataType.setIsMultiline(false);
        varcharDataType.setLength(0);
        varcharDataType.setEncodingType(INLINE_VARSIZE);

        deltix.timebase.messages.schema.DataField expectedField1 = new com.epam.deltix.timebase.messages.schema.NonStaticDataField();
        expectedField1.setName("field1");
        expectedField1.setTitle("field1_title");
        expectedField1.setDataType(varcharDataType);

        deltix.timebase.messages.schema.StaticDataField expectedField2 = new com.epam.deltix.timebase.messages.schema.StaticDataField();
        expectedField2.setName("field2");
        expectedField2.setTitle("field2_title");
        expectedField2.setDataType(varcharDataType);
        expectedField2.setStaticValue("default_value");

        expectedFields.add(expectedField2);
        expectedFields.add(expectedField1);

        expectedDescriptorMessage.setDataFields(expectedFields);

        assertThat(actualDescriptorMessage, is(expectedDescriptorMessage));
    }
}
