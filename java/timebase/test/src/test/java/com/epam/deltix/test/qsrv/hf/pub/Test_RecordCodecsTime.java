package com.epam.deltix.test.qsrv.hf.pub;

import com.epam.deltix.qsrv.hf.pub.codec.CodecFactory;
import com.epam.deltix.qsrv.hf.pub.md.Introspector;
import com.epam.deltix.qsrv.hf.pub.md.RecordClassDescriptor;
import com.epam.deltix.timebase.messages.InstrumentMessage;
import com.epam.deltix.timebase.messages.*;
import com.epam.deltix.util.annotations.TimestampNs;
import com.epam.deltix.util.memory.MemoryDataInput;
import com.epam.deltix.util.memory.MemoryDataOutput;
import org.junit.Test;

import static com.epam.deltix.qsrv.hf.pub.md.Introspector.*;
import static org.junit.Assert.assertEquals;

public class Test_RecordCodecsTime {

    public static class TestMessage extends InstrumentMessage {

        @TimestampNs
        private long customTime;

        public TestMessage() {
        }

        @SchemaType(encoding = "NANOSECOND",dataType = SchemaDataType.TIMESTAMP)
        @SchemaElement(name="customTime")
        @TimestampNs
        public long getCustomTime() {
            return customTime;
        }

        public void setCustomTime(@TimestampNs long customTime) {
            this.customTime = customTime;
        }
    }

    private CodecFactory factory;

    private void setUpComp () {
        factory = CodecFactory.newCompiledCachingFactory();
    }

    private void setUpIntp () {
        factory = CodecFactory.newInterpretingCachingFactory();
    }

    @Test
    public void testInterpretted () throws Exception {
        setUpIntp();
        testAllTypeFields();
    }

    @Test
    public void testCompiled () throws Exception {
        setUpIntp();
        testAllTypeFields();
    }

    private void testAllTypeFields () throws Exception {
        //final RecordClassDescriptor rcd = Test_RecordCodecsBase.getRCD(TestMessage.class);
        final TestMessage msg = new TestMessage();

        msg.setCustomTime(123456789L);
        TestMessage decoded = (TestMessage)encodeDecode(msg, TestMessage.class);
        assertEquals(123456789L, decoded.getCustomTime());

    }

    private InstrumentMessage encodeDecode(InstrumentMessage msg, Class<?> outClass) throws IntrospectionException {

        RecordClassDescriptor rcd = (RecordClassDescriptor) introspectSingleClass(msg.getClass());

        MemoryDataOutput out = new MemoryDataOutput();
        factory.createFixedBoundEncoder(cd -> msg.getClass(), rcd).encode(msg, out);

        MemoryDataInput in = new MemoryDataInput(out);
        return  (InstrumentMessage) factory.createFixedBoundDecoder(
                cd -> outClass, rcd).decode(in);

    }

}
