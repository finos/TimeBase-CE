/*
 * Copyright 2023 EPAM Systems, Inc
 *
 * See the NOTICE file distributed with this work for additional information
 * regarding copyright ownership. Licensed under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.epam.deltix.test.qsrv.hf.tickdb.testframework;

import com.epam.deltix.qsrv.test.messages.TestEnum;
import com.epam.deltix.timebase.messages.InstrumentMessage;

import com.epam.deltix.timebase.messages.TimeStamp;
import com.epam.deltix.qsrv.hf.pub.codec.AlphanumericCodec;
import com.epam.deltix.qsrv.hf.pub.md.*;
import com.epam.deltix.timebase.messages.*;
import com.epam.deltix.util.collections.CharSubSequence;
import com.epam.deltix.util.collections.generated.ByteArrayList;
import junit.framework.Assert;


/**
 * copy of deltix.qsrv.hf.tickdb.testframework.TestAllTypesMessage
 * with protected field and public properties
 */
public class TestAllTypesMessagePrivate extends InstrumentMessage {

    private static final Introspector ix =
            Introspector.createEmptyMessageIntrospector ();
    private static RecordClassDescriptor myDescriptor = null;


    /**
     * FIELDS
     */

    protected int                  sequence;

    //
    //  BOOLEAN
    //
    protected boolean              bool_c;
    protected byte                 bool_n;

    //
    //  FLOAT
    //
    protected float                float_c_32;
    protected float                float_n_32;
    protected double               float_c_64;
    protected double               float_n_64;
    protected double               float_c_dec;
    protected double               float_n_dec;
    protected double               float_c_dec2;
    protected double               float_n_dec2;

    //
    //  INTEGER
    //
    protected byte                 int_c_8;
    protected byte                 int_n_8;
    protected short                int_c_16;
    protected short                int_n_16;
    protected int                  int_c_32;
    protected int                  int_n_32;
    protected long                 int_c_64;
    protected long                 int_n_64;
    protected int                  puint_c_30;
    protected int                  puint_n_30;
    protected long                 puint_c_61;
    protected long                 puint_n_61;

    //
    //  CHAR
    //
    protected char                 char_c;
    protected char                 char_n;

    //
    //  VARCHAR
    //
    protected String               varchar_c_utf8;
    protected String               varchar_n_utf8;
    protected long                 varchar_c_alpha10;
    protected long                 varchar_n_alpha10;
    protected CharSequence         varchar_c_alpha5_s;
    protected CharSequence         varchar_n_alpha5_s;

    //
    //  TIMEOFDAY
    //
    protected int                  tod_c;
    protected int                  tod_n;

    //
    //  DATE
    //
    protected long                 date_c;
    protected long                 date_n;

    //
    //  ENUM
    //
    protected TestEnum enum_c;
    protected TestEnum             enum_n;

//    @Enumerated(deltix.qsrv.hf.tickdb.testframework.TestBitmask.class)
//    @SchemaElement(
//            title = "Non-nullable BITMASK"
//    )
//    @SchemaType(
//            isNullable = false
//    )
//    protected long                 bitmask_c;

    // BUG 10318
    //@Enumerated ("deltix.qsrv.hf.tickdb.testframework.TestBitmask")
    //@Title ("Nullable BITMASK")
    //public long                 bitmask_n;
    //
    //  BINARY
    //
    // BUG 10303
    //@NotNull @Title ("Non-nullable BINARY")
    //public ByteArrayList        binary_c;


    protected ByteArrayList binary_n;

    /********************************************
     * GETTERS
     *********************************************/

    @SchemaElement (
            title = "Sequence"
    )
    public int getSequence () {
        return sequence;
    }


    //
    //  BOOLEAN
    //
    @SchemaElement(
            title = "Non-nullable BOOLEAN"
    )
    public boolean isBool_c () {
        return bool_c;
    }

    @SchemaElement(
            title = "Nullable BOOLEAN"
    )
    @SchemaType (
            dataType = SchemaDataType.BOOLEAN
    )
    public byte getBool_n () {
        return bool_n;
    }


    //
    //  CHAR
    //
    @SchemaElement(
            title = "Non-nullable CHAR"
    )
    @SchemaType(
            isNullable = false
    )
    public char getChar_c () {
        return char_c;
    }

    @SchemaElement(
            title = "Nullable CHAR"
    )
    public char getChar_n () {
        return char_n;
    }


    //
    //  FLOAT
    //
    @SchemaElement(
            title = "Non-nullable FLOAT:IEEE32"
    )
    @SchemaType(
            encoding = "IEEE32",
            dataType = SchemaDataType.FLOAT,
            isNullable = false
    )
    public float getFloat_c_32 () {
        return float_c_32;
    }

    @SchemaElement(
            title = "Non-nullable FLOAT:IEEE64"
    )
    @SchemaType(
            encoding = "IEEE64",
            dataType = SchemaDataType.FLOAT,
            isNullable = false
    )
    public double getFloat_c_64 () {
        return float_c_64;
    }

    @SchemaElement(
            title = "Non-nullable FLOAT:DECIMAL(2)"
    )
    @SchemaType(
            encoding = "DECIMAL(2)",
            dataType = SchemaDataType.FLOAT,
            isNullable = false
    )
    public double getFloat_c_dec2 () {
        return float_c_dec2;
    }

    @SchemaElement(
            title = "Non-nullable FLOAT:DECIMAL"
    )
    @SchemaType(
            encoding = "DECIMAL",
            dataType = SchemaDataType.FLOAT,
            isNullable = false
    )
    public double getFloat_c_dec () {
        return float_c_dec;
    }

    @SchemaElement(
            title = "Nullable FLOAT:IEEE32"
    )
    @SchemaType(
            encoding = "IEEE32",
            dataType = SchemaDataType.FLOAT
    )
    public float getFloat_n_32 () {
        return float_n_32;
    }

    @SchemaElement(
            title = "Nullable FLOAT:IEEE64"
    )
    @SchemaType(
            encoding = "IEEE64",
            dataType = SchemaDataType.FLOAT
    )
    public double getFloat_n_64 () {
        return float_n_64;
    }

    @SchemaElement(
            title = "Nullable FLOAT:DECIMAL(2)"
    )
    @SchemaType(
            encoding = "DECIMAL(2)",
            dataType = SchemaDataType.FLOAT
    )
    public double getFloat_n_dec2 () {
        return float_n_dec2;
    }

    @SchemaElement(
            title = "Nullable FLOAT:DECIMAL"
    )
    @SchemaType(
            encoding = "DECIMAL",
            dataType = SchemaDataType.FLOAT
    )
    public double getFloat_n_dec () {
        return float_n_dec;
    }


    //
    //  INTEGER
    //
    @SchemaElement(
            title = "Non-nullable INTEGER:INT8"
    )
    @SchemaType(
            encoding = "INT8",
            dataType = SchemaDataType.INTEGER,
            isNullable = false
    )
    public byte getInt_c_8 () {
        return int_c_8;
    }

    @SchemaElement(
            title = "Nullable INTEGER:INT8"
    )
    @SchemaType(
            encoding = "INT8",
            dataType = SchemaDataType.INTEGER
    )
    public byte getInt_n_8 () {
        return int_n_8;
    }

    @SchemaElement(
            title = "Non-nullable INTEGER:INT16"
    )
    @SchemaType(
            encoding = "INT16",
            dataType = SchemaDataType.INTEGER,
            isNullable = false
    )
    public short getInt_c_16 () {
        return int_c_16;
    }

    @SchemaElement(
            title = "Nullable INTEGER:INT16"
    )
    @SchemaType(
            encoding = "INT16",
            dataType = SchemaDataType.INTEGER
    )
    public short getInt_n_16 () {
        return int_n_16;
    }

    @SchemaElement(
            title = "Non-nullable INTEGER:INT32"
    )
    @SchemaType(
            encoding = "INT32",
            dataType = SchemaDataType.INTEGER,
            isNullable = false
    )
    public int getInt_c_32 () {
        return int_c_32;
    }

    @SchemaElement(
            title = "Nullable INTEGER:INT32"
    )
    @SchemaType(
            encoding = "INT32",
            dataType = SchemaDataType.INTEGER
    )
    public int getInt_n_32 () {
        return int_n_32;
    }

    @SchemaElement(
            title = "Non-nullable INTEGER:INT64"
    )
    @SchemaType(
            encoding = "INT64",
            dataType = SchemaDataType.INTEGER,
            isNullable = false
    )
    public long getInt_c_64 () {
        return int_c_64;
    }

    @SchemaElement(
            title = "Nullable INTEGER:INT64"
    )
    @SchemaType(
            encoding = "INT64",
            dataType = SchemaDataType.INTEGER
    )
    public long getInt_n_64 () {
        return int_n_64;
    }

    @SchemaElement(
            title = "Non-nullable INTEGER:PUINT30"
    )
    @SchemaType(
            encoding = "PUINT30",
            dataType = SchemaDataType.INTEGER,
            isNullable = false
    )
    public int getPuint_c_30 () {
        return puint_c_30;
    }

    @SchemaElement(
            title = "Nullable INTEGER:PUINT30"
    )
    @SchemaType(
            encoding = "PUINT30",
            dataType = SchemaDataType.INTEGER
    )
    public int getPuint_n_30 () {
        return puint_n_30;
    }

    @SchemaElement(
            title = "Non-nullable INTEGER:PUINT61"
    )
    @SchemaType(
            encoding = "PUINT61",
            dataType = SchemaDataType.INTEGER,
            isNullable = false
    )
    public long getPuint_c_61 () {
        return puint_c_61;
    }

    @SchemaElement(
            title = "Nullable INTEGER:PUINT61"
    )
    @SchemaType(
            encoding = "PUINT61",
            dataType = SchemaDataType.INTEGER
    )
    public long getPuint_n_61 () {
        return puint_n_61;
    }


    //
    //  TIMEOFDAY
    //
    @SchemaElement(
            title = "Non-nullable TIMEOFDAY"
    )
    @SchemaType(
            dataType = SchemaDataType.TIME_OF_DAY,
            isNullable = false
    )
    public int getTod_c () {
        return tod_c;
    }

    @SchemaElement(
            title = "Nullable TIMEOFDAY"
    )
    @SchemaType(
            dataType = SchemaDataType.TIME_OF_DAY
    )
    public int getTod_n () {
        return tod_n;
    }


    //
    //  DATE
    //
    @SchemaElement(
            title = "Non-nullable DATE"
    )
    @SchemaType(
            dataType = SchemaDataType.TIMESTAMP,
            isNullable = false
    )
    public long getDate_c () {
        return date_c;
    }

    @SchemaElement(
            title = "Nullable DATE"
    )
    @SchemaType(
            dataType = SchemaDataType.TIMESTAMP
    )
    public long getDate_n () {
        return date_n;
    }

    //
    //  ENUM
    //
    @SchemaElement(
            title = "Non-nullable ENUM"
    )
    @SchemaType(
            isNullable = false
    )
    public TestEnum getEnum_c () {
        return enum_c;
    }

    @SchemaElement(
            title = "Nullable ENUM"
    )
    public TestEnum getEnum_n () {
        return enum_n;
    }


    @SchemaElement(
            title = "Nullable BINARY"
    )
    public ByteArrayList getBinary_n () {
        return binary_n;
    }

    //
    //  VARCHAR
    //
    @SchemaElement(
            title = "Non-nullable VARCHAR:UTF8"
    )
    @SchemaType(
            isNullable = false
    )
    public String getVarchar_c_utf8 () {
        return varchar_c_utf8;
    }
    @SchemaElement(
            title = "Non-nullable VARCHAR:ALPHANUMERIC(10):long"
    )
    @SchemaType(
            encoding = "ALPHANUMERIC(10)",
            dataType = SchemaDataType.VARCHAR,
            isNullable = false
    )
    public long getVarchar_c_alpha10 () {
        return varchar_c_alpha10;
    }

    @SchemaElement(
            title = "Non-nullable VARCHAR:ALPHANUMERIC(5)"
    )
    @SchemaType(
            encoding = "ALPHANUMERIC(5)",
            dataType = SchemaDataType.VARCHAR,
            isNullable = false
    )
    public CharSequence getVarchar_c_alpha5_s () {
        return varchar_c_alpha5_s;
    }

    @SchemaElement(
            title = "Nullable VARCHAR:ALPHANUMERIC(10):long"
    )
    @SchemaType(
            encoding = "ALPHANUMERIC(10)",
            dataType = SchemaDataType.VARCHAR
    )
    public long getVarchar_n_alpha10 () {
        return varchar_n_alpha10;
    }

    @SchemaElement(
            title = "Nullable VARCHAR:ALPHANUMERIC(5)"
    )
    @SchemaType(
            encoding = "ALPHANUMERIC(5)",
            dataType = SchemaDataType.VARCHAR
    )
    public CharSequence getVarchar_n_alpha5_s () {
        return varchar_n_alpha5_s;
    }

    @SchemaElement(
            title = "Nullable VARCHAR:UTF8"
    )
    public String getVarchar_n_utf8 () {
        return varchar_n_utf8;
    }


    /*****************************************************
     * SETTERS
     ****************************************************/

    public void setBinary_n (ByteArrayList binary_n) {
        this.binary_n = binary_n;
    }

    public void setBool_c (boolean bool_c) {
        this.bool_c = bool_c;
    }

    public void setBool_n (byte bool_n) {
        this.bool_n = bool_n;
    }

    public void setChar_c (char char_c) {
        this.char_c = char_c;
    }

    public void setChar_n (char char_n) {
        this.char_n = char_n;
    }

    public void setDate_c (long date_c) {
        this.date_c = date_c;
    }

    public void setDate_n (long date_n) {
        this.date_n = date_n;
    }

    public void setEnum_c (TestEnum enum_c) {
        this.enum_c = enum_c;
    }

    public void setEnum_n (TestEnum enum_n) {
        this.enum_n = enum_n;
    }

    public void setFloat_c_32 (float float_c_32) {
        this.float_c_32 = float_c_32;
    }

    public void setFloat_c_64 (double float_c_64) {
        this.float_c_64 = float_c_64;
    }

    public void setFloat_c_dec2 (double float_c_dec2) {
        this.float_c_dec2 = float_c_dec2;
    }

    public void setFloat_c_dec (double float_c_dec) {
        this.float_c_dec = float_c_dec;
    }

    public void setFloat_n_32 (float float_n_32) {
        this.float_n_32 = float_n_32;
    }

    public void setFloat_n_64 (double float_n_64) {
        this.float_n_64 = float_n_64;
    }

    public void setFloat_n_dec2 (double float_n_dec2) {
        this.float_n_dec2 = float_n_dec2;
    }

    public void setFloat_n_dec (double float_n_dec) {
        this.float_n_dec = float_n_dec;
    }

    public void setInt_c_16 (short int_c_16) {
        this.int_c_16 = int_c_16;
    }

    public void setInt_c_32 (int int_c_32) {
        this.int_c_32 = int_c_32;
    }

    public void setInt_c_64 (long int_c_64) {
        this.int_c_64 = int_c_64;
    }

    public void setInt_c_8 (byte int_c_8) {
        this.int_c_8 = int_c_8;
    }

    public void setInt_n_16 (short int_n_16) {
        this.int_n_16 = int_n_16;
    }

    public void setInt_n_32 (int int_n_32) {
        this.int_n_32 = int_n_32;
    }

    public void setInt_n_64 (long int_n_64) {
        this.int_n_64 = int_n_64;
    }

    public void setInt_n_8 (byte int_n_8) {
        this.int_n_8 = int_n_8;
    }

    public void setPuint_c_30 (int puint_c_30) {
        this.puint_c_30 = puint_c_30;
    }

    public void setPuint_c_61 (long puint_c_61) {
        this.puint_c_61 = puint_c_61;
    }

    public void setPuint_n_30 (int puint_n_30) {
        this.puint_n_30 = puint_n_30;
    }

    public void setPuint_n_61 (long puint_n_61) {
        this.puint_n_61 = puint_n_61;
    }

    public void setSequence (int sequence) {
        this.sequence = sequence;
    }

    public void setTod_c (int tod_c) {
        this.tod_c = tod_c;
    }

    public void setTod_n (int tod_n) {
        this.tod_n = tod_n;
    }

    public void setVarchar_c_alpha10 (long varchar_c_alpha10) {
        this.varchar_c_alpha10 = varchar_c_alpha10;
    }

    public void setVarchar_c_alpha5_s (CharSequence varchar_c_alpha5_s) {
        this.varchar_c_alpha5_s = varchar_c_alpha5_s;
    }

    public void setVarchar_c_utf8 (String varchar_c_utf8) {
        this.varchar_c_utf8 = varchar_c_utf8;
    }

    public void setVarchar_n_alpha10 (long varchar_n_alpha10) {
        this.varchar_n_alpha10 = varchar_n_alpha10;
    }

    public void setVarchar_n_alpha5_s (CharSequence varchar_n_alpha5_s) {
        this.varchar_n_alpha5_s = varchar_n_alpha5_s;
    }

    public void setVarchar_n_utf8 (String varchar_n_utf8) {
        this.varchar_n_utf8 = varchar_n_utf8;
    }



    public static synchronized RecordClassDescriptor getClassDescriptor () {
        if (myDescriptor == null) {
            try {
                myDescriptor = ix.introspectRecordClass (TestAllTypesMessagePrivate.class);
            } catch (Introspector.IntrospectionException x) {
                throw new RuntimeException (x);   // Unexpected, this should be reliable.
            }
        }

        return (myDescriptor);
    }

    public void         assertEquals (String diag, TestAllTypesMessagePrivate other) {
        Assert.assertEquals (diag + "sequence", this.sequence, other.sequence);
        Assert.assertEquals (diag + "bool_c", this.bool_c, other.bool_c);
        Assert.assertEquals (diag + "bool_n", this.bool_n, other.bool_n);
        Assert.assertEquals (diag + "float_c_32", this.float_c_32, other.float_c_32);
        Assert.assertEquals (diag + "float_n_32", this.float_n_32, other.float_n_32);
        Assert.assertEquals (diag + "float_c_64", this.float_c_64, other.float_c_64);
        Assert.assertEquals (diag + "float_n_64", this.float_n_64, other.float_n_64);
        Assert.assertEquals (diag + "float_c_dec", this.float_c_dec, other.float_c_dec);
        Assert.assertEquals (diag + "float_n_dec", this.float_n_dec, other.float_n_dec);
        Assert.assertEquals (diag + "float_c_dec2", this.float_c_dec2, other.float_c_dec2, 0.01);
        Assert.assertEquals (diag + "float_n_dec2", this.float_n_dec2, other.float_n_dec2, 0.01);
        Assert.assertEquals (diag + "int_c_8", this.int_c_8, other.int_c_8);
        Assert.assertEquals (diag + "int_n_8", this.int_n_8, other.int_n_8);
        Assert.assertEquals (diag + "int_c_16", this.int_c_16, other.int_c_16);
        Assert.assertEquals (diag + "int_n_16", this.int_n_16, other.int_n_16);
        Assert.assertEquals (diag + "int_c_32", this.int_c_32, other.int_c_32);
        Assert.assertEquals (diag + "int_n_32", this.int_n_32, other.int_n_32);
        Assert.assertEquals (diag + "int_c_64", this.int_c_64, other.int_c_64);
        Assert.assertEquals (diag + "int_n_64", this.int_n_64, other.int_n_64);
        Assert.assertEquals (diag + "puint_c_30", this.puint_c_30, other.puint_c_30);
        Assert.assertEquals (diag + "puint_n_30", this.puint_n_30, other.puint_n_30);
        Assert.assertEquals (diag + "puint_c_61", this.puint_c_61, other.puint_c_61);
        Assert.assertEquals (diag + "puint_n_61", this.puint_n_61, other.puint_n_61);
        Assert.assertEquals (diag + "char_c", this.char_c, other.char_c);
        Assert.assertEquals (diag + "char_n", this.char_n, other.char_n);
        Assert.assertEquals (diag + "varchar_c_utf8", this.varchar_c_utf8, other.varchar_c_utf8);
        Assert.assertEquals (diag + "varchar_n_utf8", this.varchar_n_utf8, other.varchar_n_utf8);
        Assert.assertEquals (diag + "varchar_c_alpha10", this.varchar_c_alpha10, other.varchar_c_alpha10);
        Assert.assertEquals (diag + "varchar_n_alpha10", this.varchar_n_alpha10, other.varchar_n_alpha10);
        Assert.assertEquals (diag + "varchar_c_alpha5_s", this.varchar_c_alpha5_s != null ? this.varchar_c_alpha5_s.toString() : this.varchar_c_alpha5_s, other.varchar_c_alpha5_s != null ? other.varchar_c_alpha5_s.toString() : other.varchar_c_alpha5_s);
        Assert.assertEquals (diag + "varchar_n_alpha5_s", this.varchar_n_alpha5_s != null ? this.varchar_n_alpha5_s.toString() : this.varchar_n_alpha5_s, other.varchar_n_alpha5_s!= null ? other.varchar_n_alpha5_s.toString() : other.varchar_n_alpha5_s);
        Assert.assertEquals (diag + "tod_c", this.tod_c, other.tod_c);
        Assert.assertEquals (diag + "tod_n", this.tod_n, other.tod_n);
        Assert.assertEquals (diag + "date_c", this.date_c, other.date_c);
        Assert.assertEquals (diag + "date_n", this.date_n, other.date_n);
        Assert.assertEquals (diag + "enum_c", this.enum_c, other.enum_c);
        Assert.assertEquals (diag + "enum_n", this.enum_n, other.enum_n);

        // BUG 10317
        //Assert.assertEquals (diag + "bitmask_c", this.bitmask_c, other.bitmask_c);

        // BUG 10303
        //Assert.assertEquals (diag + "binary_c", this.binary_c, other.binary_c);

        Assert.assertEquals (diag + "binary_n", this.binary_n, other.binary_n);
    }

    private final AlphanumericCodec ancodec = new AlphanumericCodec (10);
    private final CharSubSequence sub = new CharSubSequence ("123456789AB");
    private final ByteArrayList             bal = new ByteArrayList ();
    private static final long                BASETIME = 1293840000000L;  // 2011
    private static final int                 LAST_SEQ_NO = 99;
    private static final int                 NUM_SYMBOLS = 2;
    private static final String []           TEXT =
            ("I want a hero: an uncommon want,\n" +
                    "When every year and month sends forth a new one,\n" +
                    "Till, after cloying the gazettes with cant,\n" +
                    "The age discovers he is not the true one;\n" +
                    "Of such as these I should not care to vaunt,\n" +
                    "I 'll therefore take our ancient friend Don Juan—\n" +
                    "We all have seen him, in the pantomime,\n" +
                    "Sent to the devil somewhat ere his time.\n").split ("[\n ]+");

    public void                             fillMessage (
            int                                     entNo,
            int                                     seqNo
    )
    {
        int             seesaw; // up to 25 down to -25 and back up  to 0
        boolean         skip = seqNo % 10 == 9;

        if (seqNo <= 25)
            seesaw = seqNo;
        else if (seqNo <= 75)
            seesaw = 50 - seqNo;
        else
            seesaw = seqNo - 100;

        setNanoTime((BASETIME + seqNo * 1000 + entNo) * TimeStamp.NANOS_PER_MS);  //new time...
        //
        //  Symbols are of the form Sn
        //
        setSymbol("S" + entNo);

        sequence = seqNo;

        bool_c = seqNo % 3 == 1;

        float_c_32 = seesaw + 0.25F;

        float_n_32 = skip ? FloatDataType.IEEE32_NULL : float_c_32;

        float_c_64 = seesaw + 0.25;

        float_n_64 = skip ? FloatDataType.IEEE64_NULL : float_c_64;

        float_c_dec = seesaw * 0.00001;

        float_n_dec = skip ? FloatDataType.IEEE64_NULL : float_c_dec;

        float_c_dec2 = seesaw * 0.005;

        float_n_dec2 = skip ? FloatDataType.IEEE64_NULL : float_c_dec2;

        //
        //  Test minimums
        //
        switch (seqNo) {
            case 0:
                int_c_8 = (byte) 0x81;
                int_c_16 = (short) 0x8001;
                int_c_32 = 0x80000001;
                int_c_64 = 0x8000000000000001L;
                puint_c_30 = 0;
                puint_c_61 = 0;
                char_c = '\t'; // test unprintable
                varchar_c_utf8 = "";
                break;

            case LAST_SEQ_NO:
                int_c_8 = (byte) 0x7F;
                int_c_16 = (short) 0x7FFF;
                int_c_32 = 0x7FFFFFFF;
                int_c_64 = 0x7FFFFFFFFFFFFFFFL;
                puint_c_30 = 0x3FFFFFFE;
                puint_c_61 = 0x1FFFFFFFFFFFFFFEL;
                char_c = '\u4e00'; // Chinese
                varchar_c_utf8 = "\u27721\u35821\u28450\u35486";
                break;

            default:
                int_c_8 = (byte) seesaw;
                int_c_16 = (short) seesaw;
                int_c_32 = seqNo % 10;          // for some queries
                int_c_64 = seesaw;
                puint_c_30 = seqNo * 10000000;  // tests all sizes
                puint_c_61 = seqNo * 10000000000000000L; // tests all sizes
                char_c = (char) ('A' + seqNo - 1);
                varchar_c_utf8 = TEXT [seqNo % TEXT.length];
                break;
        }

        int_n_8 = skip ? IntegerDataType.INT8_NULL : int_c_8;
        int_n_16 = skip ? IntegerDataType.INT16_NULL : int_c_16;
        int_n_32 = skip ? IntegerDataType.INT32_NULL : int_c_32;
        int_n_64 = skip ? IntegerDataType.INT64_NULL : int_c_64;
        puint_n_30 = skip ? IntegerDataType.PUINT30_NULL : puint_c_30;
        puint_n_61 = skip ? IntegerDataType.PUINT61_NULL : puint_c_61;
        char_n = skip ? CharDataType.NULL : char_c;
        varchar_n_utf8 = skip ? null : varchar_c_utf8;

        sub.end = seqNo % 11;
        varchar_c_alpha10 = ancodec.encodeToLong (sub);
        varchar_n_alpha10 = skip ? VarcharDataType.ALPHANUMERIC_NULL : varchar_c_alpha10;

        sub.end = seqNo % 6;
        varchar_c_alpha5_s = sub;
        varchar_n_alpha5_s = skip ? null : varchar_c_alpha5_s;

        tod_c = seqNo * 60000;  // minutes
        tod_n = skip ? TimeOfDayDataType.NULL : tod_c;

        date_c = BASETIME + 24L * 3600000L * seqNo;   // Days of 2011
        date_n = skip ? DateTimeDataType.NULL : date_c;

        enum_c = TestEnum.values () [seqNo % TestEnum.values ().length];
        enum_n = skip ? null : enum_c;

        bool_n =
                skip ?
                        BooleanDataType.NULL :
                        bool_c ?
                                BooleanDataType.TRUE :
                                BooleanDataType.FALSE;

//        msg.bitmask_c = seqNo & ((1 << TestBitmask.values ().length) - 1);

        bal.clear ();

        for (int ii = 0; ii < seqNo; ii++)
            bal.add ((byte) seqNo);

        // BUG 10303
        //msg.binary_c = bal;

        binary_n = skip ? null : bal;
    }


}