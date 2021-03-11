package com.epam.deltix.test.qsrv.hf.pub;

import com.epam.deltix.qsrv.hf.pub.codec.UnboundDecoder;
import com.epam.deltix.qsrv.hf.pub.md.*;
import com.epam.deltix.timebase.messages.InstrumentMessage;
import com.epam.deltix.util.JUnitCategories;
import com.epam.deltix.util.collections.generated.ByteArrayList;
import com.epam.deltix.util.lang.Util;
import com.epam.deltix.util.memory.MemoryDataInput;
import com.epam.deltix.util.memory.MemoryDataOutput;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * User: BazylevD
 * Date: Jan 12, 2010
 * Time: 9:52:23 PM
 */
@Category(JUnitCategories.TickDBCodecs.class)
public class Test_RecordCodecsBinary extends Test_RecordCodecsBase {

    private static final RecordClassDescriptor cdMsgClassBinary =
            new RecordClassDescriptor(
                    "My Binary Descriptor",
                    "MsgClassBinary",
                    false,
                    null,
                    new NonStaticDataField("int", null, new IntegerDataType(IntegerDataType.ENCODING_INT32, true)),
                    new NonStaticDataField("long", null, new IntegerDataType(IntegerDataType.ENCODING_INT64, true)),
                    new NonStaticDataField("string", null, new VarcharDataType(VarcharDataType.ENCODING_INLINE_VARSIZE, true, false)),

                    new NonStaticDataField("binary1", null, new BinaryDataType(true, 0)),
                    new NonStaticDataField("binary2", null, new BinaryDataType(true, 1024, 0) )
            );

    @Test
    public void testBinaryUnboundComp() throws Exception {
        setUpComp();
        testBinaryUnbound();
    }

    @Test
    public void testBinaryUnboundIntp() throws Exception {
        setUpIntp();
        testBinaryUnbound();
    }

    private void testBinaryUnbound() throws Exception {
        // test allowed nulls
        final List<Object> values = new ArrayList<Object>(4);
        values.add(10000);
        values.add(9000000000L);
        values.add("Hi Nicolia!");
        //final byte[] bin1 = new byte[0x10000000]; // 16MB
        final byte[] bin1 = new byte[0x1000]; // 1KB
        for (int i = 0; i < bin1.length; i++) {
            bin1[i] = (byte)i;
        }
        values.add(bin1);
        final byte[] bin2 = new byte[1000];
        for (int i = 0; i < bin2.length; i++) {
            bin2[i] = (byte)i;
        }
        values.add(bin2);

        MemoryDataOutput out = unboundEncode(values, cdMsgClassBinary);
        MemoryDataInput in = new MemoryDataInput(out);
        in.reset(out.getPosition());
        final List<Object> values2 = unboundDecode(in, cdMsgClassBinary);
        assertValuesEquals(values, values2, cdMsgClassBinary);
    }

    public static class BlobMessage extends InstrumentMessage {
        public ByteArrayList bal1;
        public ByteArrayList bal2;
        //public byte[] b1;
        //public byte[] b2;

        @Override
        public String toString() {
            StringBuffer sb = new StringBuffer("bal1: ");
            if (bal1 == null)
                sb.append("null");
            else
                Util.arraydump(sb, bal1.getInternalBuffer(), 0, bal1.size());

            sb.append(" bal2: ");
            if (bal2 == null)
                sb.append("null");
            else
                Util.arraydump(sb, bal2.getInternalBuffer(), 0, bal2.size());

/*
            sb.append(" b1: ");
            if (b1 == null)
                sb.append("null");
            else
                Util.arraydump(sb, b1, 0, b1.length);

            sb.append(" b2: ");
            if (b2 == null)
                sb.append("null");
            else
                Util.arraydump(sb, b2, 0, b2.length);
*/

            return sb.toString();
        }
    }

    private static final RecordClassDescriptor cdMsgBlobMessage =
            new RecordClassDescriptor(
                    BlobMessage.class.getName(),
                    "MsgBlobMessage",
                    false,
                    null,
                    new NonStaticDataField("bal1", null, new BinaryDataType(true, 0)),
                    new NonStaticDataField("bal2", null, new BinaryDataType(true, 0))
                    //new NonStaticDataField("b1", null, new BinaryDataType(true, 1024, 0)),
                    //new NonStaticDataField("b2", null, new BinaryDataType(true, 1024, 0))
            );

    private static final RecordClassDescriptor cdMsgBlobMessageNonNullable =
            new RecordClassDescriptor(
                    BlobMessage.class.getName(),
                    "MsgBlobMessageNonNullable",
                    false,
                    null,
                    new NonStaticDataField("bal1", null, new BinaryDataType(true, 0)),
                    new NonStaticDataField("bal2", null, new BinaryDataType(false, 0))
                    //new NonStaticDataField("b1", null, new BinaryDataType(true, 1024, 0)),
                    //new NonStaticDataField("b2", null, new BinaryDataType(false, 1024, 0))
            );


    @Test
    public void testBinaryBoundComp() throws Exception {
        setUpComp();
        testBinaryBound();
    }


    @Test
    public void testBinaryBoundIntp() throws Exception {
        setUpIntp();
        testBinaryBound();
    }

    private static final byte[] BYTE_ETALON = {0x55, 0, 0x10, 0x0F, (byte) 0xFF, (byte) 0xF5, 0x77, (byte) 0x99};

    private void testBinaryBound() throws Exception {
        // test allowed nulls
        BlobMessage msg = new BlobMessage();
        msg.bal1 = new ByteArrayList(BYTE_ETALON);
        msg.bal2 = null;
        //msg.b1 = new byte[BYTE_ETALON.length];
        //System.arraycopy(BYTE_ETALON, 0, msg.b1, 0, BYTE_ETALON.length);
        //msg.b2 = null;

        MemoryDataOutput out = boundEncode(msg, cdMsgBlobMessage);
        MemoryDataInput in = new MemoryDataInput(out);
        in.reset(out.getPosition());
        final BlobMessage msg2 = new BlobMessage();
        boundDecode(msg2, cdMsgBlobMessage, in);
        Assert.assertEquals(msg.toString(), msg2.toString());

        // test not allowed nulls (on write)
        try {
            boundEncode(msg, cdMsgBlobMessageNonNullable);
            Assert.fail("IllegalArgumentException was not thrown");
        } catch (IllegalArgumentException e) {
            Assert.assertEquals("java.lang.IllegalArgumentException: 'bal2' field is not nullable", e.toString());
        }

        // on read
        boolean assertsEnabled = false;
        assert assertsEnabled = true;  // Intentional side-effect!!!
        if (assertsEnabled) {
            try {
                out = boundEncode(msg, cdMsgBlobMessage);
                in = new MemoryDataInput(out);
                in.reset(out.getPosition());
                boundDecode(msg2, cdMsgBlobMessageNonNullable, in);
                Assert.fail("AssertionError was not thrown");
            } catch (AssertionError e) {
                Assert.assertEquals("java.lang.AssertionError: 'bal2' field is not nullable", e.toString());
            } catch (IllegalStateException e) {
                Assert.assertEquals(String.format("cannot write null to not nullable field 'bal2'"), e.getMessage());
            }
        }

/*
        msg.bal2 = new ByteArrayList(BYTE_ETALON);
        try {
            boundEncode(msg, cdMsgBlobMessageNonNullable);
            Assert.fail("IllegalArgumentException was not thrown");
        } catch (IllegalArgumentException e) {
            Assert.assertEquals("java.lang.IllegalArgumentException: 'b2' field is not nullable", e.toString());
        }
        if (assertsEnabled) {
            try {
                out = boundEncode(msg, cdMsgBlobMessage);
                in = new MemoryDataInput(out);
                in.reset(out.getPosition());
                boundDecode(msg2, cdMsgBlobMessageNonNullable, in);
                Assert.fail("AssertionError was not thrown");
            } catch (AssertionError e) {
                Assert.assertEquals("java.lang.AssertionError: 'b2' field is not nullable", e.toString());
            } catch (IllegalStateException e) {
                Assert.assertEquals("java.lang.IllegalStateException: cannot write null to not nullable field", e.toString());
            }
        }
*/
    }


    @Test
    public void testBinaryIntrospectorComp() throws Exception {
        setUpComp();
        testBinaryBound();
    }

    @Test
    public void testBinaryIntrospectorIntp() throws Exception {
        setUpIntp();
        testBinaryIntrospector();
    }

    private void testBinaryIntrospector() throws Exception {
        BlobMessage msg = new BlobMessage();
        msg.bal1 = new ByteArrayList(BYTE_ETALON);
        msg.bal2 = null;
        //msg.b1 = new byte[BYTE_ETALON.length];
        //System.arraycopy(BYTE_ETALON, 0, msg.b1, 0, BYTE_ETALON.length);
        //msg.b2 = null;

        final RecordClassDescriptor rcd = (RecordClassDescriptor)Introspector.introspectSingleClass(BlobMessage.class);

        MemoryDataOutput out = boundEncode(msg, rcd);
        MemoryDataInput in = new MemoryDataInput(out);
        in.reset(out.getPosition());
        final BlobMessage msg2 = new BlobMessage();
        boundDecode(msg2, rcd, in);
        Assert.assertEquals(msg.toString(), msg2.toString());
    }

    @Test
    public void testBinaryAPIComp() throws Exception {
        setUpComp();
        testBinaryAPI();
    }

    @Test
    public void testBinaryAPIIntp() throws Exception {
        setUpIntp();
        testBinaryAPI();
    }

    private void testBinaryAPI() throws Exception {
        // test allowed nulls
        final List<Object> values = new ArrayList<Object>(4);
        values.add(10000);
        values.add(9000000000L);
        values.add("Hi Nicolia!");
        final byte[] bin1 = new byte[0x1000]; // 1KB
        for (int i = 0; i < bin1.length; i++) {
            bin1[i] = (byte)i;
        }
        values.add(bin1);
        values.add(null);

        MemoryDataOutput out = unboundEncode(values, cdMsgClassBinary);
        MemoryDataInput in = new MemoryDataInput(out);
        in.reset(out.getPosition());

        UnboundDecoder udec = factory.createFixedUnboundDecoder(cdMsgClassBinary);
        udec.beginRead(in);
        for (int i = 0; i < values.size() - 1; i++) {
            udec.nextField();
        }

        int len = udec.getBinaryLength();
        ByteArrayOutputStream os = new ByteArrayOutputStream(len);
        udec.getBinary(0, len, os);
        Assert.assertArrayEquals(bin1, os.toByteArray());

        len = udec.getBinaryLength();
        os.reset();
        udec.getBinary(0, len, os);
        Assert.assertArrayEquals(bin1, os.toByteArray());

        final byte[] binOut = new byte[len];
        udec.getBinary(0, len, binOut, 0);
        Assert.assertArrayEquals(bin1, binOut);

        udec.getBinary(0, len, binOut, 0);
        Assert.assertArrayEquals(bin1, binOut);
    }

    public static class BlobMessageRelative extends BlobMessage {
        public double close;
        public double open;

        @Override
        public String toString() {
            return String.format("%s,%f,%f", super.toString(), close, open);
        }
    }

    private static final RecordClassDescriptor cdMsgBlobMessageRelative =
            new RecordClassDescriptor(
                    BlobMessageRelative.class.getName(),
                    "MsgBlobMessageRelative",
                    false,
                    cdMsgBlobMessage,
                    new NonStaticDataField("close", null, new FloatDataType(FloatDataType.ENCODING_SCALE_AUTO, true)),
                    new NonStaticDataField("open", null, new FloatDataType(FloatDataType.ENCODING_SCALE_AUTO, true), "close")
            );

    /*
     * test the case with relative fields
     */
    @Test
    @Ignore // not implemented yet
    public void testRelativeBoundComp() throws Exception {
        setUpComp();
        testRelativeBound();
    }

    @Test
    public void testRelativeBoundIntp() throws Exception {
        setUpIntp();
        testRelativeBound();
    }

    private void testRelativeBound() throws Exception {
        final BlobMessageRelative msg = new BlobMessageRelative();
        msg.bal1 = new ByteArrayList(BYTE_ETALON);
        msg.bal2 = null;
        //msg.b1 = new byte[BYTE_ETALON.length];
        //System.arraycopy(BYTE_ETALON, 0, msg.b1, 0, BYTE_ETALON.length);
        //msg.b2 = null;
        msg.close = 55.78;
        msg.open = 54.43;

        MemoryDataOutput out = boundEncode(msg, cdMsgBlobMessageRelative);
        MemoryDataInput in = new MemoryDataInput(out);
        in.reset(out.getPosition());
        final BlobMessageRelative msg2 = new BlobMessageRelative();
        boundDecode(msg2, cdMsgBlobMessageRelative, in);
        Assert.assertEquals(msg.toString(), msg2.toString());
    }

    @Test
    public void testRelativeUnboundComp() throws Exception {
        setUpComp();
        testRelativeUnbound();
    }

    @Test
    public void testRelativeUnboundIntp() throws Exception {
        setUpIntp();
        testRelativeUnbound();
    }

    private void testRelativeUnbound() throws Exception {
        final List<Object> values = new ArrayList<Object>(4);
/*        final byte[] bin1 = new byte[0x1000]; // 1KB
        for (int i = 0; i < bin1.length; i++) {
            bin1[i] = (byte)i;
        }
        values.add(bin1);
        values.add(null);*/
        final byte[] bin2 = new byte[1000];
        for (int i = 0; i < bin2.length; i++) {
            bin2[i] = (byte)i;
        }
        values.add(bin2);
        values.add(null);
        values.add(55.78);
        values.add(54.43);

        MemoryDataOutput out = unboundEncode(values, cdMsgBlobMessageRelative);
        MemoryDataInput in = new MemoryDataInput(out);
        in.reset(out.getPosition());
        final List<Object> values2 = unboundDecode(in, cdMsgBlobMessageRelative);
        assertValuesEquals(values, values2, cdMsgBlobMessageRelative);
    }


    public static class BlobMessagePrivate extends InstrumentMessage {
        private ByteArrayList bal1;
        private ByteArrayList bal2;
        //private byte[] b1;
        //private byte[] b2;


        public ByteArrayList getBal1 () {
            return bal1;
        }

        public void setBal1 (ByteArrayList bal1) {
            this.bal1 = bal1;
        }

        public ByteArrayList getBal2 () {
            return bal2;
        }

        public void setBal2 (ByteArrayList bal2) {
            this.bal2 = bal2;
        }

        @Override
        public String toString() {
            StringBuffer sb = new StringBuffer("bal1: ");
            if (bal1 == null)
                sb.append("null");
            else
                Util.arraydump(sb, bal1.getInternalBuffer(), 0, bal1.size());

            sb.append(" bal2: ");
            if (bal2 == null)
                sb.append("null");
            else
                Util.arraydump(sb, bal2.getInternalBuffer(), 0, bal2.size());

/*
            sb.append(" b1: ");
            if (b1 == null)
                sb.append("null");
            else
                Util.arraydump(sb, b1, 0, b1.length);

            sb.append(" b2: ");
            if (b2 == null)
                sb.append("null");
            else
                Util.arraydump(sb, b2, 0, b2.length);
*/

            return sb.toString();
        }
    }

    private static final RecordClassDescriptor cdMsgBlobMessagePrivate =
            new RecordClassDescriptor(
                    BlobMessagePrivate.class.getName(),
                    "MsgBlobMessagePrivate",
                    false,
                    null,
                    new NonStaticDataField("bal1", null, new BinaryDataType(true, 0)),
                    new NonStaticDataField("bal2", null, new BinaryDataType(true, 0))
                    //new NonStaticDataField("b1", null, new BinaryDataType(true, 1024, 0)),
                    //new NonStaticDataField("b2", null, new BinaryDataType(true, 1024, 0))
            );

    private static final RecordClassDescriptor cdMsgBlobMessagePrivateNonNullable =
            new RecordClassDescriptor(
                    BlobMessagePrivate.class.getName(),
                    "MsgBlobMessagePrivateNonNullable",
                    false,
                    null,
                    new NonStaticDataField("bal1", null, new BinaryDataType(true, 0)),
                    new NonStaticDataField("bal2", null, new BinaryDataType(false, 0))
                    //new NonStaticDataField("b1", null, new BinaryDataType(true, 1024, 0)),
                    //new NonStaticDataField("b2", null, new BinaryDataType(false, 1024, 0))
            );

    /*
     * test the case with private fields
     */
    @Test
    @Ignore // not implemented yet
    public void testPrivateBoundComp() throws Exception {
        setUpComp();
        testPrivateBound();
    }

    @Test
    public void testPrivateBoundIntp() throws Exception {
        setUpIntp();
        testPrivateBound();
    }

    private void testPrivateBound() throws Exception {
        final BlobMessagePrivate msg = new BlobMessagePrivate();
        msg.bal1 = new ByteArrayList(BYTE_ETALON);
        msg.bal2 = null;
        //msg.b1 = new byte[BYTE_ETALON.length];
        //System.arraycopy(BYTE_ETALON, 0, msg.b1, 0, BYTE_ETALON.length);
        //msg.b2 = null;

        MemoryDataOutput out = boundEncode(msg, cdMsgBlobMessagePrivate);
        MemoryDataInput in = new MemoryDataInput(out);
        in.reset(out.getPosition());
        final BlobMessagePrivate msg2 = new BlobMessagePrivate();
        boundDecode(msg2, cdMsgBlobMessagePrivate, in);
        Assert.assertEquals(msg.toString(), msg2.toString());

        // test not allowed nulls (on write)
        try {
            boundEncode(msg, cdMsgBlobMessagePrivateNonNullable);
            Assert.fail("IllegalArgumentException was not thrown");
        } catch (IllegalArgumentException e) {
            Assert.assertEquals("java.lang.IllegalArgumentException: 'bal2' field is not nullable", e.toString());
        }

        // on read
        boolean assertsEnabled = false;
        assert assertsEnabled = true;  // Intentional side-effect!!!
        if (assertsEnabled) {
            try {
                out = boundEncode(msg, cdMsgBlobMessagePrivate);
                in = new MemoryDataInput(out);
                in.reset(out.getPosition());
                boundDecode(msg2, cdMsgBlobMessagePrivateNonNullable, in);
                Assert.fail("AssertionError was not thrown");
            } catch (AssertionError e) {
                Assert.assertEquals("java.lang.AssertionError: 'bal2' field is not nullable", e.toString());
            }
        }

/*
        msg.bal2 = new ByteArrayList(BYTE_ETALON);
        try {
            boundEncode(msg, cdMsgBlobMessagePrivateNonNullable);
            Assert.fail("IllegalArgumentException was not thrown");
        } catch (IllegalArgumentException e) {
            Assert.assertEquals("java.lang.IllegalArgumentException: 'b2' field is not nullable", e.toString());
        }
        if (assertsEnabled) {
            try {
                out = boundEncode(msg, cdMsgBlobMessagePrivate);
                in = new MemoryDataInput(out);
                in.reset(out.getPosition());
                boundDecode(msg2, cdMsgBlobMessagePrivateNonNullable, in);
                Assert.fail("AssertionError was not thrown");
            } catch (AssertionError e) {
                Assert.assertEquals("java.lang.AssertionError: 'b2' field is not nullable", e.toString());
            }
        }
*/
    }
}

// remove fast test to Test_RecCode2 ...

