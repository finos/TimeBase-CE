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
import com.epam.deltix.util.collections.generated.*;

/**
 */
public interface AllTypesMessageInfo extends AllSimpleTypesMessageInfo {
  /**
   * @return Object
   */
  AllSimpleTypesMessageInfo getObject();

  /**
   * @return true if Object is not null
   */
  boolean hasObject();

  /**
   * @return Lists
   */
  AllListsMessageInfo getLists();

  /**
   * @return true if Lists is not null
   */
  boolean hasLists();

  /**
   * @return Boolean List
   */
  ByteList getBooleanList();

  /**
   * @return true if Boolean List is not null
   */
  boolean hasBooleanList();

  /**
   * @return Boolean List Of Nullable
   */
  ByteList getBooleanListOfNullable();

  /**
   * @return true if Boolean List Of Nullable is not null
   */
  boolean hasBooleanListOfNullable();

  /**
   * @return Nullable Boolean List
   */
  ByteList getNullableBooleanList();

  /**
   * @return true if Nullable Boolean List is not null
   */
  boolean hasNullableBooleanList();

  /**
   * @return Nullable Boolean List Of Nullable
   */
  ByteList getNullableBooleanListOfNullable();

  /**
   * @return true if Nullable Boolean List Of Nullable is not null
   */
  boolean hasNullableBooleanListOfNullable();

  /**
   * @return Byte List
   */
  ByteList getByteList();

  /**
   * @return true if Byte List is not null
   */
  boolean hasByteList();

  /**
   * @return Byte List Of Nullable
   */
  ByteList getByteListOfNullable();

  /**
   * @return true if Byte List Of Nullable is not null
   */
  boolean hasByteListOfNullable();

  /**
   * @return Nullable Byte List
   */
  ByteList getNullableByteList();

  /**
   * @return true if Nullable Byte List is not null
   */
  boolean hasNullableByteList();

  /**
   * @return Nullable Byte List Of Nullable
   */
  ByteList getNullableByteListOfNullable();

  /**
   * @return true if Nullable Byte List Of Nullable is not null
   */
  boolean hasNullableByteListOfNullable();

  /**
   * @return Short List
   */
  ShortList getShortList();

  /**
   * @return true if Short List is not null
   */
  boolean hasShortList();

  /**
   * @return Short List Of Nullable
   */
  ShortList getShortListOfNullable();

  /**
   * @return true if Short List Of Nullable is not null
   */
  boolean hasShortListOfNullable();

  /**
   * @return Nullable Short List
   */
  ShortList getNullableShortList();

  /**
   * @return true if Nullable Short List is not null
   */
  boolean hasNullableShortList();

  /**
   * @return Nullable Short List Of Nullable
   */
  ShortList getNullableShortListOfNullable();

  /**
   * @return true if Nullable Short List Of Nullable is not null
   */
  boolean hasNullableShortListOfNullable();

  /**
   * @return Int List
   */
  IntegerList getIntList();

  /**
   * @return true if Int List is not null
   */
  boolean hasIntList();

  /**
   * @return Int List Of Nullable
   */
  IntegerList getIntListOfNullable();

  /**
   * @return true if Int List Of Nullable is not null
   */
  boolean hasIntListOfNullable();

  /**
   * @return Nullable Int List
   */
  IntegerList getNullableIntList();

  /**
   * @return true if Nullable Int List is not null
   */
  boolean hasNullableIntList();

  /**
   * @return Nullable Int List Of Nullable
   */
  IntegerList getNullableIntListOfNullable();

  /**
   * @return true if Nullable Int List Of Nullable is not null
   */
  boolean hasNullableIntListOfNullable();

  /**
   * @return Long List
   */
  LongList getLongList();

  /**
   * @return true if Long List is not null
   */
  boolean hasLongList();

  /**
   * @return Long List Of Nullable
   */
  LongList getLongListOfNullable();

  /**
   * @return true if Long List Of Nullable is not null
   */
  boolean hasLongListOfNullable();

  /**
   * @return Nullable Long List
   */
  LongList getNullableLongList();

  /**
   * @return true if Nullable Long List is not null
   */
  boolean hasNullableLongList();

  /**
   * @return Nullable Long List Of Nullable
   */
  LongList getNullableLongListOfNullable();

  /**
   * @return true if Nullable Long List Of Nullable is not null
   */
  boolean hasNullableLongListOfNullable();

  /**
   * @return Decimal List
   */
  @Decimal
  LongList getDecimalList();

  /**
   * @return true if Decimal List is not null
   */
  boolean hasDecimalList();

  /**
   * @return Decimal List Of Nullable
   */
  @Decimal
  LongList getDecimalListOfNullable();

  /**
   * @return true if Decimal List Of Nullable is not null
   */
  boolean hasDecimalListOfNullable();

  /**
   * @return Nullable Decimal List
   */
  LongList getNullableDecimalList();

  /**
   * @return true if Nullable Decimal List is not null
   */
  boolean hasNullableDecimalList();

  /**
   * @return Nullable Decimal List Of Nullable
   */
  LongList getNullableDecimalListOfNullable();

  /**
   * @return true if Nullable Decimal List Of Nullable is not null
   */
  boolean hasNullableDecimalListOfNullable();

  /**
   * @return Double List
   */
  DoubleList getDoubleList();

  /**
   * @return true if Double List is not null
   */
  boolean hasDoubleList();

  /**
   * @return Double List Of Nullable
   */
  DoubleList getDoubleListOfNullable();

  /**
   * @return true if Double List Of Nullable is not null
   */
  boolean hasDoubleListOfNullable();

  /**
   * @return Nullable Double List
   */
  DoubleList getNullableDoubleList();

  /**
   * @return true if Nullable Double List is not null
   */
  boolean hasNullableDoubleList();

  /**
   * @return Nullable Double List Of Nullable
   */
  DoubleList getNullableDoubleListOfNullable();

  /**
   * @return true if Nullable Double List Of Nullable is not null
   */
  boolean hasNullableDoubleListOfNullable();

  /**
   * @return Float List
   */
  FloatList getFloatList();

  /**
   * @return true if Float List is not null
   */
  boolean hasFloatList();

  /**
   * @return Float List Of Nullable
   */
  FloatList getFloatListOfNullable();

  /**
   * @return true if Float List Of Nullable is not null
   */
  boolean hasFloatListOfNullable();

  /**
   * @return Nullable Float List
   */
  FloatList getNullableFloatList();

  /**
   * @return true if Nullable Float List is not null
   */
  boolean hasNullableFloatList();

  /**
   * @return Nullable Float List Of Nullable
   */
  FloatList getNullableFloatListOfNullable();

  /**
   * @return true if Nullable Float List Of Nullable is not null
   */
  boolean hasNullableFloatListOfNullable();

  /**
   * @return Text List
   */
  ObjectList<CharSequence> getTextList();

  /**
   * @return true if Text List is not null
   */
  boolean hasTextList();

  /**
   * @return Text List Of Nullable
   */
  ObjectList<CharSequence> getTextListOfNullable();

  /**
   * @return true if Text List Of Nullable is not null
   */
  boolean hasTextListOfNullable();

  /**
   * @return Nullable Text List
   */
  ObjectList<CharSequence> getNullableTextList();

  /**
   * @return true if Nullable Text List is not null
   */
  boolean hasNullableTextList();

  /**
   * @return Nullable Text List Of Nullable
   */
  ObjectList<CharSequence> getNullableTextListOfNullable();

  /**
   * @return true if Nullable Text List Of Nullable is not null
   */
  boolean hasNullableTextListOfNullable();

  /**
   * @return Ascii Text List
   */
  ObjectList<CharSequence> getAsciiTextList();

  /**
   * @return true if Ascii Text List is not null
   */
  boolean hasAsciiTextList();

  /**
   * @return Ascii Text List Of Nullable
   */
  ObjectList<CharSequence> getAsciiTextListOfNullable();

  /**
   * @return true if Ascii Text List Of Nullable is not null
   */
  boolean hasAsciiTextListOfNullable();

  /**
   * @return Nullable Ascii Text List
   */
  ObjectList<CharSequence> getNullableAsciiTextList();

  /**
   * @return true if Nullable Ascii Text List is not null
   */
  boolean hasNullableAsciiTextList();

  /**
   * @return Nullable Ascii Text List Of Nullable
   */
  ObjectList<CharSequence> getNullableAsciiTextListOfNullable();

  /**
   * @return true if Nullable Ascii Text List Of Nullable is not null
   */
  boolean hasNullableAsciiTextListOfNullable();

  /**
   * @return Alphanumeric List
   */
  LongList getAlphanumericList();

  /**
   * @return true if Alphanumeric List is not null
   */
  boolean hasAlphanumericList();

  /**
   * @return Alphanumeric List Of Nullable
   */
  LongList getAlphanumericListOfNullable();

  /**
   * @return true if Alphanumeric List Of Nullable is not null
   */
  boolean hasAlphanumericListOfNullable();

  /**
   * @return Nullable Alphanumeric List
   */
  LongList getNullableAlphanumericList();

  /**
   * @return true if Nullable Alphanumeric List is not null
   */
  boolean hasNullableAlphanumericList();

  /**
   * @return Nullable Alphanumeric List Of Nullable
   */
  LongList getNullableAlphanumericListOfNullable();

  /**
   * @return true if Nullable Alphanumeric List Of Nullable is not null
   */
  boolean hasNullableAlphanumericListOfNullable();

  /**
   * @return Objects List
   */
  ObjectList<AllSimpleTypesMessageInfo> getObjectsList();

  /**
   * @return true if Objects List is not null
   */
  boolean hasObjectsList();

  /**
   * @return Objects List Of Nullable
   */
  ObjectList<AllSimpleTypesMessageInfo> getObjectsListOfNullable();

  /**
   * @return true if Objects List Of Nullable is not null
   */
  boolean hasObjectsListOfNullable();

  /**
   * @return Nullable Objects List
   */
  ObjectList<AllSimpleTypesMessageInfo> getNullableObjectsList();

  /**
   * @return true if Nullable Objects List is not null
   */
  boolean hasNullableObjectsList();

  /**
   * @return Nullable Objects List Of Nullable
   */
  ObjectList<AllSimpleTypesMessageInfo> getNullableObjectsListOfNullable();

  /**
   * @return true if Nullable Objects List Of Nullable is not null
   */
  boolean hasNullableObjectsListOfNullable();

  /**
   * @return List Of Lists
   */
  ObjectList<AllListsMessageInfo> getListOfLists();

  /**
   * @return true if List Of Lists is not null
   */
  boolean hasListOfLists();

  /**
   * @return Timestamp List
   */
  LongList getTimestampList();

  /**
   * @return true if Timestamp List is not null
   */
  boolean hasTimestampList();

  /**
   * @return Timestamp List Of Nullable
   */
  LongList getTimestampListOfNullable();

  /**
   * @return true if Timestamp List Of Nullable is not null
   */
  boolean hasTimestampListOfNullable();

  /**
   * @return Nullable Timestamp List
   */
  LongList getNullableTimestampList();

  /**
   * @return true if Nullable Timestamp List is not null
   */
  boolean hasNullableTimestampList();

  /**
   * @return Nullable Timestamp List Of Nullable
   */
  LongList getNullableTimestampListOfNullable();

  /**
   * @return true if Nullable Timestamp List Of Nullable is not null
   */
  boolean hasNullableTimestampListOfNullable();

  /**
   * @return Time Of Day List
   */
  IntegerList getTimeOfDayList();

  /**
   * @return true if Time Of Day List is not null
   */
  boolean hasTimeOfDayList();

  /**
   * @return Time Of Day List Of Nullable
   */
  IntegerList getTimeOfDayListOfNullable();

  /**
   * @return true if Time Of Day List Of Nullable is not null
   */
  boolean hasTimeOfDayListOfNullable();

  /**
   * @return Nullable Time Of Day List
   */
  IntegerList getNullableTimeOfDayList();

  /**
   * @return true if Nullable Time Of Day List is not null
   */
  boolean hasNullableTimeOfDayList();

  /**
   * @return Nullable Time Of Day List Of Nullable
   */
  IntegerList getNullableTimeOfDayListOfNullable();

  /**
   * @return true if Nullable Time Of Day List Of Nullable is not null
   */
  boolean hasNullableTimeOfDayListOfNullable();

  /**
   * @return Enum List
   */
  ObjectList<TestEnum> getEnumList();

  /**
   * @return true if Enum List is not null
   */
  boolean hasEnumList();

  /**
   * @return Enum List Of Nullable
   */
  ObjectList<TestEnum> getEnumListOfNullable();

  /**
   * @return true if Enum List Of Nullable is not null
   */
  boolean hasEnumListOfNullable();

  /**
   * @return Nullable Enum List
   */
  ObjectList<TestEnum> getNullableEnumList();

  /**
   * @return true if Nullable Enum List is not null
   */
  boolean hasNullableEnumList();

  /**
   * @return Nullable Enum List Of Nullable
   */
  ObjectList<TestEnum> getNullableEnumListOfNullable();

  /**
   * @return true if Nullable Enum List Of Nullable is not null
   */
  boolean hasNullableEnumListOfNullable();

  /**
   * Method copies state to a given instance
   */
  @Override
  AllTypesMessageInfo clone();
}
