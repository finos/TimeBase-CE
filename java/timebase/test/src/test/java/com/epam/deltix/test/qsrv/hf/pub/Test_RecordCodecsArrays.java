package com.epam.deltix.test.qsrv.hf.pub;

import com.epam.deltix.qsrv.hf.pub.md.IntegerDataType;
import com.epam.deltix.qsrv.hf.pub.md.Introspector;
import com.epam.deltix.qsrv.hf.pub.md.RecordClassDescriptor;
import com.epam.deltix.qsrv.test.messages.TimeInForce;
import com.epam.deltix.timebase.messages.*;
import com.epam.deltix.util.JUnitCategories;
import com.epam.deltix.util.collections.generated.IntegerArrayList;
import com.epam.deltix.util.collections.generated.ObjectArrayList;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.util.Arrays;

/**
 * Created by Alex Karpovich on 9/26/2018.
 */
@Category(JUnitCategories.TickDBCodecs.class)
public class Test_RecordCodecsArrays extends Test_RecordCodecsBase {

    public static class ExtraTag {

        public ExtraTag() { }

        public ExtraTag(int key, CharSequence value) {
            this.key = key;
            this.value = value;
        }

        public int              key = IntegerDataType.INT32_NULL;
        public CharSequence     value;

        @Override
        public String toString () {
            return "key"+": "+key+", "+"value"+": "+value;
        }
    }

    public static class Group {

        @SchemaArrayType(elementTypes =  {ExtraTag.class})
        public ObjectArrayList<ExtraTag> extraTags = new ObjectArrayList<ExtraTag>();

        @Override
        public String toString () {
            return "extraTags: " + Arrays.toString(extraTags.toArray());
        }
    }

    public static class Execution extends InstrumentMessage {

        private ObjectArrayList<TimeInForce> times = new ObjectArrayList<TimeInForce>();

        @SchemaArrayType(elementTypes =  {ExtraTag.class})
        public ObjectArrayList<ExtraTag> extraTags = new ObjectArrayList<ExtraTag>();

        @SchemaArrayType(elementTypes =  {Group.class})
        public ObjectArrayList<Group> partyIDs = new ObjectArrayList<Group>();

        @SchemaArrayType(elementTypes = {TimeInForce.class} )
        @SchemaElement(name="times")
        @SchemaType(isNullable = false)
        public ObjectArrayList<TimeInForce> getTimes() {
            return times;
        }

        public void setTimes(ObjectArrayList<TimeInForce> times) {
            this.times = times;
        }

        public long msgSeqNum = IntegerDataType.INT64_NULL;
        public CharSequence execID;
        public CharSequence execRefID;

        @SchemaArrayType()
        public IntegerArrayList ids = new IntegerArrayList();

        @Override
        public InstrumentMessage copyFrom(RecordInfo template) {
            throw new UnsupportedOperationException("Not implemented");
        }

        @Override
        public String toString () {
            return "extraTags:" + Arrays.toString(extraTags.toArray()) + ", " +
                    "partyIDs: "+ Arrays.toString(partyIDs.toArray()) + ", " +
                    "msgSeqNum" + ": " + msgSeqNum + ", " +
                    "execID" + ": " + execID + ", " +
                    "execRefID" + ": " + execRefID + ", " +
                    "times:" + Arrays.toString(times.toArray());
        }
    }

    @Test
    public void test() throws Exception {

        setUpComp();
        verify();

        setUpIntp();
        verify();
    }

    public void verify() throws Exception {
        RecordClassDescriptor rcd = (RecordClassDescriptor) Introspector.introspectSingleClass(Execution.class);

        final Execution msg = new Execution();
        msg.extraTags.add(new ExtraTag(1, "1"));
        msg.extraTags.add(null);
        msg.extraTags.add(new ExtraTag(2, "2"));
        msg.times.add(TimeInForce.AT_THE_CLOSE);

        Group g = new Group();
        g.extraTags.add(new ExtraTag(11, "g11"));
        g.extraTags.add(new ExtraTag(12, "g12"));
        g.extraTags.add(null);
        msg.partyIDs.add(g);

        g = new Group();
        g.extraTags.add(new ExtraTag(21, "g21"));
        g.extraTags.add(null);
        g.extraTags.add(new ExtraTag(22, "g22"));
        msg.partyIDs.add(g);

        testRcdBound(rcd, msg, new Execution());
        testRcdBound(rcd, msg, null);
    }
}
