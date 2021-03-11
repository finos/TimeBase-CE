package com.epam.deltix.test.qsrv.hf.pub;

import com.epam.deltix.qsrv.hf.pub.*;
import com.epam.deltix.qsrv.hf.pub.codec.FixedUnboundEncoder;
import com.epam.deltix.qsrv.hf.pub.codec.RecordLayout;
import com.epam.deltix.qsrv.hf.pub.codec.UnboundDecoder;
import com.epam.deltix.qsrv.hf.pub.md.*;
import com.epam.deltix.qsrv.hf.tickdb.TDBRunner;
import com.epam.deltix.qsrv.hf.tickdb.pub.*;
import com.epam.deltix.qsrv.hf.tickdb.pub.query.InstrumentMessageSource;
import com.epam.deltix.qsrv.hf.tickdb.util.ZIPUtil;
import com.epam.deltix.timebase.messages.InstrumentMessage;
import com.epam.deltix.timebase.messages.*;
import com.epam.deltix.util.JUnitCategories;
import com.epam.deltix.util.collections.generated.*;
import com.epam.deltix.util.io.Home;
import com.epam.deltix.util.lang.Util;
import com.epam.deltix.util.memory.MemoryDataInput;
import com.epam.deltix.util.memory.MemoryDataOutput;
import org.junit.Assert;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.*;

import static junit.framework.TestCase.assertNotNull;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * OBJECT, ARRAY, ARRAY OF OBJECT
 * see also Test_RecordCodecs5.testArrayOtherBindings
 */
@Category(JUnitCategories.TickDBCodecs.class)
public class Test_RecordCodecs7 extends Test_RecordCodecsBase {

    public static class Recursive extends InstrumentMessage {

        public Recursive    inner;

        public String       text;

        @Override
        public InstrumentMessage copyFrom(RecordInfo from) {
            super.copyFrom(from);

            if (from instanceof Recursive) {
                final Recursive t = (Recursive)from;
                if (t.inner != null) {
                    this.inner = (Recursive) createInstance();
                    this.inner.copyFrom(t.inner);
                }

                this.text = t.text;
            }

            return this;
        }

        @Override
        protected InstrumentMessage createInstance() {
            return new Recursive();
        }

        @Override
        public String toString ()
        {
            return "Recursive: inner={" + inner + "}; text=" + text;
        }
    }

    /**
     * Message object for:
     * CLASS "Next1" (
     *     "arrayNext2" "Next2" ARRAY
     * );
     */
    @SchemaElement(name = "Next1")
    public static class Next1 extends InstrumentMessage {
        public ObjectArrayList<Next2> arrayNext2;

        @Override
        public InstrumentMessage copyFrom(RecordInfo from) {
            super.copyFrom(from);

            if (from instanceof Next1) {
                final Next1 t = (Next1)from;
                if (t.arrayNext2 != null) {
                    arrayNext2 = new ObjectArrayList<Next2>();
                    for (int i = 0; i < t.arrayNext2.size(); ++i)
                        arrayNext2.add((Next2) t.arrayNext2.get(i).clone());
                }
            }

            return this;
        }

        @Override
        protected InstrumentMessage createInstance() {
            return new Next1();
        }

        @Override
        public String toString ()
        {
            return super.toString ()+", "+"arrayNext2"+": "+arrayNext2;
        }
    }

    /**
     * Message object for:
     * CLASS "Next2" (
     *     "arr" INTEGER ARRAY
     * );
     */
    @SchemaElement( name = "Next2")
    public static class Next2 extends InstrumentMessage {
        public LongArrayList arr;

        @Override
        public InstrumentMessage copyFrom(RecordInfo template) {
            super.copyFrom(template);

            if (template instanceof Next2) {
                final Next2 next2 = (Next2)template;
                arr = new LongArrayList(next2.arr);
            }

            return this;
        }

        @Override
        protected InstrumentMessage createInstance() {
            return new Next2();
        }

        @Override
        public String toString ()
        {
            return super.toString ()+", "+"arr"+": "+arr;
        }
    }

    /**
     * Message object for:
     * CLASS "Top" (
     *     "array" "Next1" ARRAY
     * );
     */
    @SchemaElement(name = "Top")
    public static class Top extends InstrumentMessage {
        public ObjectArrayList<Next1> array;

        @Override
        public InstrumentMessage copyFrom(RecordInfo from) {
            super.copyFrom(from);

            if (from instanceof Top) {
                final Top t = (Top) from;
                if (t.array != null) {
                    array = new ObjectArrayList<>();
                    for (int i = 0; i < t.array.size(); ++i)
                        array.add((Next1) t.array.get(i).clone());
                }
            }

            return this;
        }

        @Override
        protected InstrumentMessage createInstance() {
            return new Next1();
        }

        @Override
        public String toString ()
        {
            return super.toString ()+", "+"array"+": "+array;
        }
    }

    private static final File DIR = Home.getFile("/testdata/tickdb/misc");
    private static final File ZIP = new File(DIR, "test.zip");
    private static String createDB(String zipFileName) throws IOException, InterruptedException {
        File folder = new File(TDBRunner.getTemporaryLocation());
        //BasicIOUtil.deleteFileOrDir(folder);
        //folder.mkdirs();

        FileInputStream is = new FileInputStream(zipFileName);
        ZIPUtil.extractZipStream(is, folder);
        is.close();

        return folder.getAbsolutePath();
    }

    @Test
    public void testNestedArrays() throws Exception {
        String path = createDB(ZIP.getAbsolutePath());

        try (DXTickDB db = TickDBFactory.create(path)) {
            db.open (true);
            DXTickStream stream = db.getStream ("test");
            long                time = TimeConstants.TIMESTAMP_UNKNOWN;
            SelectionOptions options = new SelectionOptions ();
            MappingTypeLoader loader =  new MappingTypeLoader();
            loader.bind ("Next1", Next1.class);
            loader.bind ("Top", Top.class);
            loader.bind ("Next2", Next2.class);
            options.typeLoader =loader;


            try (InstrumentMessageSource cursor =
                         stream.select (time, options))
            {
                int     count = 0;
                while (cursor.next () && count++ < 100) {
                    InstrumentMessage message = cursor.getMessage();
                    assertNotNull(message);
                }
                cursor.close();
            }
            db.close();
        }
    }

    @Test
    public void testRecursive() throws Introspector.IntrospectionException {
        setUpComp();
        testInnerClass();

        setUpIntp();
        testInnerClass();
    }

    public void testInnerClass() throws Introspector.IntrospectionException {
        final RecordClassDescriptor rcd = getRCD(Recursive.class);

        Recursive msg = new Recursive();
        msg.text = "first";
        testRcdBound(rcd, msg, new Recursive());
        testRcdBound(rcd, msg, null);

        msg = new Recursive();
        msg.text = "second";
        msg.inner = new Recursive();
        msg.inner.text = "second.first";

        testRcdBound(rcd, msg, new Recursive());
        testRcdBound(rcd, msg, null);
    }

    /*
     * OBJECT fixed nullable field case
     */

    public static class FixedObjFieldsPublic extends InstrumentMessage {
        public String s1;
        @SchemaType(
                dataType = SchemaDataType.OBJECT,
                nestedTypes = {MsgClassAllPublic.class}
        )
        public MsgClassAllPublic oPub1;
        @SchemaType(
                dataType = SchemaDataType.OBJECT,
                nestedTypes = {MsgClassAllPublic.class}
        )
        public MsgClassAllPublic oPub2;
        @SchemaType(
                dataType = SchemaDataType.OBJECT,
                nestedTypes = {MsgClassAllPrivate.class}
        )
        public MsgClassAllPrivate oPri1;
        @SchemaType(
                dataType = SchemaDataType.OBJECT,
                nestedTypes = {MsgClassAllPrivate.class}
        )
        public MsgClassAllPrivate oPri2;

        @Override
        public String toString() {
            return String.valueOf(s1) + ',' +
                    String.valueOf(oPub1) + ',' + String.valueOf(oPub2) + ',' +
                    String.valueOf(oPri1) + ',' + String.valueOf(oPri2);
        }

        void setNulls() {
            s1 = null;
            oPub1 = null;
            oPub2 = null;
            oPri1 = null;
            oPri2 = null;
        }

        void setValues(boolean isNull) {
            s1 = "Hi Marco!!!";

            oPub1 = new MsgClassAllPublic();
            if (isNull)
                oPub1.setNulls();
            else
                oPub1.setValues();
            oPub2 = null;

            oPri1 = new MsgClassAllPrivate();
            if (isNull)
                oPri1.setNulls();
            else
                oPri1.setValues();
            oPri2 = null;
        }
    }

    public static class FixedObjFieldsPrivate extends InstrumentMessage {
        private String s1;
//        @SchemaType(
//                dataType = SchemaDataType.OBJECT,
//                nestedTypes = {MsgClassAllPublic.class}
//        )
        private MsgClassAllPublic oPub1;
//        @SchemaType(
//                dataType = SchemaDataType.OBJECT,
//                nestedTypes = {MsgClassAllPublic.class}
//        )
        private MsgClassAllPublic oPub2;
//        @SchemaType(
//                dataType = SchemaDataType.OBJECT,
//                nestedTypes = {MsgClassAllPrivate.class}
//        )
        private MsgClassAllPrivate oPri1;
//        @SchemaType(
//                dataType = SchemaDataType.OBJECT,
//                nestedTypes = {MsgClassAllPrivate.class}
//        )
        private MsgClassAllPrivate oPri2;

        @Override
        public String toString() {
            return String.valueOf(s1) + ',' +
                    String.valueOf(oPub1) + ',' + String.valueOf(oPub2) + ',' +
                    String.valueOf(oPri1) + ',' + String.valueOf(oPri2);
        }

        void setNulls() {
            s1 = null;
            oPub1 = null;
            oPub2 = null;
            oPri1 = null;
            oPri2 = null;
        }

        void setValues(boolean isNull) {
            s1 = "Hi Marco!!!";

            oPub1 = new MsgClassAllPublic();
            if (isNull)
                oPub1.setNulls();
            else
                oPub1.setValues();
            oPub2 = null;

            oPri1 = new MsgClassAllPrivate();
            if (isNull)
                oPri1.setNulls();
            else
                oPri1.setValues();
            oPri2 = null;
        }

        @SchemaElement
        public String getS1 () {
            return s1;
        }

        public void setS1 (String s1) {
            this.s1 = s1;
        }

        @SchemaElement
        @SchemaType(
                dataType = SchemaDataType.OBJECT,
                nestedTypes = {MsgClassAllPublic.class}
        )
        public MsgClassAllPublic getoPub1 () {
            return oPub1;
        }

        public void setoPub1 (MsgClassAllPublic oPub1) {
            this.oPub1 = oPub1;
        }

        @SchemaElement
        @SchemaType(
                dataType = SchemaDataType.OBJECT,
                nestedTypes = {MsgClassAllPublic.class}
        )
        public MsgClassAllPublic getoPub2 () {
            return oPub2;
        }

        public void setoPub2 (MsgClassAllPublic oPub2) {
            this.oPub2 = oPub2;
        }

        @SchemaElement
        @SchemaType(
                dataType = SchemaDataType.OBJECT,
                nestedTypes = {MsgClassAllPrivate.class}
        )
        public MsgClassAllPrivate getoPri1 () {
            return oPri1;
        }

        public void setoPri1 (MsgClassAllPrivate oPri1) {
            this.oPri1 = oPri1;
        }

        @SchemaElement
        @SchemaType(
                dataType = SchemaDataType.OBJECT,
                nestedTypes = {MsgClassAllPrivate.class}
        )
        public MsgClassAllPrivate getoPri2 () {
            return oPri2;
        }

        public void setoPri2 (MsgClassAllPrivate oPri2) {
            this.oPri2 = oPri2;
        }
    }

    @Test
    public void testObjectFieldFixedComp() throws Exception {
        setUpComp();
        testObjectFieldFixed();
    }

    @Test
    public void testObjectFieldFixedIntp() throws Exception {
        setUpIntp();
        testObjectFieldFixed();
    }

    private void testObjectFieldFixed() throws Exception {
        // public case
        {
            final RecordClassDescriptor rcd = getRCD(FixedObjFieldsPublic.class);

            final FixedObjFieldsPublic msg = new FixedObjFieldsPublic();

            msg.setValues(false);
            testRcdBound(rcd, msg, new FixedObjFieldsPublic());
            testRcdBound(rcd, msg, null);

            msg.setValues(true);
            testRcdBound(rcd, msg, new FixedObjFieldsPublic());
            testRcdBound(rcd, msg, null);

            msg.setNulls();
            testRcdBound(rcd, msg, new FixedObjFieldsPublic());
            testRcdBound(rcd, msg, null);
        }

        // private case
        {
            final RecordClassDescriptor rcd = getRCD(FixedObjFieldsPrivate.class);

            final FixedObjFieldsPrivate msg = new FixedObjFieldsPrivate();

            msg.setValues(false);
            testRcdBound(rcd, msg, new FixedObjFieldsPrivate());
            testRcdBound(rcd, msg, null);

            msg.setValues(true);
            testRcdBound(rcd, msg, new FixedObjFieldsPrivate());
            testRcdBound(rcd, msg, null);

            msg.setNulls();
            testRcdBound(rcd, msg, new FixedObjFieldsPrivate());
            testRcdBound(rcd, msg, null);
        }
    }

    /*
     * OBJECT polymorphic nullable field case
     */

    // hierarchy of classes for polymorphic case
    public interface InterfaceA {
        void setNulls();

        void setValues();
    }

    public static abstract class ClassAA implements InterfaceA {
        public String s1;

        @Override
        public void setNulls() {
            s1 = null;
        }

        @Override
        public void setValues() {
            s1 = "Hi Kolia";
        }

        @Override
        public String toString() {
            return String.valueOf(s1);
        }
    }

    public static class ClassAAA extends ClassAA {
        public byte mByte;
        public short mShort;

        @Override
        public void setNulls() {
            super.setNulls();
            mByte = IntegerDataType.INT8_NULL;
            mShort = IntegerDataType.INT16_NULL;
        }

        @Override
        public void setValues() {
            super.setValues();
            mByte = 1;
            mShort = 2;
        }
        @Override
        public String toString() {
            return super.toString() + ',' + mByte + ',' + mShort;
        }
    }

    public static class ClassAAB extends ClassAA {
        public int mInt;
        public long mInt48;

        @Override
        public void setNulls() {
            super.setNulls();
            mInt = IntegerDataType.INT32_NULL;
            mInt48 = IntegerDataType.INT48_NULL;
        }

        @Override
        public void setValues() {
            super.setValues();
            mInt = 3;
            mInt48 = 4;
        }
        @Override
        public String toString() {
            return super.toString() + ',' + mInt + ',' + mInt48;
        }
    }

    public static class ClassAAAA extends ClassAAA {
        public float mFloat;
        public double mDouble;

        @Override
        public void setNulls() {
            super.setNulls();
            mFloat = FloatDataType.IEEE32_NULL;
            mDouble = FloatDataType.IEEE64_NULL;
        }

        @Override
        public void setValues() {
            super.setValues();
            mFloat = 63545.34f;
            mDouble = 76456577.76;
        }
        @Override
        public String toString() {
            return super.toString() + ',' + mFloat + ',' + mDouble;
        }
    }

    public static class PolyObjFieldsPublic extends InstrumentMessage {
        public String s1;

        @SchemaType(
                dataType = SchemaDataType.OBJECT,
                nestedTypes = {ClassAAA.class, ClassAAB.class, ClassAAAA.class}
        )
        public InterfaceA   oInter1;

        @SchemaType(
                dataType = SchemaDataType.OBJECT,
                nestedTypes = {ClassAAA.class, ClassAAB.class, ClassAAAA.class}
        )
        public InterfaceA   oInter2;

        @SchemaType(
                dataType = SchemaDataType.OBJECT,
                nestedTypes = {ClassAAA.class, ClassAAB.class, ClassAAAA.class}
        )
        public ClassAA      oClass1;

        @SchemaType(
                dataType = SchemaDataType.OBJECT,
                nestedTypes = {ClassAAA.class, ClassAAB.class, ClassAAAA.class}
        )
        public ClassAA      oClass2;

        @Override
        public String toString() {
            return String.valueOf(s1) + ',' +
                    String.valueOf(oInter1) + ',' + String.valueOf(oInter2) + ',' +
                    String.valueOf(oClass1) + ',' + String.valueOf(oClass2);
        }

        void setNulls() {
            s1 = null;
            oInter1 = null;
            oInter2 = null;
            oClass1 = null;
            oClass2 = null;
        }

        void setValues(boolean isNull, Class type1, Class type2, Class type3) throws Exception {
            s1 = "Hi Marco!!!";

            oInter1 = (InterfaceA) type1.newInstance();
            if (isNull)
                oInter1.setNulls();
            else
                oInter1.setValues();
            oInter2 = null;

            oClass1 = (ClassAA) type2.newInstance();
            if (isNull)
                oClass1.setNulls();
            else
                oClass1.setValues();

            oClass2 = (ClassAA) type3.newInstance();
            if (isNull)
                oClass2.setNulls();
            else
                oClass2.setValues();
        }
    }

    public static class PolyObjFieldsPrivate extends InstrumentMessage {
        private String s1;
//        @SchemaType(
//                dataType = SchemaDataType.OBJECT,
//                nestedTypes = {ClassAAA.class, ClassAAB.class, ClassAAAA.class}
//        )
        private InterfaceA oInter1;
//        @SchemaType(
//                dataType = SchemaDataType.OBJECT,
//                nestedTypes = {ClassAAA.class, ClassAAB.class, ClassAAAA.class}
//        )
        private InterfaceA oInter2;
//        @SchemaType(
//                dataType = SchemaDataType.OBJECT,
//                nestedTypes = {ClassAAA.class, ClassAAB.class, ClassAAAA.class}
//        )
        private ClassAA oClass1;
//        @SchemaType(
//                dataType = SchemaDataType.OBJECT,
//                nestedTypes = {ClassAAA.class, ClassAAB.class, ClassAAAA.class}
//        )
        private ClassAA oClass2;

        @Override
        public String toString() {
            return String.valueOf(s1) + ',' +
                    String.valueOf(oInter1) + ',' + String.valueOf(oInter2) + ',' +
                    String.valueOf(oClass1) + ',' + String.valueOf(oClass2);
        }

        void setNulls() {
            s1 = null;
            oInter1 = null;
            oInter2 = null;
            oClass1 = null;
            oClass2 = null;
        }

        void setValues(boolean isNull, Class type1, Class type2) throws Exception {
            s1 = "Hi Marco!!!";

            oInter1 = (InterfaceA) type1.newInstance();
            if (isNull)
                oInter1.setNulls();
            else
                oInter1.setValues();
            oInter2 = null;

            oClass1 = (ClassAA) type2.newInstance();
            if (isNull)
                oClass1.setNulls();
            else
                oClass1.setValues();
            oClass2 = null;
        }

        @SchemaElement
        public String getS1 () {
            return s1;
        }

        public void setS1 (String s1) {
            this.s1 = s1;
        }

        @SchemaElement
        @SchemaType(
                dataType = SchemaDataType.OBJECT,
                nestedTypes = {ClassAAA.class, ClassAAB.class, ClassAAAA.class}
        )
        public InterfaceA getoInter1 () {
            return oInter1;
        }

        public void setoInter1 (InterfaceA oInter1) {
            this.oInter1 = oInter1;
        }

        @SchemaElement
        @SchemaType(
                dataType = SchemaDataType.OBJECT,
                nestedTypes = {ClassAAA.class, ClassAAB.class, ClassAAAA.class}
        )
        public InterfaceA getoInter2 () {
            return oInter2;
        }

        public void setoInter2 (InterfaceA oInter2) {
            this.oInter2 = oInter2;
        }

        @SchemaElement
        @SchemaType(
                dataType = SchemaDataType.OBJECT,
                nestedTypes = {ClassAAA.class, ClassAAB.class, ClassAAAA.class}
        )
        public ClassAA getoClass1 () {
            return oClass1;
        }

        public void setoClass1 (ClassAA oClass1) {
            this.oClass1 = oClass1;
        }

        @SchemaElement
        @SchemaType(
                dataType = SchemaDataType.OBJECT,
                nestedTypes = {ClassAAA.class, ClassAAB.class, ClassAAAA.class}
        )
        public ClassAA getoClass2 () {
            return oClass2;
        }

        public void setoClass2 (ClassAA oClass2) {
            this.oClass2 = oClass2;
        }
    }

    @Test
    public void testObjectFieldPolyComp() throws Exception {
        setUpComp();
        testObjectFieldPoly();
    }

    @Test
    public void testObjectFieldPolyIntp() throws Exception {
        setUpIntp();
        testObjectFieldPoly();
    }

    private void testObjectFieldPoly() throws Exception {
        // public case
        {
            final RecordClassDescriptor rcd = getRCD(PolyObjFieldsPublic.class);
            final PolyObjFieldsPublic msg = new PolyObjFieldsPublic();

            msg.setNulls();
            testRcdBound(rcd, msg, new PolyObjFieldsPublic());
            testRcdBound(rcd, msg, null);

            msg.setValues(false, ClassAAA.class, ClassAAB.class, ClassAAAA.class);
            testRcdBound(rcd, msg, new PolyObjFieldsPublic());
            testRcdBound(rcd, msg, null);

            msg.setValues(true, ClassAAA.class, ClassAAAA.class, ClassAAB.class);
            testRcdBound(rcd, msg, new PolyObjFieldsPublic());
            testRcdBound(rcd, msg, null);

            msg.setValues(false, ClassAAAA.class, ClassAAA.class, ClassAAB.class);
            testRcdBound(rcd, msg, new PolyObjFieldsPublic());
            testRcdBound(rcd, msg, null);

            msg.setValues(true, ClassAAAA.class, ClassAAB.class, ClassAAA.class);
            testRcdBound(rcd, msg, new PolyObjFieldsPublic());
            testRcdBound(rcd, msg, null);
        }

        // private case
        {
            final RecordClassDescriptor rcd = getRCD(PolyObjFieldsPrivate.class);
            final PolyObjFieldsPrivate msg = new PolyObjFieldsPrivate();

            msg.setNulls();
            testRcdBound(rcd, msg, new PolyObjFieldsPrivate());
            testRcdBound(rcd, msg, null);

            msg.setValues(false, ClassAAA.class, ClassAAB.class);
            testRcdBound(rcd, msg, new PolyObjFieldsPrivate());
            testRcdBound(rcd, msg, null);

            msg.setValues(true, ClassAAA.class, ClassAAB.class);
            testRcdBound(rcd, msg, new PolyObjFieldsPrivate());
            testRcdBound(rcd, msg, null);

            msg.setValues(false, ClassAAAA.class, ClassAAB.class);
            testRcdBound(rcd, msg, new PolyObjFieldsPrivate());
            testRcdBound(rcd, msg, null);

            msg.setValues(true, ClassAAAA.class, ClassAAB.class);
            testRcdBound(rcd, msg, new PolyObjFieldsPrivate());
            testRcdBound(rcd, msg, null);
        }
    }

    @Test
    public void testObjectFieldFixedUnbound() throws Exception {
        setUpIntp();

        final RecordClassDescriptor rcd = getRCD(FixedObjFieldsPublic.class);
        final UnboundDecoder udec = factory.createFixedUnboundDecoder(rcd);
        final List<Object> values = new ArrayList<>(5);

        // Nulls
        for (int i = 0; i < 5; i++) {
            values.add(null);
        }
        MemoryDataOutput out = unboundEncode(values, rcd);
        MemoryDataInput in = new MemoryDataInput(out);
        List<Object> values2 = unboundDecode(in, udec);
        assertValuesEquals(values, values2, rcd);

        // not Nulls
        values.clear();


        List<Object> oPub1 = new ArrayList<>();
        //oPub1.add(findRCD(rcd, "oPub1", MsgClassAllPublic.class));
//        oPub1.add("Hi Kolia");      //s1
//        oPub1.add(InstrumentType.EQUITY.toString()); //mEnum
//        oPub1.add("IBM");           //mString
//        oPub1.add("MSFT");          //mCharSequence
//
//        oPub1.add(true);    //mBoolean
//        oPub1.add(null); //mBoolByte // BooleanDataType.TRUE  assert validation code doesn't support it
//        oPub1.add('C'); //mChar
//        oPub1.add(1235746625319L); //mDateTime
//        oPub1.add(56841);           //mTimeOfDay
//
//        oPub1.add(1);       //mByte
//        oPub1.add(2);       //mShort
//        oPub1.add(3);       //mInt
//        oPub1.add(4L);      //mInt48
//        oPub1.add(5L);      //mLong
//        oPub1.add(63545.34f); //mFloat
//        oPub1.add(76456577.76);  //mDouble
//        oPub1.add(null); //mDouble2 // FloatDataType.IEEE64_NULL assert validation code doesn't support it
//
//        oPub1.add(0x1CCCAAAA); //mPUINT30
//        oPub1.add(0x1CCCAAAA1CCCAAAAL); //mPUINT61
//        oPub1.add(60000);       //mPIneterval
//        oPub1.add(1.52);        //mSCALE_AUTO
//        oPub1.add(1.53);       //mSCALE4

        oPub1.add(null); //mBoolByte // BooleanDataType.TRUE  assert validation code doesn't support it
        oPub1.add(true);    //mBoolean
        oPub1.add(1);       //mByte
        oPub1.add('C'); //mChar
        oPub1.add("MSFT");          //mCharSequence
        oPub1.add(1235746625319L); //mDateTime
        oPub1.add(76456577.76);  //mDouble
        oPub1.add(null); //mDouble2 // FloatDataType.IEEE64_NULL assert validation code doesn't support it
        oPub1.add(63545.34f); //mFloat
        oPub1.add(3);       //mInt
        oPub1.add(4L);      //mInt48
        oPub1.add(5L);      //mLong
        oPub1.add(60000);       //mPIneterval
        oPub1.add(0x1CCCAAAA); //mPUINT30
        oPub1.add(0x1CCCAAAA1CCCAAAAL); //mPUINT61
        oPub1.add(1.53);       //mSCALE4
        oPub1.add(1.52);        //mSCALE_AUTO
        oPub1.add(2);       //mShort
        oPub1.add("IBM");           //mString
        oPub1.add(56841);           //mTimeOfDay
        oPub1.add("Hi Kolia");      //s1


        values.add(oPub1);

        values.add(null);


        // oPri1 values are identical to oPub1
        List<Object> oPri1 = new ArrayList<>();
        oPri1.addAll(oPub1);
        //oPri1.set(0, findRCD(rcd, "oPri1", MsgClassAllPrivate.class));

        values.add(oPri1);
        values.add(null);
        values.add("Hi Marco!!!");
        out = unboundEncode(values, rcd);
        in = new MemoryDataInput(out);
        values2 = unboundDecode(in, udec);
        assertValuesEquals(values, values2, rcd);

        // not Nulls (with null values)
        values.clear();

        oPub1 = new ArrayList<>();
        //oPub1.add(findRCD(rcd, "oPub1", MsgClassAllPublic.class));
        for (int i = 0; i < 22; i++)
            oPub1.add(null);

        oPub1.set(1, true); // mBoolean field is not nullable
        values.add(oPub1);

        values.add(null);

        // oPri1 values are identical to oPub1
        oPri1 = new ArrayList<>();
        oPri1.addAll(oPub1);
        //oPri1.set(0, findRCD(rcd, "oPri1", MsgClassAllPrivate.class));

        values.add(oPri1);
        values.add(null);

        values.add("Hi Marco!!!");

        out = unboundEncode(values, rcd);
        in = new MemoryDataInput(out);
        values2 = unboundDecode(in, udec);
        assertValuesEquals(values, values2, rcd);
    }

    @Test
    public void testObjectFieldPolyUnbound() throws Exception {
        setUpIntp();

        final RecordClassDescriptor rcd = getRCD(PolyObjFieldsPublic.class);
        final UnboundDecoder udec = factory.createFixedUnboundDecoder(rcd);

        // Nulls
        final List<Object> values = new ArrayList<>(5);

        for (int i = 0; i < 5; i++) {
            values.add(null);
        }

        MemoryDataOutput out = unboundEncode(values, rcd);
        MemoryDataInput in = new MemoryDataInput(out);
        List<Object> values2 = unboundDecode(in, udec);
        assertValuesEquals(values, values2, rcd);

        // not Nulls: ClassAAA.class, ClassAAB.class
        values.clear();

        List<Object> oInter1 = new ArrayList<>(5);
        oInter1.add(findRCD(rcd, "oInter1", ClassAAA.class));
        oInter1.add("Hi Kolia");
        oInter1.add(1);
        oInter1.add(2);
        values.add(oInter1);

        values.add(null);

        List<Object> oClass1 = new ArrayList<>(5);
        oClass1.add(findRCD(rcd, "oClass1", ClassAAB.class));
        oClass1.add("Hi Kolia");
        oClass1.add(3);
        oClass1.add(4L);
        values.add(oClass1);

        values.add(null);
        values.add("Hi Marco!!!");


        out = unboundEncode(values, rcd);
        in = new MemoryDataInput(out);
        values2 = unboundDecode(in, udec);
        assertValuesEquals(values, values2, rcd);

        // not Nulls: ClassAAA.class, ClassAAB.class (with null values)
        values.clear();


        oInter1 = new ArrayList<>(5);
        oInter1.add(findRCD(rcd, "oInter1", ClassAAA.class));
        for (int i = 1; i < 5; i++){
            oInter1.add(null);
        }
        values.add(oInter1);

        values.add(null);
        oClass1 = new ArrayList<>(5);
        oClass1.add(findRCD(rcd, "oClass1", ClassAAB.class));
        for (int i = 1; i < 5; i++){
            oClass1.add(null);
        }
        values.add(oClass1);
        values.add(null);

        values.add("Hi Marco!!!");

        out = unboundEncode(values, rcd);
        in = new MemoryDataInput(out);
        values2 = unboundDecode(in, udec);
        assertValuesEquals(values, values2, rcd);

        // not Nulls: ClassAAAA.class, ClassAAB.class
        values.clear();

        oInter1 = new ArrayList<>(5);
        oInter1.add(findRCD(rcd, "oInter1", ClassAAAA.class));
        oInter1.add("Hi Kolia");
        oInter1.add(1);
        oInter1.add(2);
        oInter1.add(76456577.76);
        oInter1.add(63545.34f);
        values.add(oInter1);

        values.add(null);
        oClass1 = new ArrayList<>(5);
        oClass1.add(findRCD(rcd, "oClass1", ClassAAB.class));
        oClass1.add("Hi Kolia");
        oClass1.add(3);
        oClass1.add(4L);
        values.add(oClass1);
        values.add(null);
        values.add("Hi Marco!!!");

        out = unboundEncode(values, rcd);
        in = new MemoryDataInput(out);
        values2 = unboundDecode(in, udec);
        assertValuesEquals(values, values2, rcd);

        // not Nulls: ClassAAAA.class, ClassAAB.class (with null values)
        values.clear();

        oInter1 = new ArrayList<>(5);
        oInter1.add(findRCD(rcd, "oInter1", ClassAAAA.class));
        for (int i = 1; i < 7; i++){
            oInter1.add(null);
        }
        values.add(oInter1);

        values.add(null);
        oClass1 = new ArrayList<>(5);
        oClass1.add(findRCD(rcd, "oClass1", ClassAAB.class));
        for (int i = 1; i < 5; i++){
            oClass1.add(null);
        }
        values.add(oClass1);
        values.add(null);
        values.add("Hi Marco!!!");

        out = unboundEncode(values, rcd);
        in = new MemoryDataInput(out);
        values2 = unboundDecode(in, udec);
        assertValuesEquals(values, values2, rcd);
    }

    private RecordClassDescriptor findRCD(RecordClassDescriptor ownerRcd, String fieldName, Class<?> clazz) {
        for (RecordClassDescriptor rcd : ((ClassDataType) ownerRcd.getField(fieldName).getType()).getDescriptors()) {
            if (rcd.getName().equals(clazz.getName()))
                return rcd;
        }

        throw new IllegalArgumentException(clazz.getName() + " not found in field " + fieldName);
    }

   /*
    * ARRAY type: test standard bindings
    */

    public static class MsgClassArrayPublic extends InstrumentMessage {
        @SchemaArrayType(
                isElementNullable = false,
                elementDataType = SchemaDataType.BOOLEAN
        )
        public BooleanArrayList f1;
        public CharacterArrayList f2;
        @SchemaArrayType(
                elementDataType = SchemaDataType.INTEGER,
                elementEncoding = "INT8"
        )
        public ByteArrayList f3;
        public ShortArrayList f4;
        public IntegerArrayList f5;
        public LongArrayList f6;
        public FloatArrayList f7;
        public DoubleArrayList f8;
        //public ObjectArrayList f9;

        public void populate(List<Object> values) {
            values.clear();
            values.add(f1);
            values.add(f2);
            values.add(f3);
            values.add(f4);
            values.add(f5);
            values.add(f6);
            values.add(f7);
            values.add(f8);
        }

        public void populateUncasted(List<Object> values) {
            values.clear();
            values.add(new ArrayList<Object>(f1));
            values.add(new ArrayList<Object>(f2));
            values.add(new ArrayList<Object>(f3));
            values.add(new ArrayList<Object>(f4));
            values.add(new ArrayList<Object>(f5));
            values.add(new ArrayList<Object>(f6));
            values.add(new ArrayList<Object>(f7));
            values.add(new ArrayList<Object>(f8));
        }

        @Override
        public String toString() {
            final StringBuilder sb = new StringBuilder();
            sb.append("f1 ");
            dumpList(f1, sb);
            sb.append("\nf2 ");
            dumpList(f2, sb);
            sb.append("\nf3 ");
            dumpList(f3, sb);
            sb.append("\nf4 ");
            dumpList(f4, sb);
            sb.append("\nf5 ");
            dumpList(f5, sb);
            sb.append("\nf6 ");
            dumpList(f6, sb);
            sb.append("\nf7 ");
            dumpList(f7, sb);
            sb.append("\nf8 ");
            dumpList(f8, sb);
            return sb.toString();
        }

        public void copy(MsgClassArrayPublic t) {
            f1 = t.f1;
            f2 = t.f2;
            f3 = t.f3;
            f4 = t.f4;
            f5 = t.f5;
            f6 = t.f6;
            f7 = t.f7;
            f8 = t.f8;
        }
    }

    @Test
    public void testArrayPrimitiveBoundComp() throws Exception {
        setUpComp();
        testArrayPrimitiveBoundPublic();
        testArrayPrimitiveBoundPrivate();
    }

    @Test
    public void testArrayPrimitiveBoundIntp() throws Exception {
        setUpIntp();
        testArrayPrimitiveBoundPublic();
        testArrayPrimitiveBoundPrivate();
    }

    private void testArrayPrimitiveBoundPublic() throws Exception {
        final RecordClassDescriptor rcd = (RecordClassDescriptor) Introspector.introspectSingleClass(MsgClassArrayPublic.class);

        final MsgClassArrayPublic msg = new MsgClassArrayPublic();
        MemoryDataOutput out = boundEncode(msg, rcd);
        MemoryDataInput in = new MemoryDataInput(out);
        MsgClassArrayPublic msg2 = new MsgClassArrayPublic();
        boundDecode(msg2, rcd, in);
        Assert.assertEquals("public 0", msg.toString(), msg2.toString());

        msg.f1 = new BooleanArrayList();
        msg.f2 = new CharacterArrayList();
        msg.f3 = new ByteArrayList();
        msg.f4 = new ShortArrayList();
        msg.f5 = new IntegerArrayList();
        msg.f6 = new LongArrayList();
        msg.f7 = new FloatArrayList();
        msg.f8 = new DoubleArrayList();
        out = boundEncode(msg, rcd);
        in = new MemoryDataInput(out);
        msg2 = new MsgClassArrayPublic();
        boundDecode(msg2, rcd, in);
        Assert.assertEquals("public 1", msg.toString(), msg2.toString());

        msg.f1.add(false);
        msg.f2.add('X');
        msg.f3.add((byte)0x55);
        msg.f4.add((short)3546);
        msg.f5.add(232343423);
        msg.f6.add(23423423423432423l);
        msg.f7.add(24.34f);
        msg.f8.add(124.678);
        out = boundEncode(msg, rcd);
        in = new MemoryDataInput(out);
        msg2 = new MsgClassArrayPublic();
        boundDecode(msg2, rcd, in);
        Assert.assertEquals("public 2", msg.toString(), msg2.toString());

        msg.f1.add(true);
        msg.f2.add('Z');
        msg.f3.add((byte)0xf7);
        msg.f4.add((short)-13506);
        msg.f5.add(-923234342);
        msg.f6.add(-923423423423432423l);
        msg.f7.add(-94.38f);
        msg.f8.add(-9124.678);
        out = boundEncode(msg, rcd);
        in = new MemoryDataInput(out);
        msg2 = new MsgClassArrayPublic();
        boundDecode(msg2, rcd, in);
        Assert.assertEquals("public 3", msg.toString(), msg2.toString());

        MsgClassArrayPublic msg_bak = new MsgClassArrayPublic();
        msg_bak.copy(msg);
        msg.f2 = null;
        msg.f4 = null;
        msg.f6 = null;
        msg.f8 = null;
        out = boundEncode(msg, rcd);
        final int pos0 = out.getPosition();

        MsgClassArrayPublic msg3 = new MsgClassArrayPublic();
        msg3.copy(msg_bak);
        msg3.f1 = null;
        msg3.f3 = null;
        msg3.f5 = null;
        msg3.f7 = null;
        boundEncode(msg3, TypeLoaderImpl.DEFAULT_INSTANCE,  rcd, out);

        in = new MemoryDataInput(out);
        msg2 = new MsgClassArrayPublic();

        // trick MemoryDataInput to limit available bytes
        final int avail = in.getAvail();
        final int pos = in.getCurrentOffset();
        in.setBytes(in.getBytes(), pos, pos0);
        boundDecode(msg2, rcd, in);
        Assert.assertEquals("public 4a", msg.toString(), msg2.toString());
        // recover backed up length
        in.setBytes(in.getBytes(), in.getCurrentOffset(), avail - (in.getCurrentOffset() - pos));

        MsgClassArrayPublic msg4 = new MsgClassArrayPublic();
        boundDecode(msg4, rcd, in);
        Assert.assertEquals("public 4b", msg3.toString(), msg4.toString());
    }

    public static class MsgClassArrayPrivate extends InstrumentMessage {

        private BooleanArrayList f1;
        private CharacterArrayList f2;
        private ByteArrayList f3;
        private ShortArrayList f4;
        private IntegerArrayList f5;
        private LongArrayList f6;
        private FloatArrayList f7;
        private DoubleArrayList f8;
        //private ObjectArrayList f9;

        @Override
        public String toString() {
            final StringBuilder sb = new StringBuilder();
            sb.append("f1 ");
            dumpList(f1, sb);
            sb.append("\nf2 ");
            dumpList(f2, sb);
            sb.append("\nf3 ");
            dumpList(f3, sb);
            sb.append("\nf4 ");
            dumpList(f4, sb);
            sb.append("\nf5 ");
            dumpList(f5, sb);
            sb.append("\nf6 ");
            dumpList(f6, sb);
            sb.append("\nf7 ");
            dumpList(f7, sb);
            sb.append("\nf8 ");
            dumpList(f8, sb);
            return sb.toString();
        }

        public void copy(MsgClassArrayPrivate t) {
            f1 = t.f1;
            f2 = t.f2;
            f3 = t.f3;
            f4 = t.f4;
            f5 = t.f5;
            f6 = t.f6;
            f7 = t.f7;
            f8 = t.f8;
        }


        @SchemaArrayType(
                isElementNullable = false,
                elementDataType = SchemaDataType.BOOLEAN
        )
        @SchemaElement
        public BooleanArrayList getF1 () {
            return f1;
        }

        public void setF1 (BooleanArrayList f1) {
            this.f1 = f1;
        }


        @SchemaElement
        public CharacterArrayList getF2 () {
            return f2;
        }

        public void setF2 (CharacterArrayList f2) {
            this.f2 = f2;
        }

        @SchemaArrayType(
                isElementNullable = false,
                elementDataType = SchemaDataType.INTEGER,
                elementEncoding = "INT8"
        )
        @SchemaElement
        public ByteArrayList getF3 () {
            return f3;
        }

        public void setF3 (ByteArrayList f3) {
            this.f3 = f3;
        }


        @SchemaElement
        public ShortArrayList getF4 () {
            return f4;
        }

        public void setF4 (ShortArrayList f4) {
            this.f4 = f4;
        }


        @SchemaElement
        public IntegerArrayList getF5 () {
            return f5;
        }

        public void setF5 (IntegerArrayList f5) {
            this.f5 = f5;
        }


        @SchemaElement
        public LongArrayList getF6 () {
            return f6;
        }

        public void setF6 (LongArrayList f6) {
            this.f6 = f6;
        }


        @SchemaElement
        public FloatArrayList getF7 () {
            return f7;
        }

        public void setF7 (FloatArrayList f7) {
            this.f7 = f7;
        }


        @SchemaElement
        public DoubleArrayList getF8 () {
            return f8;
        }

        public void setF8 (DoubleArrayList f8) {
            this.f8 = f8;
        }
    }

    private static void dumpList(AbstractList list, StringBuilder sb) {
        if (list == null)
            sb.append("<null>");
        else {
            sb.append(list.size()).append(": [");
            for (int i = 0; i < list.size(); i++) {
                sb.append(list.get(i)).append(',');
            }
            sb.append(']');
        }
    }

    private void testArrayPrimitiveBoundPrivate() throws Exception {
        final RecordClassDescriptor rcd = (RecordClassDescriptor) Introspector.introspectSingleClass(MsgClassArrayPrivate.class);

        final MsgClassArrayPrivate msg = new MsgClassArrayPrivate();
        MemoryDataOutput out = boundEncode(msg, rcd);
        MemoryDataInput in = new MemoryDataInput(out);
        MsgClassArrayPrivate msg2 = new MsgClassArrayPrivate();
        boundDecode(msg2, rcd, in);
        Assert.assertEquals("private 0", msg.toString(), msg2.toString());

        msg.f1 = new BooleanArrayList();
        msg.f2 = new CharacterArrayList();
        msg.f3 = new ByteArrayList();
        msg.f4 = new ShortArrayList();
        msg.f5 = new IntegerArrayList();
        msg.f6 = new LongArrayList();
        msg.f7 = new FloatArrayList();
        msg.f8 = new DoubleArrayList();
        out = boundEncode(msg, rcd);
        in = new MemoryDataInput(out);
        msg2 = new MsgClassArrayPrivate();
        boundDecode(msg2, rcd, in);
        Assert.assertEquals("private 1", msg.toString(), msg2.toString());

        msg.f1.add(false);
        msg.f2.add('X');
        msg.f3.add((byte)0x55);
        msg.f4.add((short)3546);
        msg.f5.add(232343423);
        msg.f6.add(23423423423432423l);
        msg.f7.add(24.34f);
        msg.f8.add(124.678);
        out = boundEncode(msg, rcd);
        in = new MemoryDataInput(out);
        msg2 = new MsgClassArrayPrivate();
        boundDecode(msg2, rcd, in);
        Assert.assertEquals("private 2", msg.toString(), msg2.toString());

        msg.f1.add(true);
        msg.f2.add('Z');
        msg.f3.add((byte)0xf7);
        msg.f4.add((short)-13506);
        msg.f5.add(-923234342);
        msg.f6.add(-923423423423432423l);
        msg.f7.add(-94.38f);
        msg.f8.add(-9124.678);
        out = boundEncode(msg, rcd);
        in = new MemoryDataInput(out);
        msg2 = new MsgClassArrayPrivate();
        boundDecode(msg2, rcd, in);
        Assert.assertEquals("private 3", msg.toString(), msg2.toString());

        MsgClassArrayPrivate msg_bak = new MsgClassArrayPrivate();
        msg_bak.copy(msg);
        msg.f2 = null;
        msg.f4 = null;
        msg.f6 = null;
        msg.f8 = null;
        out = boundEncode(msg, rcd);
        final int pos0 = out.getPosition();

        MsgClassArrayPrivate msg3 = new MsgClassArrayPrivate();
        msg3.copy(msg_bak);
        msg3.f1 = null;
        msg3.f3 = null;
        msg3.f5 = null;
        msg3.f7 = null;
        boundEncode(msg3, TypeLoaderImpl.DEFAULT_INSTANCE,  rcd, out);

        in = new MemoryDataInput(out);
        msg2 = new MsgClassArrayPrivate();

        // trick MemoryDataInput to limit available bytes
        final int avail = in.getAvail();
        final int pos = in.getCurrentOffset();
        in.setBytes(in.getBytes(), pos, pos0);
        boundDecode(msg2, rcd, in);
        Assert.assertEquals("private 4a", msg.toString(), msg2.toString());
        // recover backed up length
        in.setBytes(in.getBytes(), in.getCurrentOffset(), avail - (in.getCurrentOffset() - pos));

        MsgClassArrayPrivate msg4 = new MsgClassArrayPrivate();
        boundDecode(msg4, rcd, in);
        Assert.assertEquals("private 4b", msg3.toString(), msg4.toString());
    }

    @Test
    public void testArrayPrimitiveUnboundComp() throws Exception {
        setUpComp();
        testArrayPrimitiveUnbound();
    }

    @Test
    public void testArrayPrimitiveUnboundIntp() throws Exception {
        setUpIntp();
        testArrayPrimitiveUnbound();
    }

    private void testArrayPrimitiveUnbound() throws Exception {
        final RecordClassDescriptor rcd = getRCD(MsgClassArrayPublic.class);

        final MsgClassArrayPublic msg = new MsgClassArrayPublic();
        testArrayUnboundDecoding(msg, rcd);

        msg.f1 = new BooleanArrayList();
        msg.f2 = new CharacterArrayList();
        msg.f3 = new ByteArrayList();
        msg.f4 = new ShortArrayList();
        msg.f5 = new IntegerArrayList();
        msg.f6 = new LongArrayList();
        msg.f7 = new FloatArrayList();
        msg.f8 = new DoubleArrayList();
        testArrayUnboundDecoding(msg, rcd);

        msg.f1.add(false);
        msg.f2.add('X');
        msg.f3.add((byte)0x55);
        msg.f4.add((short)3546);
        msg.f5.add(232343423);
        msg.f6.add(23423423423432423l);
        msg.f7.add(24.34f);
        msg.f8.add(124.678);
        testArrayUnboundDecoding(msg, rcd);

        msg.f1.add(true);
        msg.f2.add('Z');
        msg.f3.add((byte)0xf7);
        msg.f4.add((short)-13506);
        msg.f5.add(-923234342);
        msg.f6.add(-923423423423432423l);
        msg.f7.add(-94.38f);
        msg.f8.add(-9124.678);
        testArrayUnboundDecoding(msg, rcd);

        msg.f1.add(false);
        msg.f2.add(CharDataType.NULL);
        msg.f3.add((byte)0x11);
        msg.f4.add((short)5555);
        msg.f5.add(444444444);
        msg.f6.add(3333333333333333333l);
        for (int i = 0; i < msg.f6.size(); i++)
            msg.f6.set(i, IntegerDataType.INT64_NULL);
        msg.f7.add(11.22f);
        msg.f7.set(0, Float.NaN);
        msg.f8.add(22.333);
        testArrayUnboundDecoding(msg, rcd);
        testArrayUnboundDecoding2(msg, rcd);

        // make boolean field nullable
        RecordClassDescriptor newRcd = replaceDataType(rcd, "f1", new ArrayDataType(true, new BooleanDataType(true)));

        // encoding
        testArrayUnboundEncoding2(msg, newRcd);
    }

    private void testArrayUnboundDecoding(MsgClassArrayPublic msg, RecordClassDescriptor rcd) throws Exception {
        final MemoryDataOutput out = boundEncode(msg, rcd);
        final MemoryDataInput in = new MemoryDataInput(out);
        List<Object> values2 = unboundDecode(in, rcd);

        final List<Object> values = new ArrayList<Object>();
        msg.populate(values);
        assertValuesEquals(values, values2, rcd);

        // encoding
        MemoryDataOutput out2 = unboundEncode(values, rcd);
        MemoryDataInput in2 = new MemoryDataInput(out2);
        MsgClassArrayPublic msg2 = (MsgClassArrayPublic) boundDecode(null, rcd, in2);
        Assert.assertEquals("encodig", msg.toString(), msg2.toString());
    }

    @SuppressWarnings("unchecked")
    private void testArrayUnboundDecoding2(MsgClassArrayPublic msg, RecordClassDescriptor rcd) throws Exception {
        final MemoryDataOutput out = boundEncode(msg, rcd);
        final MemoryDataInput in = new MemoryDataInput(out);
        final UnboundDecoder decoder = factory.createFixedUnboundDecoder(rcd);

        final List<Object> values2 = new ArrayList<Object>();

        decoder.beginRead(in);
        while(decoder.nextField()) {
            final int len = decoder.getArrayLength();

            DataType type = decoder.getField().getType();
            DataType underlineType = ((ArrayDataType) type).getElementDataType();
            final AbstractList a = (AbstractList) RecordLayout.getNativeType(type).newInstance();
            setArrayLength(a, len);
            values2.add(a);

            // read all elements
            for (int i = 0; i < len; i++) {
                ReadableValue rv = decoder.nextReadableElement();
                setArrayElement(underlineType, a, i, rv);
            }

            // check element reading after skip
            final AbstractList a2 = (AbstractList) RecordLayout.getNativeType(type).newInstance();
            setArrayLength(a2, len);
            for (int offset = 1; offset < len - 1; offset++) {
                // reset field decoder
                if (values2.size() == 1) {
                    decoder.nextField();
                    decoder.previousField();
                } else {
                    decoder.previousField();
                    decoder.nextField();
                }

                decoder.getArrayLength();
                // skip elements
                for (int i = 0; i < offset; i++)
                    decoder.nextReadableElement();

                for (int i = offset; i < len; i++) {
                    final ReadableValue rv = decoder.nextReadableElement();
                    setArrayElement(underlineType, a2, i, rv);

                    // compare read value with original
                    Assert.assertEquals("skip read " + i, a.get(i), a2.get(i));
                }
            }


            // check array boundary violation
            try {
                decoder.nextReadableElement();
                Assert.fail("boundary violation");
            } catch (NoSuchElementException e) {
                Assert.assertEquals("bad index", "array boundary exeeded", e.getMessage());
            }
        }

        final List<Object> values = new ArrayList<Object>();
        msg.populate(values);
        assertValuesEquals(values, values2, rcd);
    }

    private static final boolean[][] UNBOUND_NULLS = {
            {true, true, true},
            {true, true, false},
            {true, false, false},
            {true, false, false},
    };

    private void testArrayUnboundEncoding2(MsgClassArrayPublic msg, RecordClassDescriptor rcd) throws Exception {
        final List<Object> originalValues = new ArrayList<Object>();
        msg.populateUncasted(originalValues);

        final List<Object> values = new ArrayList<Object>();
        for (boolean[] unboundNull : UNBOUND_NULLS) {
            // set nulls
            values.clear();
            for (Object originalValue : originalValues) {
                @SuppressWarnings("unchecked")
                final List<Object> ov = (List<Object>) originalValue;
                final List<Object> v = new ArrayList<Object>();
                for (int i = 0; i < unboundNull.length; i++) {
                    v.add(unboundNull[i] ? null : ov.get(i));
                }
                values.add(v);
            }

            // encode
            MemoryDataOutput out = unboundEncode(values, rcd);
            MemoryDataInput in = new MemoryDataInput(out);
            List<Object>  values2 = unboundDecode(in, rcd);
            assertValuesEquals(values, values2, rcd);
        }
    }

    private static RecordClassDescriptor replaceDataType(RecordClassDescriptor rcd, String fieldName, DataType dataType) {
        DataField dataField = rcd.getField(fieldName);
        if (dataField == null)
            throw new IllegalArgumentException("Field is not found " + fieldName);

        final DataField[] oldFields = rcd.getFields();
        final DataField[] newFields = new DataField[oldFields.length];
        System.arraycopy(oldFields, 0, newFields, 0, oldFields.length);
        newFields[Util.indexOf(newFields, dataField)] = new NonStaticDataField(dataField.getName(), dataField.getTitle(), dataType);

        return new RecordClassDescriptor(rcd.getName(), rcd.getTitle(), rcd.isAbstract(), rcd.getParent(), newFields);
    }

    /*
    * ARRAY type: test non-natural encodings
    *  INTEGER: PUINT30, PINTERVAL, INT48, PUINT61
    *  TIMEOFDAY; TIMESTAMP; ENUM, BITMASK
    *  VARCHAR: ALPHANUMERIC (10)
    *  FLOAT: DECIMAL(2)
    */

    private static final RecordClassDescriptor cdMsgClassArrayOthers =
            new RecordClassDescriptor(
                    "MsgClassArrayOthers",
                    null,
                    false,
                    null,
                    new NonStaticDataField("f1", null, new ArrayDataType(true, new IntegerDataType(IntegerDataType.ENCODING_PUINT30, true))),
                    new NonStaticDataField("f2", null, new ArrayDataType(true, new IntegerDataType(IntegerDataType.ENCODING_PINTERVAL, true))),
                    new NonStaticDataField("f3", null, new ArrayDataType(true, new IntegerDataType(IntegerDataType.ENCODING_INT48, true))),
                    new NonStaticDataField("f4", null, new ArrayDataType(true, new IntegerDataType(IntegerDataType.ENCODING_PUINT61, true))),
                    new NonStaticDataField("f5", null, new ArrayDataType(true, new DateTimeDataType(true))),
                    new NonStaticDataField("f6", null, new ArrayDataType(true, new TimeOfDayDataType(true))),
                    new NonStaticDataField("f7", null, new ArrayDataType(true, new EnumDataType(true, Test_RecordCodecs4.ECD_MY_ENUM))),
                    new NonStaticDataField("f8", null, new ArrayDataType(true, new EnumDataType(true, Test_RecordCodecs4.ECD_MY_BITMASK))),
                    new NonStaticDataField("f9", null, new ArrayDataType(true, new VarcharDataType(VarcharDataType.getEncodingAlphanumeric(10), true, false))),
                    new NonStaticDataField("f10", null, new ArrayDataType(true, new FloatDataType(FloatDataType.getEncodingScaled(2), true)))
            );

    private static final Object[] ELEMENT_VALUES = {
            13234454,
            21343,
            4324343348L,
            234253464566546454L,
            1314899629573L, // 2011-09-01 17:53:49.573 GMT
            81597956, // TIMEOFDAY
            "YELLOW",
            "YELLOW|GREEN",
            "ABCDEFG890",
            2356.78
    };

    private static final Object[] ELEMENT_VALUES2 = {
            13234453,
            21342,
            4324343347L,
            234253464566546453L,
            1314899629572L, // 2011-09-01 17:53:49.572 GMT
            81597955, // TIMEOFDAY
            "RED",
            "YELLOW",
            "BCDEFG8901",
            2355.67
    };

    @Test
    public void testArrayOtherBindingsUnboundComp() throws Exception {
        setUpComp();
        testArrayOtherBindingsUnbound();
    }

    @Test
    public void testArrayOtherBindingsUnboundIntp() throws Exception {
        setUpIntp();
        testArrayOtherBindingsUnbound();
    }

    private void testArrayOtherBindingsUnbound() {
        // nulls
        final ArrayList<Object> values = new ArrayList<Object>();
        for (int i = 0; i < cdMsgClassArrayOthers.getFields().length; i++)
            values.add(null);

        testArrayUnbound(values);

        // empty arrays
        values.clear();
        for (int i = 0; i < cdMsgClassArrayOthers.getFields().length; i++)
            values.add(new ArrayList<Object>());
        testArrayUnbound(values);

        // arrays with a null element
        for (int i = 0; i < ELEMENT_VALUES.length; i++) {
            @SuppressWarnings("unchecked")
            final ArrayList<Object> a = (ArrayList<Object>)values.get(i);
            a.add(null);
        }
        testArrayUnbound(values);

        // arrays with one value element
        for (int i = 0; i < ELEMENT_VALUES.length; i++) {
            @SuppressWarnings("unchecked")
            final ArrayList<Object> a = (ArrayList<Object>)values.get(i);
            a.add(ELEMENT_VALUES[i]);
        }
        testArrayUnbound(values);

        // arrays with two value elements
        for (int i = 0; i < ELEMENT_VALUES2.length; i++) {
            @SuppressWarnings("unchecked")
            final ArrayList<Object> a = (ArrayList<Object>)values.get(i);
            a.add(ELEMENT_VALUES2[i]);
        }
        testArrayUnbound(values);
    }

    private void testArrayUnbound(ArrayList<Object> values) {
        testRcdUnbound(values, cdMsgClassArrayOthers);
    }

    /*
    * ARRAY type: test constraints violations (unbound encoder)
    */

    private static final RecordClassDescriptor cdMsgClassArrayConstrains =
            new RecordClassDescriptor(
                    MsgClassArrayConstrains.class.getName(),
                    null,
                    false,
                    null,
                    new NonStaticDataField("i1", null, new ArrayDataType(false, new IntegerDataType(IntegerDataType.ENCODING_INT8, false, -5, 5))),
                    new NonStaticDataField("i2", null, new ArrayDataType(false, new IntegerDataType(IntegerDataType.ENCODING_INT16, false, -5, 5))),
                    new NonStaticDataField("i3", null, new ArrayDataType(false, new IntegerDataType(IntegerDataType.ENCODING_INT32, false, -5, 5))),
                    new NonStaticDataField("i4", null, new ArrayDataType(false, new IntegerDataType(IntegerDataType.ENCODING_INT48, false, -5, 5))),
                    new NonStaticDataField("i5", null, new ArrayDataType(false, new IntegerDataType(IntegerDataType.ENCODING_INT64, false, -5, 5))),
                    new NonStaticDataField("i6", null, new ArrayDataType(false, new IntegerDataType(IntegerDataType.ENCODING_PUINT30, false, 0, 5))),
                    new NonStaticDataField("i7", null, new ArrayDataType(false, new IntegerDataType(IntegerDataType.ENCODING_PINTERVAL, false, 1, 5))),
                    new NonStaticDataField("i8", null, new ArrayDataType(false, new IntegerDataType(IntegerDataType.ENCODING_PUINT61, false, 0, 5))),
                    new NonStaticDataField("f10", null, new ArrayDataType(false, new FloatDataType(FloatDataType.ENCODING_FIXED_FLOAT, false, -5.0, 5.0))),
                    new NonStaticDataField("f11", null, new ArrayDataType(false, new FloatDataType(FloatDataType.ENCODING_FIXED_DOUBLE, false, -5.0, 5.0))),
                    new NonStaticDataField("f12", null, new ArrayDataType(false, new FloatDataType(FloatDataType.getEncodingScaled(2), false, -5.0, 5.0))),

                    new NonStaticDataField("f13", null, new ArrayDataType(false, new DateTimeDataType(false))),
                    new NonStaticDataField("f14", null, new ArrayDataType(false, new TimeOfDayDataType(false))),
                    new NonStaticDataField("f15", null, new ArrayDataType(false, new EnumDataType(false, Test_RecordCodecs4.ECD_MY_ENUM))),
                    new NonStaticDataField("f16", null, new ArrayDataType(false, new EnumDataType(false, Test_RecordCodecs4.ECD_MY_BITMASK))),
                    new NonStaticDataField("f17", null, new ArrayDataType(false, new VarcharDataType(VarcharDataType.getEncodingAlphanumeric(10), false, false)))
            );

    private static final Object[] ELEMENT_VALUES_GOOD = {
            (byte) 1,
            (short) 2,
            3,
            4L,
            5L,
            0,
            1,
            2L,
            3.10f,
            3.11,
            3.12,
            1314899629573L, // 2011-09-01 17:53:49.573 GMT
            81597956, // TIMEOFDAY
            "YELLOW",
            "GREEN",
            "ABCDEFG890"
    };

    private static final Object[] ELEMENT_VALUES_BAD = {
            (byte)21,
            (short)22,
            23,
            24L,
            25L,
            26,
            27,
            28L,
            20.10f,
            20.11,
            20.12,
            null,
            null, // -100, TODO: make validation on negative value ???
            "BAD",
            "BAD2",
            "bad",
    };

    private static final Object[] ELEMENT_VALUES_BAD_STR = {
            "21",
            "22",
            "23",
            "24",
            "25",
            "26",
            "27",
            "28",
            "20.10",
            "20.11",
            "20.12",
            "bad dateTime",
            null, // "bad time", TODO: make validation in TimeFormatter.parseTimeOfDay
            null,
            null,
            null,
    };

    private static final String[] VALUES_BAD_MSG = {
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            "Character 'b' (0x62) in 'bad' is out of range"
    };

    @Test
    public void testArrayConstraintUnboundIntp() throws Exception {
        setUpIntp();

        final ArrayList<Object> values = new ArrayList<Object>();
        final DataField[] fields = cdMsgClassArrayConstrains.getFields();

        for (int i = 0; i < fields.length; i++)
            values.add(null);

        // null-array
        for (int i = 0; i < fields.length; i++) {
            final String fieldName = fields[i].getName();
            try {
                unboundEncode(values, cdMsgClassArrayConstrains);
                Assert.fail("null-array " + fieldName);
            } catch (IllegalArgumentException e) {
                Assert.assertEquals("null-array " + fieldName, String.format("'%s' field is not nullable", fieldName), e.getMessage());
            }
            values.set(i, new ArrayList<Object>());
        }

        // null element
        for (int i = 0; i < fields.length; i++) {
            @SuppressWarnings("unchecked")
            final ArrayList<Object> a = (ArrayList<Object>) values.get(i);
            a.add(null);
            final String fieldName = fields[i].getName();
            try {
                unboundEncode(values, cdMsgClassArrayConstrains);
                Assert.fail("not-null constraint violated: " + fieldName);
            } catch (IllegalArgumentException e) {
                Assert.assertEquals("null element " + fieldName, String.format("'%s' field array element is not nullable", fieldName), e.getMessage());
            }

            a.set(0, ELEMENT_VALUES_GOOD[i]);
        }

        // bad element
        for (int i = 0; i < fields.length; i++) {
            if (ELEMENT_VALUES_BAD[i] == null)
                continue;

            @SuppressWarnings("unchecked")
            final ArrayList<Object> a = (ArrayList<Object>) values.get(i);
            a.set(0, ELEMENT_VALUES_BAD[i]);
            final String fieldName = fields[i].getName();
            try {
                unboundEncode(values, cdMsgClassArrayConstrains);
                Assert.fail("bad element " + fieldName);
            } catch (IllegalArgumentException e) {
                final String fieldDescription = cdMsgClassArrayConstrains.getName() + "." + fieldName;
                String expected = fieldDescription + " == " + String.valueOf(ELEMENT_VALUES_BAD[i]);

                expected = (VALUES_BAD_MSG[i] != null) ? VALUES_BAD_MSG[i] : expected;
                Assert.assertEquals("bad element " + fieldName, expected, e.getMessage());
            }

            a.set(0, ELEMENT_VALUES_GOOD[i]);
        }

        // bad element of String type
        for (int i = 0; i < fields.length; i++) {
            if (ELEMENT_VALUES_BAD_STR[i] == null)
                continue;

            @SuppressWarnings("unchecked")
            final ArrayList<Object> a = (ArrayList<Object>) values.get(i);
            a.set(0, ELEMENT_VALUES_BAD_STR[i]);
            final String fieldName = fields[i].getName();
            try {
                unboundEncode(values, cdMsgClassArrayConstrains);
                Assert.fail("bad element string " + fieldName);
            } catch (IllegalArgumentException e) {
                final String fieldDescription = cdMsgClassArrayConstrains.getName() + "." + fieldName;
                String expected = String.valueOf(ELEMENT_VALUES_BAD_STR[i]);
                if (((ArrayDataType) fields[i].getType()).getElementDataType() instanceof EnumDataType)
                    expected = fieldName + " == " + expected;
                else
                    expected = fieldDescription + " == " + expected;

                expected = (VALUES_BAD_MSG[i] != null) ? VALUES_BAD_MSG[i] : expected;
                Assert.assertEquals("bad element string " + fieldName, expected, e.getMessage());
            }

            a.set(0, ELEMENT_VALUES_GOOD[i]);
        }
    }

    /*
    * ARRAY type: test constraints violations (bound encoder)
    */

    public static class MsgClassArrayConstrains extends InstrumentMessage {
        public ByteArrayList i1;
        public ShortArrayList i2;
        public IntegerArrayList i3;
        public LongArrayList i4;
        public LongArrayList i5;
        public IntegerArrayList i6;
        public IntegerArrayList i7;
        public LongArrayList i8;

        public FloatArrayList f10;
        public DoubleArrayList f11;
        public DoubleArrayList f12;

        public LongArrayList f13;
        public IntegerArrayList f14;
        public ByteArrayList f15;
        public ByteArrayList f16;
        public LongArrayList f17;

        public String toString() {
            return i1 + "," + i2 + "," + i3 + "," + i4 + "," + i5 + "," + i6 + "," + i7 + "," + i8 + "," +
                    f10 + "," + f11 + "," + f12 + "," +
                    f13 + f14 + "," + f15 + "," + f16 + "," + f17;
        }
    }

    private static final Object[] ELEMENT_VALUES_NULLS = {
            IntegerDataType.INT8_NULL,
            IntegerDataType.INT16_NULL,
            IntegerDataType.INT32_NULL,
            IntegerDataType.INT48_NULL,
            IntegerDataType.INT64_NULL,
            IntegerDataType.PUINT30_NULL,
            IntegerDataType.PINTERVAL_NULL,
            IntegerDataType.PUINT61_NULL,
            Float.NaN,
            Double.NaN,
            Double.NaN,
            DateTimeDataType.NULL,
            TimeOfDayDataType.NULL,
            null,
            null,
            null
    };

    @Test
    public void testArrayConstraintBoundComp() throws Exception {
        setUpComp();
        testArrayConstraintBound();
    }

    @Test
    public void testArrayConstraintBoundIntp() throws Exception {
        setUpIntp();
        testArrayConstraintBound();
    }

    private void testArrayConstraintBound() throws Exception {
        final MsgClassArrayConstrains msg = new MsgClassArrayConstrains();

        final Field[] fields = MsgClassArrayConstrains.class.getDeclaredFields();

        // null-array
        for (Field field : fields) {
            final String fieldName = field.getName();

            try {
                boundEncode(msg, cdMsgClassArrayConstrains);
                Assert.fail("null-array " + fieldName);
            } catch (IllegalArgumentException e) {
                Assert.assertEquals("null-array " + fieldName, String.format("'%s' field is not nullable", fieldName), e.getMessage());
            }

            field.set(msg, field.getType().newInstance());
        }

        // null element
        for (int i = 0; i < fields.length; i++) {
            final Field field = fields[i];
            final String fieldName = field.getName();
            @SuppressWarnings("unchecked")
            final AbstractList<Object> a = (AbstractList<Object>) field.get(msg);
            a.add(translate(i, ELEMENT_VALUES_NULLS[i]));

            try {
                boundEncode(msg, cdMsgClassArrayConstrains);
                Assert.fail("null element " + fieldName);
            } catch (IllegalArgumentException e) {
                //Assert.assertEquals("null element " + fieldName, String.format("'%s' field array element is not nullable", fieldName), e.getMessage());
            }

            a.set(0, translate(i, ELEMENT_VALUES_GOOD[i]));
        }

        // bad element
        for (int i = 0; i < fields.length; i++) {
            if (ELEMENT_VALUES_BAD[i] == null)
                continue;

            final Field field = fields[i];
            final String fieldName = field.getName();

            // TODO: remove it after fixing validation on Alphanumeric
            if(fieldName.equals("f17"))
                continue;

            @SuppressWarnings("unchecked")
            final AbstractList<Object> a = (AbstractList<Object>) field.get(msg);
            a.set(0, translate(i, ELEMENT_VALUES_BAD[i]));

            try {
                boundEncode(msg, cdMsgClassArrayConstrains);
                Assert.fail("bad element " + fieldName);
            } catch (IllegalArgumentException e) {
                // compiled and interpreted encoders report byte value differently (-6 vs. 250)
                final String fieldDescription = cdMsgClassArrayConstrains.getName() + "." + fieldName;
                final String expected = fieldDescription + " == " +
                        ((VALUES_BAD_MSG[i] != null) ? VALUES_BAD_MSG[i] : translate(i, ELEMENT_VALUES_BAD[i]));
                final String expected2 = fieldDescription + " == " +
                        ((VALUES_BAD_MSG[i] != null) ? VALUES_BAD_MSG[i] : translateToString(i, ELEMENT_VALUES_BAD[i]));
                if (!expected.equals(e.getMessage()))
                    Assert.assertEquals("bad element " + fieldName, expected2, e.getMessage());
            }

            a.set(0, translate(i, ELEMENT_VALUES_GOOD[i]));
        }
    }

    private static Object translate(int idx, Object value) {
        final ArrayDataType adt = (ArrayDataType) cdMsgClassArrayConstrains.getFields()[idx].getType();
        final DataType dt = adt.getElementDataType();
        if (dt instanceof EnumDataType) {
            try {
                final long lv = value != null ? ((EnumDataType) dt).descriptor.stringToLong((CharSequence) value) : -1;
                return (byte) lv;
            } catch (NumberFormatException e) {
                return (byte) 250;
            }
        } else if (dt instanceof VarcharDataType && ((VarcharDataType) dt).getEncodingType() == VarcharDataType.ALPHANUMERIC)
            return ExchangeCodec.codeToLong((CharSequence) value);
        else
            return value;
    }

    private static String translateToString(int idx, Object value) {
        Object v = translate(idx, value);
        return String.valueOf(v);
    }

   /*
    * ARRAY OF OBJECT fixed
    */

    @Test
    public void testArrayOfObjectBoundComp() throws Exception {
        setUpComp();
        testArrayOfObjectBound();
    }

    @Test
    public void testArrayOfObjectBoundIntp() throws Exception {
        setUpIntp();
        testArrayOfObjectBound();
    }

    public static class MsgClassArrayOfFixedObject extends InstrumentMessage {
        public String s1;

        @SchemaArrayType(
                isNullable = false,
                isElementNullable = false
        )
        public ObjectArrayList<MsgClassAllPublic> a2;

        public ObjectArrayList<MsgClassAllPublic> a3;

        @SchemaArrayType(
                isNullable = false
//                ,nestedTypes = {MsgClassAllPublic.class}
        )
        public ObjectArrayList<MsgClassAllPrivate> a4;

        public ObjectArrayList<MsgClassAllPrivate> a5;

        @Override
        public String toString() {
            return String.valueOf(s1) + ',' +
                    String.valueOf(a2) + ',' + String.valueOf(a3) + ',' +
                    String.valueOf(a4) + ',' + String.valueOf(a5);
        }

        void setNulls() {
            s1 = null;
            a2 = new ObjectArrayList<>();
            a3 = null;
            a4 = new ObjectArrayList<>();
            a5 = null;
        }

        void setValues(boolean isOjectFiledNull, boolean isElementNull, int len) {
            s1 = "Hi Marco!!!";

            a2 = new ObjectArrayList<>();
            for (int i = 0; i < len; i++) {
                final MsgClassAllPublic v = new MsgClassAllPublic();
                a2.add(v);
                if (isOjectFiledNull)
                    v.setNulls();
                else
                    v.setValues();
            }
            a3 = new ObjectArrayList<>();
            for (int i = 0; i < len; i++) {
                final MsgClassAllPublic v = new MsgClassAllPublic();
                if (isElementNull && i % 2 == 0)
                    a3.add(null);
                else {
                    a3.add(v);
                    if (isOjectFiledNull)
                        v.setNulls();
                    else
                        v.setValues();
                }
            }

            a4 = new ObjectArrayList<>();
            for (int i = 0; i < len; i++) {
                final MsgClassAllPrivate v = new MsgClassAllPrivate();
                a4.add(v);
                if (isOjectFiledNull)
                    v.setNulls();
                else
                    v.setValues();
            }
            a5 = new ObjectArrayList<>();
            for (int i = 0; i < len; i++) {
                final MsgClassAllPrivate v = new MsgClassAllPrivate();
                if (isElementNull && i % 2 == 0)
                    a5.add(null);
                else {
                    a5.add(v);
                    if (isOjectFiledNull)
                        v.setNulls();
                    else
                        v.setValues();
                }
            }
        }
    }

    public static class MsgClassArrayOfFixedObjectPrivate extends InstrumentMessage {
        String s1;
        ObjectArrayList<MsgClassAllPublic> a2;
        ObjectArrayList<MsgClassAllPublic> a3;
        ObjectArrayList<MsgClassAllPrivate> a4;
        ObjectArrayList<MsgClassAllPrivate> a5;

        @Override
        public String toString() {
            return String.valueOf(s1) + ',' +
                    String.valueOf(a2) + ',' + String.valueOf(a3) + ',' +
                    String.valueOf(a4) + ',' + String.valueOf(a5);
        }

        void setNulls() {
            s1 = null;
            a2 = new ObjectArrayList<>();
            a3 = null;
            a4 = new ObjectArrayList<>();
            a5 = null;
        }

        void setValues(boolean isOjectFiledNull, boolean isElementNull, int len) {
            s1 = "Hi Marco!!!";

            a2 = new ObjectArrayList<>();
            for (int i = 0; i < len; i++) {
                final MsgClassAllPublic v = new MsgClassAllPublic();
                a2.add(v);
                if (isOjectFiledNull)
                    v.setNulls();
                else
                    v.setValues();
            }
            a3 = new ObjectArrayList<>();
            for (int i = 0; i < len; i++) {
                final MsgClassAllPublic v = new MsgClassAllPublic();
                if (isElementNull && i % 2 == 0)
                    a3.add(null);
                else {
                    a3.add(v);
                    if (isOjectFiledNull)
                        v.setNulls();
                    else
                        v.setValues();
                }
            }

            a4 = new ObjectArrayList<>();
            for (int i = 0; i < len; i++) {
                final MsgClassAllPrivate v = new MsgClassAllPrivate();
                a4.add(v);
                if (isOjectFiledNull)
                    v.setNulls();
                else
                    v.setValues();
            }
            a5 = new ObjectArrayList<>();
            for (int i = 0; i < len; i++) {
                final MsgClassAllPrivate v = new MsgClassAllPrivate();
                if (isElementNull && i % 2 == 0)
                    a5.add(null);
                else {
                    a5.add(v);
                    if (isOjectFiledNull)
                        v.setNulls();
                    else
                        v.setValues();
                }
            }
        }

        @SchemaElement
        public String getS1 () {
            return s1;
        }

        public void setS1 (String s1) {
            this.s1 = s1;
        }


        @SchemaElement
        @SchemaArrayType(
                isNullable = false,
                isElementNullable = false
        )
        public ObjectArrayList<MsgClassAllPublic> getA2 () {
            return a2;
        }

        public void setA2 (ObjectArrayList<MsgClassAllPublic> a2) {
            this.a2 = a2;
        }

        @SchemaElement
        public ObjectArrayList<MsgClassAllPublic> getA3 () {
            return a3;
        }

        public void setA3 (ObjectArrayList<MsgClassAllPublic> a3) {
            this.a3 = a3;
        }

        @SchemaElement
        @SchemaArrayType(isNullable = false)
        public ObjectArrayList<MsgClassAllPrivate> getA4 () {
            return a4;
        }

        public void setA4 (ObjectArrayList<MsgClassAllPrivate> a4) {
            this.a4 = a4;
        }

        @SchemaElement
        public ObjectArrayList<MsgClassAllPrivate> getA5 () {
            return a5;
        }

        public void setA5 (ObjectArrayList<MsgClassAllPrivate> a5) {
            this.a5 = a5;
        }
    }

    private void testArrayOfObjectBound() throws Exception {
        // public case
        {
            final RecordClassDescriptor rcd = getRCD(MsgClassArrayOfFixedObject.class);
            final MsgClassArrayOfFixedObject msg = new MsgClassArrayOfFixedObject();

            msg.setNulls();
            testRcdBound(rcd, msg, new MsgClassArrayOfFixedObject());
            testRcdBound(rcd, msg, null);

            msg.setValues(true, false, 1);
            testRcdBound(rcd, msg, new MsgClassArrayOfFixedObject());
            testRcdBound(rcd, msg, null);

            msg.setValues(true, true, 1);
            testRcdBound(rcd, msg, new MsgClassArrayOfFixedObject());
            testRcdBound(rcd, msg, null);

            msg.setValues(false, false, 1);
            testRcdBound(rcd, msg, new MsgClassArrayOfFixedObject());
            testRcdBound(rcd, msg, null);

            msg.setValues(false, true, 1);
            testRcdBound(rcd, msg, new MsgClassArrayOfFixedObject());
            testRcdBound(rcd, msg, null);

            msg.setValues(true, false, 3);
            testRcdBound(rcd, msg, new MsgClassArrayOfFixedObject());
            testRcdBound(rcd, msg, null);

            msg.setValues(true, true, 3);
            testRcdBound(rcd, msg, new MsgClassArrayOfFixedObject());
            testRcdBound(rcd, msg, null);

            msg.setValues(false, false, 3);
            testRcdBound(rcd, msg, new MsgClassArrayOfFixedObject());
            testRcdBound(rcd, msg, null);

            msg.setValues(false, true, 3);
            testRcdBound(rcd, msg, new MsgClassArrayOfFixedObject());
            testRcdBound(rcd, msg, null);

            msg.setNulls();
            testRcdBound(rcd, msg, new MsgClassArrayOfFixedObject());
            testRcdBound(rcd, msg, null);

            // check for null-field and null-element exceptions
            try {
                msg.a2 = null;
                testRcdBound(rcd, msg, null);
                Assert.fail("array NOT NULLABLE");
            } catch (IllegalArgumentException e) {
                assertEquals("array NOT NULLABLE", "'a2' field is not nullable", e.getMessage());
            }
            try {
                msg.setNulls();
                msg.a2.add(null);
                testRcdBound(rcd, msg, null);
                Assert.fail("array element NOT NULLABLE");
            } catch (IllegalArgumentException e) {
                //assertEquals("array element NOT NULLABLE", "'a2' field array element is not nullable", e.getMessage());
            }
        }

        // private case
        {
            final RecordClassDescriptor rcd = getRCD(MsgClassArrayOfFixedObjectPrivate.class);
            final MsgClassArrayOfFixedObjectPrivate msg = new MsgClassArrayOfFixedObjectPrivate();

            msg.setNulls();
            testRcdBound(rcd, msg, new MsgClassArrayOfFixedObjectPrivate());
            testRcdBound(rcd, msg, null);

            msg.setValues(true, false, 1);
            testRcdBound(rcd, msg, new MsgClassArrayOfFixedObjectPrivate());
            testRcdBound(rcd, msg, null);

            msg.setValues(true, true, 1);
            testRcdBound(rcd, msg, new MsgClassArrayOfFixedObjectPrivate());
            testRcdBound(rcd, msg, null);

            msg.setValues(false, false, 1);
            testRcdBound(rcd, msg, new MsgClassArrayOfFixedObjectPrivate());
            testRcdBound(rcd, msg, null);

            msg.setValues(false, true, 1);
            testRcdBound(rcd, msg, new MsgClassArrayOfFixedObjectPrivate());
            testRcdBound(rcd, msg, null);

            msg.setValues(true, false, 3);
            testRcdBound(rcd, msg, new MsgClassArrayOfFixedObjectPrivate());
            testRcdBound(rcd, msg, null);

            msg.setValues(true, true, 3);
            testRcdBound(rcd, msg, new MsgClassArrayOfFixedObjectPrivate());
            testRcdBound(rcd, msg, null);

            msg.setValues(false, false, 3);
            testRcdBound(rcd, msg, new MsgClassArrayOfFixedObjectPrivate());
            testRcdBound(rcd, msg, null);

            msg.setValues(false, true, 3);
            testRcdBound(rcd, msg, new MsgClassArrayOfFixedObjectPrivate());
            testRcdBound(rcd, msg, null);

            msg.setNulls();
            testRcdBound(rcd, msg, new MsgClassArrayOfFixedObjectPrivate());
            testRcdBound(rcd, msg, null);

            // check for null-field and null-element exceptions
            try {
                msg.a2 = null;
                testRcdBound(rcd, msg, null);
                Assert.fail("array NOT NULLABLE");
            } catch (IllegalArgumentException e) {
                assertEquals("array NOT NULLABLE", "'a2' field is not nullable", e.getMessage());
            }
            try {
                msg.setNulls();
                msg.a2.add(null);
                testRcdBound(rcd, msg, null);
                Assert.fail("array element NOT NULLABLE");
            } catch (IllegalArgumentException e) {
                //assertEquals("array element NOT NULLABLE", "'a2' field array element is not nullable", e.getMessage());
            }
        }
    }

    @Test
    public void testArrayOfObjectUnboundIntp() throws Exception {
        setUpIntp();
        testArrayOfObjectMultileReadsUnbound();
    }

    @Test
    public void testArrayOfObjectUnboundIntp1() throws Exception {
        setUpIntp();
        testArrayOfObjectUnbound();
    }

    private void testArrayOfObjectUnbound() throws Exception {
        final RecordClassDescriptor rcd = getRCD(MsgClassArrayOfFixedObject.class);
        final UnboundDecoder udec = factory.createFixedUnboundDecoder(rcd);
        final List<Object> values = new ArrayList<>(5);
        final List<Object> a2 = new ArrayList<>(5);
        final List<Object> a4 = new ArrayList<>(5);

        // Nulls
        for (int i = 0; i < 5; i++) {
            values.add(null);
        }

        // a2 and a4 are NOT NULLABLE
        values.set(0, a2);
        values.set(2, a4);

        MemoryDataOutput out = unboundEncode(values, rcd);
        MemoryDataInput in = new MemoryDataInput(out);
        List<Object> values2 = unboundDecode(in, udec);
        assertValuesEquals(values, values2, rcd);

        // not Nulls
        values.clear();

        values.add(a2);
        final List<Object> a2Elem1 = new ArrayList<>(5);
        MsgClassAllPublic.setValues(a2Elem1);
        a2.add(a2Elem1);
        final List<Object> a2Elem2 = new ArrayList<>(5);
        MsgClassAllPublic.setNullValues(a2Elem2);
        a2.add(a2Elem2);
        values.add(a2); // a3, a4, a5 are the same as a2
        values.add(a2);
        values.add(a2);
        values.add("Hi Kolia !!!");
        out = unboundEncode(values, rcd);
        in = new MemoryDataInput(out);
        values2 = unboundDecode(in, udec);
        assertValuesEquals(values, values2, rcd);
    }

    private void testArrayOfObjectMultileReadsUnbound() throws Exception {
        final RecordClassDescriptor rcd = getRCD(MsgClassArrayOfFixedObject.class);
        final UnboundDecoder udec = factory.createFixedUnboundDecoder(rcd);

        final List<Object> values = new ArrayList<>(5);
        final List<Object> a2 = new ArrayList<>(5);
        values.add(a2);
        final List<Object> a2Elem1 = new ArrayList<>(5);
        MsgClassAllPublic.setValues(a2Elem1);

        a2.add(a2Elem1);
        final List<Object> a2Elem2 = new ArrayList<>(5);
        MsgClassAllPublic.setNullValues(a2Elem2);
        a2.add(a2Elem2);
        values.add(a2); // a3, a4, a5 are the same as a2
        values.add(a2);
        values.add(a2);
        values.add("Hi Kolia !!!");

        // multiple reads of the same ARRAY OF OBJECT field
        MemoryDataOutput out = unboundEncode(values, rcd);
        MemoryDataInput in = new MemoryDataInput(out);
        udec.beginRead(in);
        udec.nextField(); // s1
        udec.nextField(); // a2
        udec.isNull();
        //System.out.println(udec.getArrayLength());
        udec.getArrayLength();
        ReadableValue rv = udec.nextReadableElement();
        rv.isNull();
        rv.isNull();
        UnboundDecoder elementDecoder = rv.getFieldDecoder();
        elementDecoder.nextField();
        elementDecoder.isNull();
        //System.out.println(elementDecoder.getString());
        elementDecoder.nextField();
        elementDecoder.isNull();
        //System.out.println(elementDecoder.getString());

        elementDecoder = rv.getFieldDecoder();
        elementDecoder.nextField();
        elementDecoder.isNull();
        //System.out.println(elementDecoder.getString());
        elementDecoder.nextField();
        elementDecoder.isNull();
        //System.out.println(elementDecoder.getString());

        rv = udec.nextReadableElement();
        rv.isNull();
        elementDecoder = rv.getFieldDecoder();
        elementDecoder.nextField();
        //System.out.println(elementDecoder.isNull());

        elementDecoder = rv.getFieldDecoder();
        elementDecoder.nextField();
        elementDecoder.isNull();
        //System.out.println(elementDecoder.isNull());
        //System.out.println(elementDecoder.getString());
    }

    /*
    * ARRAY OF OBJECT poly
    */

    public static abstract class OrderType {

        public abstract String getName();
    }

    public static final class MarketOrderType extends OrderType {

        public static final String NAME = "MKT";
        public MarketOrderType () {}

        @Override
        public String getName() {
            return NAME;
        }

        public String toString () {
            return "MARKET";
        }
    }

    public static class LimitOrder extends OrderType {
        public double       price;
        public double       discretionAmount;
        public CharSequence comment;

        public LimitOrder() {
        }

        public LimitOrder(double price, double discretionAmount, CharSequence comment) {
            this.price = price;
            this.discretionAmount = discretionAmount;
            this.comment = comment;
        }

        @Override
        public String getName() {
            throw new UnsupportedOperationException();
        }

        @Override
        public String toString () {
            return "LIMIT " + price + "," + discretionAmount + "," + comment;
        }
    }

    public static class StopOrder extends OrderType {
        public double stopPrice;

        public StopOrder() {
        }

        public StopOrder(double stopPrice) {
            this.stopPrice = stopPrice;
        }

        @Override
        public String getName() {
            throw new UnsupportedOperationException();
        }

        public String toString() {
            return "STOP " + stopPrice;
        }

    }

    public static class MsgPolyArray extends InstrumentMessage {

        public MsgPolyArray() {
        }

        public CharSequence requestId;

        @SchemaArrayType(
                elementTypes = {MarketOrderType.class, LimitOrder.class, StopOrder.class}
        )
        public ObjectArrayList<OrderType> poly;

        @Override
        public String toString() {
            final StringBuilder sb = new StringBuilder();
            sb.append(requestId).append(",poly: [");
            if(poly != null)
                for (OrderType orderType : poly) {
                    sb.append("[");
                    sb.append(orderType);
                    sb.append("],");
                }
            sb.append(']');

            return sb.toString();
        }
    }

    @Test
    public void testArrayOfObjectPolyBoundComp() throws Exception {
        setUpComp();
        testArrayOfObjectPolyBound();
    }

    @Test
    public void testArrayOfObjectPolyBoundIntp() throws Exception {
        setUpIntp();
        testArrayOfObjectPolyBound();
    }

    private void testArrayOfObjectPolyBound() throws Exception {
        RecordClassDescriptor rcd = (RecordClassDescriptor) Introspector.introspectSingleClass(MsgPolyArray.class);
        MsgPolyArray msg = new MsgPolyArray();

        // Nulls
        testRcdBound(rcd, msg, new MsgPolyArray());
        testRcdBound(rcd, msg, null);

        // Null elements
        msg.requestId = "req2";
        msg.poly = new ObjectArrayList<>();
        for (int i = 0; i < 5; i++) {
            msg.poly.add(null);
        }
        testRcdBound(rcd, msg, new MsgPolyArray());
        testRcdBound(rcd, msg, null);

        // not Nulls
        msg.requestId = "req2";
        msg.poly.clear();
        msg.poly.add(new MarketOrderType());
        msg.poly.add(new LimitOrder(500.05, 0.05, "1"));
        msg.poly.add(new StopOrder(510.05));
        msg.poly.add(new LimitOrder(503.05, 3.05, "2"));
        testRcdBound(rcd, msg, new MsgPolyArray());
        testRcdBound(rcd, msg, null);
    }

    private static final int NUM_OF_ELEMENTS = 5;
    private static final Object UNBOUND_ORDER_DATA[][] = {
            {0},
            {1, "1", 0.05, 500.05},
            {2, 510.05},
            {1, "2", 3.05, 503.05}
    };

    @Test
    public void testArrayOfObjectPolyUnboundIntp() throws Exception {
        setUpIntp();

        RecordClassDescriptor rcd = (RecordClassDescriptor) Introspector.introspectSingleClass(MsgPolyArray.class);
        RecordClassDescriptor[] rcdPoly = ((ClassDataType)((ArrayDataType) rcd.getField("poly").getType()).getElementDataType()).getDescriptors();
        FixedUnboundEncoder encoder = factory.createFixedUnboundEncoder(rcd);
        UnboundDecoder decoder = factory.createFixedUnboundDecoder(rcd);
        MemoryDataOutput out = new MemoryDataOutput();

        // Nulls
        encoder.beginWrite(out);
        encoder.nextField(); // requestId
        encoder.nextField(); // poly

        MemoryDataInput in = new MemoryDataInput(out);
        List<Object> values2 = unboundDecode(in, decoder);
        List<Object> values = new ArrayList<>();
        values.add(null);
        values.add(null);
        assertValuesEquals(values, values2, rcd);

        // Null elements
        values.clear();
        ArrayList<Object> poly = new ArrayList<>();
        values.add(poly);
        for (int i = 0; i < NUM_OF_ELEMENTS; i++)
            poly.add(null);
        values.add("req1");

        out.reset();
        encoder.beginWrite(out);

        out = unboundEncode(values, rcd);
        in = new MemoryDataInput(out);
        values2 = unboundDecode(in, decoder);
        assertValuesEquals(values, values2, rcd);

        // Not Nulls
        values.clear();
        poly = new ArrayList<>();
        values.add(poly);
        for (int i = 0; i < NUM_OF_ELEMENTS; i++) {
            Object[] data = UNBOUND_ORDER_DATA[i % UNBOUND_ORDER_DATA.length];

            ArrayList<Object> objFieldValues = new ArrayList<>();
            objFieldValues.add(rcdPoly[(Integer) data[0]]);

            for (int j = 1; j < data.length; j++)
                objFieldValues.add(data[j]);

            poly.add(objFieldValues);
        }
        values.add("req1");

        out.reset();
        encoder.beginWrite(out);

        out = unboundEncode(values, rcd);
        in = new MemoryDataInput(out);
        values2 = unboundDecode(in, decoder);
        assertValuesEquals(values, values2, rcd);
    }

    /*
     * violation of NOT-NULLABLE restriction at field and subfield/element level
     */

    public static class AllCompoundFields extends InstrumentMessage {
        @SchemaType(
                isNullable = false,
                dataType = SchemaDataType.OBJECT,
                nestedTypes = {MsgClassAllPublic.class}
        )
        public MsgClassAllPublic oPub1;

//        @SchemaType(
//                isNullable = false,
//                dataType = SchemaDataType.OBJECT,
//                nestedTypes = {MsgClassAllPublic.class}
//        )
        private MsgClassAllPublic oPub2;

        @SchemaType(
                isNullable = false,
                dataType = SchemaDataType.OBJECT,
                nestedTypes = {MsgClassAllPrivate.class}
        )
        public MsgClassAllPrivate oPri3;

//        @SchemaType(
//                isNullable = false,
//                dataType = SchemaDataType.OBJECT,
//                nestedTypes = {MsgClassAllPrivate.class}
//        )
        private MsgClassAllPrivate oPri4;

        @SchemaArrayType(
                elementDataType = SchemaDataType.INTEGER,
                elementEncoding = "INT8",
                isNullable = false,
                isElementNullable = false
        )
        public ByteArrayList aByte5;

//        @SchemaArrayType(
//                elementDataType = SchemaDataType.INTEGER,
//                elementEncoding = "INT8",
//                isNullable = false,
//                isElementNullable = false
//        )
        private ByteArrayList aByte6;

        @SchemaArrayType(
                isNullable = false
        )
        public ObjectArrayList<MsgClassAllPublic> aObj7;

        private ObjectArrayList<MsgClassAllPublic> aObj8;

        @SchemaArrayType(
                isNullable = false, isElementNullable = false
        )
        public ObjectArrayList<MsgClassAllPrivate> aObj9;

//        @SchemaArrayType(
//                isNullable = false
//        )
        private ObjectArrayList<MsgClassAllPrivate> aObj10;

        @SchemaArrayType(
                isNullable = false,
                isElementNullable = false,
                elementDataType = SchemaDataType.OBJECT,
                elementTypes = {ClassAAA.class, ClassAAB.class, ClassAAAA.class}
        )
        public ObjectArrayList<InterfaceA> aPoly11;

//        @SchemaArrayType(
//                isNullable = false,
//                isElementNullable = false,
//                elementDataType = SchemaDataType.OBJECT,
//                elementTypes = {Test_RecordCodecs7.ClassAAA.class, Test_RecordCodecs7.ClassAAB.class, Test_RecordCodecs7.ClassAAAA.class}
//        )
        private ObjectArrayList<InterfaceA> aPoly12;

        void setValues() {
            oPub1 = new MsgClassAllPublic();
            oPub2 = new MsgClassAllPublic();
            oPri3 = new MsgClassAllPrivate();
            oPri4 = new MsgClassAllPrivate();
            aByte5 = new ByteArrayList();
            aByte6 = new ByteArrayList();
            aObj7 = new ObjectArrayList<>();
            aObj8 = new ObjectArrayList<>();
            aObj9 = new ObjectArrayList<>();
            aObj10 = new ObjectArrayList<>();
            aPoly11 = new ObjectArrayList<>();
            aPoly12 = new ObjectArrayList<>();
        }

        @SchemaElement
        @SchemaType(
                isNullable = false,
                dataType = SchemaDataType.OBJECT,
                nestedTypes = {MsgClassAllPublic.class}
        )
        public MsgClassAllPublic getoPub2 () {
            return oPub2;
        }

        public void setoPub2 (MsgClassAllPublic oPub2) {
            this.oPub2 = oPub2;
        }


        @SchemaElement
        @SchemaType(
                isNullable = false,
                dataType = SchemaDataType.OBJECT,
                nestedTypes = {MsgClassAllPrivate.class}
        )
        public MsgClassAllPrivate getoPri4 () {
            return oPri4;
        }

        public void setoPri4 (MsgClassAllPrivate oPri4) {
            this.oPri4 = oPri4;
        }


        @SchemaElement
        @SchemaArrayType(
                elementDataType = SchemaDataType.INTEGER,
                elementEncoding = "INT8",
                isNullable = false,
                isElementNullable = false
        )
        public ByteArrayList getaByte6 () {
            return aByte6;
        }

        public void setaByte6 (ByteArrayList aByte6) {
            this.aByte6 = aByte6;
        }

        @SchemaElement
        @SchemaArrayType(
                isNullable = false
        )
        public ObjectArrayList<MsgClassAllPublic> getaObj8 () {
            return aObj8;
        }

        public void setaObj8 (ObjectArrayList<MsgClassAllPublic> aObj8) {
            this.aObj8 = aObj8;
        }


        @SchemaElement
        @SchemaArrayType(
                isNullable = false
        )
        public ObjectArrayList<MsgClassAllPrivate> getaObj10 () {
            return aObj10;
        }

        public void setaObj10 (ObjectArrayList<MsgClassAllPrivate> aObj10) {
            this.aObj10 = aObj10;
        }


        @SchemaElement
        @SchemaArrayType(
                isNullable = false,
                isElementNullable = false,
                elementDataType = SchemaDataType.OBJECT,
                elementTypes = {ClassAAA.class, ClassAAB.class, ClassAAAA.class}
        )
        public ObjectArrayList<InterfaceA> getaPoly12 () {
            return aPoly12;
        }

        public void setaPoly12 (ObjectArrayList<InterfaceA> aPoly12) {
            this.aPoly12 = aPoly12;
        }
    }

    @Test
    public void testNotNullable4CompoundComp() throws Exception {
        setUpComp();
        testNotNullable4CompoundBound();
    }

    @Test
    public void testNotNullable4CompoundIntp() throws Exception {
        setUpIntp();
        testNotNullable4CompoundBound();
    }

    private void testNotNullable4CompoundBound() throws Exception {
        final RecordClassDescriptor rcd = getRCD(AllCompoundFields.class);
        final RecordClassDescriptor rcdNullable = changeRCDNullability(rcd, true);
        final AllCompoundFields msg = new AllCompoundFields();

        // this should be ok
        msg.setValues();
        testRcdBound(rcd, msg, new AllCompoundFields());
        testRcdBound(rcd, msg, null);

        // field level on encode
        final Field[] fields = AllCompoundFields.class.getDeclaredFields();
        Arrays.sort(fields, ((o1, o2) -> o1.getName().toLowerCase().compareTo(o2.getName().toLowerCase())));
        for (int i = fields.length - 1; i >= 0; i--) {
            final String fieldName = fields[i].getName();
            Util.setFieldValue(msg, fieldName, null);
            try {
                testRcdBound(rcd, msg, null);
                fail("field level encode " + fieldName);
            } catch (IllegalArgumentException e) {
                Assert.assertEquals(String.format("'%s' field is not nullable", fieldName), e.getMessage());
            }
        }
        // field level on decode
        msg.setValues();
        for (int i = fields.length - 1; i >= 0; i--) {
            final String fieldName = fields[i].getName();
            Util.setFieldValue(msg, fieldName, null);
            try {
                MemoryDataOutput out = boundEncode(msg, rcdNullable);
                MemoryDataInput in = new MemoryDataInput(out);
                boundDecode(null, rcd, in);
                fail("field level decode " + fieldName);
            } catch (AssertionError e1) {
                Assert.assertEquals(String.format("'%s' field is not nullable", fieldName), e1.getMessage());
            }
        }

        // element level on encode
        msg.setValues();
        msg.aByte5.add(IntegerDataType.INT8_NULL);
        String fieldName = "aByte5";
        try {
            testRcdBound(rcd, msg, null);
            fail("primitive element level encode " + fieldName);
        } catch (IllegalArgumentException e) {
            //Assert.assertEquals(String.format("'%s' field array element is not nullable", fieldName), e.getMessage());
        }
        // element level on decode
        try {
            MemoryDataOutput out = boundEncode(msg, rcdNullable);
            MemoryDataInput in = new MemoryDataInput(out);
            boundDecode(null, rcd, in);
            fail("primitive element level decode " + fieldName);
        } catch (AssertionError e) {
            Assert.assertEquals(String.format("'%s[]' field array element is not nullable", fieldName), e.getMessage());
        }

        msg.setValues();
        msg.aByte6.add(IntegerDataType.INT8_NULL);
        fieldName = "aByte6";
        try {
            testRcdBound(rcd, msg, null);
            fail("primitive element level encode " + fieldName);
        } catch (IllegalArgumentException e) {
            //Assert.assertEquals(String.format("'%s' field array element is not nullable", fieldName), e.getMessage());
        }
        try {
            MemoryDataOutput out = boundEncode(msg, rcdNullable);
            MemoryDataInput in = new MemoryDataInput(out);
            boundDecode(null, rcd, in);
            fail("primitive element level decode " + fieldName);
        } catch (AssertionError e) {
            Assert.assertEquals(String.format("'%s[]' field array element is not nullable", fieldName), e.getMessage());
        }

        for (int i = 7; i >= 0; i--) {
            msg.setValues();

            fieldName = fields[i].getName();
            // stop on primitive element type
            if(fieldName.equals("aByte6"))
                break;

            fields[i].setAccessible(true);
            final ObjectArrayList<?> list = (ObjectArrayList<?>) fields[i].get(msg);
            list.add(null);

            boolean checkFail = false;
            try {
                testRcdBound(rcd, msg, null);
            } catch (IllegalArgumentException e) {
                checkFail = true;
                //Assert.assertEquals(String.format("'%s' field array element is not nullable", fieldName), e.getMessage());
            }

            try {
                MemoryDataOutput out = boundEncode(msg, rcdNullable);
                MemoryDataInput in = new MemoryDataInput(out);
                boundDecode(null, rcd, in);
                if (checkFail)
                    fail("object element level decode " + fieldName);
            } catch (AssertionError e) {
                Assert.assertEquals("element level decode " + fieldName, String.format("'%s[]' field array element is not nullable", fieldName), e.getMessage());
            }
        }
    }

    /*
    * Misc cases, which are uncovered by regular ones
    */

    @Test
    public void testMiscComp() throws Exception {
        setUpComp();
        testNullOnDecode();
        testNullThenValueArrayFieldRead();
        testSecondaryObjectFieldRead();
        testArrayOfObjectRemapping();
    }

    @Test
    public void testMiscIntp() throws Exception {
        setUpIntp();
        testNullOnDecode();
        testNullThenValueArrayFieldRead();
        testSecondaryObjectFieldRead();
        testArrayOfObjectRemapping();
    }

    public static class MsgClassArrayTwoFieldPublic extends InstrumentMessage {
        public IntegerArrayList f1;
        public LongArrayList f2;

        @Override
        public String toString() {
            final StringBuilder sb = new StringBuilder();
            sb.append("f1 ");
            dumpList(f1, sb);
            sb.append("\nf2 ");
            dumpList(f2, sb);
            return sb.toString();
        }
    }

    private void testNullOnDecode() throws Exception {
        // rev #27638: ARRAY OF OBJECT: Not Nullable check fix
        // make sure that decoder may read null-value of an array element into a field bound to NonNull dataType, when Java assertions are off
        final RecordClassDescriptor rcd = getRCD(MsgClassArrayTwoFieldPublic.class);
        final RecordClassDescriptor rcdNotNull = changeRCDNullability(rcd, false);
        final MsgClassArrayTwoFieldPublic msg = new MsgClassArrayTwoFieldPublic();

        // 1. totally empty
        MemoryDataOutput out = boundEncode(msg, rcd);
        MemoryDataInput in = new MemoryDataInput(out);
        try {
            boundDecode(null, rcdNotNull, in);
            Assert.fail("totally empty");
        } catch (AssertionError e) {
            Assert.assertEquals("'f1' field is not nullable", e.getMessage());
        }

        // 2. 1st field != null, 2nd == null
        msg.f1 = new IntegerArrayList();
        msg.f1.add(1);
        out = boundEncode(msg, rcd);
        in = new MemoryDataInput(out);
        try {
            boundDecode(null, rcdNotNull, in);
            Assert.fail("1st field != null, 2nd == null");
        } catch (AssertionError e) {
            Assert.assertEquals("'f2' field is not nullable", e.getMessage());
        }
        catch (IllegalStateException e2) {
        //    Assert.assertTrue(IKVMUtil.IS_IKVM);
            Assert.assertEquals("cannot write null to not nullable field 'f1'", e2.getMessage());
        }

        // 3. array has null elements
        msg.f1.add(IntegerDataType.INT32_NULL);
        out = boundEncode(msg, rcd);
        in = new MemoryDataInput(out);
        try {
            boundDecode(null, rcdNotNull, in);
            Assert.fail("array has null elements");
        } catch (AssertionError e) {
            Assert.assertEquals("'f1[]' field array element is not nullable", e.getMessage());
        }
        catch (IllegalStateException e2) {
         //   Assert.assertTrue(IKVMUtil.IS_IKVM);
            Assert.assertEquals("cannot write null to not nullable field 'f1'", e2.getMessage());
        }
    }

    // test fix in rev #27742
    private void testNullThenValueArrayFieldRead() throws Exception {
        // 1. isNull/getDecoder; fixed element size ARRAY decoder
        final RecordClassDescriptor rcd = getRCD(MsgClassArrayTwoFieldPublic.class);
        final MsgClassArrayTwoFieldPublic msg = new MsgClassArrayTwoFieldPublic();
        MemoryDataOutput out = boundEncode(msg, rcd);
        MemoryDataInput in = new MemoryDataInput(out);

        UnboundDecoder udec = factory.createFixedUnboundDecoder(rcd);
        // all null
        testDoubleRead(in, true, udec);

        // all non-null
        msg.f1 = new IntegerArrayList();
        msg.f2 = new LongArrayList();
        out = boundEncode(msg, rcd);
        in = new MemoryDataInput(out);
        testDoubleRead(in, true, udec);

        // null + non-null
        msg.f1 = null;
        out = boundEncode(msg, rcd);
        in = new MemoryDataInput(out);
        testDoubleRead(in, true, udec);

        // null + non-null(1 element)
        msg.f1 = null;
        msg.f2.add(555);
        out = boundEncode(msg, rcd);
        in = new MemoryDataInput(out);
        testDoubleRead(in, true, udec);

        // non-null(1 element) + null
        msg.f1 = new IntegerArrayList();
        msg.f1.add(444);
        msg.f2 = null;
        out = boundEncode(msg, rcd);
        in = new MemoryDataInput(out);
        testDoubleRead(in, true, udec);
    }

    // test fix in rev #27742
    private void testSecondaryObjectFieldRead() throws Exception {
        final RecordClassDescriptor rcd = getRCD(FixedObjFieldsPublic.class);
        final FixedObjFieldsPublic msg = new FixedObjFieldsPublic();
        MemoryDataOutput out = boundEncode(msg, rcd);
        UnboundDecoder udec = factory.createFixedUnboundDecoder(rcd);

        MemoryDataInput in = new MemoryDataInput(out);
        // all null
        testDoubleRead(in, false, udec);

        // all non-null
        msg.oPub1 = new MsgClassAllPublic();
        msg.oPub2 = new MsgClassAllPublic();
        out = boundEncode(msg, rcd);
        in = new MemoryDataInput(out);
        testDoubleRead(in, false, udec);

        // null + not-null
        msg.oPub1 = null;
        msg.oPub2 = new MsgClassAllPublic();
        out = boundEncode(msg, rcd);
        in = new MemoryDataInput(out);
        testDoubleRead(in, false, udec);
    }

    private static final RecordClassDescriptor cdMsgClassSimple =
            new RecordClassDescriptor(
                    "x.Simple",
                    null,
                    false,
                    null,
                    new NonStaticDataField("f1", null, new IntegerDataType(IntegerDataType.ENCODING_INT64, true)),
                    new NonStaticDataField("f2", null, new IntegerDataType(IntegerDataType.ENCODING_INT64, true))
            );

    private static final RecordClassDescriptor cdMsgClassArrayOfObjectRemapping =
            new RecordClassDescriptor(
                    "x.ArrayOfObjectRemapping",
                    null,
                    false,
                    null,
                    new NonStaticDataField("f1", null, new ArrayDataType(true, new ClassDataType(true, cdMsgClassSimple))),
                    new NonStaticDataField("f2", null, new ArrayDataType(true, new ClassDataType(true, cdMsgClassSimple)))
            );

    public static class Simple {
        public long f1;
        private long f2;

        @Override
        public String toString() {
            return String.valueOf(f1) + ',' + String.valueOf(f2);
        }

        void setNulls() {
            f1 = IntegerDataType.INT64_NULL;
            f2 = IntegerDataType.INT64_NULL;
        }

        void setValues() {
            f1 = 12456789124567890L;
            f2 = 1245678912456789L;
        }

        public long getF2 () {
            return f2;
        }

        public void setF2 (long f2) {
            this.f2 = f2;
        }
    }

    public static class ArrayOfObjectRemapped extends InstrumentMessage {
        public ObjectArrayList<Simple> f1;
        private ObjectArrayList<Simple> f2;

        @Override
        public String toString() {
            final StringBuilder sb = new StringBuilder();
            sb.append("f1 ");
            dumpList(f1, sb);
            sb.append("\nf2 ");
            dumpList(f2, sb);
            return sb.toString();
        }

        void setNulls() {
            f1 = null;
            f2 = null;
        }

        void setValues(boolean isNull) {
            f1 = new ObjectArrayList<>();
            final Simple item = new Simple();
            f1.add(item);

            f2 = new ObjectArrayList<>();
            f2.add(item);

            if (isNull)
                item.setNulls();
            else
                item.setValues();
        }

        public ObjectArrayList<Simple> getF2 () {
            return f2;
        }

        public void setF2 (ObjectArrayList<Simple> f2) {
            this.f2 = f2;
        }
    }

    // test fix for bug #15977
    private void testArrayOfObjectRemapping() throws Exception {
        final SimpleTypeLoader typeLoader = new SimpleTypeLoader(cdMsgClassArrayOfObjectRemapping.getName(), ArrayOfObjectRemapped.class,
                cdMsgClassSimple.getName(), Simple.class);

        final ArrayOfObjectRemapped inMsg = new ArrayOfObjectRemapped();

        inMsg.setNulls();
        testRcdBound(cdMsgClassArrayOfObjectRemapping, typeLoader, inMsg, null);

        inMsg.setValues(false);
        testRcdBound(cdMsgClassArrayOfObjectRemapping, typeLoader, inMsg, null);
        inMsg.setValues(true);
        testRcdBound(cdMsgClassArrayOfObjectRemapping, typeLoader, inMsg, null);
    }

    private void testDoubleRead(MemoryDataInput in, boolean singleReadOfValue, UnboundDecoder udec) {
        udec.beginRead(in);
        while (udec.nextField()) {
            final boolean isNull = udec.isNull();
            try {
                Object o1 = readField(udec);
                if (!singleReadOfValue) {
                    Object o2 = readField(udec);
                    Assert.assertEquals("field " + udec.getField().getName(), o1, o2);
                }

                Assert.assertEquals("field " + udec.getField().getName(), isNull, o1 == null);
            } catch (NullValueException e) {
                Assert.assertTrue("field " + udec.getField().getName(), isNull);
            }
        }
    }

    private RecordClassDescriptor changeRCDNullability(RecordClassDescriptor rcd, boolean isNullable) {
        final DataField[] fields = rcd.getFields();
        final DataField[] newFields = new DataField[fields.length];
        for (int i = 0; i < fields.length; i++) {
            final DataField field = fields[i];
            final DataType dt = field.getType();
            final DataType newDT;
            if (dt.isNullable() != isNullable) {
                if (dt instanceof ArrayDataType)
                    newDT = new ArrayDataType(isNullable, ((ArrayDataType) dt).getElementDataType().nullableInstance(isNullable));
                else
                    newDT = dt.nullableInstance(isNullable);
            } else
                newDT = dt;

            newFields[i] = new NonStaticDataField(field.getName(), null, newDT);
        }

        return new RecordClassDescriptor(rcd.getName(), null, false, null, newFields);
    }
}

// TODO:
// 1. testNotNullable4CompoundBound on decode ?
