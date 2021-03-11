package com.epam.deltix.qsrv.hf.tickdb.pub;

import com.epam.deltix.qsrv.hf.pub.md.RecordClassDescriptor;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class Test_StreamOptions {

    @Test
    public void testGetSchemaChangeMessageDescriptor() {
        RecordClassDescriptor schemaChangeMessageDescriptor = StreamOptions.getSchemaChangeMessageDescriptor();

        assertNotNull(schemaChangeMessageDescriptor);
        assertEquals(4, schemaChangeMessageDescriptor.getFields().length);
        assertEquals("deltix.timebase.messages.schema.SchemaChangeMessage", schemaChangeMessageDescriptor.getName());
    }
}
