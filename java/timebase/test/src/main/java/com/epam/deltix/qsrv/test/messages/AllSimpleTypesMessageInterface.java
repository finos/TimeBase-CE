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

import com.epam.deltix.containers.interfaces.BinaryArrayReadOnly;
import com.epam.deltix.dfp.Decimal;
import com.epam.deltix.timebase.messages.MessageInterface;
import com.epam.deltix.timebase.messages.RecordInfo;

/**
 */
public interface AllSimpleTypesMessageInterface extends AllSimpleTypesMessageInfo, MessageInterface {
  /**
   * @param value - Bool Field
   */
  void setBoolField(byte value);

  /**
   */
  void nullifyBoolField();

  /**
   * @param value - Bool Nullable Field
   */
  void setBoolNullableField(byte value);

  /**
   */
  void nullifyBoolNullableField();

  /**
   * @param value - Binary Field
   */
  void setBinaryField(BinaryArrayReadOnly value);

  /**
   */
  void nullifyBinaryField();

  /**
   * @param value - Binary Nullable Field
   */
  void setBinaryNullableField(BinaryArrayReadOnly value);

  /**
   */
  void nullifyBinaryNullableField();

  /**
   * @param value - Byte Field
   */
  void setByteField(byte value);

  /**
   */
  void nullifyByteField();

  /**
   * @param value - Byte Nullable Field
   */
  void setByteNullableField(byte value);

  /**
   */
  void nullifyByteNullableField();

  /**
   * @param value - Short Field
   */
  void setShortField(short value);

  /**
   */
  void nullifyShortField();

  /**
   * @param value - Short Nullable Field
   */
  void setShortNullableField(short value);

  /**
   */
  void nullifyShortNullableField();

  /**
   * @param value - Int Field
   */
  void setIntField(int value);

  /**
   */
  void nullifyIntField();

  /**
   * @param value - Int Nullable Field
   */
  void setIntNullableField(int value);

  /**
   */
  void nullifyIntNullableField();

  /**
   * @param value - Long Field
   */
  void setLongField(long value);

  /**
   */
  void nullifyLongField();

  /**
   * @param value - Long Nullable Field
   */
  void setLongNullableField(long value);

  /**
   */
  void nullifyLongNullableField();

  /**
   * @param value - Float Field
   */
  void setFloatField(float value);

  /**
   */
  void nullifyFloatField();

  /**
   * @param value - Float Nullable Field
   */
  void setFloatNullableField(float value);

  /**
   */
  void nullifyFloatNullableField();

  /**
   * @param value - Double Field
   */
  void setDoubleField(double value);

  /**
   */
  void nullifyDoubleField();

  /**
   * @param value - Double Nullable Field
   */
  void setDoubleNullableField(double value);

  /**
   */
  void nullifyDoubleNullableField();

  /**
   * @param value - Decimal Field
   */
  void setDecimalField(@Decimal long value);

  /**
   */
  void nullifyDecimalField();

  /**
   * @param value - Decimal Nullable Field
   */
  void setDecimalNullableField(@Decimal long value);

  /**
   */
  void nullifyDecimalNullableField();

  /**
   * @param value - Text Alpha Numeric Field
   */
  void setTextAlphaNumericField(long value);

  /**
   */
  void nullifyTextAlphaNumericField();

  /**
   * @param value - Text Alpha Numeric Nullable Field
   */
  void setTextAlphaNumericNullableField(long value);

  /**
   */
  void nullifyTextAlphaNumericNullableField();

  /**
   * @param value - Text Field
   */
  void setTextField(CharSequence value);

  /**
   */
  void nullifyTextField();

  /**
   * @param value - Text Nullable Field
   */
  void setTextNullableField(CharSequence value);

  /**
   */
  void nullifyTextNullableField();

  /**
   * @param value - Ascii Text Field
   */
  void setAsciiTextField(CharSequence value);

  /**
   */
  void nullifyAsciiTextField();

  /**
   * @param value - Ascii Text Nullable Field
   */
  void setAsciiTextNullableField(CharSequence value);

  /**
   */
  void nullifyAsciiTextNullableField();

  /**
   * @param value - Time Of Day Field
   */
  void setTimeOfDayField(int value);

  /**
   */
  void nullifyTimeOfDayField();

  /**
   * @param value - Time Of Day Nullable Field
   */
  void setTimeOfDayNullableField(int value);

  /**
   */
  void nullifyTimeOfDayNullableField();

  /**
   * @param value - Timestamp Field
   */
  void setTimestampField(long value);

  /**
   */
  void nullifyTimestampField();

  /**
   * @param value - Timestamp Nullable Field
   */
  void setTimestampNullableField(long value);

  /**
   */
  void nullifyTimestampNullableField();

  /**
   * @param value - Enum Field
   */
  void setEnumField(TestEnum value);

  /**
   */
  void nullifyEnumField();

  /**
   * @param value - Enum Nullable Field
   */
  void setEnumNullableField(TestEnum value);

  /**
   */
  void nullifyEnumNullableField();

  /**
   * Method nullifies all instance properties
   */
  @Override
  AllSimpleTypesMessageInterface nullify();

  /**
   * Resets all instance properties to their default values
   */
  @Override
  AllSimpleTypesMessageInterface reset();

  @Override
  AllSimpleTypesMessageInterface copyFrom(RecordInfo template);
}
