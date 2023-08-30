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
package com.epam.deltix.test.qsrv.hf.pub;

import com.epam.deltix.qsrv.hf.pub.md.*;
import com.epam.deltix.qsrv.test.messages.TestEnum;
import com.epam.deltix.timebase.messages.*;

import java.util.List;

/**
 * Date: Mar 14, 2011
 * @author BazylevD
 */
public class MsgClassAllPublic {
    public String s1;

    public TestEnum mEnum;
    public String mString;
    public CharSequence mCharSequence;
    public boolean mBoolean; // unnullable case
    @SchemaType(
            dataType = SchemaDataType.BOOLEAN
    )
    public byte mBoolByte; // nullable case
    public char mChar;
    public long mDateTime;
    public int mTimeOfDay;

    public byte mByte;
    public short mShort;
    public int mInt;
    @SchemaType(
            encoding = "INT48",
            dataType = SchemaDataType.INTEGER
    )
    public long mInt48;
    public long mLong;
    public float mFloat;
    public double mDouble;
    @RelativeTo("mDouble")
    public double mDouble2;

    @SchemaType(
            encoding = "PUINT30",
            dataType = SchemaDataType.INTEGER
    )
    public int mPUINT30;
    @SchemaType(
            encoding = "PUINT61",
            dataType = SchemaDataType.INTEGER
    )
    public long mPUINT61;
    @SchemaType(
            encoding = "PINTERVAL",
            dataType = SchemaDataType.INTEGER
    )
    public int mPIneterval;
    @SchemaType(
            encoding = "DECIMAL",
            dataType = SchemaDataType.FLOAT
    )
    public double mSCALE_AUTO;
    @SchemaType(
            encoding = "DECIMAL(4)",
            dataType = SchemaDataType.FLOAT
    )
    public double mSCALE4;

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
        mEnum = TestEnum.ONE;
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

    static void setValues(List<Object> values) {
        values.add(null); //mBoolByte // BooleanDataType.TRUE  assert validation code doesn't support it
        values.add(true);   //mBoolean
        values.add(1);  //mByte
        values.add('C');  //mChar
        values.add("MSFT");         //mCharSequence
        values.add(1235746625319L); //mDateTime
        values.add(76456577.76);  //mDouble
        values.add(null); //mDouble2  // FloatDataType.IEEE64_NULL assert validation code doesn't support it
        values.add(TestEnum.ONE.toString());  //mEnum
        values.add(63545.34f);  //mFloat
        values.add(3);  //mInt
        values.add(4L);  //mInt48
        values.add(5L);  //mLong
        values.add(60000);    //mPIneterval
        values.add(0x1CCCAAAA);  //mPUINT30
        values.add(0x1CCCAAAA1CCCAAAAL);  //mPUINT61
        values.add(1.53);   //mSCALE4
        values.add(1.52);    //mSCALE_AUTO
        values.add(2);   //mShort
        values.add("IBM");          //mString
        values.add(56841);       //mTimeOfDay
        values.add("Hi Kolia");     //s1
    }

    static void setNullValues(List<Object> values) {
        for (int i = 0; i < 22; i++) {
            values.add(null);
        }
        values.set(1, true); // mBoolean field is not nullable
    }
}