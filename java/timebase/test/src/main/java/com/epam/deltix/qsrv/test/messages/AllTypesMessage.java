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

import com.epam.deltix.containers.BinaryAsciiString;
import com.epam.deltix.containers.CharSequenceUtils;
import com.epam.deltix.containers.MutableString;
import com.epam.deltix.dfp.Decimal;
import com.epam.deltix.dfp.Decimal64Utils;
import com.epam.deltix.timebase.messages.*;
import com.epam.deltix.util.collections.generated.*;


/**
 */
public class AllTypesMessage extends AllSimpleTypesMessage implements AllTypesMessageInterface {
  public static final String CLASS_NAME = AllTypesMessage.class.getName();

  /**
   */
  protected AllSimpleTypesMessageInfo object = null;

  /**
   */
  protected AllListsMessageInfo lists = null;

  /**
   */
  protected ByteArrayList booleanList = null;

  /**
   */
  protected ByteArrayList booleanListOfNullable = null;

  /**
   */
  protected ByteArrayList nullableBooleanList = null;

  /**
   */
  protected ByteArrayList nullableBooleanListOfNullable = null;

  /**
   */
  protected ByteArrayList byteList = null;

  /**
   */
  protected ByteArrayList byteListOfNullable = null;

  /**
   */
  protected ByteArrayList nullableByteList = null;

  /**
   */
  protected ByteArrayList nullableByteListOfNullable = null;

  /**
   */
  protected ShortArrayList shortList = null;

  /**
   */
  protected ShortArrayList shortListOfNullable = null;

  /**
   */
  protected ShortArrayList nullableShortList = null;

  /**
   */
  protected ShortArrayList nullableShortListOfNullable = null;

  /**
   */
  protected IntegerArrayList intList = null;

  /**
   */
  protected IntegerArrayList intListOfNullable = null;

  /**
   */
  protected IntegerArrayList nullableIntList = null;

  /**
   */
  protected IntegerArrayList nullableIntListOfNullable = null;

  /**
   */
  protected LongArrayList longList = null;

  /**
   */
  protected LongArrayList longListOfNullable = null;

  /**
   */
  protected LongArrayList nullableLongList = null;

  /**
   */
  protected LongArrayList nullableLongListOfNullable = null;

  /**
   */
  @Decimal
  protected LongArrayList decimalList = null;

  /**
   */
  @Decimal
  protected LongArrayList decimalListOfNullable = null;

  /**
   */
  protected LongArrayList nullableDecimalList = null;

  /**
   */
  protected LongArrayList nullableDecimalListOfNullable = null;

  /**
   */
  protected DoubleArrayList doubleList = null;

  /**
   */
  protected DoubleArrayList doubleListOfNullable = null;

  /**
   */
  protected DoubleArrayList nullableDoubleList = null;

  /**
   */
  protected DoubleArrayList nullableDoubleListOfNullable = null;

  /**
   */
  protected FloatArrayList floatList = null;

  /**
   */
  protected FloatArrayList floatListOfNullable = null;

  /**
   */
  protected FloatArrayList nullableFloatList = null;

  /**
   */
  protected FloatArrayList nullableFloatListOfNullable = null;

  /**
   */
  protected ObjectArrayList<CharSequence> textList = null;

  /**
   */
  protected ObjectArrayList<CharSequence> textListOfNullable = null;

  /**
   */
  protected ObjectArrayList<CharSequence> nullableTextList = null;

  /**
   */
  protected ObjectArrayList<CharSequence> nullableTextListOfNullable = null;

  /**
   */
  protected ObjectArrayList<CharSequence> asciiTextList = null;

  /**
   */
  protected ObjectArrayList<CharSequence> asciiTextListOfNullable = null;

  /**
   */
  protected ObjectArrayList<CharSequence> nullableAsciiTextList = null;

  /**
   */
  protected ObjectArrayList<CharSequence> nullableAsciiTextListOfNullable = null;

  /**
   */
  protected LongArrayList alphanumericList = null;

  /**
   */
  protected LongArrayList alphanumericListOfNullable = null;

  /**
   */
  protected LongArrayList nullableAlphanumericList = null;

  /**
   */
  protected LongArrayList nullableAlphanumericListOfNullable = null;

  /**
   */
  protected ObjectArrayList<AllSimpleTypesMessageInfo> objectsList = null;

  /**
   */
  protected ObjectArrayList<AllSimpleTypesMessageInfo> objectsListOfNullable = null;

  /**
   */
  protected ObjectArrayList<AllSimpleTypesMessageInfo> nullableObjectsList = null;

  /**
   */
  protected ObjectArrayList<AllSimpleTypesMessageInfo> nullableObjectsListOfNullable = null;

  /**
   */
  protected ObjectArrayList<AllListsMessageInfo> listOfLists = null;

  /**
   */
  protected LongArrayList timestampList = null;

  /**
   */
  protected LongArrayList timestampListOfNullable = null;

  /**
   */
  protected LongArrayList nullableTimestampList = null;

  /**
   */
  protected LongArrayList nullableTimestampListOfNullable = null;

  /**
   */
  protected IntegerArrayList timeOfDayList = null;

  /**
   */
  protected IntegerArrayList timeOfDayListOfNullable = null;

  /**
   */
  protected IntegerArrayList nullableTimeOfDayList = null;

  /**
   */
  protected IntegerArrayList nullableTimeOfDayListOfNullable = null;

  /**
   */
  protected ObjectArrayList<TestEnum> enumList = null;

  /**
   */
  protected ObjectArrayList<TestEnum> enumListOfNullable = null;

  /**
   */
  protected ObjectArrayList<TestEnum> nullableEnumList = null;

  /**
   */
  protected ObjectArrayList<TestEnum> nullableEnumListOfNullable = null;

  /**
   * @return Object
   */
  @SchemaElement
  @SchemaType(
      dataType = SchemaDataType.OBJECT,
      nestedTypes =  {
            AllSimpleTypesMessage.class}

  )
  public AllSimpleTypesMessageInfo getObject() {
    return object;
  }

  /**
   * @param value - Object
   */
  public void setObject(AllSimpleTypesMessageInfo value) {
    this.object = value;
  }

  /**
   * @return true if Object is not null
   */
  public boolean hasObject() {
    return object != null;
  }

  /**
   */
  public void nullifyObject() {
    this.object = null;
  }

  /**
   * @return Lists
   */
  @SchemaElement
  @SchemaType(
      dataType = SchemaDataType.OBJECT,
      nestedTypes =  {
            AllListsMessage.class}

  )
  public AllListsMessageInfo getLists() {
    return lists;
  }

  /**
   * @param value - Lists
   */
  public void setLists(AllListsMessageInfo value) {
    this.lists = value;
  }

  /**
   * @return true if Lists is not null
   */
  public boolean hasLists() {
    return lists != null;
  }

  /**
   */
  public void nullifyLists() {
    this.lists = null;
  }

  /**
   * @return Boolean List
   */
  @SchemaElement
  @SchemaArrayType(
      isNullable = false,
      isElementNullable = false,
      elementDataType = SchemaDataType.BOOLEAN
  )
  public ByteArrayList getBooleanList() {
    return booleanList;
  }

  /**
   * @param value - Boolean List
   */
  public void setBooleanList(ByteArrayList value) {
    this.booleanList = value;
  }

  /**
   * @return true if Boolean List is not null
   */
  public boolean hasBooleanList() {
    return booleanList != null;
  }

  /**
   */
  public void nullifyBooleanList() {
    this.booleanList = null;
  }

  /**
   * @return Boolean List Of Nullable
   */
  @SchemaElement
  @SchemaArrayType(
      isNullable = false,
      isElementNullable = true,
      elementDataType = SchemaDataType.BOOLEAN
  )
  public ByteArrayList getBooleanListOfNullable() {
    return booleanListOfNullable;
  }

  /**
   * @param value - Boolean List Of Nullable
   */
  public void setBooleanListOfNullable(ByteArrayList value) {
    this.booleanListOfNullable = value;
  }

  /**
   * @return true if Boolean List Of Nullable is not null
   */
  public boolean hasBooleanListOfNullable() {
    return booleanListOfNullable != null;
  }

  /**
   */
  public void nullifyBooleanListOfNullable() {
    this.booleanListOfNullable = null;
  }

  /**
   * @return Nullable Boolean List
   */
  @SchemaElement
  @SchemaArrayType(
      isNullable = true,
      isElementNullable = false,
      elementDataType = SchemaDataType.BOOLEAN
  )
  public ByteArrayList getNullableBooleanList() {
    return nullableBooleanList;
  }

  /**
   * @param value - Nullable Boolean List
   */
  public void setNullableBooleanList(ByteArrayList value) {
    this.nullableBooleanList = value;
  }

  /**
   * @return true if Nullable Boolean List is not null
   */
  public boolean hasNullableBooleanList() {
    return nullableBooleanList != null;
  }

  /**
   */
  public void nullifyNullableBooleanList() {
    this.nullableBooleanList = null;
  }

  /**
   * @return Nullable Boolean List Of Nullable
   */
  @SchemaElement
  @SchemaArrayType(
      isNullable = true,
      isElementNullable = true,
      elementDataType = SchemaDataType.BOOLEAN
  )
  public ByteArrayList getNullableBooleanListOfNullable() {
    return nullableBooleanListOfNullable;
  }

  /**
   * @param value - Nullable Boolean List Of Nullable
   */
  public void setNullableBooleanListOfNullable(ByteArrayList value) {
    this.nullableBooleanListOfNullable = value;
  }

  /**
   * @return true if Nullable Boolean List Of Nullable is not null
   */
  public boolean hasNullableBooleanListOfNullable() {
    return nullableBooleanListOfNullable != null;
  }

  /**
   */
  public void nullifyNullableBooleanListOfNullable() {
    this.nullableBooleanListOfNullable = null;
  }

  /**
   * @return Byte List
   */
  @SchemaElement
  @SchemaArrayType(
      isNullable = false,
      isElementNullable = false,
      elementEncoding = "INT8",
      elementDataType = SchemaDataType.INTEGER
  )
  public ByteArrayList getByteList() {
    return byteList;
  }

  /**
   * @param value - Byte List
   */
  public void setByteList(ByteArrayList value) {
    this.byteList = value;
  }

  /**
   * @return true if Byte List is not null
   */
  public boolean hasByteList() {
    return byteList != null;
  }

  /**
   */
  public void nullifyByteList() {
    this.byteList = null;
  }

  /**
   * @return Byte List Of Nullable
   */
  @SchemaElement
  @SchemaArrayType(
      isNullable = false,
      isElementNullable = true,
      elementEncoding = "INT8",
      elementDataType = SchemaDataType.INTEGER
  )
  public ByteArrayList getByteListOfNullable() {
    return byteListOfNullable;
  }

  /**
   * @param value - Byte List Of Nullable
   */
  public void setByteListOfNullable(ByteArrayList value) {
    this.byteListOfNullable = value;
  }

  /**
   * @return true if Byte List Of Nullable is not null
   */
  public boolean hasByteListOfNullable() {
    return byteListOfNullable != null;
  }

  /**
   */
  public void nullifyByteListOfNullable() {
    this.byteListOfNullable = null;
  }

  /**
   * @return Nullable Byte List
   */
  @SchemaElement
  @SchemaArrayType(
      isNullable = true,
      isElementNullable = false,
      elementEncoding = "INT8",
      elementDataType = SchemaDataType.INTEGER
  )
  public ByteArrayList getNullableByteList() {
    return nullableByteList;
  }

  /**
   * @param value - Nullable Byte List
   */
  public void setNullableByteList(ByteArrayList value) {
    this.nullableByteList = value;
  }

  /**
   * @return true if Nullable Byte List is not null
   */
  public boolean hasNullableByteList() {
    return nullableByteList != null;
  }

  /**
   */
  public void nullifyNullableByteList() {
    this.nullableByteList = null;
  }

  /**
   * @return Nullable Byte List Of Nullable
   */
  @SchemaElement
  @SchemaArrayType(
      isNullable = true,
      isElementNullable = true,
      elementEncoding = "INT8",
      elementDataType = SchemaDataType.INTEGER
  )
  public ByteArrayList getNullableByteListOfNullable() {
    return nullableByteListOfNullable;
  }

  /**
   * @param value - Nullable Byte List Of Nullable
   */
  public void setNullableByteListOfNullable(ByteArrayList value) {
    this.nullableByteListOfNullable = value;
  }

  /**
   * @return true if Nullable Byte List Of Nullable is not null
   */
  public boolean hasNullableByteListOfNullable() {
    return nullableByteListOfNullable != null;
  }

  /**
   */
  public void nullifyNullableByteListOfNullable() {
    this.nullableByteListOfNullable = null;
  }

  /**
   * @return Short List
   */
  @SchemaElement
  @SchemaArrayType(
      isNullable = false,
      isElementNullable = false
  )
  public ShortArrayList getShortList() {
    return shortList;
  }

  /**
   * @param value - Short List
   */
  public void setShortList(ShortArrayList value) {
    this.shortList = value;
  }

  /**
   * @return true if Short List is not null
   */
  public boolean hasShortList() {
    return shortList != null;
  }

  /**
   */
  public void nullifyShortList() {
    this.shortList = null;
  }

  /**
   * @return Short List Of Nullable
   */
  @SchemaElement
  @SchemaArrayType(
      isNullable = false,
      isElementNullable = true
  )
  public ShortArrayList getShortListOfNullable() {
    return shortListOfNullable;
  }

  /**
   * @param value - Short List Of Nullable
   */
  public void setShortListOfNullable(ShortArrayList value) {
    this.shortListOfNullable = value;
  }

  /**
   * @return true if Short List Of Nullable is not null
   */
  public boolean hasShortListOfNullable() {
    return shortListOfNullable != null;
  }

  /**
   */
  public void nullifyShortListOfNullable() {
    this.shortListOfNullable = null;
  }

  /**
   * @return Nullable Short List
   */
  @SchemaElement
  @SchemaArrayType(
      isNullable = true,
      isElementNullable = false
  )
  public ShortArrayList getNullableShortList() {
    return nullableShortList;
  }

  /**
   * @param value - Nullable Short List
   */
  public void setNullableShortList(ShortArrayList value) {
    this.nullableShortList = value;
  }

  /**
   * @return true if Nullable Short List is not null
   */
  public boolean hasNullableShortList() {
    return nullableShortList != null;
  }

  /**
   */
  public void nullifyNullableShortList() {
    this.nullableShortList = null;
  }

  /**
   * @return Nullable Short List Of Nullable
   */
  @SchemaElement
  @SchemaArrayType(
      isNullable = true,
      isElementNullable = true
  )
  public ShortArrayList getNullableShortListOfNullable() {
    return nullableShortListOfNullable;
  }

  /**
   * @param value - Nullable Short List Of Nullable
   */
  public void setNullableShortListOfNullable(ShortArrayList value) {
    this.nullableShortListOfNullable = value;
  }

  /**
   * @return true if Nullable Short List Of Nullable is not null
   */
  public boolean hasNullableShortListOfNullable() {
    return nullableShortListOfNullable != null;
  }

  /**
   */
  public void nullifyNullableShortListOfNullable() {
    this.nullableShortListOfNullable = null;
  }

  /**
   * @return Int List
   */
  @SchemaElement
  @SchemaArrayType(
      isNullable = false,
      isElementNullable = false
  )
  public IntegerArrayList getIntList() {
    return intList;
  }

  /**
   * @param value - Int List
   */
  public void setIntList(IntegerArrayList value) {
    this.intList = value;
  }

  /**
   * @return true if Int List is not null
   */
  public boolean hasIntList() {
    return intList != null;
  }

  /**
   */
  public void nullifyIntList() {
    this.intList = null;
  }

  /**
   * @return Int List Of Nullable
   */
  @SchemaElement
  @SchemaArrayType(
      isNullable = false,
      isElementNullable = true
  )
  public IntegerArrayList getIntListOfNullable() {
    return intListOfNullable;
  }

  /**
   * @param value - Int List Of Nullable
   */
  public void setIntListOfNullable(IntegerArrayList value) {
    this.intListOfNullable = value;
  }

  /**
   * @return true if Int List Of Nullable is not null
   */
  public boolean hasIntListOfNullable() {
    return intListOfNullable != null;
  }

  /**
   */
  public void nullifyIntListOfNullable() {
    this.intListOfNullable = null;
  }

  /**
   * @return Nullable Int List
   */
  @SchemaElement
  @SchemaArrayType(
      isNullable = true,
      isElementNullable = false
  )
  public IntegerArrayList getNullableIntList() {
    return nullableIntList;
  }

  /**
   * @param value - Nullable Int List
   */
  public void setNullableIntList(IntegerArrayList value) {
    this.nullableIntList = value;
  }

  /**
   * @return true if Nullable Int List is not null
   */
  public boolean hasNullableIntList() {
    return nullableIntList != null;
  }

  /**
   */
  public void nullifyNullableIntList() {
    this.nullableIntList = null;
  }

  /**
   * @return Nullable Int List Of Nullable
   */
  @SchemaElement
  @SchemaArrayType(
      isNullable = true,
      isElementNullable = true
  )
  public IntegerArrayList getNullableIntListOfNullable() {
    return nullableIntListOfNullable;
  }

  /**
   * @param value - Nullable Int List Of Nullable
   */
  public void setNullableIntListOfNullable(IntegerArrayList value) {
    this.nullableIntListOfNullable = value;
  }

  /**
   * @return true if Nullable Int List Of Nullable is not null
   */
  public boolean hasNullableIntListOfNullable() {
    return nullableIntListOfNullable != null;
  }

  /**
   */
  public void nullifyNullableIntListOfNullable() {
    this.nullableIntListOfNullable = null;
  }

  /**
   * @return Long List
   */
  @SchemaElement
  @SchemaArrayType(
      isNullable = false,
      isElementNullable = false
  )
  public LongArrayList getLongList() {
    return longList;
  }

  /**
   * @param value - Long List
   */
  public void setLongList(LongArrayList value) {
    this.longList = value;
  }

  /**
   * @return true if Long List is not null
   */
  public boolean hasLongList() {
    return longList != null;
  }

  /**
   */
  public void nullifyLongList() {
    this.longList = null;
  }

  /**
   * @return Long List Of Nullable
   */
  @SchemaElement
  @SchemaArrayType(
      isNullable = false,
      isElementNullable = true
  )
  public LongArrayList getLongListOfNullable() {
    return longListOfNullable;
  }

  /**
   * @param value - Long List Of Nullable
   */
  public void setLongListOfNullable(LongArrayList value) {
    this.longListOfNullable = value;
  }

  /**
   * @return true if Long List Of Nullable is not null
   */
  public boolean hasLongListOfNullable() {
    return longListOfNullable != null;
  }

  /**
   */
  public void nullifyLongListOfNullable() {
    this.longListOfNullable = null;
  }

  /**
   * @return Nullable Long List
   */
  @SchemaElement
  @SchemaArrayType(
      isNullable = true,
      isElementNullable = false
  )
  public LongArrayList getNullableLongList() {
    return nullableLongList;
  }

  /**
   * @param value - Nullable Long List
   */
  public void setNullableLongList(LongArrayList value) {
    this.nullableLongList = value;
  }

  /**
   * @return true if Nullable Long List is not null
   */
  public boolean hasNullableLongList() {
    return nullableLongList != null;
  }

  /**
   */
  public void nullifyNullableLongList() {
    this.nullableLongList = null;
  }

  /**
   * @return Nullable Long List Of Nullable
   */
  @SchemaElement
  @SchemaArrayType(
      isNullable = true,
      isElementNullable = true
  )
  public LongArrayList getNullableLongListOfNullable() {
    return nullableLongListOfNullable;
  }

  /**
   * @param value - Nullable Long List Of Nullable
   */
  public void setNullableLongListOfNullable(LongArrayList value) {
    this.nullableLongListOfNullable = value;
  }

  /**
   * @return true if Nullable Long List Of Nullable is not null
   */
  public boolean hasNullableLongListOfNullable() {
    return nullableLongListOfNullable != null;
  }

  /**
   */
  public void nullifyNullableLongListOfNullable() {
    this.nullableLongListOfNullable = null;
  }

  /**
   * @return Decimal List
   */
  @Decimal
  @SchemaElement
  @SchemaArrayType(
      isNullable = false,
      isElementNullable = false,
      elementEncoding = "DECIMAL64",
      elementDataType = SchemaDataType.FLOAT
  )
  public LongArrayList getDecimalList() {
    return decimalList;
  }

  /**
   * @param value - Decimal List
   */
  public void setDecimalList(@Decimal LongArrayList value) {
    this.decimalList = value;
  }

  /**
   * @return true if Decimal List is not null
   */
  public boolean hasDecimalList() {
    return decimalList != null;
  }

  /**
   */
  public void nullifyDecimalList() {
    this.decimalList = null;
  }

  /**
   * @return Decimal List Of Nullable
   */
  @Decimal
  @SchemaElement
  @SchemaArrayType(
      isNullable = false,
      isElementNullable = true,
      elementEncoding = "DECIMAL64",
      elementDataType = SchemaDataType.FLOAT
  )
  public LongArrayList getDecimalListOfNullable() {
    return decimalListOfNullable;
  }

  /**
   * @param value - Decimal List Of Nullable
   */
  public void setDecimalListOfNullable(@Decimal LongArrayList value) {
    this.decimalListOfNullable = value;
  }

  /**
   * @return true if Decimal List Of Nullable is not null
   */
  public boolean hasDecimalListOfNullable() {
    return decimalListOfNullable != null;
  }

  /**
   */
  public void nullifyDecimalListOfNullable() {
    this.decimalListOfNullable = null;
  }

  /**
   * @return Nullable Decimal List
   */
  @SchemaElement
  @SchemaArrayType(
      isNullable = true,
      isElementNullable = false,
      elementEncoding = "DECIMAL64",
      elementDataType = SchemaDataType.FLOAT
  )
  public LongArrayList getNullableDecimalList() {
    return nullableDecimalList;
  }

  /**
   * @param value - Nullable Decimal List
   */
  public void setNullableDecimalList(LongArrayList value) {
    this.nullableDecimalList = value;
  }

  /**
   * @return true if Nullable Decimal List is not null
   */
  public boolean hasNullableDecimalList() {
    return nullableDecimalList != null;
  }

  /**
   */
  public void nullifyNullableDecimalList() {
    this.nullableDecimalList = null;
  }

  /**
   * @return Nullable Decimal List Of Nullable
   */
  @SchemaElement
  @SchemaArrayType(
      isNullable = true,
      isElementNullable = true,
      elementEncoding = "DECIMAL64",
      elementDataType = SchemaDataType.FLOAT
  )
  public LongArrayList getNullableDecimalListOfNullable() {
    return nullableDecimalListOfNullable;
  }

  /**
   * @param value - Nullable Decimal List Of Nullable
   */
  public void setNullableDecimalListOfNullable(LongArrayList value) {
    this.nullableDecimalListOfNullable = value;
  }

  /**
   * @return true if Nullable Decimal List Of Nullable is not null
   */
  public boolean hasNullableDecimalListOfNullable() {
    return nullableDecimalListOfNullable != null;
  }

  /**
   */
  public void nullifyNullableDecimalListOfNullable() {
    this.nullableDecimalListOfNullable = null;
  }

  /**
   * @return Double List
   */
  @SchemaElement
  @SchemaArrayType(
      isNullable = false,
      isElementNullable = false
  )
  public DoubleArrayList getDoubleList() {
    return doubleList;
  }

  /**
   * @param value - Double List
   */
  public void setDoubleList(DoubleArrayList value) {
    this.doubleList = value;
  }

  /**
   * @return true if Double List is not null
   */
  public boolean hasDoubleList() {
    return doubleList != null;
  }

  /**
   */
  public void nullifyDoubleList() {
    this.doubleList = null;
  }

  /**
   * @return Double List Of Nullable
   */
  @SchemaElement
  @SchemaArrayType(
      isNullable = false,
      isElementNullable = true
  )
  public DoubleArrayList getDoubleListOfNullable() {
    return doubleListOfNullable;
  }

  /**
   * @param value - Double List Of Nullable
   */
  public void setDoubleListOfNullable(DoubleArrayList value) {
    this.doubleListOfNullable = value;
  }

  /**
   * @return true if Double List Of Nullable is not null
   */
  public boolean hasDoubleListOfNullable() {
    return doubleListOfNullable != null;
  }

  /**
   */
  public void nullifyDoubleListOfNullable() {
    this.doubleListOfNullable = null;
  }

  /**
   * @return Nullable Double List
   */
  @SchemaElement
  @SchemaArrayType(
      isNullable = true,
      isElementNullable = false
  )
  public DoubleArrayList getNullableDoubleList() {
    return nullableDoubleList;
  }

  /**
   * @param value - Nullable Double List
   */
  public void setNullableDoubleList(DoubleArrayList value) {
    this.nullableDoubleList = value;
  }

  /**
   * @return true if Nullable Double List is not null
   */
  public boolean hasNullableDoubleList() {
    return nullableDoubleList != null;
  }

  /**
   */
  public void nullifyNullableDoubleList() {
    this.nullableDoubleList = null;
  }

  /**
   * @return Nullable Double List Of Nullable
   */
  @SchemaElement
  @SchemaArrayType(
      isNullable = true,
      isElementNullable = true
  )
  public DoubleArrayList getNullableDoubleListOfNullable() {
    return nullableDoubleListOfNullable;
  }

  /**
   * @param value - Nullable Double List Of Nullable
   */
  public void setNullableDoubleListOfNullable(DoubleArrayList value) {
    this.nullableDoubleListOfNullable = value;
  }

  /**
   * @return true if Nullable Double List Of Nullable is not null
   */
  public boolean hasNullableDoubleListOfNullable() {
    return nullableDoubleListOfNullable != null;
  }

  /**
   */
  public void nullifyNullableDoubleListOfNullable() {
    this.nullableDoubleListOfNullable = null;
  }

  /**
   * @return Float List
   */
  @SchemaElement
  @SchemaArrayType(
      isNullable = false,
      isElementNullable = false
  )
  public FloatArrayList getFloatList() {
    return floatList;
  }

  /**
   * @param value - Float List
   */
  public void setFloatList(FloatArrayList value) {
    this.floatList = value;
  }

  /**
   * @return true if Float List is not null
   */
  public boolean hasFloatList() {
    return floatList != null;
  }

  /**
   */
  public void nullifyFloatList() {
    this.floatList = null;
  }

  /**
   * @return Float List Of Nullable
   */
  @SchemaElement
  @SchemaArrayType(
      isNullable = false,
      isElementNullable = true
  )
  public FloatArrayList getFloatListOfNullable() {
    return floatListOfNullable;
  }

  /**
   * @param value - Float List Of Nullable
   */
  public void setFloatListOfNullable(FloatArrayList value) {
    this.floatListOfNullable = value;
  }

  /**
   * @return true if Float List Of Nullable is not null
   */
  public boolean hasFloatListOfNullable() {
    return floatListOfNullable != null;
  }

  /**
   */
  public void nullifyFloatListOfNullable() {
    this.floatListOfNullable = null;
  }

  /**
   * @return Nullable Float List
   */
  @SchemaElement
  @SchemaArrayType(
      isNullable = true,
      isElementNullable = false
  )
  public FloatArrayList getNullableFloatList() {
    return nullableFloatList;
  }

  /**
   * @param value - Nullable Float List
   */
  public void setNullableFloatList(FloatArrayList value) {
    this.nullableFloatList = value;
  }

  /**
   * @return true if Nullable Float List is not null
   */
  public boolean hasNullableFloatList() {
    return nullableFloatList != null;
  }

  /**
   */
  public void nullifyNullableFloatList() {
    this.nullableFloatList = null;
  }

  /**
   * @return Nullable Float List Of Nullable
   */
  @SchemaElement
  @SchemaArrayType(
      isNullable = true,
      isElementNullable = true
  )
  public FloatArrayList getNullableFloatListOfNullable() {
    return nullableFloatListOfNullable;
  }

  /**
   * @param value - Nullable Float List Of Nullable
   */
  public void setNullableFloatListOfNullable(FloatArrayList value) {
    this.nullableFloatListOfNullable = value;
  }

  /**
   * @return true if Nullable Float List Of Nullable is not null
   */
  public boolean hasNullableFloatListOfNullable() {
    return nullableFloatListOfNullable != null;
  }

  /**
   */
  public void nullifyNullableFloatListOfNullable() {
    this.nullableFloatListOfNullable = null;
  }

  /**
   * @return Text List
   */
  @SchemaElement
  @SchemaArrayType(
      isNullable = false,
      isElementNullable = false
  )
  public ObjectArrayList<CharSequence> getTextList() {
    return textList;
  }

  /**
   * @param value - Text List
   */
  public void setTextList(ObjectArrayList<CharSequence> value) {
    this.textList = value;
  }

  /**
   * @return true if Text List is not null
   */
  public boolean hasTextList() {
    return textList != null;
  }

  /**
   */
  public void nullifyTextList() {
    this.textList = null;
  }

  /**
   * @return Text List Of Nullable
   */
  @SchemaElement
  @SchemaArrayType(
      isNullable = false,
      isElementNullable = true
  )
  public ObjectArrayList<CharSequence> getTextListOfNullable() {
    return textListOfNullable;
  }

  /**
   * @param value - Text List Of Nullable
   */
  public void setTextListOfNullable(ObjectArrayList<CharSequence> value) {
    this.textListOfNullable = value;
  }

  /**
   * @return true if Text List Of Nullable is not null
   */
  public boolean hasTextListOfNullable() {
    return textListOfNullable != null;
  }

  /**
   */
  public void nullifyTextListOfNullable() {
    this.textListOfNullable = null;
  }

  /**
   * @return Nullable Text List
   */
  @SchemaElement
  @SchemaArrayType(
      isNullable = true,
      isElementNullable = false
  )
  public ObjectArrayList<CharSequence> getNullableTextList() {
    return nullableTextList;
  }

  /**
   * @param value - Nullable Text List
   */
  public void setNullableTextList(ObjectArrayList<CharSequence> value) {
    this.nullableTextList = value;
  }

  /**
   * @return true if Nullable Text List is not null
   */
  public boolean hasNullableTextList() {
    return nullableTextList != null;
  }

  /**
   */
  public void nullifyNullableTextList() {
    this.nullableTextList = null;
  }

  /**
   * @return Nullable Text List Of Nullable
   */
  @SchemaElement
  @SchemaArrayType(
      isNullable = true,
      isElementNullable = true
  )
  public ObjectArrayList<CharSequence> getNullableTextListOfNullable() {
    return nullableTextListOfNullable;
  }

  /**
   * @param value - Nullable Text List Of Nullable
   */
  public void setNullableTextListOfNullable(ObjectArrayList<CharSequence> value) {
    this.nullableTextListOfNullable = value;
  }

  /**
   * @return true if Nullable Text List Of Nullable is not null
   */
  public boolean hasNullableTextListOfNullable() {
    return nullableTextListOfNullable != null;
  }

  /**
   */
  public void nullifyNullableTextListOfNullable() {
    this.nullableTextListOfNullable = null;
  }

  /**
   * @return Ascii Text List
   */
  @SchemaElement
  @SchemaArrayType(
      isNullable = false,
      isElementNullable = false
  )
  public ObjectArrayList<CharSequence> getAsciiTextList() {
    return asciiTextList;
  }

  /**
   * @param value - Ascii Text List
   */
  public void setAsciiTextList(ObjectArrayList<CharSequence> value) {
    this.asciiTextList = value;
  }

  /**
   * @return true if Ascii Text List is not null
   */
  public boolean hasAsciiTextList() {
    return asciiTextList != null;
  }

  /**
   */
  public void nullifyAsciiTextList() {
    this.asciiTextList = null;
  }

  /**
   * @return Ascii Text List Of Nullable
   */
  @SchemaElement
  @SchemaArrayType(
      isNullable = false,
      isElementNullable = true
  )
  public ObjectArrayList<CharSequence> getAsciiTextListOfNullable() {
    return asciiTextListOfNullable;
  }

  /**
   * @param value - Ascii Text List Of Nullable
   */
  public void setAsciiTextListOfNullable(ObjectArrayList<CharSequence> value) {
    this.asciiTextListOfNullable = value;
  }

  /**
   * @return true if Ascii Text List Of Nullable is not null
   */
  public boolean hasAsciiTextListOfNullable() {
    return asciiTextListOfNullable != null;
  }

  /**
   */
  public void nullifyAsciiTextListOfNullable() {
    this.asciiTextListOfNullable = null;
  }

  /**
   * @return Nullable Ascii Text List
   */
  @SchemaElement
  @SchemaArrayType(
      isNullable = true,
      isElementNullable = false
  )
  public ObjectArrayList<CharSequence> getNullableAsciiTextList() {
    return nullableAsciiTextList;
  }

  /**
   * @param value - Nullable Ascii Text List
   */
  public void setNullableAsciiTextList(ObjectArrayList<CharSequence> value) {
    this.nullableAsciiTextList = value;
  }

  /**
   * @return true if Nullable Ascii Text List is not null
   */
  public boolean hasNullableAsciiTextList() {
    return nullableAsciiTextList != null;
  }

  /**
   */
  public void nullifyNullableAsciiTextList() {
    this.nullableAsciiTextList = null;
  }

  /**
   * @return Nullable Ascii Text List Of Nullable
   */
  @SchemaElement
  @SchemaArrayType(
      isNullable = true,
      isElementNullable = true
  )
  public ObjectArrayList<CharSequence> getNullableAsciiTextListOfNullable() {
    return nullableAsciiTextListOfNullable;
  }

  /**
   * @param value - Nullable Ascii Text List Of Nullable
   */
  public void setNullableAsciiTextListOfNullable(ObjectArrayList<CharSequence> value) {
    this.nullableAsciiTextListOfNullable = value;
  }

  /**
   * @return true if Nullable Ascii Text List Of Nullable is not null
   */
  public boolean hasNullableAsciiTextListOfNullable() {
    return nullableAsciiTextListOfNullable != null;
  }

  /**
   */
  public void nullifyNullableAsciiTextListOfNullable() {
    this.nullableAsciiTextListOfNullable = null;
  }

  /**
   * @return Alphanumeric List
   */
  @SchemaElement
  @SchemaArrayType(
      isNullable = false,
      isElementNullable = false,
      elementEncoding = "ALPHANUMERIC(10)",
      elementDataType = SchemaDataType.VARCHAR
  )
  public LongArrayList getAlphanumericList() {
    return alphanumericList;
  }

  /**
   * @param value - Alphanumeric List
   */
  public void setAlphanumericList(LongArrayList value) {
    this.alphanumericList = value;
  }

  /**
   * @return true if Alphanumeric List is not null
   */
  public boolean hasAlphanumericList() {
    return alphanumericList != null;
  }

  /**
   */
  public void nullifyAlphanumericList() {
    this.alphanumericList = null;
  }

  /**
   * @return Alphanumeric List Of Nullable
   */
  @SchemaElement
  @SchemaArrayType(
      isNullable = false,
      isElementNullable = true,
      elementEncoding = "ALPHANUMERIC(10)",
      elementDataType = SchemaDataType.VARCHAR
  )
  public LongArrayList getAlphanumericListOfNullable() {
    return alphanumericListOfNullable;
  }

  /**
   * @param value - Alphanumeric List Of Nullable
   */
  public void setAlphanumericListOfNullable(LongArrayList value) {
    this.alphanumericListOfNullable = value;
  }

  /**
   * @return true if Alphanumeric List Of Nullable is not null
   */
  public boolean hasAlphanumericListOfNullable() {
    return alphanumericListOfNullable != null;
  }

  /**
   */
  public void nullifyAlphanumericListOfNullable() {
    this.alphanumericListOfNullable = null;
  }

  /**
   * @return Nullable Alphanumeric List
   */
  @SchemaElement
  @SchemaArrayType(
      isNullable = true,
      isElementNullable = false,
      elementEncoding = "ALPHANUMERIC(10)",
      elementDataType = SchemaDataType.VARCHAR
  )
  public LongArrayList getNullableAlphanumericList() {
    return nullableAlphanumericList;
  }

  /**
   * @param value - Nullable Alphanumeric List
   */
  public void setNullableAlphanumericList(LongArrayList value) {
    this.nullableAlphanumericList = value;
  }

  /**
   * @return true if Nullable Alphanumeric List is not null
   */
  public boolean hasNullableAlphanumericList() {
    return nullableAlphanumericList != null;
  }

  /**
   */
  public void nullifyNullableAlphanumericList() {
    this.nullableAlphanumericList = null;
  }

  /**
   * @return Nullable Alphanumeric List Of Nullable
   */
  @SchemaElement
  @SchemaArrayType(
      isNullable = true,
      isElementNullable = true,
      elementEncoding = "ALPHANUMERIC(10)",
      elementDataType = SchemaDataType.VARCHAR
  )
  public LongArrayList getNullableAlphanumericListOfNullable() {
    return nullableAlphanumericListOfNullable;
  }

  /**
   * @param value - Nullable Alphanumeric List Of Nullable
   */
  public void setNullableAlphanumericListOfNullable(LongArrayList value) {
    this.nullableAlphanumericListOfNullable = value;
  }

  /**
   * @return true if Nullable Alphanumeric List Of Nullable is not null
   */
  public boolean hasNullableAlphanumericListOfNullable() {
    return nullableAlphanumericListOfNullable != null;
  }

  /**
   */
  public void nullifyNullableAlphanumericListOfNullable() {
    this.nullableAlphanumericListOfNullable = null;
  }

  /**
   * @return Objects List
   */
  @SchemaElement
  @SchemaArrayType(
      isNullable = false,
      isElementNullable = false,
      elementTypes =  {
            AllSimpleTypesMessage.class}

  )
  public ObjectArrayList<AllSimpleTypesMessageInfo> getObjectsList() {
    return objectsList;
  }

  /**
   * @param value - Objects List
   */
  public void setObjectsList(ObjectArrayList<AllSimpleTypesMessageInfo> value) {
    this.objectsList = value;
  }

  /**
   * @return true if Objects List is not null
   */
  public boolean hasObjectsList() {
    return objectsList != null;
  }

  /**
   */
  public void nullifyObjectsList() {
    this.objectsList = null;
  }

  /**
   * @return Objects List Of Nullable
   */
  @SchemaElement
  @SchemaArrayType(
      isNullable = false,
      isElementNullable = true,
      elementTypes =  {
            AllSimpleTypesMessage.class}

  )
  public ObjectArrayList<AllSimpleTypesMessageInfo> getObjectsListOfNullable() {
    return objectsListOfNullable;
  }

  /**
   * @param value - Objects List Of Nullable
   */
  public void setObjectsListOfNullable(ObjectArrayList<AllSimpleTypesMessageInfo> value) {
    this.objectsListOfNullable = value;
  }

  /**
   * @return true if Objects List Of Nullable is not null
   */
  public boolean hasObjectsListOfNullable() {
    return objectsListOfNullable != null;
  }

  /**
   */
  public void nullifyObjectsListOfNullable() {
    this.objectsListOfNullable = null;
  }

  /**
   * @return Nullable Objects List
   */
  @SchemaElement
  @SchemaArrayType(
      isNullable = true,
      isElementNullable = false,
      elementTypes =  {
            AllSimpleTypesMessage.class}

  )
  public ObjectArrayList<AllSimpleTypesMessageInfo> getNullableObjectsList() {
    return nullableObjectsList;
  }

  /**
   * @param value - Nullable Objects List
   */
  public void setNullableObjectsList(ObjectArrayList<AllSimpleTypesMessageInfo> value) {
    this.nullableObjectsList = value;
  }

  /**
   * @return true if Nullable Objects List is not null
   */
  public boolean hasNullableObjectsList() {
    return nullableObjectsList != null;
  }

  /**
   */
  public void nullifyNullableObjectsList() {
    this.nullableObjectsList = null;
  }

  /**
   * @return Nullable Objects List Of Nullable
   */
  @SchemaElement
  @SchemaArrayType(
      isNullable = true,
      isElementNullable = true,
      elementTypes =  {
            AllSimpleTypesMessage.class}

  )
  public ObjectArrayList<AllSimpleTypesMessageInfo> getNullableObjectsListOfNullable() {
    return nullableObjectsListOfNullable;
  }

  /**
   * @param value - Nullable Objects List Of Nullable
   */
  public void setNullableObjectsListOfNullable(ObjectArrayList<AllSimpleTypesMessageInfo> value) {
    this.nullableObjectsListOfNullable = value;
  }

  /**
   * @return true if Nullable Objects List Of Nullable is not null
   */
  public boolean hasNullableObjectsListOfNullable() {
    return nullableObjectsListOfNullable != null;
  }

  /**
   */
  public void nullifyNullableObjectsListOfNullable() {
    this.nullableObjectsListOfNullable = null;
  }

  /**
   * @return List Of Lists
   */
  @SchemaElement
  @SchemaArrayType(
      isNullable = false,
      isElementNullable = false,
      elementTypes =  {
            AllListsMessage.class}

  )
  public ObjectArrayList<AllListsMessageInfo> getListOfLists() {
    return listOfLists;
  }

  /**
   * @param value - List Of Lists
   */
  public void setListOfLists(ObjectArrayList<AllListsMessageInfo> value) {
    this.listOfLists = value;
  }

  /**
   * @return true if List Of Lists is not null
   */
  public boolean hasListOfLists() {
    return listOfLists != null;
  }

  /**
   */
  public void nullifyListOfLists() {
    this.listOfLists = null;
  }

  /**
   * @return Timestamp List
   */
  @SchemaElement
  @SchemaArrayType(
      isNullable = false,
      isElementNullable = false,
      elementDataType = SchemaDataType.TIMESTAMP
  )
  public LongArrayList getTimestampList() {
    return timestampList;
  }

  /**
   * @param value - Timestamp List
   */
  public void setTimestampList(LongArrayList value) {
    this.timestampList = value;
  }

  /**
   * @return true if Timestamp List is not null
   */
  public boolean hasTimestampList() {
    return timestampList != null;
  }

  /**
   */
  public void nullifyTimestampList() {
    this.timestampList = null;
  }

  /**
   * @return Timestamp List Of Nullable
   */
  @SchemaElement
  @SchemaArrayType(
      isNullable = false,
      isElementNullable = true,
      elementDataType = SchemaDataType.TIMESTAMP
  )
  public LongArrayList getTimestampListOfNullable() {
    return timestampListOfNullable;
  }

  /**
   * @param value - Timestamp List Of Nullable
   */
  public void setTimestampListOfNullable(LongArrayList value) {
    this.timestampListOfNullable = value;
  }

  /**
   * @return true if Timestamp List Of Nullable is not null
   */
  public boolean hasTimestampListOfNullable() {
    return timestampListOfNullable != null;
  }

  /**
   */
  public void nullifyTimestampListOfNullable() {
    this.timestampListOfNullable = null;
  }

  /**
   * @return Nullable Timestamp List
   */
  @SchemaElement
  @SchemaArrayType(
      isNullable = true,
      isElementNullable = false,
      elementDataType = SchemaDataType.TIMESTAMP
  )
  public LongArrayList getNullableTimestampList() {
    return nullableTimestampList;
  }

  /**
   * @param value - Nullable Timestamp List
   */
  public void setNullableTimestampList(LongArrayList value) {
    this.nullableTimestampList = value;
  }

  /**
   * @return true if Nullable Timestamp List is not null
   */
  public boolean hasNullableTimestampList() {
    return nullableTimestampList != null;
  }

  /**
   */
  public void nullifyNullableTimestampList() {
    this.nullableTimestampList = null;
  }

  /**
   * @return Nullable Timestamp List Of Nullable
   */
  @SchemaElement
  @SchemaArrayType(
      isNullable = true,
      isElementNullable = true,
      elementDataType = SchemaDataType.TIMESTAMP
  )
  public LongArrayList getNullableTimestampListOfNullable() {
    return nullableTimestampListOfNullable;
  }

  /**
   * @param value - Nullable Timestamp List Of Nullable
   */
  public void setNullableTimestampListOfNullable(LongArrayList value) {
    this.nullableTimestampListOfNullable = value;
  }

  /**
   * @return true if Nullable Timestamp List Of Nullable is not null
   */
  public boolean hasNullableTimestampListOfNullable() {
    return nullableTimestampListOfNullable != null;
  }

  /**
   */
  public void nullifyNullableTimestampListOfNullable() {
    this.nullableTimestampListOfNullable = null;
  }

  /**
   * @return Time Of Day List
   */
  @SchemaElement
  @SchemaArrayType(
      isNullable = false,
      isElementNullable = false,
      elementDataType = SchemaDataType.TIME_OF_DAY
  )
  public IntegerArrayList getTimeOfDayList() {
    return timeOfDayList;
  }

  /**
   * @param value - Time Of Day List
   */
  public void setTimeOfDayList(IntegerArrayList value) {
    this.timeOfDayList = value;
  }

  /**
   * @return true if Time Of Day List is not null
   */
  public boolean hasTimeOfDayList() {
    return timeOfDayList != null;
  }

  /**
   */
  public void nullifyTimeOfDayList() {
    this.timeOfDayList = null;
  }

  /**
   * @return Time Of Day List Of Nullable
   */
  @SchemaElement
  @SchemaArrayType(
      isNullable = false,
      isElementNullable = true,
      elementDataType = SchemaDataType.TIME_OF_DAY
  )
  public IntegerArrayList getTimeOfDayListOfNullable() {
    return timeOfDayListOfNullable;
  }

  /**
   * @param value - Time Of Day List Of Nullable
   */
  public void setTimeOfDayListOfNullable(IntegerArrayList value) {
    this.timeOfDayListOfNullable = value;
  }

  /**
   * @return true if Time Of Day List Of Nullable is not null
   */
  public boolean hasTimeOfDayListOfNullable() {
    return timeOfDayListOfNullable != null;
  }

  /**
   */
  public void nullifyTimeOfDayListOfNullable() {
    this.timeOfDayListOfNullable = null;
  }

  /**
   * @return Nullable Time Of Day List
   */
  @SchemaElement
  @SchemaArrayType(
      isNullable = true,
      isElementNullable = false,
      elementDataType = SchemaDataType.TIME_OF_DAY
  )
  public IntegerArrayList getNullableTimeOfDayList() {
    return nullableTimeOfDayList;
  }

  /**
   * @param value - Nullable Time Of Day List
   */
  public void setNullableTimeOfDayList(IntegerArrayList value) {
    this.nullableTimeOfDayList = value;
  }

  /**
   * @return true if Nullable Time Of Day List is not null
   */
  public boolean hasNullableTimeOfDayList() {
    return nullableTimeOfDayList != null;
  }

  /**
   */
  public void nullifyNullableTimeOfDayList() {
    this.nullableTimeOfDayList = null;
  }

  /**
   * @return Nullable Time Of Day List Of Nullable
   */
  @SchemaElement
  @SchemaArrayType(
      isNullable = true,
      isElementNullable = true,
      elementDataType = SchemaDataType.TIME_OF_DAY
  )
  public IntegerArrayList getNullableTimeOfDayListOfNullable() {
    return nullableTimeOfDayListOfNullable;
  }

  /**
   * @param value - Nullable Time Of Day List Of Nullable
   */
  public void setNullableTimeOfDayListOfNullable(IntegerArrayList value) {
    this.nullableTimeOfDayListOfNullable = value;
  }

  /**
   * @return true if Nullable Time Of Day List Of Nullable is not null
   */
  public boolean hasNullableTimeOfDayListOfNullable() {
    return nullableTimeOfDayListOfNullable != null;
  }

  /**
   */
  public void nullifyNullableTimeOfDayListOfNullable() {
    this.nullableTimeOfDayListOfNullable = null;
  }

  /**
   * @return Enum List
   */
  @SchemaElement
  @SchemaArrayType(
      isNullable = false,
      isElementNullable = false
  )
  public ObjectArrayList<TestEnum> getEnumList() {
    return enumList;
  }

  /**
   * @param value - Enum List
   */
  public void setEnumList(ObjectArrayList<TestEnum> value) {
    this.enumList = value;
  }

  /**
   * @return true if Enum List is not null
   */
  public boolean hasEnumList() {
    return enumList != null;
  }

  /**
   */
  public void nullifyEnumList() {
    this.enumList = null;
  }

  /**
   * @return Enum List Of Nullable
   */
  @SchemaElement
  @SchemaArrayType(
      isNullable = false,
      isElementNullable = true
  )
  public ObjectArrayList<TestEnum> getEnumListOfNullable() {
    return enumListOfNullable;
  }

  /**
   * @param value - Enum List Of Nullable
   */
  public void setEnumListOfNullable(ObjectArrayList<TestEnum> value) {
    this.enumListOfNullable = value;
  }

  /**
   * @return true if Enum List Of Nullable is not null
   */
  public boolean hasEnumListOfNullable() {
    return enumListOfNullable != null;
  }

  /**
   */
  public void nullifyEnumListOfNullable() {
    this.enumListOfNullable = null;
  }

  /**
   * @return Nullable Enum List
   */
  @SchemaElement
  @SchemaArrayType(
      isNullable = true,
      isElementNullable = false
  )
  public ObjectArrayList<TestEnum> getNullableEnumList() {
    return nullableEnumList;
  }

  /**
   * @param value - Nullable Enum List
   */
  public void setNullableEnumList(ObjectArrayList<TestEnum> value) {
    this.nullableEnumList = value;
  }

  /**
   * @return true if Nullable Enum List is not null
   */
  public boolean hasNullableEnumList() {
    return nullableEnumList != null;
  }

  /**
   */
  public void nullifyNullableEnumList() {
    this.nullableEnumList = null;
  }

  /**
   * @return Nullable Enum List Of Nullable
   */
  @SchemaElement
  @SchemaArrayType(
      isNullable = true,
      isElementNullable = true
  )
  public ObjectArrayList<TestEnum> getNullableEnumListOfNullable() {
    return nullableEnumListOfNullable;
  }

  /**
   * @param value - Nullable Enum List Of Nullable
   */
  public void setNullableEnumListOfNullable(ObjectArrayList<TestEnum> value) {
    this.nullableEnumListOfNullable = value;
  }

  /**
   * @return true if Nullable Enum List Of Nullable is not null
   */
  public boolean hasNullableEnumListOfNullable() {
    return nullableEnumListOfNullable != null;
  }

  /**
   */
  public void nullifyNullableEnumListOfNullable() {
    this.nullableEnumListOfNullable = null;
  }

  /**
   * Creates new instance of this class.
   * @return new instance of this class.
   */
  @Override
  protected AllTypesMessage createInstance() {
    return new AllTypesMessage();
  }

  /**
   * Method nullifies all instance properties
   */
  @Override
  public AllTypesMessage nullify() {
    super.nullify();
    nullifyObject();
    nullifyLists();
    nullifyBooleanList();
    nullifyBooleanListOfNullable();
    nullifyNullableBooleanList();
    nullifyNullableBooleanListOfNullable();
    nullifyByteList();
    nullifyByteListOfNullable();
    nullifyNullableByteList();
    nullifyNullableByteListOfNullable();
    nullifyShortList();
    nullifyShortListOfNullable();
    nullifyNullableShortList();
    nullifyNullableShortListOfNullable();
    nullifyIntList();
    nullifyIntListOfNullable();
    nullifyNullableIntList();
    nullifyNullableIntListOfNullable();
    nullifyLongList();
    nullifyLongListOfNullable();
    nullifyNullableLongList();
    nullifyNullableLongListOfNullable();
    nullifyDecimalList();
    nullifyDecimalListOfNullable();
    nullifyNullableDecimalList();
    nullifyNullableDecimalListOfNullable();
    nullifyDoubleList();
    nullifyDoubleListOfNullable();
    nullifyNullableDoubleList();
    nullifyNullableDoubleListOfNullable();
    nullifyFloatList();
    nullifyFloatListOfNullable();
    nullifyNullableFloatList();
    nullifyNullableFloatListOfNullable();
    nullifyTextList();
    nullifyTextListOfNullable();
    nullifyNullableTextList();
    nullifyNullableTextListOfNullable();
    nullifyAsciiTextList();
    nullifyAsciiTextListOfNullable();
    nullifyNullableAsciiTextList();
    nullifyNullableAsciiTextListOfNullable();
    nullifyAlphanumericList();
    nullifyAlphanumericListOfNullable();
    nullifyNullableAlphanumericList();
    nullifyNullableAlphanumericListOfNullable();
    nullifyObjectsList();
    nullifyObjectsListOfNullable();
    nullifyNullableObjectsList();
    nullifyNullableObjectsListOfNullable();
    nullifyListOfLists();
    nullifyTimestampList();
    nullifyTimestampListOfNullable();
    nullifyNullableTimestampList();
    nullifyNullableTimestampListOfNullable();
    nullifyTimeOfDayList();
    nullifyTimeOfDayListOfNullable();
    nullifyNullableTimeOfDayList();
    nullifyNullableTimeOfDayListOfNullable();
    nullifyEnumList();
    nullifyEnumListOfNullable();
    nullifyNullableEnumList();
    nullifyNullableEnumListOfNullable();
    return this;
  }

  /**
   * Resets all instance properties to their default values
   */
  @Override
  public AllTypesMessage reset() {
    super.reset();
    object = null;
    lists = null;
    booleanList = null;
    booleanListOfNullable = null;
    nullableBooleanList = null;
    nullableBooleanListOfNullable = null;
    byteList = null;
    byteListOfNullable = null;
    nullableByteList = null;
    nullableByteListOfNullable = null;
    shortList = null;
    shortListOfNullable = null;
    nullableShortList = null;
    nullableShortListOfNullable = null;
    intList = null;
    intListOfNullable = null;
    nullableIntList = null;
    nullableIntListOfNullable = null;
    longList = null;
    longListOfNullable = null;
    nullableLongList = null;
    nullableLongListOfNullable = null;
    decimalList = null;
    decimalListOfNullable = null;
    nullableDecimalList = null;
    nullableDecimalListOfNullable = null;
    doubleList = null;
    doubleListOfNullable = null;
    nullableDoubleList = null;
    nullableDoubleListOfNullable = null;
    floatList = null;
    floatListOfNullable = null;
    nullableFloatList = null;
    nullableFloatListOfNullable = null;
    textList = null;
    textListOfNullable = null;
    nullableTextList = null;
    nullableTextListOfNullable = null;
    asciiTextList = null;
    asciiTextListOfNullable = null;
    nullableAsciiTextList = null;
    nullableAsciiTextListOfNullable = null;
    alphanumericList = null;
    alphanumericListOfNullable = null;
    nullableAlphanumericList = null;
    nullableAlphanumericListOfNullable = null;
    objectsList = null;
    objectsListOfNullable = null;
    nullableObjectsList = null;
    nullableObjectsListOfNullable = null;
    listOfLists = null;
    timestampList = null;
    timestampListOfNullable = null;
    nullableTimestampList = null;
    nullableTimestampListOfNullable = null;
    timeOfDayList = null;
    timeOfDayListOfNullable = null;
    nullableTimeOfDayList = null;
    nullableTimeOfDayListOfNullable = null;
    enumList = null;
    enumListOfNullable = null;
    nullableEnumList = null;
    nullableEnumListOfNullable = null;
    return this;
  }

  /**
   * Method copies state to a given instance
   */
  @Override
  public AllTypesMessage clone() {
    AllTypesMessage t = createInstance();
    t.copyFrom(this);
    return t;
  }

//  /**
//   * Indicates whether some other object is "equal to" this one.
//   */
//  @Override
//  public boolean equals(Object obj) {
//    if (this == obj) return true;
//    boolean superEquals = super.equals(obj);
//    if (!superEquals) return false;
//    if (!(obj instanceof AllTypesMessageInfo)) return false;
//    AllTypesMessageInfo other =(AllTypesMessageInfo)obj;
//    if (hasObject() != other.hasObject()) return false;
//    if (hasObject() && !(getObject().equals(other.getObject()))) return false;
//    if (hasLists() != other.hasLists()) return false;
//    if (hasLists() && !(getLists().equals(other.getLists()))) return false;
//    if (hasBooleanList() != other.hasBooleanList()) return false;
//    if (hasBooleanList()) {
//      if (getBooleanList().size() != other.getBooleanList().size()) return false;
//      else for (int j = 0; j < getBooleanList().size(); ++j) {
//        if (getBooleanList().get(j) != other.getBooleanList().get(j)) return false;
//      }
//    }
//    if (hasBooleanListOfNullable() != other.hasBooleanListOfNullable()) return false;
//    if (hasBooleanListOfNullable()) {
//      if (getBooleanListOfNullable().size() != other.getBooleanListOfNullable().size()) return false;
//      else for (int j = 0; j < getBooleanListOfNullable().size(); ++j) {
//        if (getBooleanListOfNullable().get(j) != other.getBooleanListOfNullable().get(j)) return false;
//      }
//    }
//    if (hasNullableBooleanList() != other.hasNullableBooleanList()) return false;
//    if (hasNullableBooleanList()) {
//      if (getNullableBooleanList().size() != other.getNullableBooleanList().size()) return false;
//      else for (int j = 0; j < getNullableBooleanList().size(); ++j) {
//        if (getNullableBooleanList().get(j) != other.getNullableBooleanList().get(j)) return false;
//      }
//    }
//    if (hasNullableBooleanListOfNullable() != other.hasNullableBooleanListOfNullable()) return false;
//    if (hasNullableBooleanListOfNullable()) {
//      if (getNullableBooleanListOfNullable().size() != other.getNullableBooleanListOfNullable().size()) return false;
//      else for (int j = 0; j < getNullableBooleanListOfNullable().size(); ++j) {
//        if (getNullableBooleanListOfNullable().get(j) != other.getNullableBooleanListOfNullable().get(j)) return false;
//      }
//    }
//    if (hasByteList() != other.hasByteList()) return false;
//    if (hasByteList()) {
//      if (getByteList().size() != other.getByteList().size()) return false;
//      else for (int j = 0; j < getByteList().size(); ++j) {
//        if (getByteList().get(j) != other.getByteList().get(j)) return false;
//      }
//    }
//    if (hasByteListOfNullable() != other.hasByteListOfNullable()) return false;
//    if (hasByteListOfNullable()) {
//      if (getByteListOfNullable().size() != other.getByteListOfNullable().size()) return false;
//      else for (int j = 0; j < getByteListOfNullable().size(); ++j) {
//        if (getByteListOfNullable().get(j) != other.getByteListOfNullable().get(j)) return false;
//      }
//    }
//    if (hasNullableByteList() != other.hasNullableByteList()) return false;
//    if (hasNullableByteList()) {
//      if (getNullableByteList().size() != other.getNullableByteList().size()) return false;
//      else for (int j = 0; j < getNullableByteList().size(); ++j) {
//        if (getNullableByteList().get(j) != other.getNullableByteList().get(j)) return false;
//      }
//    }
//    if (hasNullableByteListOfNullable() != other.hasNullableByteListOfNullable()) return false;
//    if (hasNullableByteListOfNullable()) {
//      if (getNullableByteListOfNullable().size() != other.getNullableByteListOfNullable().size()) return false;
//      else for (int j = 0; j < getNullableByteListOfNullable().size(); ++j) {
//        if (getNullableByteListOfNullable().get(j) != other.getNullableByteListOfNullable().get(j)) return false;
//      }
//    }
//    if (hasShortList() != other.hasShortList()) return false;
//    if (hasShortList()) {
//      if (getShortList().size() != other.getShortList().size()) return false;
//      else for (int j = 0; j < getShortList().size(); ++j) {
//        if (getShortList().get(j) != other.getShortList().get(j)) return false;
//      }
//    }
//    if (hasShortListOfNullable() != other.hasShortListOfNullable()) return false;
//    if (hasShortListOfNullable()) {
//      if (getShortListOfNullable().size() != other.getShortListOfNullable().size()) return false;
//      else for (int j = 0; j < getShortListOfNullable().size(); ++j) {
//        if (getShortListOfNullable().get(j) != other.getShortListOfNullable().get(j)) return false;
//      }
//    }
//    if (hasNullableShortList() != other.hasNullableShortList()) return false;
//    if (hasNullableShortList()) {
//      if (getNullableShortList().size() != other.getNullableShortList().size()) return false;
//      else for (int j = 0; j < getNullableShortList().size(); ++j) {
//        if (getNullableShortList().get(j) != other.getNullableShortList().get(j)) return false;
//      }
//    }
//    if (hasNullableShortListOfNullable() != other.hasNullableShortListOfNullable()) return false;
//    if (hasNullableShortListOfNullable()) {
//      if (getNullableShortListOfNullable().size() != other.getNullableShortListOfNullable().size()) return false;
//      else for (int j = 0; j < getNullableShortListOfNullable().size(); ++j) {
//        if (getNullableShortListOfNullable().get(j) != other.getNullableShortListOfNullable().get(j)) return false;
//      }
//    }
//    if (hasIntList() != other.hasIntList()) return false;
//    if (hasIntList()) {
//      if (getIntList().size() != other.getIntList().size()) return false;
//      else for (int j = 0; j < getIntList().size(); ++j) {
//        if (getIntList().get(j) != other.getIntList().get(j)) return false;
//      }
//    }
//    if (hasIntListOfNullable() != other.hasIntListOfNullable()) return false;
//    if (hasIntListOfNullable()) {
//      if (getIntListOfNullable().size() != other.getIntListOfNullable().size()) return false;
//      else for (int j = 0; j < getIntListOfNullable().size(); ++j) {
//        if (getIntListOfNullable().get(j) != other.getIntListOfNullable().get(j)) return false;
//      }
//    }
//    if (hasNullableIntList() != other.hasNullableIntList()) return false;
//    if (hasNullableIntList()) {
//      if (getNullableIntList().size() != other.getNullableIntList().size()) return false;
//      else for (int j = 0; j < getNullableIntList().size(); ++j) {
//        if (getNullableIntList().get(j) != other.getNullableIntList().get(j)) return false;
//      }
//    }
//    if (hasNullableIntListOfNullable() != other.hasNullableIntListOfNullable()) return false;
//    if (hasNullableIntListOfNullable()) {
//      if (getNullableIntListOfNullable().size() != other.getNullableIntListOfNullable().size()) return false;
//      else for (int j = 0; j < getNullableIntListOfNullable().size(); ++j) {
//        if (getNullableIntListOfNullable().get(j) != other.getNullableIntListOfNullable().get(j)) return false;
//      }
//    }
//    if (hasLongList() != other.hasLongList()) return false;
//    if (hasLongList()) {
//      if (getLongList().size() != other.getLongList().size()) return false;
//      else for (int j = 0; j < getLongList().size(); ++j) {
//        if (getLongList().get(j) != other.getLongList().get(j)) return false;
//      }
//    }
//    if (hasLongListOfNullable() != other.hasLongListOfNullable()) return false;
//    if (hasLongListOfNullable()) {
//      if (getLongListOfNullable().size() != other.getLongListOfNullable().size()) return false;
//      else for (int j = 0; j < getLongListOfNullable().size(); ++j) {
//        if (getLongListOfNullable().get(j) != other.getLongListOfNullable().get(j)) return false;
//      }
//    }
//    if (hasNullableLongList() != other.hasNullableLongList()) return false;
//    if (hasNullableLongList()) {
//      if (getNullableLongList().size() != other.getNullableLongList().size()) return false;
//      else for (int j = 0; j < getNullableLongList().size(); ++j) {
//        if (getNullableLongList().get(j) != other.getNullableLongList().get(j)) return false;
//      }
//    }
//    if (hasNullableLongListOfNullable() != other.hasNullableLongListOfNullable()) return false;
//    if (hasNullableLongListOfNullable()) {
//      if (getNullableLongListOfNullable().size() != other.getNullableLongListOfNullable().size()) return false;
//      else for (int j = 0; j < getNullableLongListOfNullable().size(); ++j) {
//        if (getNullableLongListOfNullable().get(j) != other.getNullableLongListOfNullable().get(j)) return false;
//      }
//    }
//    if (hasDecimalList() != other.hasDecimalList()) return false;
//    if (hasDecimalList()) {
//      if (getDecimalList().size() != other.getDecimalList().size()) return false;
//      else for (int j = 0; j < getDecimalList().size(); ++j) {
//        if ( !Decimal64Utils.equals((long)getDecimalList().get(j), (long)other.getDecimalList().get(j))) return false;
//      }
//    }
//    if (hasDecimalListOfNullable() != other.hasDecimalListOfNullable()) return false;
//    if (hasDecimalListOfNullable()) {
//      if (getDecimalListOfNullable().size() != other.getDecimalListOfNullable().size()) return false;
//      else for (int j = 0; j < getDecimalListOfNullable().size(); ++j) {
//        if ( !Decimal64Utils.equals((long)getDecimalListOfNullable().get(j), (long)other.getDecimalListOfNullable().get(j))) return false;
//      }
//    }
//    if (hasNullableDecimalList() != other.hasNullableDecimalList()) return false;
//    if (hasNullableDecimalList()) {
//      if (getNullableDecimalList().size() != other.getNullableDecimalList().size()) return false;
//      else for (int j = 0; j < getNullableDecimalList().size(); ++j) {
//        if ( !Decimal64Utils.equals((long)getNullableDecimalList().get(j), (long)other.getNullableDecimalList().get(j))) return false;
//      }
//    }
//    if (hasNullableDecimalListOfNullable() != other.hasNullableDecimalListOfNullable()) return false;
//    if (hasNullableDecimalListOfNullable()) {
//      if (getNullableDecimalListOfNullable().size() != other.getNullableDecimalListOfNullable().size()) return false;
//      else for (int j = 0; j < getNullableDecimalListOfNullable().size(); ++j) {
//        if ( !Decimal64Utils.equals((long)getNullableDecimalListOfNullable().get(j), (long)other.getNullableDecimalListOfNullable().get(j))) return false;
//      }
//    }
//    if (hasDoubleList() != other.hasDoubleList()) return false;
//    if (hasDoubleList()) {
//      if (getDoubleList().size() != other.getDoubleList().size()) return false;
//      else for (int j = 0; j < getDoubleList().size(); ++j) {
//        if (getDoubleList().get(j) != other.getDoubleList().get(j)) return false;
//      }
//    }
//    if (hasDoubleListOfNullable() != other.hasDoubleListOfNullable()) return false;
//    if (hasDoubleListOfNullable()) {
//      if (getDoubleListOfNullable().size() != other.getDoubleListOfNullable().size()) return false;
//      else for (int j = 0; j < getDoubleListOfNullable().size(); ++j) {
//        if (getDoubleListOfNullable().get(j) != other.getDoubleListOfNullable().get(j)) return false;
//      }
//    }
//    if (hasNullableDoubleList() != other.hasNullableDoubleList()) return false;
//    if (hasNullableDoubleList()) {
//      if (getNullableDoubleList().size() != other.getNullableDoubleList().size()) return false;
//      else for (int j = 0; j < getNullableDoubleList().size(); ++j) {
//        if (getNullableDoubleList().get(j) != other.getNullableDoubleList().get(j)) return false;
//      }
//    }
//    if (hasNullableDoubleListOfNullable() != other.hasNullableDoubleListOfNullable()) return false;
//    if (hasNullableDoubleListOfNullable()) {
//      if (getNullableDoubleListOfNullable().size() != other.getNullableDoubleListOfNullable().size()) return false;
//      else for (int j = 0; j < getNullableDoubleListOfNullable().size(); ++j) {
//        if (getNullableDoubleListOfNullable().get(j) != other.getNullableDoubleListOfNullable().get(j)) return false;
//      }
//    }
//    if (hasFloatList() != other.hasFloatList()) return false;
//    if (hasFloatList()) {
//      if (getFloatList().size() != other.getFloatList().size()) return false;
//      else for (int j = 0; j < getFloatList().size(); ++j) {
//      }
//    }
//    if (hasFloatListOfNullable() != other.hasFloatListOfNullable()) return false;
//    if (hasFloatListOfNullable()) {
//      if (getFloatListOfNullable().size() != other.getFloatListOfNullable().size()) return false;
//      else for (int j = 0; j < getFloatListOfNullable().size(); ++j) {
//      }
//    }
//    if (hasNullableFloatList() != other.hasNullableFloatList()) return false;
//    if (hasNullableFloatList()) {
//      if (getNullableFloatList().size() != other.getNullableFloatList().size()) return false;
//      else for (int j = 0; j < getNullableFloatList().size(); ++j) {
//      }
//    }
//    if (hasNullableFloatListOfNullable() != other.hasNullableFloatListOfNullable()) return false;
//    if (hasNullableFloatListOfNullable()) {
//      if (getNullableFloatListOfNullable().size() != other.getNullableFloatListOfNullable().size()) return false;
//      else for (int j = 0; j < getNullableFloatListOfNullable().size(); ++j) {
//      }
//    }
//    if (hasTextList() != other.hasTextList()) return false;
//    if (hasTextList()) {
//      if (getTextList().size() != other.getTextList().size()) return false;
//      else for (int j = 0; j < getTextList().size(); ++j) {
//        if ((getTextList().get(j) != null) != (other.getTextList().get(j) != null)) return false;
//        if (getTextList().get(j) != null && getTextList().get(j).length() != other.getTextList().get(j).length()) return false; else {
//          CharSequence s1 = getTextList().get(j);
//          CharSequence s2 = other.getTextList().get(j);
//          if ((s1 instanceof MutableString && s2 instanceof MutableString) || (s1 instanceof String && s2 instanceof String) || (s1 instanceof BinaryAsciiString && s2 instanceof BinaryAsciiString)) {
//            if (!s1.equals(s2)) return false;
//          } else {
//            return CharSequenceUtils.equals(s1, s2);
//          }
//        }
//      }
//    }
//    if (hasTextListOfNullable() != other.hasTextListOfNullable()) return false;
//    if (hasTextListOfNullable()) {
//      if (getTextListOfNullable().size() != other.getTextListOfNullable().size()) return false;
//      else for (int j = 0; j < getTextListOfNullable().size(); ++j) {
//        if ((getTextListOfNullable().get(j) != null) != (other.getTextListOfNullable().get(j) != null)) return false;
//        if (getTextListOfNullable().get(j) != null && getTextListOfNullable().get(j).length() != other.getTextListOfNullable().get(j).length()) return false; else {
//          CharSequence s1 = getTextListOfNullable().get(j);
//          CharSequence s2 = other.getTextListOfNullable().get(j);
//          if ((s1 instanceof MutableString && s2 instanceof MutableString) || (s1 instanceof String && s2 instanceof String) || (s1 instanceof BinaryAsciiString && s2 instanceof BinaryAsciiString)) {
//            if (!s1.equals(s2)) return false;
//          } else {
//            return CharSequenceUtils.equals(s1, s2);
//          }
//        }
//      }
//    }
//    if (hasNullableTextList() != other.hasNullableTextList()) return false;
//    if (hasNullableTextList()) {
//      if (getNullableTextList().size() != other.getNullableTextList().size()) return false;
//      else for (int j = 0; j < getNullableTextList().size(); ++j) {
//        if ((getNullableTextList().get(j) != null) != (other.getNullableTextList().get(j) != null)) return false;
//        if (getNullableTextList().get(j) != null && getNullableTextList().get(j).length() != other.getNullableTextList().get(j).length()) return false; else {
//          CharSequence s1 = getNullableTextList().get(j);
//          CharSequence s2 = other.getNullableTextList().get(j);
//          if ((s1 instanceof MutableString && s2 instanceof MutableString) || (s1 instanceof String && s2 instanceof String) || (s1 instanceof BinaryAsciiString && s2 instanceof BinaryAsciiString)) {
//            if (!s1.equals(s2)) return false;
//          } else {
//            return CharSequenceUtils.equals(s1, s2);
//          }
//        }
//      }
//    }
//    if (hasNullableTextListOfNullable() != other.hasNullableTextListOfNullable()) return false;
//    if (hasNullableTextListOfNullable()) {
//      if (getNullableTextListOfNullable().size() != other.getNullableTextListOfNullable().size()) return false;
//      else for (int j = 0; j < getNullableTextListOfNullable().size(); ++j) {
//        if ((getNullableTextListOfNullable().get(j) != null) != (other.getNullableTextListOfNullable().get(j) != null)) return false;
//        if (getNullableTextListOfNullable().get(j) != null && getNullableTextListOfNullable().get(j).length() != other.getNullableTextListOfNullable().get(j).length()) return false; else {
//          CharSequence s1 = getNullableTextListOfNullable().get(j);
//          CharSequence s2 = other.getNullableTextListOfNullable().get(j);
//          if ((s1 instanceof MutableString && s2 instanceof MutableString) || (s1 instanceof String && s2 instanceof String) || (s1 instanceof BinaryAsciiString && s2 instanceof BinaryAsciiString)) {
//            if (!s1.equals(s2)) return false;
//          } else {
//            return CharSequenceUtils.equals(s1, s2);
//          }
//        }
//      }
//    }
//    if (hasAsciiTextList() != other.hasAsciiTextList()) return false;
//    if (hasAsciiTextList()) {
//      if (getAsciiTextList().size() != other.getAsciiTextList().size()) return false;
//      else for (int j = 0; j < getAsciiTextList().size(); ++j) {
//        if ((getAsciiTextList().get(j) != null) != (other.getAsciiTextList().get(j) != null)) return false;
//        if (getAsciiTextList().get(j) != null && getAsciiTextList().get(j).length() != other.getAsciiTextList().get(j).length()) return false; else {
//          CharSequence s1 = getAsciiTextList().get(j);
//          CharSequence s2 = other.getAsciiTextList().get(j);
//          if ((s1 instanceof MutableString && s2 instanceof MutableString) || (s1 instanceof String && s2 instanceof String) || (s1 instanceof BinaryAsciiString && s2 instanceof BinaryAsciiString)) {
//            if (!s1.equals(s2)) return false;
//          } else {
//            return CharSequenceUtils.equals(s1, s2);
//          }
//        }
//      }
//    }
//    if (hasAsciiTextListOfNullable() != other.hasAsciiTextListOfNullable()) return false;
//    if (hasAsciiTextListOfNullable()) {
//      if (getAsciiTextListOfNullable().size() != other.getAsciiTextListOfNullable().size()) return false;
//      else for (int j = 0; j < getAsciiTextListOfNullable().size(); ++j) {
//        if ((getAsciiTextListOfNullable().get(j) != null) != (other.getAsciiTextListOfNullable().get(j) != null)) return false;
//        if (getAsciiTextListOfNullable().get(j) != null && getAsciiTextListOfNullable().get(j).length() != other.getAsciiTextListOfNullable().get(j).length()) return false; else {
//          CharSequence s1 = getAsciiTextListOfNullable().get(j);
//          CharSequence s2 = other.getAsciiTextListOfNullable().get(j);
//          if ((s1 instanceof MutableString && s2 instanceof MutableString) || (s1 instanceof String && s2 instanceof String) || (s1 instanceof BinaryAsciiString && s2 instanceof BinaryAsciiString)) {
//            if (!s1.equals(s2)) return false;
//          } else {
//            return CharSequenceUtils.equals(s1, s2);
//          }
//        }
//      }
//    }
//    if (hasNullableAsciiTextList() != other.hasNullableAsciiTextList()) return false;
//    if (hasNullableAsciiTextList()) {
//      if (getNullableAsciiTextList().size() != other.getNullableAsciiTextList().size()) return false;
//      else for (int j = 0; j < getNullableAsciiTextList().size(); ++j) {
//        if ((getNullableAsciiTextList().get(j) != null) != (other.getNullableAsciiTextList().get(j) != null)) return false;
//        if (getNullableAsciiTextList().get(j) != null && getNullableAsciiTextList().get(j).length() != other.getNullableAsciiTextList().get(j).length()) return false; else {
//          CharSequence s1 = getNullableAsciiTextList().get(j);
//          CharSequence s2 = other.getNullableAsciiTextList().get(j);
//          if ((s1 instanceof MutableString && s2 instanceof MutableString) || (s1 instanceof String && s2 instanceof String) || (s1 instanceof BinaryAsciiString && s2 instanceof BinaryAsciiString)) {
//            if (!s1.equals(s2)) return false;
//          } else {
//            return CharSequenceUtils.equals(s1, s2);
//          }
//        }
//      }
//    }
//    if (hasNullableAsciiTextListOfNullable() != other.hasNullableAsciiTextListOfNullable()) return false;
//    if (hasNullableAsciiTextListOfNullable()) {
//      if (getNullableAsciiTextListOfNullable().size() != other.getNullableAsciiTextListOfNullable().size()) return false;
//      else for (int j = 0; j < getNullableAsciiTextListOfNullable().size(); ++j) {
//        if ((getNullableAsciiTextListOfNullable().get(j) != null) != (other.getNullableAsciiTextListOfNullable().get(j) != null)) return false;
//        if (getNullableAsciiTextListOfNullable().get(j) != null && getNullableAsciiTextListOfNullable().get(j).length() != other.getNullableAsciiTextListOfNullable().get(j).length()) return false; else {
//          CharSequence s1 = getNullableAsciiTextListOfNullable().get(j);
//          CharSequence s2 = other.getNullableAsciiTextListOfNullable().get(j);
//          if ((s1 instanceof MutableString && s2 instanceof MutableString) || (s1 instanceof String && s2 instanceof String) || (s1 instanceof BinaryAsciiString && s2 instanceof BinaryAsciiString)) {
//            if (!s1.equals(s2)) return false;
//          } else {
//            return CharSequenceUtils.equals(s1, s2);
//          }
//        }
//      }
//    }
//    if (hasAlphanumericList() != other.hasAlphanumericList()) return false;
//    if (hasAlphanumericList()) {
//      if (getAlphanumericList().size() != other.getAlphanumericList().size()) return false;
//      else for (int j = 0; j < getAlphanumericList().size(); ++j) {
//        if (getAlphanumericList().get(j) != other.getAlphanumericList().get(j)) return false;
//      }
//    }
//    if (hasAlphanumericListOfNullable() != other.hasAlphanumericListOfNullable()) return false;
//    if (hasAlphanumericListOfNullable()) {
//      if (getAlphanumericListOfNullable().size() != other.getAlphanumericListOfNullable().size()) return false;
//      else for (int j = 0; j < getAlphanumericListOfNullable().size(); ++j) {
//        if (getAlphanumericListOfNullable().get(j) != other.getAlphanumericListOfNullable().get(j)) return false;
//      }
//    }
//    if (hasNullableAlphanumericList() != other.hasNullableAlphanumericList()) return false;
//    if (hasNullableAlphanumericList()) {
//      if (getNullableAlphanumericList().size() != other.getNullableAlphanumericList().size()) return false;
//      else for (int j = 0; j < getNullableAlphanumericList().size(); ++j) {
//        if (getNullableAlphanumericList().get(j) != other.getNullableAlphanumericList().get(j)) return false;
//      }
//    }
//    if (hasNullableAlphanumericListOfNullable() != other.hasNullableAlphanumericListOfNullable()) return false;
//    if (hasNullableAlphanumericListOfNullable()) {
//      if (getNullableAlphanumericListOfNullable().size() != other.getNullableAlphanumericListOfNullable().size()) return false;
//      else for (int j = 0; j < getNullableAlphanumericListOfNullable().size(); ++j) {
//        if (getNullableAlphanumericListOfNullable().get(j) != other.getNullableAlphanumericListOfNullable().get(j)) return false;
//      }
//    }
//    if (hasObjectsList() != other.hasObjectsList()) return false;
//    if (hasObjectsList()) {
//      if (getObjectsList().size() != other.getObjectsList().size()) return false;
//      else for (int j = 0; j < getObjectsList().size(); ++j) {
//        if ((getObjectsList().get(j) != null) != (other.getObjectsList().get(j) != null)) return false;
//        if (getObjectsList().get(j) != null && !getObjectsList().get(j).equals(other.getObjectsList().get(j))) return false;
//      }
//    }
//    if (hasObjectsListOfNullable() != other.hasObjectsListOfNullable()) return false;
//    if (hasObjectsListOfNullable()) {
//      if (getObjectsListOfNullable().size() != other.getObjectsListOfNullable().size()) return false;
//      else for (int j = 0; j < getObjectsListOfNullable().size(); ++j) {
//        if ((getObjectsListOfNullable().get(j) != null) != (other.getObjectsListOfNullable().get(j) != null)) return false;
//        if (getObjectsListOfNullable().get(j) != null && !getObjectsListOfNullable().get(j).equals(other.getObjectsListOfNullable().get(j))) return false;
//      }
//    }
//    if (hasNullableObjectsList() != other.hasNullableObjectsList()) return false;
//    if (hasNullableObjectsList()) {
//      if (getNullableObjectsList().size() != other.getNullableObjectsList().size()) return false;
//      else for (int j = 0; j < getNullableObjectsList().size(); ++j) {
//        if ((getNullableObjectsList().get(j) != null) != (other.getNullableObjectsList().get(j) != null)) return false;
//        if (getNullableObjectsList().get(j) != null && !getNullableObjectsList().get(j).equals(other.getNullableObjectsList().get(j))) return false;
//      }
//    }
//    if (hasNullableObjectsListOfNullable() != other.hasNullableObjectsListOfNullable()) return false;
//    if (hasNullableObjectsListOfNullable()) {
//      if (getNullableObjectsListOfNullable().size() != other.getNullableObjectsListOfNullable().size()) return false;
//      else for (int j = 0; j < getNullableObjectsListOfNullable().size(); ++j) {
//        if ((getNullableObjectsListOfNullable().get(j) != null) != (other.getNullableObjectsListOfNullable().get(j) != null)) return false;
//        if (getNullableObjectsListOfNullable().get(j) != null && !getNullableObjectsListOfNullable().get(j).equals(other.getNullableObjectsListOfNullable().get(j))) return false;
//      }
//    }
//    if (hasListOfLists() != other.hasListOfLists()) return false;
//    if (hasListOfLists()) {
//      if (getListOfLists().size() != other.getListOfLists().size()) return false;
//      else for (int j = 0; j < getListOfLists().size(); ++j) {
//        if ((getListOfLists().get(j) != null) != (other.getListOfLists().get(j) != null)) return false;
//        if (getListOfLists().get(j) != null && !getListOfLists().get(j).equals(other.getListOfLists().get(j))) return false;
//      }
//    }
//    if (hasTimestampList() != other.hasTimestampList()) return false;
//    if (hasTimestampList()) {
//      if (getTimestampList().size() != other.getTimestampList().size()) return false;
//      else for (int j = 0; j < getTimestampList().size(); ++j) {
//        if (getTimestampList().get(j) != other.getTimestampList().get(j)) return false;
//      }
//    }
//    if (hasTimestampListOfNullable() != other.hasTimestampListOfNullable()) return false;
//    if (hasTimestampListOfNullable()) {
//      if (getTimestampListOfNullable().size() != other.getTimestampListOfNullable().size()) return false;
//      else for (int j = 0; j < getTimestampListOfNullable().size(); ++j) {
//        if (getTimestampListOfNullable().get(j) != other.getTimestampListOfNullable().get(j)) return false;
//      }
//    }
//    if (hasNullableTimestampList() != other.hasNullableTimestampList()) return false;
//    if (hasNullableTimestampList()) {
//      if (getNullableTimestampList().size() != other.getNullableTimestampList().size()) return false;
//      else for (int j = 0; j < getNullableTimestampList().size(); ++j) {
//        if (getNullableTimestampList().get(j) != other.getNullableTimestampList().get(j)) return false;
//      }
//    }
//    if (hasNullableTimestampListOfNullable() != other.hasNullableTimestampListOfNullable()) return false;
//    if (hasNullableTimestampListOfNullable()) {
//      if (getNullableTimestampListOfNullable().size() != other.getNullableTimestampListOfNullable().size()) return false;
//      else for (int j = 0; j < getNullableTimestampListOfNullable().size(); ++j) {
//        if (getNullableTimestampListOfNullable().get(j) != other.getNullableTimestampListOfNullable().get(j)) return false;
//      }
//    }
//    if (hasTimeOfDayList() != other.hasTimeOfDayList()) return false;
//    if (hasTimeOfDayList()) {
//      if (getTimeOfDayList().size() != other.getTimeOfDayList().size()) return false;
//      else for (int j = 0; j < getTimeOfDayList().size(); ++j) {
//        if (getTimeOfDayList().get(j) != other.getTimeOfDayList().get(j)) return false;
//      }
//    }
//    if (hasTimeOfDayListOfNullable() != other.hasTimeOfDayListOfNullable()) return false;
//    if (hasTimeOfDayListOfNullable()) {
//      if (getTimeOfDayListOfNullable().size() != other.getTimeOfDayListOfNullable().size()) return false;
//      else for (int j = 0; j < getTimeOfDayListOfNullable().size(); ++j) {
//        if (getTimeOfDayListOfNullable().get(j) != other.getTimeOfDayListOfNullable().get(j)) return false;
//      }
//    }
//    if (hasNullableTimeOfDayList() != other.hasNullableTimeOfDayList()) return false;
//    if (hasNullableTimeOfDayList()) {
//      if (getNullableTimeOfDayList().size() != other.getNullableTimeOfDayList().size()) return false;
//      else for (int j = 0; j < getNullableTimeOfDayList().size(); ++j) {
//        if (getNullableTimeOfDayList().get(j) != other.getNullableTimeOfDayList().get(j)) return false;
//      }
//    }
//    if (hasNullableTimeOfDayListOfNullable() != other.hasNullableTimeOfDayListOfNullable()) return false;
//    if (hasNullableTimeOfDayListOfNullable()) {
//      if (getNullableTimeOfDayListOfNullable().size() != other.getNullableTimeOfDayListOfNullable().size()) return false;
//      else for (int j = 0; j < getNullableTimeOfDayListOfNullable().size(); ++j) {
//        if (getNullableTimeOfDayListOfNullable().get(j) != other.getNullableTimeOfDayListOfNullable().get(j)) return false;
//      }
//    }
//    if (hasEnumList() != other.hasEnumList()) return false;
//    if (hasEnumList()) {
//      if (getEnumList().size() != other.getEnumList().size()) return false;
//      else for (int j = 0; j < getEnumList().size(); ++j) {
//        if ((getEnumList().get(j) != null) != (other.getEnumList().get(j) != null)) return false;
//        if (getEnumList().get(j) != null && getEnumList().get(j) != other.getEnumList().get(j)) return false;
//      }
//    }
//    if (hasEnumListOfNullable() != other.hasEnumListOfNullable()) return false;
//    if (hasEnumListOfNullable()) {
//      if (getEnumListOfNullable().size() != other.getEnumListOfNullable().size()) return false;
//      else for (int j = 0; j < getEnumListOfNullable().size(); ++j) {
//        if ((getEnumListOfNullable().get(j) != null) != (other.getEnumListOfNullable().get(j) != null)) return false;
//        if (getEnumListOfNullable().get(j) != null && getEnumListOfNullable().get(j) != other.getEnumListOfNullable().get(j)) return false;
//      }
//    }
//    if (hasNullableEnumList() != other.hasNullableEnumList()) return false;
//    if (hasNullableEnumList()) {
//      if (getNullableEnumList().size() != other.getNullableEnumList().size()) return false;
//      else for (int j = 0; j < getNullableEnumList().size(); ++j) {
//        if ((getNullableEnumList().get(j) != null) != (other.getNullableEnumList().get(j) != null)) return false;
//        if (getNullableEnumList().get(j) != null && getNullableEnumList().get(j) != other.getNullableEnumList().get(j)) return false;
//      }
//    }
//    if (hasNullableEnumListOfNullable() != other.hasNullableEnumListOfNullable()) return false;
//    if (hasNullableEnumListOfNullable()) {
//      if (getNullableEnumListOfNullable().size() != other.getNullableEnumListOfNullable().size()) return false;
//      else for (int j = 0; j < getNullableEnumListOfNullable().size(); ++j) {
//        if ((getNullableEnumListOfNullable().get(j) != null) != (other.getNullableEnumListOfNullable().get(j) != null)) return false;
//        if (getNullableEnumListOfNullable().get(j) != null && getNullableEnumListOfNullable().get(j) != other.getNullableEnumListOfNullable().get(j)) return false;
//      }
//    }
//    return true;
//  }

  /**
   * Returns a hash code value for the object. This method is * supported for the benefit of hash tables such as those provided by.
   */
  @Override
  public int hashCode() {
    int hash = super.hashCode();
    if (hasObject()) {
      hash = hash * 31 + getObject().hashCode();
    }
    if (hasLists()) {
      hash = hash * 31 + getLists().hashCode();
    }
    if (hasBooleanList()) {
      hash = hash * 31 + getBooleanList().hashCode();
    }
    if (hasBooleanListOfNullable()) {
      hash = hash * 31 + getBooleanListOfNullable().hashCode();
    }
    if (hasNullableBooleanList()) {
      hash = hash * 31 + getNullableBooleanList().hashCode();
    }
    if (hasNullableBooleanListOfNullable()) {
      hash = hash * 31 + getNullableBooleanListOfNullable().hashCode();
    }
    if (hasByteList()) {
      hash = hash * 31 + getByteList().hashCode();
    }
    if (hasByteListOfNullable()) {
      hash = hash * 31 + getByteListOfNullable().hashCode();
    }
    if (hasNullableByteList()) {
      hash = hash * 31 + getNullableByteList().hashCode();
    }
    if (hasNullableByteListOfNullable()) {
      hash = hash * 31 + getNullableByteListOfNullable().hashCode();
    }
    if (hasShortList()) {
      hash = hash * 31 + getShortList().hashCode();
    }
    if (hasShortListOfNullable()) {
      hash = hash * 31 + getShortListOfNullable().hashCode();
    }
    if (hasNullableShortList()) {
      hash = hash * 31 + getNullableShortList().hashCode();
    }
    if (hasNullableShortListOfNullable()) {
      hash = hash * 31 + getNullableShortListOfNullable().hashCode();
    }
    if (hasIntList()) {
      hash = hash * 31 + getIntList().hashCode();
    }
    if (hasIntListOfNullable()) {
      hash = hash * 31 + getIntListOfNullable().hashCode();
    }
    if (hasNullableIntList()) {
      hash = hash * 31 + getNullableIntList().hashCode();
    }
    if (hasNullableIntListOfNullable()) {
      hash = hash * 31 + getNullableIntListOfNullable().hashCode();
    }
    if (hasLongList()) {
      hash = hash * 31 + getLongList().hashCode();
    }
    if (hasLongListOfNullable()) {
      hash = hash * 31 + getLongListOfNullable().hashCode();
    }
    if (hasNullableLongList()) {
      hash = hash * 31 + getNullableLongList().hashCode();
    }
    if (hasNullableLongListOfNullable()) {
      hash = hash * 31 + getNullableLongListOfNullable().hashCode();
    }
    if (hasDecimalList()) {
      hash = hash * 31 + getDecimalList().hashCode();
    }
    if (hasDecimalListOfNullable()) {
      hash = hash * 31 + getDecimalListOfNullable().hashCode();
    }
    if (hasNullableDecimalList()) {
      hash = hash * 31 + getNullableDecimalList().hashCode();
    }
    if (hasNullableDecimalListOfNullable()) {
      hash = hash * 31 + getNullableDecimalListOfNullable().hashCode();
    }
    if (hasDoubleList()) {
      hash = hash * 31 + getDoubleList().hashCode();
    }
    if (hasDoubleListOfNullable()) {
      hash = hash * 31 + getDoubleListOfNullable().hashCode();
    }
    if (hasNullableDoubleList()) {
      hash = hash * 31 + getNullableDoubleList().hashCode();
    }
    if (hasNullableDoubleListOfNullable()) {
      hash = hash * 31 + getNullableDoubleListOfNullable().hashCode();
    }
    if (hasTextList()) {
      for (int j = 0; j < getTextList().size(); ++j) {
        hash ^= getTextList().get(j).hashCode();
      }
    }
    if (hasTextListOfNullable()) {
      for (int j = 0; j < getTextListOfNullable().size(); ++j) {
        hash ^= getTextListOfNullable().get(j).hashCode();
      }
    }
    if (hasNullableTextList()) {
      for (int j = 0; j < getNullableTextList().size(); ++j) {
        hash ^= getNullableTextList().get(j).hashCode();
      }
    }
    if (hasNullableTextListOfNullable()) {
      for (int j = 0; j < getNullableTextListOfNullable().size(); ++j) {
        hash ^= getNullableTextListOfNullable().get(j).hashCode();
      }
    }
    if (hasAsciiTextList()) {
      for (int j = 0; j < getAsciiTextList().size(); ++j) {
        hash ^= getAsciiTextList().get(j).hashCode();
      }
    }
    if (hasAsciiTextListOfNullable()) {
      for (int j = 0; j < getAsciiTextListOfNullable().size(); ++j) {
        hash ^= getAsciiTextListOfNullable().get(j).hashCode();
      }
    }
    if (hasNullableAsciiTextList()) {
      for (int j = 0; j < getNullableAsciiTextList().size(); ++j) {
        hash ^= getNullableAsciiTextList().get(j).hashCode();
      }
    }
    if (hasNullableAsciiTextListOfNullable()) {
      for (int j = 0; j < getNullableAsciiTextListOfNullable().size(); ++j) {
        hash ^= getNullableAsciiTextListOfNullable().get(j).hashCode();
      }
    }
    if (hasAlphanumericList()) {
      hash = hash * 31 + getAlphanumericList().hashCode();
    }
    if (hasAlphanumericListOfNullable()) {
      hash = hash * 31 + getAlphanumericListOfNullable().hashCode();
    }
    if (hasNullableAlphanumericList()) {
      hash = hash * 31 + getNullableAlphanumericList().hashCode();
    }
    if (hasNullableAlphanumericListOfNullable()) {
      hash = hash * 31 + getNullableAlphanumericListOfNullable().hashCode();
    }
    if (hasObjectsList()) {
      for (int j = 0; j < getObjectsList().size(); ++j) {
        hash ^= getObjectsList().get(j).hashCode();
      }
    }
    if (hasObjectsListOfNullable()) {
      for (int j = 0; j < getObjectsListOfNullable().size(); ++j) {
        hash ^= getObjectsListOfNullable().get(j).hashCode();
      }
    }
    if (hasNullableObjectsList()) {
      for (int j = 0; j < getNullableObjectsList().size(); ++j) {
        hash ^= getNullableObjectsList().get(j).hashCode();
      }
    }
    if (hasNullableObjectsListOfNullable()) {
      for (int j = 0; j < getNullableObjectsListOfNullable().size(); ++j) {
        hash ^= getNullableObjectsListOfNullable().get(j).hashCode();
      }
    }
    if (hasListOfLists()) {
      for (int j = 0; j < getListOfLists().size(); ++j) {
        hash ^= getListOfLists().get(j).hashCode();
      }
    }
    if (hasTimestampList()) {
      hash = hash * 31 + getTimestampList().hashCode();
    }
    if (hasTimestampListOfNullable()) {
      hash = hash * 31 + getTimestampListOfNullable().hashCode();
    }
    if (hasNullableTimestampList()) {
      hash = hash * 31 + getNullableTimestampList().hashCode();
    }
    if (hasNullableTimestampListOfNullable()) {
      hash = hash * 31 + getNullableTimestampListOfNullable().hashCode();
    }
    if (hasTimeOfDayList()) {
      hash = hash * 31 + getTimeOfDayList().hashCode();
    }
    if (hasTimeOfDayListOfNullable()) {
      hash = hash * 31 + getTimeOfDayListOfNullable().hashCode();
    }
    if (hasNullableTimeOfDayList()) {
      hash = hash * 31 + getNullableTimeOfDayList().hashCode();
    }
    if (hasNullableTimeOfDayListOfNullable()) {
      hash = hash * 31 + getNullableTimeOfDayListOfNullable().hashCode();
    }
    if (hasEnumList()) {
      for (int j = 0; j < getEnumList().size(); ++j) {
        hash = hash * 31 + getEnumList().get(j).getNumber();
      }
    }
    if (hasEnumListOfNullable()) {
      for (int j = 0; j < getEnumListOfNullable().size(); ++j) {
        hash = hash * 31 + getEnumListOfNullable().get(j).getNumber();
      }
    }
    if (hasNullableEnumList()) {
      for (int j = 0; j < getNullableEnumList().size(); ++j) {
        hash = hash * 31 + getNullableEnumList().get(j).getNumber();
      }
    }
    if (hasNullableEnumListOfNullable()) {
      for (int j = 0; j < getNullableEnumListOfNullable().size(); ++j) {
        hash = hash * 31 + getNullableEnumListOfNullable().get(j).getNumber();
      }
    }
    return hash;
  }

  /**
   * Method copies state to a given instance
   * @param template class instance that should be used as a copy source
   */
  @Override
  public AllTypesMessage copyFrom(RecordInfo template) {
    super.copyFrom(template);
    if (template instanceof AllTypesMessageInfo) {
      AllTypesMessageInfo t = (AllTypesMessageInfo)template;
      if (t.hasObject()) {
        if (hasObject() && getObject() instanceof RecordInterface) {
          ((RecordInterface)getObject()).copyFrom(t.getObject());
        } else {
          setObject((AllSimpleTypesMessageInfo)t.getObject().clone());
        }
      } else {
        nullifyObject();
      }
      if (t.hasLists()) {
        if (hasLists() && getLists() instanceof RecordInterface) {
          ((RecordInterface)getLists()).copyFrom(t.getLists());
        } else {
          setLists((AllListsMessageInfo)t.getLists().clone());
        }
      } else {
        nullifyLists();
      }
      if (t.hasBooleanList()) {
        if (!hasBooleanList()) {
          setBooleanList(new ByteArrayList(t.getBooleanList().size()));
        } else {
          getBooleanList().clear();
        }
        for (int i = 0; i < getBooleanList().size(); ++i) ((ByteArrayList)getBooleanList()).add(t.getBooleanList().get(i));
      } else {
        nullifyBooleanList();
      }
      if (t.hasBooleanListOfNullable()) {
        if (!hasBooleanListOfNullable()) {
          setBooleanListOfNullable(new ByteArrayList(t.getBooleanListOfNullable().size()));
        } else {
          getBooleanListOfNullable().clear();
        }
        for (int i = 0; i < getBooleanListOfNullable().size(); ++i) ((ByteArrayList)getBooleanListOfNullable()).add(t.getBooleanListOfNullable().get(i));
      } else {
        nullifyBooleanListOfNullable();
      }
      if (t.hasNullableBooleanList()) {
        if (!hasNullableBooleanList()) {
          setNullableBooleanList(new ByteArrayList(t.getNullableBooleanList().size()));
        } else {
          getNullableBooleanList().clear();
        }
        for (int i = 0; i < getNullableBooleanList().size(); ++i) ((ByteArrayList)getNullableBooleanList()).add(t.getNullableBooleanList().get(i));
      } else {
        nullifyNullableBooleanList();
      }
      if (t.hasNullableBooleanListOfNullable()) {
        if (!hasNullableBooleanListOfNullable()) {
          setNullableBooleanListOfNullable(new ByteArrayList(t.getNullableBooleanListOfNullable().size()));
        } else {
          getNullableBooleanListOfNullable().clear();
        }
        for (int i = 0; i < getNullableBooleanListOfNullable().size(); ++i) ((ByteArrayList)getNullableBooleanListOfNullable()).add(t.getNullableBooleanListOfNullable().get(i));
      } else {
        nullifyNullableBooleanListOfNullable();
      }
      if (t.hasByteList()) {
        if (!hasByteList()) {
          setByteList(new ByteArrayList(t.getByteList().size()));
        } else {
          getByteList().clear();
        }
        for (int i = 0; i < getByteList().size(); ++i) ((ByteArrayList)getByteList()).add(t.getByteList().get(i));
      } else {
        nullifyByteList();
      }
      if (t.hasByteListOfNullable()) {
        if (!hasByteListOfNullable()) {
          setByteListOfNullable(new ByteArrayList(t.getByteListOfNullable().size()));
        } else {
          getByteListOfNullable().clear();
        }
        for (int i = 0; i < getByteListOfNullable().size(); ++i) ((ByteArrayList)getByteListOfNullable()).add(t.getByteListOfNullable().get(i));
      } else {
        nullifyByteListOfNullable();
      }
      if (t.hasNullableByteList()) {
        if (!hasNullableByteList()) {
          setNullableByteList(new ByteArrayList(t.getNullableByteList().size()));
        } else {
          getNullableByteList().clear();
        }
        for (int i = 0; i < getNullableByteList().size(); ++i) ((ByteArrayList)getNullableByteList()).add(t.getNullableByteList().get(i));
      } else {
        nullifyNullableByteList();
      }
      if (t.hasNullableByteListOfNullable()) {
        if (!hasNullableByteListOfNullable()) {
          setNullableByteListOfNullable(new ByteArrayList(t.getNullableByteListOfNullable().size()));
        } else {
          getNullableByteListOfNullable().clear();
        }
        for (int i = 0; i < getNullableByteListOfNullable().size(); ++i) ((ByteArrayList)getNullableByteListOfNullable()).add(t.getNullableByteListOfNullable().get(i));
      } else {
        nullifyNullableByteListOfNullable();
      }
      if (t.hasShortList()) {
        if (!hasShortList()) {
          setShortList(new ShortArrayList(t.getShortList().size()));
        } else {
          getShortList().clear();
        }
        for (int i = 0; i < getShortList().size(); ++i) ((ShortArrayList)getShortList()).add(t.getShortList().get(i));
      } else {
        nullifyShortList();
      }
      if (t.hasShortListOfNullable()) {
        if (!hasShortListOfNullable()) {
          setShortListOfNullable(new ShortArrayList(t.getShortListOfNullable().size()));
        } else {
          getShortListOfNullable().clear();
        }
        for (int i = 0; i < getShortListOfNullable().size(); ++i) ((ShortArrayList)getShortListOfNullable()).add(t.getShortListOfNullable().get(i));
      } else {
        nullifyShortListOfNullable();
      }
      if (t.hasNullableShortList()) {
        if (!hasNullableShortList()) {
          setNullableShortList(new ShortArrayList(t.getNullableShortList().size()));
        } else {
          getNullableShortList().clear();
        }
        for (int i = 0; i < getNullableShortList().size(); ++i) ((ShortArrayList)getNullableShortList()).add(t.getNullableShortList().get(i));
      } else {
        nullifyNullableShortList();
      }
      if (t.hasNullableShortListOfNullable()) {
        if (!hasNullableShortListOfNullable()) {
          setNullableShortListOfNullable(new ShortArrayList(t.getNullableShortListOfNullable().size()));
        } else {
          getNullableShortListOfNullable().clear();
        }
        for (int i = 0; i < getNullableShortListOfNullable().size(); ++i) ((ShortArrayList)getNullableShortListOfNullable()).add(t.getNullableShortListOfNullable().get(i));
      } else {
        nullifyNullableShortListOfNullable();
      }
      if (t.hasIntList()) {
        if (!hasIntList()) {
          setIntList(new IntegerArrayList(t.getIntList().size()));
        } else {
          getIntList().clear();
        }
        for (int i = 0; i < getIntList().size(); ++i) ((IntegerArrayList)getIntList()).add(t.getIntList().get(i));
      } else {
        nullifyIntList();
      }
      if (t.hasIntListOfNullable()) {
        if (!hasIntListOfNullable()) {
          setIntListOfNullable(new IntegerArrayList(t.getIntListOfNullable().size()));
        } else {
          getIntListOfNullable().clear();
        }
        for (int i = 0; i < getIntListOfNullable().size(); ++i) ((IntegerArrayList)getIntListOfNullable()).add(t.getIntListOfNullable().get(i));
      } else {
        nullifyIntListOfNullable();
      }
      if (t.hasNullableIntList()) {
        if (!hasNullableIntList()) {
          setNullableIntList(new IntegerArrayList(t.getNullableIntList().size()));
        } else {
          getNullableIntList().clear();
        }
        for (int i = 0; i < getNullableIntList().size(); ++i) ((IntegerArrayList)getNullableIntList()).add(t.getNullableIntList().get(i));
      } else {
        nullifyNullableIntList();
      }
      if (t.hasNullableIntListOfNullable()) {
        if (!hasNullableIntListOfNullable()) {
          setNullableIntListOfNullable(new IntegerArrayList(t.getNullableIntListOfNullable().size()));
        } else {
          getNullableIntListOfNullable().clear();
        }
        for (int i = 0; i < getNullableIntListOfNullable().size(); ++i) ((IntegerArrayList)getNullableIntListOfNullable()).add(t.getNullableIntListOfNullable().get(i));
      } else {
        nullifyNullableIntListOfNullable();
      }
      if (t.hasLongList()) {
        if (!hasLongList()) {
          setLongList(new LongArrayList(t.getLongList().size()));
        } else {
          getLongList().clear();
        }
        for (int i = 0; i < getLongList().size(); ++i) ((LongArrayList)getLongList()).add(t.getLongList().get(i));
      } else {
        nullifyLongList();
      }
      if (t.hasLongListOfNullable()) {
        if (!hasLongListOfNullable()) {
          setLongListOfNullable(new LongArrayList(t.getLongListOfNullable().size()));
        } else {
          getLongListOfNullable().clear();
        }
        for (int i = 0; i < getLongListOfNullable().size(); ++i) ((LongArrayList)getLongListOfNullable()).add(t.getLongListOfNullable().get(i));
      } else {
        nullifyLongListOfNullable();
      }
      if (t.hasNullableLongList()) {
        if (!hasNullableLongList()) {
          setNullableLongList(new LongArrayList(t.getNullableLongList().size()));
        } else {
          getNullableLongList().clear();
        }
        for (int i = 0; i < getNullableLongList().size(); ++i) ((LongArrayList)getNullableLongList()).add(t.getNullableLongList().get(i));
      } else {
        nullifyNullableLongList();
      }
      if (t.hasNullableLongListOfNullable()) {
        if (!hasNullableLongListOfNullable()) {
          setNullableLongListOfNullable(new LongArrayList(t.getNullableLongListOfNullable().size()));
        } else {
          getNullableLongListOfNullable().clear();
        }
        for (int i = 0; i < getNullableLongListOfNullable().size(); ++i) ((LongArrayList)getNullableLongListOfNullable()).add(t.getNullableLongListOfNullable().get(i));
      } else {
        nullifyNullableLongListOfNullable();
      }
      if (t.hasDecimalList()) {
        if (!hasDecimalList()) {
          setDecimalList(new LongArrayList(t.getDecimalList().size()));
        } else {
          getDecimalList().clear();
        }
        for (int i = 0; i < getDecimalList().size(); ++i) ((LongArrayList)getDecimalList()).add(t.getDecimalList().get(i));
      } else {
        nullifyDecimalList();
      }
      if (t.hasDecimalListOfNullable()) {
        if (!hasDecimalListOfNullable()) {
          setDecimalListOfNullable(new LongArrayList(t.getDecimalListOfNullable().size()));
        } else {
          getDecimalListOfNullable().clear();
        }
        for (int i = 0; i < getDecimalListOfNullable().size(); ++i) ((LongArrayList)getDecimalListOfNullable()).add(t.getDecimalListOfNullable().get(i));
      } else {
        nullifyDecimalListOfNullable();
      }
      if (t.hasNullableDecimalList()) {
        if (!hasNullableDecimalList()) {
          setNullableDecimalList(new LongArrayList(t.getNullableDecimalList().size()));
        } else {
          getNullableDecimalList().clear();
        }
        for (int i = 0; i < getNullableDecimalList().size(); ++i) ((LongArrayList)getNullableDecimalList()).add(t.getNullableDecimalList().get(i));
      } else {
        nullifyNullableDecimalList();
      }
      if (t.hasNullableDecimalListOfNullable()) {
        if (!hasNullableDecimalListOfNullable()) {
          setNullableDecimalListOfNullable(new LongArrayList(t.getNullableDecimalListOfNullable().size()));
        } else {
          getNullableDecimalListOfNullable().clear();
        }
        for (int i = 0; i < getNullableDecimalListOfNullable().size(); ++i) ((LongArrayList)getNullableDecimalListOfNullable()).add(t.getNullableDecimalListOfNullable().get(i));
      } else {
        nullifyNullableDecimalListOfNullable();
      }
      if (t.hasDoubleList()) {
        if (!hasDoubleList()) {
          setDoubleList(new DoubleArrayList(t.getDoubleList().size()));
        } else {
          getDoubleList().clear();
        }
        for (int i = 0; i < getDoubleList().size(); ++i) ((DoubleArrayList)getDoubleList()).add(t.getDoubleList().get(i));
      } else {
        nullifyDoubleList();
      }
      if (t.hasDoubleListOfNullable()) {
        if (!hasDoubleListOfNullable()) {
          setDoubleListOfNullable(new DoubleArrayList(t.getDoubleListOfNullable().size()));
        } else {
          getDoubleListOfNullable().clear();
        }
        for (int i = 0; i < getDoubleListOfNullable().size(); ++i) ((DoubleArrayList)getDoubleListOfNullable()).add(t.getDoubleListOfNullable().get(i));
      } else {
        nullifyDoubleListOfNullable();
      }
      if (t.hasNullableDoubleList()) {
        if (!hasNullableDoubleList()) {
          setNullableDoubleList(new DoubleArrayList(t.getNullableDoubleList().size()));
        } else {
          getNullableDoubleList().clear();
        }
        for (int i = 0; i < getNullableDoubleList().size(); ++i) ((DoubleArrayList)getNullableDoubleList()).add(t.getNullableDoubleList().get(i));
      } else {
        nullifyNullableDoubleList();
      }
      if (t.hasNullableDoubleListOfNullable()) {
        if (!hasNullableDoubleListOfNullable()) {
          setNullableDoubleListOfNullable(new DoubleArrayList(t.getNullableDoubleListOfNullable().size()));
        } else {
          getNullableDoubleListOfNullable().clear();
        }
        for (int i = 0; i < getNullableDoubleListOfNullable().size(); ++i) ((DoubleArrayList)getNullableDoubleListOfNullable()).add(t.getNullableDoubleListOfNullable().get(i));
      } else {
        nullifyNullableDoubleListOfNullable();
      }
      if (t.hasFloatList()) {
        if (!hasFloatList()) {
          setFloatList(new FloatArrayList(t.getFloatList().size()));
        } else {
          getFloatList().clear();
        }
        for (int i = 0; i < getFloatList().size(); ++i) ((FloatArrayList)getFloatList()).add(t.getFloatList().get(i));
      } else {
        nullifyFloatList();
      }
      if (t.hasFloatListOfNullable()) {
        if (!hasFloatListOfNullable()) {
          setFloatListOfNullable(new FloatArrayList(t.getFloatListOfNullable().size()));
        } else {
          getFloatListOfNullable().clear();
        }
        for (int i = 0; i < getFloatListOfNullable().size(); ++i) ((FloatArrayList)getFloatListOfNullable()).add(t.getFloatListOfNullable().get(i));
      } else {
        nullifyFloatListOfNullable();
      }
      if (t.hasNullableFloatList()) {
        if (!hasNullableFloatList()) {
          setNullableFloatList(new FloatArrayList(t.getNullableFloatList().size()));
        } else {
          getNullableFloatList().clear();
        }
        for (int i = 0; i < getNullableFloatList().size(); ++i) ((FloatArrayList)getNullableFloatList()).add(t.getNullableFloatList().get(i));
      } else {
        nullifyNullableFloatList();
      }
      if (t.hasNullableFloatListOfNullable()) {
        if (!hasNullableFloatListOfNullable()) {
          setNullableFloatListOfNullable(new FloatArrayList(t.getNullableFloatListOfNullable().size()));
        } else {
          getNullableFloatListOfNullable().clear();
        }
        for (int i = 0; i < getNullableFloatListOfNullable().size(); ++i) ((FloatArrayList)getNullableFloatListOfNullable()).add(t.getNullableFloatListOfNullable().get(i));
      } else {
        nullifyNullableFloatListOfNullable();
      }
      if (t.hasTextList()) {
        if (!hasTextList()) {
          setTextList(new ObjectArrayList<CharSequence>(t.getTextList().size()));
        } else {
          getTextList().clear();
        }
        for (int i = 0; i < getTextList().size(); ++i) ((ObjectArrayList<CharSequence>)getTextList()).add(t.getTextList().get(i));
      } else {
        nullifyTextList();
      }
      if (t.hasTextListOfNullable()) {
        if (!hasTextListOfNullable()) {
          setTextListOfNullable(new ObjectArrayList<CharSequence>(t.getTextListOfNullable().size()));
        } else {
          getTextListOfNullable().clear();
        }
        for (int i = 0; i < getTextListOfNullable().size(); ++i) ((ObjectArrayList<CharSequence>)getTextListOfNullable()).add(t.getTextListOfNullable().get(i));
      } else {
        nullifyTextListOfNullable();
      }
      if (t.hasNullableTextList()) {
        if (!hasNullableTextList()) {
          setNullableTextList(new ObjectArrayList<CharSequence>(t.getNullableTextList().size()));
        } else {
          getNullableTextList().clear();
        }
        for (int i = 0; i < getNullableTextList().size(); ++i) ((ObjectArrayList<CharSequence>)getNullableTextList()).add(t.getNullableTextList().get(i));
      } else {
        nullifyNullableTextList();
      }
      if (t.hasNullableTextListOfNullable()) {
        if (!hasNullableTextListOfNullable()) {
          setNullableTextListOfNullable(new ObjectArrayList<CharSequence>(t.getNullableTextListOfNullable().size()));
        } else {
          getNullableTextListOfNullable().clear();
        }
        for (int i = 0; i < getNullableTextListOfNullable().size(); ++i) ((ObjectArrayList<CharSequence>)getNullableTextListOfNullable()).add(t.getNullableTextListOfNullable().get(i));
      } else {
        nullifyNullableTextListOfNullable();
      }
      if (t.hasAsciiTextList()) {
        if (!hasAsciiTextList()) {
          setAsciiTextList(new ObjectArrayList<CharSequence>(t.getAsciiTextList().size()));
        } else {
          getAsciiTextList().clear();
        }
        for (int i = 0; i < getAsciiTextList().size(); ++i) ((ObjectArrayList<CharSequence>)getAsciiTextList()).add(t.getAsciiTextList().get(i));
      } else {
        nullifyAsciiTextList();
      }
      if (t.hasAsciiTextListOfNullable()) {
        if (!hasAsciiTextListOfNullable()) {
          setAsciiTextListOfNullable(new ObjectArrayList<CharSequence>(t.getAsciiTextListOfNullable().size()));
        } else {
          getAsciiTextListOfNullable().clear();
        }
        for (int i = 0; i < getAsciiTextListOfNullable().size(); ++i) ((ObjectArrayList<CharSequence>)getAsciiTextListOfNullable()).add(t.getAsciiTextListOfNullable().get(i));
      } else {
        nullifyAsciiTextListOfNullable();
      }
      if (t.hasNullableAsciiTextList()) {
        if (!hasNullableAsciiTextList()) {
          setNullableAsciiTextList(new ObjectArrayList<CharSequence>(t.getNullableAsciiTextList().size()));
        } else {
          getNullableAsciiTextList().clear();
        }
        for (int i = 0; i < getNullableAsciiTextList().size(); ++i) ((ObjectArrayList<CharSequence>)getNullableAsciiTextList()).add(t.getNullableAsciiTextList().get(i));
      } else {
        nullifyNullableAsciiTextList();
      }
      if (t.hasNullableAsciiTextListOfNullable()) {
        if (!hasNullableAsciiTextListOfNullable()) {
          setNullableAsciiTextListOfNullable(new ObjectArrayList<CharSequence>(t.getNullableAsciiTextListOfNullable().size()));
        } else {
          getNullableAsciiTextListOfNullable().clear();
        }
        for (int i = 0; i < getNullableAsciiTextListOfNullable().size(); ++i) ((ObjectArrayList<CharSequence>)getNullableAsciiTextListOfNullable()).add(t.getNullableAsciiTextListOfNullable().get(i));
      } else {
        nullifyNullableAsciiTextListOfNullable();
      }
      if (t.hasAlphanumericList()) {
        if (!hasAlphanumericList()) {
          setAlphanumericList(new LongArrayList(t.getAlphanumericList().size()));
        } else {
          getAlphanumericList().clear();
        }
        for (int i = 0; i < getAlphanumericList().size(); ++i) ((LongArrayList)getAlphanumericList()).add(t.getAlphanumericList().get(i));
      } else {
        nullifyAlphanumericList();
      }
      if (t.hasAlphanumericListOfNullable()) {
        if (!hasAlphanumericListOfNullable()) {
          setAlphanumericListOfNullable(new LongArrayList(t.getAlphanumericListOfNullable().size()));
        } else {
          getAlphanumericListOfNullable().clear();
        }
        for (int i = 0; i < getAlphanumericListOfNullable().size(); ++i) ((LongArrayList)getAlphanumericListOfNullable()).add(t.getAlphanumericListOfNullable().get(i));
      } else {
        nullifyAlphanumericListOfNullable();
      }
      if (t.hasNullableAlphanumericList()) {
        if (!hasNullableAlphanumericList()) {
          setNullableAlphanumericList(new LongArrayList(t.getNullableAlphanumericList().size()));
        } else {
          getNullableAlphanumericList().clear();
        }
        for (int i = 0; i < getNullableAlphanumericList().size(); ++i) ((LongArrayList)getNullableAlphanumericList()).add(t.getNullableAlphanumericList().get(i));
      } else {
        nullifyNullableAlphanumericList();
      }
      if (t.hasNullableAlphanumericListOfNullable()) {
        if (!hasNullableAlphanumericListOfNullable()) {
          setNullableAlphanumericListOfNullable(new LongArrayList(t.getNullableAlphanumericListOfNullable().size()));
        } else {
          getNullableAlphanumericListOfNullable().clear();
        }
        for (int i = 0; i < getNullableAlphanumericListOfNullable().size(); ++i) ((LongArrayList)getNullableAlphanumericListOfNullable()).add(t.getNullableAlphanumericListOfNullable().get(i));
      } else {
        nullifyNullableAlphanumericListOfNullable();
      }
      if (t.hasObjectsList()) {
        if (!hasObjectsList()) {
          setObjectsList(new ObjectArrayList<AllSimpleTypesMessageInfo>(t.getObjectsList().size()));
        } else {
          getObjectsList().clear();
        }
        for (int i = 0; i < t.getObjectsList().size(); ++i) ((ObjectArrayList<AllSimpleTypesMessageInfo>)getObjectsList()).add((AllSimpleTypesMessageInfo)t.getObjectsList().get(i).clone());
      } else {
        nullifyObjectsList();
      }
      if (t.hasObjectsListOfNullable()) {
        if (!hasObjectsListOfNullable()) {
          setObjectsListOfNullable(new ObjectArrayList<AllSimpleTypesMessageInfo>(t.getObjectsListOfNullable().size()));
        } else {
          getObjectsListOfNullable().clear();
        }
        for (int i = 0; i < t.getObjectsListOfNullable().size(); ++i) ((ObjectArrayList<AllSimpleTypesMessageInfo>)getObjectsListOfNullable()).add((AllSimpleTypesMessageInfo)t.getObjectsListOfNullable().get(i).clone());
      } else {
        nullifyObjectsListOfNullable();
      }
      if (t.hasNullableObjectsList()) {
        if (!hasNullableObjectsList()) {
          setNullableObjectsList(new ObjectArrayList<AllSimpleTypesMessageInfo>(t.getNullableObjectsList().size()));
        } else {
          getNullableObjectsList().clear();
        }
        for (int i = 0; i < t.getNullableObjectsList().size(); ++i) ((ObjectArrayList<AllSimpleTypesMessageInfo>)getNullableObjectsList()).add((AllSimpleTypesMessageInfo)t.getNullableObjectsList().get(i).clone());
      } else {
        nullifyNullableObjectsList();
      }
      if (t.hasNullableObjectsListOfNullable()) {
        if (!hasNullableObjectsListOfNullable()) {
          setNullableObjectsListOfNullable(new ObjectArrayList<AllSimpleTypesMessageInfo>(t.getNullableObjectsListOfNullable().size()));
        } else {
          getNullableObjectsListOfNullable().clear();
        }
        for (int i = 0; i < t.getNullableObjectsListOfNullable().size(); ++i) ((ObjectArrayList<AllSimpleTypesMessageInfo>)getNullableObjectsListOfNullable()).add((AllSimpleTypesMessageInfo)t.getNullableObjectsListOfNullable().get(i).clone());
      } else {
        nullifyNullableObjectsListOfNullable();
      }
      if (t.hasListOfLists()) {
        if (!hasListOfLists()) {
          setListOfLists(new ObjectArrayList<AllListsMessageInfo>(t.getListOfLists().size()));
        } else {
          getListOfLists().clear();
        }
        for (int i = 0; i < t.getListOfLists().size(); ++i) ((ObjectArrayList<AllListsMessageInfo>)getListOfLists()).add((AllListsMessageInfo)t.getListOfLists().get(i).clone());
      } else {
        nullifyListOfLists();
      }
      if (t.hasTimestampList()) {
        if (!hasTimestampList()) {
          setTimestampList(new LongArrayList(t.getTimestampList().size()));
        } else {
          getTimestampList().clear();
        }
        for (int i = 0; i < getTimestampList().size(); ++i) ((LongArrayList)getTimestampList()).add(t.getTimestampList().get(i));
      } else {
        nullifyTimestampList();
      }
      if (t.hasTimestampListOfNullable()) {
        if (!hasTimestampListOfNullable()) {
          setTimestampListOfNullable(new LongArrayList(t.getTimestampListOfNullable().size()));
        } else {
          getTimestampListOfNullable().clear();
        }
        for (int i = 0; i < getTimestampListOfNullable().size(); ++i) ((LongArrayList)getTimestampListOfNullable()).add(t.getTimestampListOfNullable().get(i));
      } else {
        nullifyTimestampListOfNullable();
      }
      if (t.hasNullableTimestampList()) {
        if (!hasNullableTimestampList()) {
          setNullableTimestampList(new LongArrayList(t.getNullableTimestampList().size()));
        } else {
          getNullableTimestampList().clear();
        }
        for (int i = 0; i < getNullableTimestampList().size(); ++i) ((LongArrayList)getNullableTimestampList()).add(t.getNullableTimestampList().get(i));
      } else {
        nullifyNullableTimestampList();
      }
      if (t.hasNullableTimestampListOfNullable()) {
        if (!hasNullableTimestampListOfNullable()) {
          setNullableTimestampListOfNullable(new LongArrayList(t.getNullableTimestampListOfNullable().size()));
        } else {
          getNullableTimestampListOfNullable().clear();
        }
        for (int i = 0; i < getNullableTimestampListOfNullable().size(); ++i) ((LongArrayList)getNullableTimestampListOfNullable()).add(t.getNullableTimestampListOfNullable().get(i));
      } else {
        nullifyNullableTimestampListOfNullable();
      }
      if (t.hasTimeOfDayList()) {
        if (!hasTimeOfDayList()) {
          setTimeOfDayList(new IntegerArrayList(t.getTimeOfDayList().size()));
        } else {
          getTimeOfDayList().clear();
        }
        for (int i = 0; i < getTimeOfDayList().size(); ++i) ((IntegerArrayList)getTimeOfDayList()).add(t.getTimeOfDayList().get(i));
      } else {
        nullifyTimeOfDayList();
      }
      if (t.hasTimeOfDayListOfNullable()) {
        if (!hasTimeOfDayListOfNullable()) {
          setTimeOfDayListOfNullable(new IntegerArrayList(t.getTimeOfDayListOfNullable().size()));
        } else {
          getTimeOfDayListOfNullable().clear();
        }
        for (int i = 0; i < getTimeOfDayListOfNullable().size(); ++i) ((IntegerArrayList)getTimeOfDayListOfNullable()).add(t.getTimeOfDayListOfNullable().get(i));
      } else {
        nullifyTimeOfDayListOfNullable();
      }
      if (t.hasNullableTimeOfDayList()) {
        if (!hasNullableTimeOfDayList()) {
          setNullableTimeOfDayList(new IntegerArrayList(t.getNullableTimeOfDayList().size()));
        } else {
          getNullableTimeOfDayList().clear();
        }
        for (int i = 0; i < getNullableTimeOfDayList().size(); ++i) ((IntegerArrayList)getNullableTimeOfDayList()).add(t.getNullableTimeOfDayList().get(i));
      } else {
        nullifyNullableTimeOfDayList();
      }
      if (t.hasNullableTimeOfDayListOfNullable()) {
        if (!hasNullableTimeOfDayListOfNullable()) {
          setNullableTimeOfDayListOfNullable(new IntegerArrayList(t.getNullableTimeOfDayListOfNullable().size()));
        } else {
          getNullableTimeOfDayListOfNullable().clear();
        }
        for (int i = 0; i < getNullableTimeOfDayListOfNullable().size(); ++i) ((IntegerArrayList)getNullableTimeOfDayListOfNullable()).add(t.getNullableTimeOfDayListOfNullable().get(i));
      } else {
        nullifyNullableTimeOfDayListOfNullable();
      }
      if (t.hasEnumList()) {
        if (!hasEnumList()) {
          setEnumList(new ObjectArrayList<TestEnum>(t.getEnumList().size()));
        } else {
          getEnumList().clear();
        }
        for (int i = 0; i < getEnumList().size(); ++i) ((ObjectArrayList<TestEnum>)getEnumList()).add(t.getEnumList().get(i));
      } else {
        nullifyEnumList();
      }
      if (t.hasEnumListOfNullable()) {
        if (!hasEnumListOfNullable()) {
          setEnumListOfNullable(new ObjectArrayList<TestEnum>(t.getEnumListOfNullable().size()));
        } else {
          getEnumListOfNullable().clear();
        }
        for (int i = 0; i < getEnumListOfNullable().size(); ++i) ((ObjectArrayList<TestEnum>)getEnumListOfNullable()).add(t.getEnumListOfNullable().get(i));
      } else {
        nullifyEnumListOfNullable();
      }
      if (t.hasNullableEnumList()) {
        if (!hasNullableEnumList()) {
          setNullableEnumList(new ObjectArrayList<TestEnum>(t.getNullableEnumList().size()));
        } else {
          getNullableEnumList().clear();
        }
        for (int i = 0; i < getNullableEnumList().size(); ++i) ((ObjectArrayList<TestEnum>)getNullableEnumList()).add(t.getNullableEnumList().get(i));
      } else {
        nullifyNullableEnumList();
      }
      if (t.hasNullableEnumListOfNullable()) {
        if (!hasNullableEnumListOfNullable()) {
          setNullableEnumListOfNullable(new ObjectArrayList<TestEnum>(t.getNullableEnumListOfNullable().size()));
        } else {
          getNullableEnumListOfNullable().clear();
        }
        for (int i = 0; i < getNullableEnumListOfNullable().size(); ++i) ((ObjectArrayList<TestEnum>)getNullableEnumListOfNullable()).add(t.getNullableEnumListOfNullable().get(i));
      } else {
        nullifyNullableEnumListOfNullable();
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
    str.append("{ \"$type\":  \"AllTypesMessage\"");
    if (hasObject()) {
      str.append(", \"object\": ");
      getObject().toString(str);
    }
    if (hasLists()) {
      str.append(", \"lists\": ");
      getLists().toString(str);
    }
    if (hasBooleanList()) {
      str.append(", \"booleanList\": [");
      if (getBooleanList().size() > 0) {
        str.append(getBooleanList().get(0));
      }
      for (int i = 1; i < getBooleanList().size(); ++i) {
        str.append(", ");
        str.append(getBooleanList().get(i));
      }
      str.append("]");
    }
    if (hasBooleanListOfNullable()) {
      str.append(", \"booleanListOfNullable\": [");
      if (getBooleanListOfNullable().size() > 0) {
        str.append(getBooleanListOfNullable().get(0));
      }
      for (int i = 1; i < getBooleanListOfNullable().size(); ++i) {
        str.append(", ");
        str.append(getBooleanListOfNullable().get(i));
      }
      str.append("]");
    }
    if (hasNullableBooleanList()) {
      str.append(", \"nullableBooleanList\": [");
      if (getNullableBooleanList().size() > 0) {
        str.append(getNullableBooleanList().get(0));
      }
      for (int i = 1; i < getNullableBooleanList().size(); ++i) {
        str.append(", ");
        str.append(getNullableBooleanList().get(i));
      }
      str.append("]");
    }
    if (hasNullableBooleanListOfNullable()) {
      str.append(", \"nullableBooleanListOfNullable\": [");
      if (getNullableBooleanListOfNullable().size() > 0) {
        str.append(getNullableBooleanListOfNullable().get(0));
      }
      for (int i = 1; i < getNullableBooleanListOfNullable().size(); ++i) {
        str.append(", ");
        str.append(getNullableBooleanListOfNullable().get(i));
      }
      str.append("]");
    }
    if (hasByteList()) {
      str.append(", \"byteList\": [");
      if (getByteList().size() > 0) {
        str.append(getByteList().get(0));
      }
      for (int i = 1; i < getByteList().size(); ++i) {
        str.append(", ");
        str.append(getByteList().get(i));
      }
      str.append("]");
    }
    if (hasByteListOfNullable()) {
      str.append(", \"byteListOfNullable\": [");
      if (getByteListOfNullable().size() > 0) {
        str.append(getByteListOfNullable().get(0));
      }
      for (int i = 1; i < getByteListOfNullable().size(); ++i) {
        str.append(", ");
        str.append(getByteListOfNullable().get(i));
      }
      str.append("]");
    }
    if (hasNullableByteList()) {
      str.append(", \"nullableByteList\": [");
      if (getNullableByteList().size() > 0) {
        str.append(getNullableByteList().get(0));
      }
      for (int i = 1; i < getNullableByteList().size(); ++i) {
        str.append(", ");
        str.append(getNullableByteList().get(i));
      }
      str.append("]");
    }
    if (hasNullableByteListOfNullable()) {
      str.append(", \"nullableByteListOfNullable\": [");
      if (getNullableByteListOfNullable().size() > 0) {
        str.append(getNullableByteListOfNullable().get(0));
      }
      for (int i = 1; i < getNullableByteListOfNullable().size(); ++i) {
        str.append(", ");
        str.append(getNullableByteListOfNullable().get(i));
      }
      str.append("]");
    }
    if (hasShortList()) {
      str.append(", \"shortList\": [");
      if (getShortList().size() > 0) {
        str.append(getShortList().get(0));
      }
      for (int i = 1; i < getShortList().size(); ++i) {
        str.append(", ");
        str.append(getShortList().get(i));
      }
      str.append("]");
    }
    if (hasShortListOfNullable()) {
      str.append(", \"shortListOfNullable\": [");
      if (getShortListOfNullable().size() > 0) {
        str.append(getShortListOfNullable().get(0));
      }
      for (int i = 1; i < getShortListOfNullable().size(); ++i) {
        str.append(", ");
        str.append(getShortListOfNullable().get(i));
      }
      str.append("]");
    }
    if (hasNullableShortList()) {
      str.append(", \"nullableShortList\": [");
      if (getNullableShortList().size() > 0) {
        str.append(getNullableShortList().get(0));
      }
      for (int i = 1; i < getNullableShortList().size(); ++i) {
        str.append(", ");
        str.append(getNullableShortList().get(i));
      }
      str.append("]");
    }
    if (hasNullableShortListOfNullable()) {
      str.append(", \"nullableShortListOfNullable\": [");
      if (getNullableShortListOfNullable().size() > 0) {
        str.append(getNullableShortListOfNullable().get(0));
      }
      for (int i = 1; i < getNullableShortListOfNullable().size(); ++i) {
        str.append(", ");
        str.append(getNullableShortListOfNullable().get(i));
      }
      str.append("]");
    }
    if (hasIntList()) {
      str.append(", \"intList\": [");
      if (getIntList().size() > 0) {
        str.append(getIntList().get(0));
      }
      for (int i = 1; i < getIntList().size(); ++i) {
        str.append(", ");
        str.append(getIntList().get(i));
      }
      str.append("]");
    }
    if (hasIntListOfNullable()) {
      str.append(", \"intListOfNullable\": [");
      if (getIntListOfNullable().size() > 0) {
        str.append(getIntListOfNullable().get(0));
      }
      for (int i = 1; i < getIntListOfNullable().size(); ++i) {
        str.append(", ");
        str.append(getIntListOfNullable().get(i));
      }
      str.append("]");
    }
    if (hasNullableIntList()) {
      str.append(", \"nullableIntList\": [");
      if (getNullableIntList().size() > 0) {
        str.append(getNullableIntList().get(0));
      }
      for (int i = 1; i < getNullableIntList().size(); ++i) {
        str.append(", ");
        str.append(getNullableIntList().get(i));
      }
      str.append("]");
    }
    if (hasNullableIntListOfNullable()) {
      str.append(", \"nullableIntListOfNullable\": [");
      if (getNullableIntListOfNullable().size() > 0) {
        str.append(getNullableIntListOfNullable().get(0));
      }
      for (int i = 1; i < getNullableIntListOfNullable().size(); ++i) {
        str.append(", ");
        str.append(getNullableIntListOfNullable().get(i));
      }
      str.append("]");
    }
    if (hasLongList()) {
      str.append(", \"longList\": [");
      if (getLongList().size() > 0) {
        str.append(getLongList().get(0));
      }
      for (int i = 1; i < getLongList().size(); ++i) {
        str.append(", ");
        str.append(getLongList().get(i));
      }
      str.append("]");
    }
    if (hasLongListOfNullable()) {
      str.append(", \"longListOfNullable\": [");
      if (getLongListOfNullable().size() > 0) {
        str.append(getLongListOfNullable().get(0));
      }
      for (int i = 1; i < getLongListOfNullable().size(); ++i) {
        str.append(", ");
        str.append(getLongListOfNullable().get(i));
      }
      str.append("]");
    }
    if (hasNullableLongList()) {
      str.append(", \"nullableLongList\": [");
      if (getNullableLongList().size() > 0) {
        str.append(getNullableLongList().get(0));
      }
      for (int i = 1; i < getNullableLongList().size(); ++i) {
        str.append(", ");
        str.append(getNullableLongList().get(i));
      }
      str.append("]");
    }
    if (hasNullableLongListOfNullable()) {
      str.append(", \"nullableLongListOfNullable\": [");
      if (getNullableLongListOfNullable().size() > 0) {
        str.append(getNullableLongListOfNullable().get(0));
      }
      for (int i = 1; i < getNullableLongListOfNullable().size(); ++i) {
        str.append(", ");
        str.append(getNullableLongListOfNullable().get(i));
      }
      str.append("]");
    }
    if (hasDecimalList()) {
      str.append(", \"decimalList\": [");
      if (getDecimalList().size() > 0) {
        str.append(getDecimalList().get(0));
      }
      for (int i = 1; i < getDecimalList().size(); ++i) {
        str.append(", ");
        str.append(getDecimalList().get(i));
      }
      str.append("]");
    }
    if (hasDecimalListOfNullable()) {
      str.append(", \"decimalListOfNullable\": [");
      if (getDecimalListOfNullable().size() > 0) {
        str.append(getDecimalListOfNullable().get(0));
      }
      for (int i = 1; i < getDecimalListOfNullable().size(); ++i) {
        str.append(", ");
        str.append(getDecimalListOfNullable().get(i));
      }
      str.append("]");
    }
    if (hasNullableDecimalList()) {
      str.append(", \"nullableDecimalList\": [");
      if (getNullableDecimalList().size() > 0) {
        str.append(getNullableDecimalList().get(0));
      }
      for (int i = 1; i < getNullableDecimalList().size(); ++i) {
        str.append(", ");
        str.append(getNullableDecimalList().get(i));
      }
      str.append("]");
    }
    if (hasNullableDecimalListOfNullable()) {
      str.append(", \"nullableDecimalListOfNullable\": [");
      if (getNullableDecimalListOfNullable().size() > 0) {
        str.append(getNullableDecimalListOfNullable().get(0));
      }
      for (int i = 1; i < getNullableDecimalListOfNullable().size(); ++i) {
        str.append(", ");
        str.append(getNullableDecimalListOfNullable().get(i));
      }
      str.append("]");
    }
    if (hasDoubleList()) {
      str.append(", \"doubleList\": [");
      if (getDoubleList().size() > 0) {
        str.append(getDoubleList().get(0));
      }
      for (int i = 1; i < getDoubleList().size(); ++i) {
        str.append(", ");
        str.append(getDoubleList().get(i));
      }
      str.append("]");
    }
    if (hasDoubleListOfNullable()) {
      str.append(", \"doubleListOfNullable\": [");
      if (getDoubleListOfNullable().size() > 0) {
        str.append(getDoubleListOfNullable().get(0));
      }
      for (int i = 1; i < getDoubleListOfNullable().size(); ++i) {
        str.append(", ");
        str.append(getDoubleListOfNullable().get(i));
      }
      str.append("]");
    }
    if (hasNullableDoubleList()) {
      str.append(", \"nullableDoubleList\": [");
      if (getNullableDoubleList().size() > 0) {
        str.append(getNullableDoubleList().get(0));
      }
      for (int i = 1; i < getNullableDoubleList().size(); ++i) {
        str.append(", ");
        str.append(getNullableDoubleList().get(i));
      }
      str.append("]");
    }
    if (hasNullableDoubleListOfNullable()) {
      str.append(", \"nullableDoubleListOfNullable\": [");
      if (getNullableDoubleListOfNullable().size() > 0) {
        str.append(getNullableDoubleListOfNullable().get(0));
      }
      for (int i = 1; i < getNullableDoubleListOfNullable().size(); ++i) {
        str.append(", ");
        str.append(getNullableDoubleListOfNullable().get(i));
      }
      str.append("]");
    }
    if (hasFloatList()) {
      str.append(", \"floatList\": [");
      if (getFloatList().size() > 0) {
        str.append(getFloatList().get(0));
      }
      for (int i = 1; i < getFloatList().size(); ++i) {
        str.append(", ");
        str.append(getFloatList().get(i));
      }
      str.append("]");
    }
    if (hasFloatListOfNullable()) {
      str.append(", \"floatListOfNullable\": [");
      if (getFloatListOfNullable().size() > 0) {
        str.append(getFloatListOfNullable().get(0));
      }
      for (int i = 1; i < getFloatListOfNullable().size(); ++i) {
        str.append(", ");
        str.append(getFloatListOfNullable().get(i));
      }
      str.append("]");
    }
    if (hasNullableFloatList()) {
      str.append(", \"nullableFloatList\": [");
      if (getNullableFloatList().size() > 0) {
        str.append(getNullableFloatList().get(0));
      }
      for (int i = 1; i < getNullableFloatList().size(); ++i) {
        str.append(", ");
        str.append(getNullableFloatList().get(i));
      }
      str.append("]");
    }
    if (hasNullableFloatListOfNullable()) {
      str.append(", \"nullableFloatListOfNullable\": [");
      if (getNullableFloatListOfNullable().size() > 0) {
        str.append(getNullableFloatListOfNullable().get(0));
      }
      for (int i = 1; i < getNullableFloatListOfNullable().size(); ++i) {
        str.append(", ");
        str.append(getNullableFloatListOfNullable().get(i));
      }
      str.append("]");
    }
    if (hasTextList()) {
      str.append(", \"textList\": [");
      if (getTextList().size() > 0) {
        str.append(getTextList().get(0));
      }
      for (int i = 1; i < getTextList().size(); ++i) {
        str.append(", ");
        str.append(getTextList().get(i));
      }
      str.append("]");
    }
    if (hasTextListOfNullable()) {
      str.append(", \"textListOfNullable\": [");
      if (getTextListOfNullable().size() > 0) {
        str.append(getTextListOfNullable().get(0));
      }
      for (int i = 1; i < getTextListOfNullable().size(); ++i) {
        str.append(", ");
        str.append(getTextListOfNullable().get(i));
      }
      str.append("]");
    }
    if (hasNullableTextList()) {
      str.append(", \"nullableTextList\": [");
      if (getNullableTextList().size() > 0) {
        str.append(getNullableTextList().get(0));
      }
      for (int i = 1; i < getNullableTextList().size(); ++i) {
        str.append(", ");
        str.append(getNullableTextList().get(i));
      }
      str.append("]");
    }
    if (hasNullableTextListOfNullable()) {
      str.append(", \"nullableTextListOfNullable\": [");
      if (getNullableTextListOfNullable().size() > 0) {
        str.append(getNullableTextListOfNullable().get(0));
      }
      for (int i = 1; i < getNullableTextListOfNullable().size(); ++i) {
        str.append(", ");
        str.append(getNullableTextListOfNullable().get(i));
      }
      str.append("]");
    }
    if (hasAsciiTextList()) {
      str.append(", \"asciiTextList\": [");
      if (getAsciiTextList().size() > 0) {
        str.append(getAsciiTextList().get(0));
      }
      for (int i = 1; i < getAsciiTextList().size(); ++i) {
        str.append(", ");
        str.append(getAsciiTextList().get(i));
      }
      str.append("]");
    }
    if (hasAsciiTextListOfNullable()) {
      str.append(", \"asciiTextListOfNullable\": [");
      if (getAsciiTextListOfNullable().size() > 0) {
        str.append(getAsciiTextListOfNullable().get(0));
      }
      for (int i = 1; i < getAsciiTextListOfNullable().size(); ++i) {
        str.append(", ");
        str.append(getAsciiTextListOfNullable().get(i));
      }
      str.append("]");
    }
    if (hasNullableAsciiTextList()) {
      str.append(", \"nullableAsciiTextList\": [");
      if (getNullableAsciiTextList().size() > 0) {
        str.append(getNullableAsciiTextList().get(0));
      }
      for (int i = 1; i < getNullableAsciiTextList().size(); ++i) {
        str.append(", ");
        str.append(getNullableAsciiTextList().get(i));
      }
      str.append("]");
    }
    if (hasNullableAsciiTextListOfNullable()) {
      str.append(", \"nullableAsciiTextListOfNullable\": [");
      if (getNullableAsciiTextListOfNullable().size() > 0) {
        str.append(getNullableAsciiTextListOfNullable().get(0));
      }
      for (int i = 1; i < getNullableAsciiTextListOfNullable().size(); ++i) {
        str.append(", ");
        str.append(getNullableAsciiTextListOfNullable().get(i));
      }
      str.append("]");
    }
    if (hasAlphanumericList()) {
      str.append(", \"alphanumericList\": [");
      if (getAlphanumericList().size() > 0) {
        str.append(getAlphanumericList().get(0));
      }
      for (int i = 1; i < getAlphanumericList().size(); ++i) {
        str.append(", ");
        str.append(getAlphanumericList().get(i));
      }
      str.append("]");
    }
    if (hasAlphanumericListOfNullable()) {
      str.append(", \"alphanumericListOfNullable\": [");
      if (getAlphanumericListOfNullable().size() > 0) {
        str.append(getAlphanumericListOfNullable().get(0));
      }
      for (int i = 1; i < getAlphanumericListOfNullable().size(); ++i) {
        str.append(", ");
        str.append(getAlphanumericListOfNullable().get(i));
      }
      str.append("]");
    }
    if (hasNullableAlphanumericList()) {
      str.append(", \"nullableAlphanumericList\": [");
      if (getNullableAlphanumericList().size() > 0) {
        str.append(getNullableAlphanumericList().get(0));
      }
      for (int i = 1; i < getNullableAlphanumericList().size(); ++i) {
        str.append(", ");
        str.append(getNullableAlphanumericList().get(i));
      }
      str.append("]");
    }
    if (hasNullableAlphanumericListOfNullable()) {
      str.append(", \"nullableAlphanumericListOfNullable\": [");
      if (getNullableAlphanumericListOfNullable().size() > 0) {
        str.append(getNullableAlphanumericListOfNullable().get(0));
      }
      for (int i = 1; i < getNullableAlphanumericListOfNullable().size(); ++i) {
        str.append(", ");
        str.append(getNullableAlphanumericListOfNullable().get(i));
      }
      str.append("]");
    }
    if (hasObjectsList()) {
      str.append(", \"objectsList\": [");
      if (getObjectsList().size() > 0) {
        if (getObjectsList().get(0) == null) {
          str.append("null");
        } else {
          getObjectsList().get(0).toString(str);
        }
      }
      for (int i = 1; i < getObjectsList().size(); ++i) {
        str.append(", ");
        if (getObjectsList().get(i) == null) {
          str.append("null");
        } else {
          getObjectsList().get(i).toString(str);
        }
      }
      str.append("]");
    }
    if (hasObjectsListOfNullable()) {
      str.append(", \"objectsListOfNullable\": [");
      if (getObjectsListOfNullable().size() > 0) {
        if (getObjectsListOfNullable().get(0) == null) {
          str.append("null");
        } else {
          getObjectsListOfNullable().get(0).toString(str);
        }
      }
      for (int i = 1; i < getObjectsListOfNullable().size(); ++i) {
        str.append(", ");
        if (getObjectsListOfNullable().get(i) == null) {
          str.append("null");
        } else {
          getObjectsListOfNullable().get(i).toString(str);
        }
      }
      str.append("]");
    }
    if (hasNullableObjectsList()) {
      str.append(", \"nullableObjectsList\": [");
      if (getNullableObjectsList().size() > 0) {
        if (getNullableObjectsList().get(0) == null) {
          str.append("null");
        } else {
          getNullableObjectsList().get(0).toString(str);
        }
      }
      for (int i = 1; i < getNullableObjectsList().size(); ++i) {
        str.append(", ");
        if (getNullableObjectsList().get(i) == null) {
          str.append("null");
        } else {
          getNullableObjectsList().get(i).toString(str);
        }
      }
      str.append("]");
    }
    if (hasNullableObjectsListOfNullable()) {
      str.append(", \"nullableObjectsListOfNullable\": [");
      if (getNullableObjectsListOfNullable().size() > 0) {
        if (getNullableObjectsListOfNullable().get(0) == null) {
          str.append("null");
        } else {
          getNullableObjectsListOfNullable().get(0).toString(str);
        }
      }
      for (int i = 1; i < getNullableObjectsListOfNullable().size(); ++i) {
        str.append(", ");
        if (getNullableObjectsListOfNullable().get(i) == null) {
          str.append("null");
        } else {
          getNullableObjectsListOfNullable().get(i).toString(str);
        }
      }
      str.append("]");
    }
    if (hasListOfLists()) {
      str.append(", \"listOfLists\": [");
      if (getListOfLists().size() > 0) {
        if (getListOfLists().get(0) == null) {
          str.append("null");
        } else {
          getListOfLists().get(0).toString(str);
        }
      }
      for (int i = 1; i < getListOfLists().size(); ++i) {
        str.append(", ");
        if (getListOfLists().get(i) == null) {
          str.append("null");
        } else {
          getListOfLists().get(i).toString(str);
        }
      }
      str.append("]");
    }
    if (hasTimestampList()) {
      str.append(", \"timestampList\": [");
      if (getTimestampList().size() > 0) {
        str.append(getTimestampList().get(0));
      }
      for (int i = 1; i < getTimestampList().size(); ++i) {
        str.append(", ");
        str.append(getTimestampList().get(i));
      }
      str.append("]");
    }
    if (hasTimestampListOfNullable()) {
      str.append(", \"timestampListOfNullable\": [");
      if (getTimestampListOfNullable().size() > 0) {
        str.append(getTimestampListOfNullable().get(0));
      }
      for (int i = 1; i < getTimestampListOfNullable().size(); ++i) {
        str.append(", ");
        str.append(getTimestampListOfNullable().get(i));
      }
      str.append("]");
    }
    if (hasNullableTimestampList()) {
      str.append(", \"nullableTimestampList\": [");
      if (getNullableTimestampList().size() > 0) {
        str.append(getNullableTimestampList().get(0));
      }
      for (int i = 1; i < getNullableTimestampList().size(); ++i) {
        str.append(", ");
        str.append(getNullableTimestampList().get(i));
      }
      str.append("]");
    }
    if (hasNullableTimestampListOfNullable()) {
      str.append(", \"nullableTimestampListOfNullable\": [");
      if (getNullableTimestampListOfNullable().size() > 0) {
        str.append(getNullableTimestampListOfNullable().get(0));
      }
      for (int i = 1; i < getNullableTimestampListOfNullable().size(); ++i) {
        str.append(", ");
        str.append(getNullableTimestampListOfNullable().get(i));
      }
      str.append("]");
    }
    if (hasTimeOfDayList()) {
      str.append(", \"timeOfDayList\": [");
      if (getTimeOfDayList().size() > 0) {
        str.append(getTimeOfDayList().get(0));
      }
      for (int i = 1; i < getTimeOfDayList().size(); ++i) {
        str.append(", ");
        str.append(getTimeOfDayList().get(i));
      }
      str.append("]");
    }
    if (hasTimeOfDayListOfNullable()) {
      str.append(", \"timeOfDayListOfNullable\": [");
      if (getTimeOfDayListOfNullable().size() > 0) {
        str.append(getTimeOfDayListOfNullable().get(0));
      }
      for (int i = 1; i < getTimeOfDayListOfNullable().size(); ++i) {
        str.append(", ");
        str.append(getTimeOfDayListOfNullable().get(i));
      }
      str.append("]");
    }
    if (hasNullableTimeOfDayList()) {
      str.append(", \"nullableTimeOfDayList\": [");
      if (getNullableTimeOfDayList().size() > 0) {
        str.append(getNullableTimeOfDayList().get(0));
      }
      for (int i = 1; i < getNullableTimeOfDayList().size(); ++i) {
        str.append(", ");
        str.append(getNullableTimeOfDayList().get(i));
      }
      str.append("]");
    }
    if (hasNullableTimeOfDayListOfNullable()) {
      str.append(", \"nullableTimeOfDayListOfNullable\": [");
      if (getNullableTimeOfDayListOfNullable().size() > 0) {
        str.append(getNullableTimeOfDayListOfNullable().get(0));
      }
      for (int i = 1; i < getNullableTimeOfDayListOfNullable().size(); ++i) {
        str.append(", ");
        str.append(getNullableTimeOfDayListOfNullable().get(i));
      }
      str.append("]");
    }
    if (hasEnumList()) {
      str.append(", \"enumList\": [");
      if (getEnumList().size() > 0) {
        str.append(getEnumList().get(0));
      }
      for (int i = 1; i < getEnumList().size(); ++i) {
        str.append(", ");
        str.append(getEnumList().get(i));
      }
      str.append("]");
    }
    if (hasEnumListOfNullable()) {
      str.append(", \"enumListOfNullable\": [");
      if (getEnumListOfNullable().size() > 0) {
        str.append(getEnumListOfNullable().get(0));
      }
      for (int i = 1; i < getEnumListOfNullable().size(); ++i) {
        str.append(", ");
        str.append(getEnumListOfNullable().get(i));
      }
      str.append("]");
    }
    if (hasNullableEnumList()) {
      str.append(", \"nullableEnumList\": [");
      if (getNullableEnumList().size() > 0) {
        str.append(getNullableEnumList().get(0));
      }
      for (int i = 1; i < getNullableEnumList().size(); ++i) {
        str.append(", ");
        str.append(getNullableEnumList().get(i));
      }
      str.append("]");
    }
    if (hasNullableEnumListOfNullable()) {
      str.append(", \"nullableEnumListOfNullable\": [");
      if (getNullableEnumListOfNullable().size() > 0) {
        str.append(getNullableEnumListOfNullable().get(0));
      }
      for (int i = 1; i < getNullableEnumListOfNullable().size(); ++i) {
        str.append(", ");
        str.append(getNullableEnumListOfNullable().get(i));
      }
      str.append("]");
    }
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
