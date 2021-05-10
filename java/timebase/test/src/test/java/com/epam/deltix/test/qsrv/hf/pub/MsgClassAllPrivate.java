package com.epam.deltix.test.qsrv.hf.pub;

import com.epam.deltix.qsrv.hf.pub.md.*;
import com.epam.deltix.test.qsrv.hf.tickdb.testframework.TestEnum;
import com.epam.deltix.timebase.messages.*;

/**
 * Date: Mar 14, 2011
 * @author BazylevD
 */
public class MsgClassAllPrivate {
    private String s1;

    private TestEnum mEnum;
    private String mString;
    private CharSequence mCharSequence;
    public boolean mBoolean; // unnullable case
    @SchemaType(
            dataType = SchemaDataType.BOOLEAN
    )
    public byte mBoolByte; // nullable case
    private char mChar;
    private long mDateTime;
    private int mTimeOfDay;

    private byte mByte;
    private short mShort;
    private int mInt;

    private long mInt48;
    private long mLong;
    private float mFloat;
    private double mDouble;
    private double mDouble2;

    private int mPUINT30;
    private long mPUINT61;


    int mPIneterval;
    private double mSCALE_AUTO;
    private double mSCALE4;

    public String toString() {
        return s1 + " " + mEnum + " " + mString + " " + mCharSequence + " " + mBoolean + " " + mBoolByte + " " + mChar + " " + mDateTime + " "
                + mTimeOfDay + " " + mByte + " " + mShort + " " + mInt + " " + mInt48 + " " + mLong + " " + mFloat + " "
                + mDouble + " " + mDouble2 + " " + mPUINT30 + " " + mPUINT61 + " " + mPIneterval + " " + mSCALE_AUTO + " "
                + mSCALE4;
    }

    public String toString2() {
        return s1 + mEnum + " " + mString + " " + mCharSequence + " " + mBoolByte + " " + mChar + " " + mDateTime + " "
                + mTimeOfDay + " " + mByte + " " + mShort + " " + mInt + " " + mInt48 + " " + mLong + " " + mFloat + " "
                + mDouble + " " + mDouble2 + " " + mPUINT30 + " " + mPUINT61 + " " + mPIneterval + " " + mSCALE_AUTO + " "
                + mSCALE4;
    }

    // Set all fields to null values (except mBoolean and mDouble2)
    void setNulls() {
        mEnum = null;
        mString = null;
        mCharSequence = null;
        //mBoolean = true;
        mBoolByte = BooleanDataType.NULL;
        mChar = CharDataType.NULL;
        mDateTime = DateTimeDataType.NULL;
        mTimeOfDay = TimeOfDayDataType.NULL;

        mByte = IntegerDataType.INT8_NULL;
        mShort = IntegerDataType.INT16_NULL;
        mInt = IntegerDataType.INT32_NULL;
        mInt48 = IntegerDataType.INT48_NULL;
        mLong = IntegerDataType.INT64_NULL;
        mFloat = FloatDataType.IEEE32_NULL;
        mDouble = FloatDataType.IEEE64_NULL;
        //mDouble2 = FloatDataType.IEEE64_NULL;
        mPUINT30 = IntegerDataType.PUINT30_NULL;
        mPUINT61 = IntegerDataType.PUINT61_NULL;
        mPIneterval = IntegerDataType.PINTERVAL_NULL;
        mSCALE_AUTO = FloatDataType.DECIMAL_NULL;
        mSCALE4 = FloatDataType.DECIMAL_NULL;
    }

    // Set all fields to reasonable non-null values
    void setValues() {
        s1 = "Hi Kolia";
        mString = "IBM";
        mCharSequence = "MSFT";
        mEnum = TestEnum.RED;
        mByte = 1;
        mShort = 2;
        mInt = 3;
        mInt48 = 4;
        mLong = 5;
        mFloat = 63545.34f;
        mDouble = 76456577.76;

        mBoolean = true;
        mBoolByte = BooleanDataType.TRUE;
        mChar = 'C';
        mDateTime = 1235746625319L;
        mTimeOfDay = 56841;

        mPUINT30 = 0x1CCCAAAA;
        mPUINT61 = 0x1CCCAAAA1CCCAAAAL;
        mPIneterval = 60000;
        mSCALE_AUTO = 1.52;
        mSCALE4 = 1.53;
        //mSCALE4 = Double.NaN;
    }

    @SchemaElement
    public String getS1 () {
        return s1;
    }

    public void setS1 (String s1) {
        this.s1 = s1;
    }

    @SchemaType(
            encoding = "PUINT30",
            dataType = SchemaDataType.INTEGER
    )
    @SchemaElement
    public int getmPUINT30 () {
        return mPUINT30;
    }

    public void setmPUINT30 (int mPUINT30) {
        this.mPUINT30 = mPUINT30;
    }

    @SchemaType(
            encoding = "PUINT61",
            dataType = SchemaDataType.INTEGER
    )
    @SchemaElement
    public long getmPUINT61 () {
        return mPUINT61;
    }

    public void setmPUINT61 (long mPUINT61) {
        this.mPUINT61 = mPUINT61;
    }


    @SchemaElement
    @SchemaType(
            encoding = "DECIMAL(4)",
            dataType = SchemaDataType.FLOAT
    )
    public double getmSCALE4 () {
        return mSCALE4;
    }

    public void setmSCALE4 (double mSCALE4) {
        this.mSCALE4 = mSCALE4;
    }

    @SchemaType(
            encoding = "DECIMAL",
            dataType = SchemaDataType.FLOAT
    )
    @SchemaElement
    public double getmSCALE_AUTO () {
        return mSCALE_AUTO;
    }

    public void setmSCALE_AUTO (double mSCALE_AUTO) {
        this.mSCALE_AUTO = mSCALE_AUTO;
    }


    @SchemaElement
    public TestEnum getmEnum () {
        return mEnum;
    }

    public void setmEnum (TestEnum mEnum) {
        this.mEnum = mEnum;
    }


    @SchemaElement
    public String getmString () {
        return mString;
    }

    public void setmString (String mString) {
        this.mString = mString;
    }


    @SchemaElement
    public CharSequence getmCharSequence () {
        return mCharSequence;
    }

    public void setmCharSequence (CharSequence mCharSequence) {
        this.mCharSequence = mCharSequence;
    }


    @SchemaElement
    public char getmChar () {
        return mChar;
    }

    public void setmChar (char mChar) {
        this.mChar = mChar;
    }


    @SchemaElement
    public long getmDateTime () {
        return mDateTime;
    }

    public void setmDateTime (long mDateTime) {
        this.mDateTime = mDateTime;
    }


    @SchemaElement
    public int getmTimeOfDay () {
        return mTimeOfDay;
    }

    public void setmTimeOfDay (int mTimeOfDay) {
        this.mTimeOfDay = mTimeOfDay;
    }


    @SchemaElement
    public byte getmByte () {
        return mByte;
    }

    public void setmByte (byte mByte) {
        this.mByte = mByte;
    }


    @SchemaElement
    public short getmShort () {
        return mShort;
    }

    public void setmShort (short mShort) {
        this.mShort = mShort;
    }


    @SchemaElement
    public int getmInt () {
        return mInt;
    }

    public void setmInt (int mInt) {
        this.mInt = mInt;
    }

    @SchemaType(
            encoding = "INT48",
            dataType = SchemaDataType.INTEGER
    )
    @SchemaElement
    public long getmInt48 () {
        return mInt48;
    }

    public void setmInt48 (long mInt48) {
        this.mInt48 = mInt48;
    }


    @SchemaElement
    public long getmLong () {
        return mLong;
    }

    public void setmLong (long mLong) {
        this.mLong = mLong;
    }


    @SchemaElement
    public float getmFloat () {
        return mFloat;
    }

    public void setmFloat (float mFloat) {
        this.mFloat = mFloat;
    }


    @SchemaElement
    public double getmDouble () {
        return mDouble;
    }

    public void setmDouble (double mDouble) {
        this.mDouble = mDouble;
    }

    @RelativeTo("mDouble")
    @SchemaElement
    public double getmDouble2 () {
        return mDouble2;
    }

    public void setmDouble2 (double mDouble2) {
        this.mDouble2 = mDouble2;
    }

    @SchemaType(
            encoding = "PINTERVAL",
            dataType = SchemaDataType.INTEGER
    )
    @SchemaElement
    public int getmPIneterval () {
        return mPIneterval;
    }

    public void setmPIneterval (int mPIneterval) {
        this.mPIneterval = mPIneterval;
    }

}
