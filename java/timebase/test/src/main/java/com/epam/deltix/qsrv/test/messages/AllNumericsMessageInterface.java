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

import com.epam.deltix.dfp.Decimal;
import com.epam.deltix.timebase.messages.RecordInfo;
import com.epam.deltix.util.collections.generated.*;

/**
 */
public interface AllNumericsMessageInterface extends AllNumericsMessageInfo, AllSimpleNumericsMessageInterface {
  /**
   * @param value - Byte List
   */
  void setByteList(ByteArrayList value);

  /**
   */
  void nullifyByteList();

  /**
   * @param value - Byte List Of Nullable
   */
  void setByteListOfNullable(ByteArrayList value);

  /**
   */
  void nullifyByteListOfNullable();

  /**
   * @param value - Nullable Byte List
   */
  void setNullableByteList(ByteArrayList value);

  /**
   */
  void nullifyNullableByteList();

  /**
   * @param value - Nullable Byte List Of Nullable
   */
  void setNullableByteListOfNullable(ByteArrayList value);

  /**
   */
  void nullifyNullableByteListOfNullable();

  /**
   * @param value - Short List
   */
  void setShortList(ShortArrayList value);

  /**
   */
  void nullifyShortList();

  /**
   * @param value - Short List Of Nullable
   */
  void setShortListOfNullable(ShortArrayList value);

  /**
   */
  void nullifyShortListOfNullable();

  /**
   * @param value - Nullable Short List
   */
  void setNullableShortList(ShortArrayList value);

  /**
   */
  void nullifyNullableShortList();

  /**
   * @param value - Nullable Short List Of Nullable
   */
  void setNullableShortListOfNullable(ShortArrayList value);

  /**
   */
  void nullifyNullableShortListOfNullable();

  /**
   * @param value - Int List
   */
  void setIntList(IntegerArrayList value);

  /**
   */
  void nullifyIntList();

  /**
   * @param value - Int List Of Nullable
   */
  void setIntListOfNullable(IntegerArrayList value);

  /**
   */
  void nullifyIntListOfNullable();

  /**
   * @param value - Nullable Int List
   */
  void setNullableIntList(IntegerArrayList value);

  /**
   */
  void nullifyNullableIntList();

  /**
   * @param value - Nullable Int List Of Nullable
   */
  void setNullableIntListOfNullable(IntegerArrayList value);

  /**
   */
  void nullifyNullableIntListOfNullable();

  /**
   * @param value - Long List
   */
  void setLongList(LongArrayList value);

  /**
   */
  void nullifyLongList();

  /**
   * @param value - Long List Of Nullable
   */
  void setLongListOfNullable(LongArrayList value);

  /**
   */
  void nullifyLongListOfNullable();

  /**
   * @param value - Nullable Long List
   */
  void setNullableLongList(LongArrayList value);

  /**
   */
  void nullifyNullableLongList();

  /**
   * @param value - Nullable Long List Of Nullable
   */
  void setNullableLongListOfNullable(LongArrayList value);

  /**
   */
  void nullifyNullableLongListOfNullable();

  /**
   * @param value - Decimal List
   */
  void setDecimalList(@Decimal LongArrayList value);

  /**
   */
  void nullifyDecimalList();

  /**
   * @param value - Decimal List Of Nullable
   */
  void setDecimalListOfNullable(@Decimal LongArrayList value);

  /**
   */
  void nullifyDecimalListOfNullable();

  /**
   * @param value - Nullable Decimal List
   */
  void setNullableDecimalList(LongArrayList value);

  /**
   */
  void nullifyNullableDecimalList();

  /**
   * @param value - Nullable Decimal List Of Nullable
   */
  void setNullableDecimalListOfNullable(LongArrayList value);

  /**
   */
  void nullifyNullableDecimalListOfNullable();

  /**
   * @param value - Double List
   */
  void setDoubleList(DoubleArrayList value);

  /**
   */
  void nullifyDoubleList();

  /**
   * @param value - Double List Of Nullable
   */
  void setDoubleListOfNullable(DoubleArrayList value);

  /**
   */
  void nullifyDoubleListOfNullable();

  /**
   * @param value - Nullable Double List
   */
  void setNullableDoubleList(DoubleArrayList value);

  /**
   */
  void nullifyNullableDoubleList();

  /**
   * @param value - Nullable Double List Of Nullable
   */
  void setNullableDoubleListOfNullable(DoubleArrayList value);

  /**
   */
  void nullifyNullableDoubleListOfNullable();

  /**
   * @param value - Float List
   */
  void setFloatList(FloatArrayList value);

  /**
   */
  void nullifyFloatList();

  /**
   * @param value - Float List Of Nullable
   */
  void setFloatListOfNullable(FloatArrayList value);

  /**
   */
  void nullifyFloatListOfNullable();

  /**
   * @param value - Nullable Float List
   */
  void setNullableFloatList(FloatArrayList value);

  /**
   */
  void nullifyNullableFloatList();

  /**
   * @param value - Nullable Float List Of Nullable
   */
  void setNullableFloatListOfNullable(FloatArrayList value);

  /**
   */
  void nullifyNullableFloatListOfNullable();

  /**
   * Method nullifies all instance properties
   */
  @Override
  AllNumericsMessageInterface nullify();

  /**
   * Resets all instance properties to their default values
   */
  @Override
  AllNumericsMessageInterface reset();

  @Override
  AllNumericsMessageInterface copyFrom(RecordInfo template);
}
