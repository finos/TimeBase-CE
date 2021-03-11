package com.epam.deltix.test.qsrv.hf.pub;

import com.epam.deltix.dfp.Decimal64;
import com.epam.deltix.dfp.Decimal64Utils;
import com.epam.deltix.qsrv.hf.pub.TypeLoader;
import com.epam.deltix.qsrv.hf.pub.TypeLoaderImpl;
import com.epam.deltix.qsrv.hf.pub.md.Introspector;
import com.epam.deltix.qsrv.hf.pub.md.RecordClassDescriptor;
import com.epam.deltix.timebase.messages.*;
import com.epam.deltix.util.JUnitCategories;
import com.epam.deltix.util.collections.generated.LongArrayList;
import com.epam.deltix.util.memory.MemoryDataInput;
import com.epam.deltix.util.memory.MemoryDataOutput;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;

/**
 * Created by Alex Karpovich on 3/12/2018.
 */
@Category(JUnitCategories.TickDBCodecs.class)
public class Test_RecordCodecDecimals extends Test_RecordCodecsBase {

    static final TypeLoader CL = TypeLoaderImpl.DEFAULT_INSTANCE;

    private static RecordClassDescriptor LONG_RCD;
    private static RecordClassDescriptor DOUBLE_RCD;

    public static class LongPriceTestMessage extends LongPriceBaseMessage {

        public LongPriceTestMessage() {
        }

        public LongPriceTestMessage(long price, long size) {
            super(price, size);
        }

        public boolean hasPrice() {
            return price != TypeConstants.DECIMAL_NULL;
        }

        public void nullifyPrice() {
            this.price = TypeConstants.DECIMAL_NULL;
        }

        public boolean hasSize() {
            return size != TypeConstants.DECIMAL_NULL;
        }

        public void nullifySize() {
            this.size = TypeConstants.DECIMAL_NULL;
        }
    }

    public static class LongPriceBaseMessage extends InstrumentMessage {

        protected long price = TypeConstants.DECIMAL_NULL;
        protected long size = TypeConstants.DECIMAL_NULL;

        public LongPriceBaseMessage() { }

        public LongPriceBaseMessage(long price, long size) {
            this.price = price;
            this.size = size;
        }

        @SchemaElement(title = "Price")
        @SchemaType(dataType = SchemaDataType.FLOAT, encoding = "DECIMAL64")
        public long getPrice() {
            return price;
        }

        public void setPrice(long value) {
            this.price = value;
        }

        @SchemaElement(title = "Size")
        @SchemaType(dataType = SchemaDataType.FLOAT, encoding = "DECIMAL64")
        public long getSize() {
            return size;
        }

        public void setSize(long value) {
            this.size = value;
        }

        @Override
        public boolean equals(Object obj) {
            boolean superEquals = super.equals(obj);

            if (!superEquals) return false;
            if (!(obj instanceof LongPriceTestMessage)) return false;
            LongPriceTestMessage other =(LongPriceTestMessage)obj;
            if (getPrice() != other.getPrice()) return false;
            if (getSize() != other.getSize()) return false;

            return true;
        }

        @Override
        public String toString() {
            StringBuilder str = new StringBuilder().append(super.toString()).append(". ");
            str.append(getClass().getName());
            str.append(",").append(getPrice());
            str.append(",").append(getSize());
            return str.toString();
        }
    }

    public static class DecimalPriceBaseMessage extends InstrumentMessage {

        protected Decimal64 price;
        protected Decimal64 size;

        public DecimalPriceBaseMessage() { }

        public DecimalPriceBaseMessage(long price, long size) {
            this.price = Decimal64.fromUnderlying(price);
            this.size = Decimal64.fromUnderlying(price);
        }

        @SchemaElement(title = "Price")
        @SchemaType(dataType = SchemaDataType.FLOAT, encoding = "DECIMAL64")
        public Decimal64 getPrice() {
            return price;
        }

        public void setPrice(Decimal64 value) {
            this.price = value;
        }

        @SchemaElement(title = "Size")
        @SchemaType(dataType = SchemaDataType.FLOAT, encoding = "DECIMAL64")
        public Decimal64 getSize() {
            return size;
        }

        public void setSize(Decimal64 value) {
            this.size = value;
        }

        @Override
        public boolean equals(Object obj) {
            if (super.equals(obj))
                return true;

            if (!(obj instanceof DecimalPriceBaseMessage))
                return false;

            DecimalPriceBaseMessage other = (DecimalPriceBaseMessage)obj;

            if (getPrice() != other.getPrice()) return false;
            if (getSize() != other.getSize()) return false;

            return true;
        }

        @Override
        public String toString() {
            StringBuilder str = new StringBuilder().append(super.toString()).append(". ");
            str.append(getClass().getName());
            str.append(",").append(getPrice());
            str.append(",").append(getSize());
            return str.toString();
        }
    }

    public static class LongFlatPriceTestMessage extends InstrumentMessage {

        @SchemaElement(title = "Price")
        @SchemaType(dataType = SchemaDataType.FLOAT, encoding = "DECIMAL64")
        public long price = TypeConstants.DECIMAL_NULL;

        @SchemaElement(title = "Size")
        @SchemaType(dataType = SchemaDataType.FLOAT, encoding = "DECIMAL64")
        public long size = TypeConstants.DECIMAL_NULL;

        public LongFlatPriceTestMessage() { }

        public LongFlatPriceTestMessage(long price, long size) {
            this.price = price;
            this.size = size;
        }

        @Override
        public String toString() {
            StringBuilder str = new StringBuilder().append(super.toString()).append(". ");
            str.append("BasePriceEntry");
            str.append(",").append(price);
            str.append(",").append(size);
            return str.toString();
        }
    }

    public static class DoublePriceTestMessage extends InstrumentMessage {

        protected double price = TypeConstants.IEEE64_NULL;

        protected double size = TypeConstants.IEEE64_NULL;

        @SchemaElement(title = "Price")
        public double getPrice() {
            return price;
        }

        public void setPrice(double value) {
            this.price = value;
        }

        public boolean hasPrice() {
            return price != TypeConstants.IEEE64_NULL;
        }

        public void nullifyPrice() {
            this.price = TypeConstants.IEEE64_NULL;
        }

        @SchemaElement(title = "Size")
        public double getSize() {
            return size;
        }

        public void setSize(double value) {
            this.size = value;
        }

        public boolean hasSize() {
            return size != TypeConstants.IEEE64_NULL;
        }

        public void nullifySize() {
            this.size = TypeConstants.IEEE64_NULL;
        }

        @Override
        public boolean equals(Object obj) {
            boolean superEquals = super.equals(obj);
            if (!superEquals) return false;
            if (!(obj instanceof DoublePriceTestMessage)) return false;

            DoublePriceTestMessage other =(DoublePriceTestMessage)obj;
            if (getPrice() != other.getPrice()) return false;
            if (getSize() != other.getSize()) return false;

            return true;
        }

        @Override
        public String toString() {
            StringBuilder str = new StringBuilder().append(super.toString()).append(". ");
            str.append("BasePriceEntry");
            str.append(",").append(hasPrice() ? getPrice() : "null");
            str.append(",").append(hasSize() ? getSize() : "null");
            return str.toString();
        }
    }

    public static class LongArrayPriceTestMessage extends InstrumentMessage {

        private LongArrayList values;

        @SchemaArrayType(elementDataType = SchemaDataType.FLOAT, elementEncoding = "DECIMAL64")
        public LongArrayList getValues() {
            return values;
        }

        public void setValues(LongArrayList value) {
            this.values = value;
        }
    }

    @BeforeClass
    public static void setUp() throws Throwable {
        LONG_RCD = (RecordClassDescriptor) Introspector.introspectSingleClass(LongPriceTestMessage.class);
        DOUBLE_RCD = (RecordClassDescriptor) Introspector.introspectSingleClass(DoublePriceTestMessage.class);
    }

    @Test
    public void compiled() throws Exception {
        setUpComp();
        testCodecs(0.12345, 123.456789);
        testCodecs(Double.NaN, Double.NaN);
        testCodecs(0.689, 0.689);
        //testCodecs1();
    }

    @Test
    public void interpretted() throws Exception {
        setUpIntp();
        testCodecs(0.12345, 123.456789);
        testCodecs(Double.NaN, Double.NaN);
    }

    private void checkValues(double price, double size, DoublePriceTestMessage msg) {
        if (Double.isNaN(price))
            Assert.assertTrue(Double.isNaN(msg.getPrice()));
        else
            Assert.assertEquals(price, msg.getPrice(),1E-16);

        if (Double.isNaN(size))
            Assert.assertTrue(Double.isNaN(msg.getSize()));
        else
            Assert.assertEquals(size, msg.getSize(), 1E-16);
    }

    private void checkValues(long price, long size, long p, long s) {

        if (Decimal64Utils.isNaN(price))
            Assert.assertTrue(Decimal64Utils.isNaN(p));
        else
            Assert.assertEquals(price, p);

        if (Decimal64Utils.isNaN(size))
            Assert.assertTrue(Decimal64Utils.isNaN(s));
        else
            Assert.assertEquals(size, s);
    }

    private void checkValues(Decimal64 price, Decimal64 size, Decimal64 p, Decimal64 s) {
        if (price == null)
            Assert.assertNull(p);
        else
            Assert.assertTrue(Decimal64.isIdentical(price, p));
        if (size == null)
            Assert.assertNull(s);
        else
            Assert.assertTrue(Decimal64.isIdentical(size, s));
    }

    public void testCodecs(double price, double size) throws Exception {

        long longPrice = Double.isNaN(price) ? Decimal64Utils.NULL : Decimal64Utils.fromDouble(price);
        long longSize = Double.isNaN(size) ? Decimal64Utils.NULL : Decimal64Utils.fromDouble(size);

        // Decimal64
        DecimalPriceBaseMessage dc = new DecimalPriceBaseMessage(longPrice, longSize);
        DecimalPriceBaseMessage dce = (DecimalPriceBaseMessage) encodeDecode(dc, DecimalPriceBaseMessage.class);
        checkValues(dc.getPrice(), dc.getSize(), dce.getPrice(), dce.getSize());

        // long decimal without 'hasers'
        LongPriceBaseMessage msg1 = new LongPriceBaseMessage(longPrice, longSize);
        LongPriceBaseMessage lbm = (LongPriceBaseMessage) encodeDecode(msg1, LongPriceTestMessage.class);
        checkValues(msg1.getPrice(), msg1.getSize(), lbm.getPrice(), lbm.getSize());
        checkValues(price, size, (DoublePriceTestMessage)encodeDecode(msg1, DoublePriceTestMessage.class));

        DecimalPriceBaseMessage d = (DecimalPriceBaseMessage) encodeDecode(msg1, DecimalPriceBaseMessage.class);
        checkValues(longPrice, longSize, Decimal64.toUnderlying(d.price), Decimal64.toUnderlying(d.size));

        // long decimal with 'hasers'
        LongPriceTestMessage msg = new LongPriceTestMessage(longPrice, longSize);
        LongPriceTestMessage lm = (LongPriceTestMessage) encodeDecode(msg, LongPriceTestMessage.class);
        checkValues(msg.getPrice(), msg.getSize(), lm.getPrice(), lm.getSize());
        checkValues(price, size, (DoublePriceTestMessage)encodeDecode(msg, DoublePriceTestMessage.class));

        // long decimal with 'hasers'
        LongFlatPriceTestMessage msg2 = new LongFlatPriceTestMessage(longPrice, longSize);
        LongFlatPriceTestMessage fm = (LongFlatPriceTestMessage) encodeDecode(msg2, LongFlatPriceTestMessage.class);
        checkValues(msg2.price, msg2.size, fm.price, fm.size);
        checkValues(price, size, (DoublePriceTestMessage)encodeDecode(msg2, DoublePriceTestMessage.class));
    }

    public void testCodecs1() throws Exception {
        double price = 0.12345;
        double size = 123.456789;
        test2(new LongFlatPriceTestMessage(Decimal64Utils.fromDouble(price), Decimal64Utils.fromDouble(size)), 0.12345, 123.456789);
        test2(new LongFlatPriceTestMessage(Decimal64Utils.NULL, Decimal64Utils.NULL), Double.NaN, Double.NaN);
    }

    public void testArray() throws Exception {
    }


    private void test1(double price, double size) throws Exception {
        LongPriceTestMessage msg = new LongPriceTestMessage();
        msg.setPrice(Decimal64Utils.fromDouble(price));
        msg.setSize(Decimal64Utils.fromDouble(size));
        test1(msg, price, size);
    }

    private InstrumentMessage encodeDecode(InstrumentMessage msg, Class outClass) throws Introspector.IntrospectionException {

        RecordClassDescriptor rcd = (RecordClassDescriptor) Introspector.introspectSingleClass(msg.getClass());

        MemoryDataOutput out = new MemoryDataOutput();
        factory.createFixedBoundEncoder(cd -> msg.getClass(), rcd).encode(msg, out);

        MemoryDataInput in = new MemoryDataInput(out);
        return  (InstrumentMessage) factory.createFixedBoundDecoder(
                cd -> outClass, rcd).decode(in);

//        Assert.assertEquals(msg.getPrice(), l.getPrice());
//        Assert.assertEquals(msg.getSize(), l.getSize());
//
//        in = new MemoryDataInput(out);
//        DoublePriceTestMessage d = (DoublePriceTestMessage) factory.createFixedBoundDecoder(
//                cd -> DoublePriceTestMessage.class, LONG_RCD).decode(in);
//
//        Assert.assertEquals(price, d.getPrice(), 1E-16);
//        Assert.assertEquals(size, d.getSize(), 1E-16);
    }

    private void test1(LongPriceBaseMessage msg, double price, double size){

        MemoryDataOutput out = new MemoryDataOutput();
        factory.createFixedBoundEncoder(cd -> msg.getClass(), LONG_RCD).encode(msg, out);

        MemoryDataInput in = new MemoryDataInput(out);
        LongPriceBaseMessage l = (LongPriceBaseMessage) factory.createFixedBoundDecoder(
                cd -> msg.getClass(), LONG_RCD).decode(in);

        Assert.assertEquals(msg.getPrice(), l.getPrice());
        Assert.assertEquals(msg.getSize(), l.getSize());

        in = new MemoryDataInput(out);
        DoublePriceTestMessage d = (DoublePriceTestMessage) factory.createFixedBoundDecoder(
                cd -> DoublePriceTestMessage.class, LONG_RCD).decode(in);

        Assert.assertEquals(price, d.getPrice(), 1E-16);
        Assert.assertEquals(size, d.getSize(), 1E-16);
    }

    private void test2(LongFlatPriceTestMessage msg, double price, double size) throws Exception {

        MemoryDataOutput out = new MemoryDataOutput();
        factory.createFixedBoundEncoder(cd -> LongFlatPriceTestMessage.class, LONG_RCD).encode(msg, out);

        MemoryDataInput in = new MemoryDataInput(out);
        LongFlatPriceTestMessage l = (LongFlatPriceTestMessage) factory.createFixedBoundDecoder(
                cd -> LongFlatPriceTestMessage.class, LONG_RCD).decode(in);

        Assert.assertEquals(msg.price, l.price);
        Assert.assertEquals(msg.size, l.size);

        in = new MemoryDataInput(out);
        DoublePriceTestMessage d = (DoublePriceTestMessage) factory.createFixedBoundDecoder(
                cd -> DoublePriceTestMessage.class, LONG_RCD).decode(in);

        Assert.assertEquals(price, d.getPrice(), 1E-16);
        Assert.assertEquals(size, d.getSize(), 1E-16);
    }

    private void test2(double price, double size) throws Exception {

        DoublePriceTestMessage msg = new DoublePriceTestMessage();
        msg.setPrice(price);
        msg.setSize(size);

        MemoryDataOutput out = new MemoryDataOutput();
        factory.createFixedBoundEncoder(cd -> DoublePriceTestMessage.class, DOUBLE_RCD).encode(msg, out);

        MemoryDataInput in = new MemoryDataInput(out);
        DoublePriceTestMessage d = (DoublePriceTestMessage) factory.createFixedBoundDecoder(
                cd -> DoublePriceTestMessage.class, DOUBLE_RCD).decode(in);

        Assert.assertEquals(msg.price, d.getPrice(), 1E-16);
        Assert.assertEquals(msg.size, d.getSize(), 1E-16);

        in = new MemoryDataInput(out);
        LongPriceTestMessage l = (LongPriceTestMessage) factory.createFixedBoundDecoder(
                cd -> LongPriceTestMessage.class, DOUBLE_RCD).decode(in);

        Assert.assertEquals(msg.getPrice(), Decimal64Utils.toDouble(l.getPrice()), 1E-16);
        Assert.assertEquals(msg.getSize(), Decimal64Utils.toDouble(l.getSize()), 1E-16);
    }

}
