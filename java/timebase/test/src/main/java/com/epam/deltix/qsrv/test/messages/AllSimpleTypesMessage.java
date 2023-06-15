/*
 * Copyright 2021 EPAM Systems, Inc
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
package com.epam.deltix.qsrv.test.messages;

import com.epam.deltix.containers.BinaryArray;
import com.epam.deltix.containers.BinaryAsciiString;
import com.epam.deltix.containers.CharSequenceUtils;
import com.epam.deltix.containers.MutableString;
import com.epam.deltix.containers.interfaces.BinaryArrayReadOnly;
import com.epam.deltix.containers.interfaces.BinaryArrayReadWrite;
import com.epam.deltix.dfp.Decimal;
import com.epam.deltix.dfp.Decimal64Utils;
import com.epam.deltix.timebase.messages.InstrumentMessage;
import com.epam.deltix.timebase.messages.*;
import com.epam.deltix.timebase.messages.TypeConstants;

/**
 */
public class AllSimpleTypesMessage extends InstrumentMessage implements AllSimpleTypesMessageInterface {
  public static final String CLASS_NAME = AllSimpleTypesMessage.class.getName();

  /**
   */
  protected byte boolField = TypeConstants.INT8_NULL;

  /**
   */
  protected byte boolNullableField = TypeConstants.INT8_NULL;

  /**
   */
  protected BinaryArrayReadOnly binaryField = null;

  /**
   */
  protected BinaryArrayReadOnly binaryNullableField = null;

  /**
   */
  protected byte byteField = TypeConstants.INT8_NULL;

  /**
   */
  protected byte byteNullableField = TypeConstants.INT8_NULL;

  /**
   */
  protected short shortField = TypeConstants.INT16_NULL;

  /**
   */
  protected short shortNullableField = TypeConstants.INT16_NULL;

  /**
   */
  protected int intField = TypeConstants.INT32_NULL;

  /**
   */
  protected int intNullableField = TypeConstants.INT32_NULL;

  /**
   */
  protected long longField = TypeConstants.INT64_NULL;

  /**
   */
  protected long longNullableField = TypeConstants.INT64_NULL;

  /**
   */
  protected float floatField = TypeConstants.IEEE32_NULL;

  /**
   */
  protected float floatNullableField = TypeConstants.IEEE32_NULL;

  /**
   */
  protected double doubleField = TypeConstants.IEEE64_NULL;

  /**
   */
  protected double doubleNullableField = TypeConstants.IEEE64_NULL;

  /**
   */
  @Decimal
  protected long decimalField = TypeConstants.DECIMAL_NULL;

  /**
   */
  @Decimal
  protected long decimalNullableField = TypeConstants.DECIMAL_NULL;

  /**
   */
  protected long textAlphaNumericField = TypeConstants.INT64_NULL;

  /**
   */
  protected long textAlphaNumericNullableField = TypeConstants.INT64_NULL;

  /**
   */
  protected CharSequence textField = null;

  /**
   */
  protected CharSequence textNullableField = null;

  /**
   */
  protected CharSequence asciiTextField = null;

  /**
   */
  protected CharSequence asciiTextNullableField = null;

  /**
   */
  protected int timeOfDayField = TypeConstants.INT32_NULL;

  /**
   */
  protected int timeOfDayNullableField = TypeConstants.INT32_NULL;

  /**
   */
  protected long timestampField = TypeConstants.INT64_NULL;

  /**
   */
  protected long timestampNullableField = TypeConstants.INT64_NULL;

  /**
   */
  protected TestEnum enumField = null;

  /**
   */
  protected TestEnum enumNullableField = null;

  /**
   * @return Bool Field
   */
  @SchemaElement
  @SchemaType(
      isNullable = false,
      dataType = SchemaDataType.BOOLEAN
  )
  public byte getBoolField() {
    return boolField;
  }

  /**
   * @param value - Bool Field
   */
  public void setBoolField(byte value) {
    this.boolField = value;
  }

  /**
   * @return true if Bool Field is not null
   */
  public boolean hasBoolField() {
    return boolField != TypeConstants.INT8_NULL;
  }

  /**
   */
  public void nullifyBoolField() {
    this.boolField = TypeConstants.INT8_NULL;
  }

  /**
   * @return Bool Nullable Field
   */
  @SchemaElement
  @SchemaType(
      isNullable = true,
      dataType = SchemaDataType.BOOLEAN
  )
  public byte getBoolNullableField() {
    return boolNullableField;
  }

  /**
   * @param value - Bool Nullable Field
   */
  public void setBoolNullableField(byte value) {
    this.boolNullableField = value;
  }

  /**
   * @return true if Bool Nullable Field is not null
   */
  public boolean hasBoolNullableField() {
    return boolNullableField != TypeConstants.INT8_NULL;
  }

  /**
   */
  public void nullifyBoolNullableField() {
    this.boolNullableField = TypeConstants.INT8_NULL;
  }

  /**
   * @return Binary Field
   */
  @SchemaElement
  @SchemaType(
      isNullable = false
  )
  public BinaryArrayReadOnly getBinaryField() {
    return binaryField;
  }

  /**
   * @param value - Binary Field
   */
  public void setBinaryField(BinaryArrayReadOnly value) {
    this.binaryField = value;
  }

  /**
   * @return true if Binary Field is not null
   */
  public boolean hasBinaryField() {
    return binaryField != null;
  }

  /**
   */
  public void nullifyBinaryField() {
    this.binaryField = null;
  }

  /**
   * @return Binary Nullable Field
   */
  @SchemaElement
  public BinaryArrayReadOnly getBinaryNullableField() {
    return binaryNullableField;
  }

  /**
   * @param value - Binary Nullable Field
   */
  public void setBinaryNullableField(BinaryArrayReadOnly value) {
    this.binaryNullableField = value;
  }

  /**
   * @return true if Binary Nullable Field is not null
   */
  public boolean hasBinaryNullableField() {
    return binaryNullableField != null;
  }

  /**
   */
  public void nullifyBinaryNullableField() {
    this.binaryNullableField = null;
  }

  /**
   * @return Byte Field
   */
  @SchemaElement
  @SchemaType(
      isNullable = false
  )
  public byte getByteField() {
    return byteField;
  }

  /**
   * @param value - Byte Field
   */
  public void setByteField(byte value) {
    this.byteField = value;
  }

  /**
   * @return true if Byte Field is not null
   */
  public boolean hasByteField() {
    return byteField != TypeConstants.INT8_NULL;
  }

  /**
   */
  public void nullifyByteField() {
    this.byteField = TypeConstants.INT8_NULL;
  }

  /**
   * @return Byte Nullable Field
   */
  @SchemaElement
  @SchemaType(
      isNullable = true
  )
  public byte getByteNullableField() {
    return byteNullableField;
  }

  /**
   * @param value - Byte Nullable Field
   */
  public void setByteNullableField(byte value) {
    this.byteNullableField = value;
  }

  /**
   * @return true if Byte Nullable Field is not null
   */
  public boolean hasByteNullableField() {
    return byteNullableField != TypeConstants.INT8_NULL;
  }

  /**
   */
  public void nullifyByteNullableField() {
    this.byteNullableField = TypeConstants.INT8_NULL;
  }

  /**
   * @return Short Field
   */
  @SchemaElement
  @SchemaType(
      isNullable = false
  )
  public short getShortField() {
    return shortField;
  }

  /**
   * @param value - Short Field
   */
  public void setShortField(short value) {
    this.shortField = value;
  }

  /**
   * @return true if Short Field is not null
   */
  public boolean hasShortField() {
    return shortField != TypeConstants.INT16_NULL;
  }

  /**
   */
  public void nullifyShortField() {
    this.shortField = TypeConstants.INT16_NULL;
  }

  /**
   * @return Short Nullable Field
   */
  @SchemaElement
  @SchemaType(
      isNullable = true
  )
  public short getShortNullableField() {
    return shortNullableField;
  }

  /**
   * @param value - Short Nullable Field
   */
  public void setShortNullableField(short value) {
    this.shortNullableField = value;
  }

  /**
   * @return true if Short Nullable Field is not null
   */
  public boolean hasShortNullableField() {
    return shortNullableField != TypeConstants.INT16_NULL;
  }

  /**
   */
  public void nullifyShortNullableField() {
    this.shortNullableField = TypeConstants.INT16_NULL;
  }

  /**
   * @return Int Field
   */
  @SchemaElement
  @SchemaType(
      isNullable = false
  )
  public int getIntField() {
    return intField;
  }

  /**
   * @param value - Int Field
   */
  public void setIntField(int value) {
    this.intField = value;
  }

  /**
   * @return true if Int Field is not null
   */
  public boolean hasIntField() {
    return intField != TypeConstants.INT32_NULL;
  }

  /**
   */
  public void nullifyIntField() {
    this.intField = TypeConstants.INT32_NULL;
  }

  /**
   * @return Int Nullable Field
   */
  @SchemaElement
  @SchemaType(
      isNullable = true
  )
  public int getIntNullableField() {
    return intNullableField;
  }

  /**
   * @param value - Int Nullable Field
   */
  public void setIntNullableField(int value) {
    this.intNullableField = value;
  }

  /**
   * @return true if Int Nullable Field is not null
   */
  public boolean hasIntNullableField() {
    return intNullableField != TypeConstants.INT32_NULL;
  }

  /**
   */
  public void nullifyIntNullableField() {
    this.intNullableField = TypeConstants.INT32_NULL;
  }

  /**
   * @return Long Field
   */
  @SchemaElement
  @SchemaType(
      isNullable = false
  )
  public long getLongField() {
    return longField;
  }

  /**
   * @param value - Long Field
   */
  public void setLongField(long value) {
    this.longField = value;
  }

  /**
   * @return true if Long Field is not null
   */
  public boolean hasLongField() {
    return longField != TypeConstants.INT64_NULL;
  }

  /**
   */
  public void nullifyLongField() {
    this.longField = TypeConstants.INT64_NULL;
  }

  /**
   * @return Long Nullable Field
   */
  @SchemaElement
  @SchemaType(
      isNullable = true
  )
  public long getLongNullableField() {
    return longNullableField;
  }

  /**
   * @param value - Long Nullable Field
   */
  public void setLongNullableField(long value) {
    this.longNullableField = value;
  }

  /**
   * @return true if Long Nullable Field is not null
   */
  public boolean hasLongNullableField() {
    return longNullableField != TypeConstants.INT64_NULL;
  }

  /**
   */
  public void nullifyLongNullableField() {
    this.longNullableField = TypeConstants.INT64_NULL;
  }

  /**
   * @return Float Field
   */
  @SchemaElement
  @SchemaType(
      isNullable = false
  )
  public float getFloatField() {
    return floatField;
  }

  /**
   * @param value - Float Field
   */
  public void setFloatField(float value) {
    this.floatField = value;
  }

  /**
   * @return true if Float Field is not null
   */
  public boolean hasFloatField() {
    return !Float.isNaN(floatField);
  }

  /**
   */
  public void nullifyFloatField() {
    this.floatField = TypeConstants.IEEE32_NULL;
  }

  /**
   * @return Float Nullable Field
   */
  @SchemaElement
  @SchemaType(
      isNullable = true
  )
  public float getFloatNullableField() {
    return floatNullableField;
  }

  /**
   * @param value - Float Nullable Field
   */
  public void setFloatNullableField(float value) {
    this.floatNullableField = value;
  }

  /**
   * @return true if Float Nullable Field is not null
   */
  public boolean hasFloatNullableField() {
    return !Float.isNaN(floatNullableField);
  }

  /**
   */
  public void nullifyFloatNullableField() {
    this.floatNullableField = TypeConstants.IEEE32_NULL;
  }

  /**
   * @return Double Field
   */
  @SchemaElement
  @SchemaType(
      isNullable = false
  )
  public double getDoubleField() {
    return doubleField;
  }

  /**
   * @param value - Double Field
   */
  public void setDoubleField(double value) {
    this.doubleField = value;
  }

  /**
   * @return true if Double Field is not null
   */
  public boolean hasDoubleField() {
    return !Double.isNaN(doubleField);
  }

  /**
   */
  public void nullifyDoubleField() {
    this.doubleField = TypeConstants.IEEE64_NULL;
  }

  /**
   * @return Double Nullable Field
   */
  @SchemaElement
  @SchemaType(
      isNullable = true
  )
  public double getDoubleNullableField() {
    return doubleNullableField;
  }

  /**
   * @param value - Double Nullable Field
   */
  public void setDoubleNullableField(double value) {
    this.doubleNullableField = value;
  }

  /**
   * @return true if Double Nullable Field is not null
   */
  public boolean hasDoubleNullableField() {
    return !Double.isNaN(doubleNullableField);
  }

  /**
   */
  public void nullifyDoubleNullableField() {
    this.doubleNullableField = TypeConstants.IEEE64_NULL;
  }

  /**
   * @return Decimal Field
   */
  @Decimal
  @SchemaElement
  @SchemaType(
      encoding = "DECIMAL64",
      isNullable = false,
      dataType = SchemaDataType.FLOAT
  )
  public long getDecimalField() {
    return decimalField;
  }

  /**
   * @param value - Decimal Field
   */
  public void setDecimalField(@Decimal long value) {
    this.decimalField = value;
  }

  /**
   * @return true if Decimal Field is not null
   */
  public boolean hasDecimalField() {
    return decimalField != TypeConstants.DECIMAL_NULL;
  }

  /**
   */
  public void nullifyDecimalField() {
    this.decimalField = TypeConstants.DECIMAL_NULL;
  }

  /**
   * @return Decimal Nullable Field
   */
  @Decimal
  @SchemaElement
  @SchemaType(
      encoding = "DECIMAL64",
      isNullable = true,
      dataType = SchemaDataType.FLOAT
  )
  public long getDecimalNullableField() {
    return decimalNullableField;
  }

  /**
   * @param value - Decimal Nullable Field
   */
  public void setDecimalNullableField(@Decimal long value) {
    this.decimalNullableField = value;
  }

  /**
   * @return true if Decimal Nullable Field is not null
   */
  public boolean hasDecimalNullableField() {
    return decimalNullableField != TypeConstants.DECIMAL_NULL;
  }

  /**
   */
  public void nullifyDecimalNullableField() {
    this.decimalNullableField = TypeConstants.DECIMAL_NULL;
  }

  /**
   * @return Text Alpha Numeric Field
   */
  @SchemaElement
  @SchemaType(
      encoding = "ALPHANUMERIC(10)",
      dataType = SchemaDataType.VARCHAR
  )
  public long getTextAlphaNumericField() {
    return textAlphaNumericField;
  }

  /**
   * @param value - Text Alpha Numeric Field
   */
  public void setTextAlphaNumericField(long value) {
    this.textAlphaNumericField = value;
  }

  /**
   * @return true if Text Alpha Numeric Field is not null
   */
  public boolean hasTextAlphaNumericField() {
    return textAlphaNumericField != TypeConstants.INT64_NULL;
  }

  /**
   */
  public void nullifyTextAlphaNumericField() {
    this.textAlphaNumericField = TypeConstants.INT64_NULL;
  }

  /**
   * @return Text Alpha Numeric Nullable Field
   */
  @SchemaElement
  @SchemaType(
      encoding = "ALPHANUMERIC(10)",
      isNullable = true,
      dataType = SchemaDataType.VARCHAR
  )
  public long getTextAlphaNumericNullableField() {
    return textAlphaNumericNullableField;
  }

  /**
   * @param value - Text Alpha Numeric Nullable Field
   */
  public void setTextAlphaNumericNullableField(long value) {
    this.textAlphaNumericNullableField = value;
  }

  /**
   * @return true if Text Alpha Numeric Nullable Field is not null
   */
  public boolean hasTextAlphaNumericNullableField() {
    return textAlphaNumericNullableField != TypeConstants.INT64_NULL;
  }

  /**
   */
  public void nullifyTextAlphaNumericNullableField() {
    this.textAlphaNumericNullableField = TypeConstants.INT64_NULL;
  }

  /**
   * @return Text Field
   */
  @SchemaElement
  @SchemaType(
      isNullable = false
  )
  public CharSequence getTextField() {
    return textField;
  }

  /**
   * @param value - Text Field
   */
  public void setTextField(CharSequence value) {
    this.textField = value;
  }

  /**
   * @return true if Text Field is not null
   */
  public boolean hasTextField() {
    return textField != null;
  }

  /**
   */
  public void nullifyTextField() {
    this.textField = null;
  }

  /**
   * @return Text Nullable Field
   */
  @SchemaElement
  @SchemaType(
      isNullable = true
  )
  public CharSequence getTextNullableField() {
    return textNullableField;
  }

  /**
   * @param value - Text Nullable Field
   */
  public void setTextNullableField(CharSequence value) {
    this.textNullableField = value;
  }

  /**
   * @return true if Text Nullable Field is not null
   */
  public boolean hasTextNullableField() {
    return textNullableField != null;
  }

  /**
   */
  public void nullifyTextNullableField() {
    this.textNullableField = null;
  }

  /**
   * @return Ascii Text Field
   */
  @SchemaElement
  @SchemaType(
      isNullable = false
  )
  public CharSequence getAsciiTextField() {
    return asciiTextField;
  }

  /**
   * @param value - Ascii Text Field
   */
  public void setAsciiTextField(CharSequence value) {
    this.asciiTextField = value;
  }

  /**
   * @return true if Ascii Text Field is not null
   */
  public boolean hasAsciiTextField() {
    return asciiTextField != null;
  }

  /**
   */
  public void nullifyAsciiTextField() {
    this.asciiTextField = null;
  }

  /**
   * @return Ascii Text Nullable Field
   */
  @SchemaElement
  @SchemaType(
      isNullable = true
  )
  public CharSequence getAsciiTextNullableField() {
    return asciiTextNullableField;
  }

  /**
   * @param value - Ascii Text Nullable Field
   */
  public void setAsciiTextNullableField(CharSequence value) {
    this.asciiTextNullableField = value;
  }

  /**
   * @return true if Ascii Text Nullable Field is not null
   */
  public boolean hasAsciiTextNullableField() {
    return asciiTextNullableField != null;
  }

  /**
   */
  public void nullifyAsciiTextNullableField() {
    this.asciiTextNullableField = null;
  }

  /**
   * @return Time Of Day Field
   */
  @SchemaElement
  @SchemaType(
      isNullable = false,
      dataType = SchemaDataType.TIME_OF_DAY
  )
  public int getTimeOfDayField() {
    return timeOfDayField;
  }

  /**
   * @param value - Time Of Day Field
   */
  public void setTimeOfDayField(int value) {
    this.timeOfDayField = value;
  }

  /**
   * @return true if Time Of Day Field is not null
   */
  public boolean hasTimeOfDayField() {
    return timeOfDayField != TypeConstants.INT32_NULL;
  }

  /**
   */
  public void nullifyTimeOfDayField() {
    this.timeOfDayField = TypeConstants.INT32_NULL;
  }

  /**
   * @return Time Of Day Nullable Field
   */
  @SchemaElement
  @SchemaType(
      isNullable = true,
      dataType = SchemaDataType.TIME_OF_DAY
  )
  public int getTimeOfDayNullableField() {
    return timeOfDayNullableField;
  }

  /**
   * @param value - Time Of Day Nullable Field
   */
  public void setTimeOfDayNullableField(int value) {
    this.timeOfDayNullableField = value;
  }

  /**
   * @return true if Time Of Day Nullable Field is not null
   */
  public boolean hasTimeOfDayNullableField() {
    return timeOfDayNullableField != TypeConstants.INT32_NULL;
  }

  /**
   */
  public void nullifyTimeOfDayNullableField() {
    this.timeOfDayNullableField = TypeConstants.INT32_NULL;
  }

  /**
   * @return Timestamp Field
   */
  @SchemaElement
  @SchemaType(
      isNullable = false,
      dataType = SchemaDataType.TIMESTAMP
  )
  public long getTimestampField() {
    return timestampField;
  }

  /**
   * @param value - Timestamp Field
   */
  public void setTimestampField(long value) {
    this.timestampField = value;
  }

  /**
   * @return true if Timestamp Field is not null
   */
  public boolean hasTimestampField() {
    return timestampField != TypeConstants.INT64_NULL;
  }

  /**
   */
  public void nullifyTimestampField() {
    this.timestampField = TypeConstants.INT64_NULL;
  }

  /**
   * @return Timestamp Nullable Field
   */
  @SchemaElement
  @SchemaType(
      isNullable = true,
      dataType = SchemaDataType.TIMESTAMP
  )
  public long getTimestampNullableField() {
    return timestampNullableField;
  }

  /**
   * @param value - Timestamp Nullable Field
   */
  public void setTimestampNullableField(long value) {
    this.timestampNullableField = value;
  }

  /**
   * @return true if Timestamp Nullable Field is not null
   */
  public boolean hasTimestampNullableField() {
    return timestampNullableField != TypeConstants.INT64_NULL;
  }

  /**
   */
  public void nullifyTimestampNullableField() {
    this.timestampNullableField = TypeConstants.INT64_NULL;
  }

  /**
   * @return Enum Field
   */
  @SchemaElement
  @SchemaType(
      isNullable = false
  )
  public TestEnum getEnumField() {
    return enumField;
  }

  /**
   * @param value - Enum Field
   */
  public void setEnumField(TestEnum value) {
    this.enumField = value;
  }

  /**
   * @return true if Enum Field is not null
   */
  public boolean hasEnumField() {
    return enumField != null;
  }

  /**
   */
  public void nullifyEnumField() {
    this.enumField = null;
  }

  /**
   * @return Enum Nullable Field
   */
  @SchemaElement
  @SchemaType(
      isNullable = true
  )
  public TestEnum getEnumNullableField() {
    return enumNullableField;
  }

  /**
   * @param value - Enum Nullable Field
   */
  public void setEnumNullableField(TestEnum value) {
    this.enumNullableField = value;
  }

  /**
   * @return true if Enum Nullable Field is not null
   */
  public boolean hasEnumNullableField() {
    return enumNullableField != null;
  }

  /**
   */
  public void nullifyEnumNullableField() {
    this.enumNullableField = null;
  }

  /**
   * Creates new instance of this class.
   * @return new instance of this class.
   */
  @Override
  protected AllSimpleTypesMessage createInstance() {
    return new AllSimpleTypesMessage();
  }

  /**
   * Method nullifies all instance properties
   */
  @Override
  public AllSimpleTypesMessage nullify() {
    super.nullify();
    nullifyBoolField();
    nullifyBoolNullableField();
    nullifyBinaryField();
    nullifyBinaryNullableField();
    nullifyByteField();
    nullifyByteNullableField();
    nullifyShortField();
    nullifyShortNullableField();
    nullifyIntField();
    nullifyIntNullableField();
    nullifyLongField();
    nullifyLongNullableField();
    nullifyFloatField();
    nullifyFloatNullableField();
    nullifyDoubleField();
    nullifyDoubleNullableField();
    nullifyDecimalField();
    nullifyDecimalNullableField();
    nullifyTextAlphaNumericField();
    nullifyTextAlphaNumericNullableField();
    nullifyTextField();
    nullifyTextNullableField();
    nullifyAsciiTextField();
    nullifyAsciiTextNullableField();
    nullifyTimeOfDayField();
    nullifyTimeOfDayNullableField();
    nullifyTimestampField();
    nullifyTimestampNullableField();
    nullifyEnumField();
    nullifyEnumNullableField();
    return this;
  }

  /**
   * Resets all instance properties to their default values
   */
  @Override
  public AllSimpleTypesMessage reset() {
    super.reset();
    boolField = TypeConstants.INT8_NULL;
    boolNullableField = TypeConstants.INT8_NULL;
    binaryField = null;
    binaryNullableField = null;
    byteField = TypeConstants.INT8_NULL;
    byteNullableField = TypeConstants.INT8_NULL;
    shortField = TypeConstants.INT16_NULL;
    shortNullableField = TypeConstants.INT16_NULL;
    intField = TypeConstants.INT32_NULL;
    intNullableField = TypeConstants.INT32_NULL;
    longField = TypeConstants.INT64_NULL;
    longNullableField = TypeConstants.INT64_NULL;
    floatField = TypeConstants.IEEE32_NULL;
    floatNullableField = TypeConstants.IEEE32_NULL;
    doubleField = TypeConstants.IEEE64_NULL;
    doubleNullableField = TypeConstants.IEEE64_NULL;
    decimalField = TypeConstants.DECIMAL_NULL;
    decimalNullableField = TypeConstants.DECIMAL_NULL;
    textAlphaNumericField = TypeConstants.INT64_NULL;
    textAlphaNumericNullableField = TypeConstants.INT64_NULL;
    textField = null;
    textNullableField = null;
    asciiTextField = null;
    asciiTextNullableField = null;
    timeOfDayField = TypeConstants.INT32_NULL;
    timeOfDayNullableField = TypeConstants.INT32_NULL;
    timestampField = TypeConstants.INT64_NULL;
    timestampNullableField = TypeConstants.INT64_NULL;
    enumField = null;
    enumNullableField = null;
    return this;
  }

  /**
   * Method copies state to a given instance
   */
  @Override
  public AllSimpleTypesMessage clone() {
    AllSimpleTypesMessage t = createInstance();
    t.copyFrom(this);
    return t;
  }

  /**
   * Indicates whether some other object is "equal to" this one.
   */
  @Override
  public boolean equals(Object obj) {
    if (this == obj) return true;
    boolean superEquals = super.equals(obj);
    if (!superEquals) return false;
    if (!(obj instanceof AllSimpleTypesMessageInfo)) return false;
    AllSimpleTypesMessageInfo other =(AllSimpleTypesMessageInfo)obj;
    if (hasBoolField() != other.hasBoolField()) return false;
    if (hasBoolField() && getBoolField() != other.getBoolField()) return false;
    if (hasBoolNullableField() != other.hasBoolNullableField()) return false;
    if (hasBoolNullableField() && getBoolNullableField() != other.getBoolNullableField()) return false;
    if (hasBinaryField() != other.hasBinaryField()) return false;
    if (hasBinaryField()) {
      if (getBinaryField().size() != other.getBinaryField().size()) return false; else {
        BinaryArrayReadOnly b1 = getBinaryField();
        BinaryArrayReadOnly b2 = other.getBinaryField();
        if ((b1 instanceof BinaryArray && b2 instanceof BinaryArray)) {
          if (!b1.equals(b2)) return false;
        } else {
          for (int i = 0; i < b1.size(); ++i) if (b1.get(i) != b2.get(i)) return false;
        }
      }
    }
    if (hasBinaryNullableField() != other.hasBinaryNullableField()) return false;
    if (hasBinaryNullableField()) {
      if (getBinaryNullableField().size() != other.getBinaryNullableField().size()) return false; else {
        BinaryArrayReadOnly b1 = getBinaryNullableField();
        BinaryArrayReadOnly b2 = other.getBinaryNullableField();
        if ((b1 instanceof BinaryArray && b2 instanceof BinaryArray)) {
          if (!b1.equals(b2)) return false;
        } else {
          for (int i = 0; i < b1.size(); ++i) if (b1.get(i) != b2.get(i)) return false;
        }
      }
    }
    if (hasByteField() != other.hasByteField()) return false;
    if (hasByteField() && getByteField() != other.getByteField()) return false;
    if (hasByteNullableField() != other.hasByteNullableField()) return false;
    if (hasByteNullableField() && getByteNullableField() != other.getByteNullableField()) return false;
    if (hasShortField() != other.hasShortField()) return false;
    if (hasShortField() && getShortField() != other.getShortField()) return false;
    if (hasShortNullableField() != other.hasShortNullableField()) return false;
    if (hasShortNullableField() && getShortNullableField() != other.getShortNullableField()) return false;
    if (hasIntField() != other.hasIntField()) return false;
    if (hasIntField() && getIntField() != other.getIntField()) return false;
    if (hasIntNullableField() != other.hasIntNullableField()) return false;
    if (hasIntNullableField() && getIntNullableField() != other.getIntNullableField()) return false;
    if (hasLongField() != other.hasLongField()) return false;
    if (hasLongField() && getLongField() != other.getLongField()) return false;
    if (hasLongNullableField() != other.hasLongNullableField()) return false;
    if (hasLongNullableField() && getLongNullableField() != other.getLongNullableField()) return false;
    if (hasFloatField() != other.hasFloatField()) return false;
    if (hasFloatNullableField() != other.hasFloatNullableField()) return false;
    if (hasDoubleField() != other.hasDoubleField()) return false;
    if (hasDoubleField() && getDoubleField() != other.getDoubleField()) return false;
    if (hasDoubleNullableField() != other.hasDoubleNullableField()) return false;
    if (hasDoubleNullableField() && getDoubleNullableField() != other.getDoubleNullableField()) return false;
    if (hasDecimalField() != other.hasDecimalField()) return false;
    if (hasDecimalField() && !Decimal64Utils.equals(getDecimalField(), other.getDecimalField())) return false;
    if (hasDecimalNullableField() != other.hasDecimalNullableField()) return false;
    if (hasDecimalNullableField() && !Decimal64Utils.equals(getDecimalNullableField(), other.getDecimalNullableField())) return false;
    if (hasTextAlphaNumericField() != other.hasTextAlphaNumericField()) return false;
    if (hasTextAlphaNumericField() && getTextAlphaNumericField() != other.getTextAlphaNumericField()) return false;
    if (hasTextAlphaNumericNullableField() != other.hasTextAlphaNumericNullableField()) return false;
    if (hasTextAlphaNumericNullableField() && getTextAlphaNumericNullableField() != other.getTextAlphaNumericNullableField()) return false;
    if (hasTextField() != other.hasTextField()) return false;
    if (hasTextField()) {
      if (getTextField().length() != other.getTextField().length()) return false; else {
        CharSequence s1 = getTextField();
        CharSequence s2 = other.getTextField();
        if ((s1 instanceof MutableString && s2 instanceof MutableString) || (s1 instanceof String && s2 instanceof String) || (s1 instanceof BinaryAsciiString && s2 instanceof BinaryAsciiString)) {
          if (!s1.equals(s2)) return false;
        } else {
          if (!CharSequenceUtils.equals(s1, s2)) return false;
        }
      }
    }
    if (hasTextNullableField() != other.hasTextNullableField()) return false;
    if (hasTextNullableField()) {
      if (getTextNullableField().length() != other.getTextNullableField().length()) return false; else {
        CharSequence s1 = getTextNullableField();
        CharSequence s2 = other.getTextNullableField();
        if ((s1 instanceof MutableString && s2 instanceof MutableString) || (s1 instanceof String && s2 instanceof String) || (s1 instanceof BinaryAsciiString && s2 instanceof BinaryAsciiString)) {
          if (!s1.equals(s2)) return false;
        } else {
          if (!CharSequenceUtils.equals(s1, s2)) return false;
        }
      }
    }
    if (hasAsciiTextField() != other.hasAsciiTextField()) return false;
    if (hasAsciiTextField()) {
      if (getAsciiTextField().length() != other.getAsciiTextField().length()) return false; else {
        CharSequence s1 = getAsciiTextField();
        CharSequence s2 = other.getAsciiTextField();
        if ((s1 instanceof MutableString && s2 instanceof MutableString) || (s1 instanceof String && s2 instanceof String) || (s1 instanceof BinaryAsciiString && s2 instanceof BinaryAsciiString)) {
          if (!s1.equals(s2)) return false;
        } else {
          if (!CharSequenceUtils.equals(s1, s2)) return false;
        }
      }
    }
    if (hasAsciiTextNullableField() != other.hasAsciiTextNullableField()) return false;
    if (hasAsciiTextNullableField()) {
      if (getAsciiTextNullableField().length() != other.getAsciiTextNullableField().length()) return false; else {
        CharSequence s1 = getAsciiTextNullableField();
        CharSequence s2 = other.getAsciiTextNullableField();
        if ((s1 instanceof MutableString && s2 instanceof MutableString) || (s1 instanceof String && s2 instanceof String) || (s1 instanceof BinaryAsciiString && s2 instanceof BinaryAsciiString)) {
          if (!s1.equals(s2)) return false;
        } else {
          if (!CharSequenceUtils.equals(s1, s2)) return false;
        }
      }
    }
    if (hasTimeOfDayField() != other.hasTimeOfDayField()) return false;
    if (hasTimeOfDayField() && getTimeOfDayField() != other.getTimeOfDayField()) return false;
    if (hasTimeOfDayNullableField() != other.hasTimeOfDayNullableField()) return false;
    if (hasTimeOfDayNullableField() && getTimeOfDayNullableField() != other.getTimeOfDayNullableField()) return false;
    if (hasTimestampField() != other.hasTimestampField()) return false;
    if (hasTimestampField() && getTimestampField() != other.getTimestampField()) return false;
    if (hasTimestampNullableField() != other.hasTimestampNullableField()) return false;
    if (hasTimestampNullableField() && getTimestampNullableField() != other.getTimestampNullableField()) return false;
    if (hasEnumField() != other.hasEnumField()) return false;
    if (hasEnumField() && getEnumField() != other.getEnumField()) return false;
    if (hasEnumNullableField() != other.hasEnumNullableField()) return false;
    if (hasEnumNullableField() && getEnumNullableField() != other.getEnumNullableField()) return false;
    return true;
  }

  /**
   * Returns a hash code value for the object. This method is * supported for the benefit of hash tables such as those provided by.
   */
  @Override
  public int hashCode() {
    int hash = super.hashCode();
    if (hasBoolField()) {
      hash = hash * 31 + ((int)getBoolField());
    }
    if (hasBoolNullableField()) {
      hash = hash * 31 + ((int)getBoolNullableField());
    }
    if (hasBinaryField()) {
      hash = hash * 31 + getBinaryField().hashCode();
    }
    if (hasBinaryNullableField()) {
      hash = hash * 31 + getBinaryNullableField().hashCode();
    }
    if (hasByteField()) {
      hash = hash * 31 + ((int)getByteField());
    }
    if (hasByteNullableField()) {
      hash = hash * 31 + ((int)getByteNullableField());
    }
    if (hasShortField()) {
      hash = hash * 31 + ((int)getShortField());
    }
    if (hasShortNullableField()) {
      hash = hash * 31 + ((int)getShortNullableField());
    }
    if (hasIntField()) {
      hash = hash * 31 + (getIntField());
    }
    if (hasIntNullableField()) {
      hash = hash * 31 + (getIntNullableField());
    }
    if (hasLongField()) {
      hash = hash * 31 + ((int)(getLongField() ^ (getLongField() >>> 32)));
    }
    if (hasLongNullableField()) {
      hash = hash * 31 + ((int)(getLongNullableField() ^ (getLongNullableField() >>> 32)));
    }
    if (hasDoubleField()) {
      hash = hash * 31 + ((int)(Double.doubleToLongBits(getDoubleField()) ^ (Double.doubleToLongBits(getDoubleField()) >>> 32)));
    }
    if (hasDoubleNullableField()) {
      hash = hash * 31 + ((int)(Double.doubleToLongBits(getDoubleNullableField()) ^ (Double.doubleToLongBits(getDoubleNullableField()) >>> 32)));
    }
    if (hasDecimalField()) {
      hash = hash * 31 + ((int)(getDecimalField() ^ (getDecimalField() >>> 32)));
    }
    if (hasDecimalNullableField()) {
      hash = hash * 31 + ((int)(getDecimalNullableField() ^ (getDecimalNullableField() >>> 32)));
    }
    if (hasTextAlphaNumericField()) {
      hash = hash * 31 + ((int)(getTextAlphaNumericField() ^ (getTextAlphaNumericField() >>> 32)));
    }
    if (hasTextAlphaNumericNullableField()) {
      hash = hash * 31 + ((int)(getTextAlphaNumericNullableField() ^ (getTextAlphaNumericNullableField() >>> 32)));
    }
    if (hasTextField()) {
      hash = hash * 31 + getTextField().hashCode();
    }
    if (hasTextNullableField()) {
      hash = hash * 31 + getTextNullableField().hashCode();
    }
    if (hasAsciiTextField()) {
      hash = hash * 31 + getAsciiTextField().hashCode();
    }
    if (hasAsciiTextNullableField()) {
      hash = hash * 31 + getAsciiTextNullableField().hashCode();
    }
    if (hasTimeOfDayField()) {
      hash = hash * 31 + (getTimeOfDayField());
    }
    if (hasTimeOfDayNullableField()) {
      hash = hash * 31 + (getTimeOfDayNullableField());
    }
    if (hasTimestampField()) {
      hash = hash * 31 + ((int)(getTimestampField() ^ (getTimestampField() >>> 32)));
    }
    if (hasTimestampNullableField()) {
      hash = hash * 31 + ((int)(getTimestampNullableField() ^ (getTimestampNullableField() >>> 32)));
    }
    if (hasEnumField()) {
      hash = hash * 31 + getEnumField().getNumber();
    }
    if (hasEnumNullableField()) {
      hash = hash * 31 + getEnumNullableField().getNumber();
    }
    return hash;
  }

  /**
   * Method copies state to a given instance
   * @param template class instance that should be used as a copy source
   */
  @Override
  public AllSimpleTypesMessage copyFrom(RecordInfo template) {
    super.copyFrom(template);
    if (template instanceof AllSimpleTypesMessageInfo) {
      AllSimpleTypesMessageInfo t = (AllSimpleTypesMessageInfo)template;
      if (t.hasBoolField()) {
        setBoolField(t.getBoolField());
      } else {
        nullifyBoolField();
      }
      if (t.hasBoolNullableField()) {
        setBoolNullableField(t.getBoolNullableField());
      } else {
        nullifyBoolNullableField();
      }
      if (t.hasBinaryField()) {
        if (!(hasBinaryField() && getBinaryField() instanceof BinaryArrayReadWrite)) {
          setBinaryField(new BinaryArray());
        }
        ((BinaryArrayReadWrite)getBinaryField()).assign(t.getBinaryField());
      } else {
        nullifyBinaryField();
      }
      if (t.hasBinaryNullableField()) {
        if (!(hasBinaryNullableField() && getBinaryNullableField() instanceof BinaryArrayReadWrite)) {
          setBinaryNullableField(new BinaryArray());
        }
        ((BinaryArrayReadWrite)getBinaryNullableField()).assign(t.getBinaryNullableField());
      } else {
        nullifyBinaryNullableField();
      }
      if (t.hasByteField()) {
        setByteField(t.getByteField());
      } else {
        nullifyByteField();
      }
      if (t.hasByteNullableField()) {
        setByteNullableField(t.getByteNullableField());
      } else {
        nullifyByteNullableField();
      }
      if (t.hasShortField()) {
        setShortField(t.getShortField());
      } else {
        nullifyShortField();
      }
      if (t.hasShortNullableField()) {
        setShortNullableField(t.getShortNullableField());
      } else {
        nullifyShortNullableField();
      }
      if (t.hasIntField()) {
        setIntField(t.getIntField());
      } else {
        nullifyIntField();
      }
      if (t.hasIntNullableField()) {
        setIntNullableField(t.getIntNullableField());
      } else {
        nullifyIntNullableField();
      }
      if (t.hasLongField()) {
        setLongField(t.getLongField());
      } else {
        nullifyLongField();
      }
      if (t.hasLongNullableField()) {
        setLongNullableField(t.getLongNullableField());
      } else {
        nullifyLongNullableField();
      }
      if (t.hasFloatField()) {
        setFloatField(t.getFloatField());
      } else {
        nullifyFloatField();
      }
      if (t.hasFloatNullableField()) {
        setFloatNullableField(t.getFloatNullableField());
      } else {
        nullifyFloatNullableField();
      }
      if (t.hasDoubleField()) {
        setDoubleField(t.getDoubleField());
      } else {
        nullifyDoubleField();
      }
      if (t.hasDoubleNullableField()) {
        setDoubleNullableField(t.getDoubleNullableField());
      } else {
        nullifyDoubleNullableField();
      }
      if (t.hasDecimalField()) {
        setDecimalField(t.getDecimalField());
      } else {
        nullifyDecimalField();
      }
      if (t.hasDecimalNullableField()) {
        setDecimalNullableField(t.getDecimalNullableField());
      } else {
        nullifyDecimalNullableField();
      }
      if (t.hasTextAlphaNumericField()) {
        setTextAlphaNumericField(t.getTextAlphaNumericField());
      } else {
        nullifyTextAlphaNumericField();
      }
      if (t.hasTextAlphaNumericNullableField()) {
        setTextAlphaNumericNullableField(t.getTextAlphaNumericNullableField());
      } else {
        nullifyTextAlphaNumericNullableField();
      }
      if (t.hasTextField()) {
        if (hasTextField() && getTextField() instanceof StringBuilder) {
          ((StringBuilder)getTextField()).setLength(0);
        } else {
          setTextField(new StringBuilder());
        }
        ((StringBuilder)getTextField()).append(t.getTextField());
      } else {
        nullifyTextField();
      }
      if (t.hasTextNullableField()) {
        if (hasTextNullableField() && getTextNullableField() instanceof StringBuilder) {
          ((StringBuilder)getTextNullableField()).setLength(0);
        } else {
          setTextNullableField(new StringBuilder());
        }
        ((StringBuilder)getTextNullableField()).append(t.getTextNullableField());
      } else {
        nullifyTextNullableField();
      }
      if (t.hasAsciiTextField()) {
        if (hasAsciiTextField() && getAsciiTextField() instanceof StringBuilder) {
          ((StringBuilder)getAsciiTextField()).setLength(0);
        } else {
          setAsciiTextField(new StringBuilder());
        }
        ((StringBuilder)getAsciiTextField()).append(t.getAsciiTextField());
      } else {
        nullifyAsciiTextField();
      }
      if (t.hasAsciiTextNullableField()) {
        if (hasAsciiTextNullableField() && getAsciiTextNullableField() instanceof StringBuilder) {
          ((StringBuilder)getAsciiTextNullableField()).setLength(0);
        } else {
          setAsciiTextNullableField(new StringBuilder());
        }
        ((StringBuilder)getAsciiTextNullableField()).append(t.getAsciiTextNullableField());
      } else {
        nullifyAsciiTextNullableField();
      }
      if (t.hasTimeOfDayField()) {
        setTimeOfDayField(t.getTimeOfDayField());
      } else {
        nullifyTimeOfDayField();
      }
      if (t.hasTimeOfDayNullableField()) {
        setTimeOfDayNullableField(t.getTimeOfDayNullableField());
      } else {
        nullifyTimeOfDayNullableField();
      }
      if (t.hasTimestampField()) {
        setTimestampField(t.getTimestampField());
      } else {
        nullifyTimestampField();
      }
      if (t.hasTimestampNullableField()) {
        setTimestampNullableField(t.getTimestampNullableField());
      } else {
        nullifyTimestampNullableField();
      }
      if (t.hasEnumField()) {
        setEnumField(t.getEnumField());
      } else {
        nullifyEnumField();
      }
      if (t.hasEnumNullableField()) {
        setEnumNullableField(t.getEnumNullableField());
      } else {
        nullifyEnumNullableField();
      }
    }
    return this;
  }

  /**
   * @return a string representation of this class object.
   */
  @Override
  public String toString() {
    StringBuilder str = new StringBuilder();
    return toString(str).toString();
  }

  /**
   * @return a string representation of this class object.
   */
  @Override
  public StringBuilder toString(StringBuilder str) {
    str.append("{ \"$type\":  \"AllSimpleTypesMessage\"");
    if (hasBoolField()) {
      str.append(", \"boolField\": ").append(getBoolField());
    }
    if (hasBoolNullableField()) {
      str.append(", \"boolNullableField\": ").append(getBoolNullableField());
    }
    if (hasBinaryField()) {
      str.append(", \"binaryField\": ").append(getBinaryField());
    }
    if (hasBinaryNullableField()) {
      str.append(", \"binaryNullableField\": ").append(getBinaryNullableField());
    }
    if (hasByteField()) {
      str.append(", \"byteField\": ").append(getByteField());
    }
    if (hasByteNullableField()) {
      str.append(", \"byteNullableField\": ").append(getByteNullableField());
    }
    if (hasShortField()) {
      str.append(", \"shortField\": ").append(getShortField());
    }
    if (hasShortNullableField()) {
      str.append(", \"shortNullableField\": ").append(getShortNullableField());
    }
    if (hasIntField()) {
      str.append(", \"intField\": ").append(getIntField());
    }
    if (hasIntNullableField()) {
      str.append(", \"intNullableField\": ").append(getIntNullableField());
    }
    if (hasLongField()) {
      str.append(", \"longField\": ").append(getLongField());
    }
    if (hasLongNullableField()) {
      str.append(", \"longNullableField\": ").append(getLongNullableField());
    }
    if (hasFloatField()) {
      str.append(", \"floatField\": ").append(getFloatField());
    }
    if (hasFloatNullableField()) {
      str.append(", \"floatNullableField\": ").append(getFloatNullableField());
    }
    if (hasDoubleField()) {
      str.append(", \"doubleField\": ").append(getDoubleField());
    }
    if (hasDoubleNullableField()) {
      str.append(", \"doubleNullableField\": ").append(getDoubleNullableField());
    }
    if (hasDecimalField()) {
      str.append(", \"decimalField\": ");
      Decimal64Utils.appendTo(getDecimalField(), str);
    }
    if (hasDecimalNullableField()) {
      str.append(", \"decimalNullableField\": ");
      Decimal64Utils.appendTo(getDecimalNullableField(), str);
    }
    if (hasTextAlphaNumericField()) {
      str.append(", \"textAlphaNumericField\": ").append(getTextAlphaNumericField());
    }
    if (hasTextAlphaNumericNullableField()) {
      str.append(", \"textAlphaNumericNullableField\": ").append(getTextAlphaNumericNullableField());
    }
    if (hasTextField()) {
      str.append(", \"textField\": \"").append(getTextField()).append("\"");
    }
    if (hasTextNullableField()) {
      str.append(", \"textNullableField\": \"").append(getTextNullableField()).append("\"");
    }
    if (hasAsciiTextField()) {
      str.append(", \"asciiTextField\": \"").append(getAsciiTextField()).append("\"");
    }
    if (hasAsciiTextNullableField()) {
      str.append(", \"asciiTextNullableField\": \"").append(getAsciiTextNullableField()).append("\"");
    }
    if (hasTimeOfDayField()) {
      str.append(", \"timeOfDayField\": ").append(getTimeOfDayField());
    }
    if (hasTimeOfDayNullableField()) {
      str.append(", \"timeOfDayNullableField\": ").append(getTimeOfDayNullableField());
    }
    if (hasTimestampField()) {
      str.append(", \"timestampField\": ").append(getTimestampField());
    }
    if (hasTimestampNullableField()) {
      str.append(", \"timestampNullableField\": ").append(getTimestampNullableField());
    }
    if (hasEnumField()) {
      str.append(", \"enumField\": \"").append(getEnumField()).append("\"");
    }
    if (hasEnumNullableField()) {
      str.append(", \"enumNullableField\": \"").append(getEnumNullableField()).append("\"");
    }
    if (hasTimeStampMs()) {
      str.append(", \"timestamp\": \"").append(formatNanos(getTimeStampMs(), (int)getNanoTime())).append("\"");
    }
    if (hasSymbol()) {
      str.append(", \"symbol\": \"").append(getSymbol()).append("\"");
    }
    str.append("}");
    return str;
  }
}
