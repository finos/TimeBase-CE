package com.epam.deltix.qsrv.test.messages;

import com.epam.deltix.dfp.Decimal;
import com.epam.deltix.timebase.messages.RecordInfo;
import com.epam.deltix.util.collections.generated.*;

/**
 */
public interface AllTypesMessageInterface extends AllTypesMessageInfo, AllSimpleTypesMessageInterface {
  /**
   * @param value - Object
   */
  void setObject(AllSimpleTypesMessageInfo value);

  /**
   */
  void nullifyObject();

  /**
   * @param value - Lists
   */
  void setLists(AllListsMessageInfo value);

  /**
   */
  void nullifyLists();

  /**
   * @param value - Boolean List
   */
  void setBooleanList(ByteArrayList value);

  /**
   */
  void nullifyBooleanList();

  /**
   * @param value - Boolean List Of Nullable
   */
  void setBooleanListOfNullable(ByteArrayList value);

  /**
   */
  void nullifyBooleanListOfNullable();

  /**
   * @param value - Nullable Boolean List
   */
  void setNullableBooleanList(ByteArrayList value);

  /**
   */
  void nullifyNullableBooleanList();

  /**
   * @param value - Nullable Boolean List Of Nullable
   */
  void setNullableBooleanListOfNullable(ByteArrayList value);

  /**
   */
  void nullifyNullableBooleanListOfNullable();

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
   * @param value - Text List
   */
  void setTextList(ObjectArrayList<CharSequence> value);

  /**
   */
  void nullifyTextList();

  /**
   * @param value - Text List Of Nullable
   */
  void setTextListOfNullable(ObjectArrayList<CharSequence> value);

  /**
   */
  void nullifyTextListOfNullable();

  /**
   * @param value - Nullable Text List
   */
  void setNullableTextList(ObjectArrayList<CharSequence> value);

  /**
   */
  void nullifyNullableTextList();

  /**
   * @param value - Nullable Text List Of Nullable
   */
  void setNullableTextListOfNullable(ObjectArrayList<CharSequence> value);

  /**
   */
  void nullifyNullableTextListOfNullable();

  /**
   * @param value - Ascii Text List
   */
  void setAsciiTextList(ObjectArrayList<CharSequence> value);

  /**
   */
  void nullifyAsciiTextList();

  /**
   * @param value - Ascii Text List Of Nullable
   */
  void setAsciiTextListOfNullable(ObjectArrayList<CharSequence> value);

  /**
   */
  void nullifyAsciiTextListOfNullable();

  /**
   * @param value - Nullable Ascii Text List
   */
  void setNullableAsciiTextList(ObjectArrayList<CharSequence> value);

  /**
   */
  void nullifyNullableAsciiTextList();

  /**
   * @param value - Nullable Ascii Text List Of Nullable
   */
  void setNullableAsciiTextListOfNullable(ObjectArrayList<CharSequence> value);

  /**
   */
  void nullifyNullableAsciiTextListOfNullable();

  /**
   * @param value - Alphanumeric List
   */
  void setAlphanumericList(LongArrayList value);

  /**
   */
  void nullifyAlphanumericList();

  /**
   * @param value - Alphanumeric List Of Nullable
   */
  void setAlphanumericListOfNullable(LongArrayList value);

  /**
   */
  void nullifyAlphanumericListOfNullable();

  /**
   * @param value - Nullable Alphanumeric List
   */
  void setNullableAlphanumericList(LongArrayList value);

  /**
   */
  void nullifyNullableAlphanumericList();

  /**
   * @param value - Nullable Alphanumeric List Of Nullable
   */
  void setNullableAlphanumericListOfNullable(LongArrayList value);

  /**
   */
  void nullifyNullableAlphanumericListOfNullable();

  /**
   * @param value - Objects List
   */
  void setObjectsList(ObjectArrayList<AllSimpleTypesMessageInfo> value);

  /**
   */
  void nullifyObjectsList();

  /**
   * @param value - Objects List Of Nullable
   */
  void setObjectsListOfNullable(ObjectArrayList<AllSimpleTypesMessageInfo> value);

  /**
   */
  void nullifyObjectsListOfNullable();

  /**
   * @param value - Nullable Objects List
   */
  void setNullableObjectsList(ObjectArrayList<AllSimpleTypesMessageInfo> value);

  /**
   */
  void nullifyNullableObjectsList();

  /**
   * @param value - Nullable Objects List Of Nullable
   */
  void setNullableObjectsListOfNullable(ObjectArrayList<AllSimpleTypesMessageInfo> value);

  /**
   */
  void nullifyNullableObjectsListOfNullable();

  /**
   * @param value - List Of Lists
   */
  void setListOfLists(ObjectArrayList<AllListsMessageInfo> value);

  /**
   */
  void nullifyListOfLists();

  /**
   * @param value - Timestamp List
   */
  void setTimestampList(LongArrayList value);

  /**
   */
  void nullifyTimestampList();

  /**
   * @param value - Timestamp List Of Nullable
   */
  void setTimestampListOfNullable(LongArrayList value);

  /**
   */
  void nullifyTimestampListOfNullable();

  /**
   * @param value - Nullable Timestamp List
   */
  void setNullableTimestampList(LongArrayList value);

  /**
   */
  void nullifyNullableTimestampList();

  /**
   * @param value - Nullable Timestamp List Of Nullable
   */
  void setNullableTimestampListOfNullable(LongArrayList value);

  /**
   */
  void nullifyNullableTimestampListOfNullable();

  /**
   * @param value - Time Of Day List
   */
  void setTimeOfDayList(IntegerArrayList value);

  /**
   */
  void nullifyTimeOfDayList();

  /**
   * @param value - Time Of Day List Of Nullable
   */
  void setTimeOfDayListOfNullable(IntegerArrayList value);

  /**
   */
  void nullifyTimeOfDayListOfNullable();

  /**
   * @param value - Nullable Time Of Day List
   */
  void setNullableTimeOfDayList(IntegerArrayList value);

  /**
   */
  void nullifyNullableTimeOfDayList();

  /**
   * @param value - Nullable Time Of Day List Of Nullable
   */
  void setNullableTimeOfDayListOfNullable(IntegerArrayList value);

  /**
   */
  void nullifyNullableTimeOfDayListOfNullable();

  /**
   * @param value - Enum List
   */
  void setEnumList(ObjectArrayList<TestEnum> value);

  /**
   */
  void nullifyEnumList();

  /**
   * @param value - Enum List Of Nullable
   */
  void setEnumListOfNullable(ObjectArrayList<TestEnum> value);

  /**
   */
  void nullifyEnumListOfNullable();

  /**
   * @param value - Nullable Enum List
   */
  void setNullableEnumList(ObjectArrayList<TestEnum> value);

  /**
   */
  void nullifyNullableEnumList();

  /**
   * @param value - Nullable Enum List Of Nullable
   */
  void setNullableEnumListOfNullable(ObjectArrayList<TestEnum> value);

  /**
   */
  void nullifyNullableEnumListOfNullable();

  /**
   * Method nullifies all instance properties
   */
  @Override
  AllTypesMessageInterface nullify();

  /**
   * Resets all instance properties to their default values
   */
  @Override
  AllTypesMessageInterface reset();

  @Override
  AllTypesMessageInterface copyFrom(RecordInfo template);
}
