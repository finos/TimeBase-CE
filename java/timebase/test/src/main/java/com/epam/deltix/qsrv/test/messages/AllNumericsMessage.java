package com.epam.deltix.qsrv.test.messages;

import com.epam.deltix.dfp.Decimal;
import com.epam.deltix.dfp.Decimal64Utils;
import com.epam.deltix.timebase.messages.*;
import com.epam.deltix.util.collections.generated.*;

/**
 */
public class AllNumericsMessage extends AllSimpleNumericsMessage implements AllNumericsMessageInterface {
  public static final String CLASS_NAME = AllNumericsMessage.class.getName();

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
   * Creates new instance of this class.
   * @return new instance of this class.
   */
  @Override
  protected AllNumericsMessage createInstance() {
    return new AllNumericsMessage();
  }

  /**
   * Method nullifies all instance properties
   */
  @Override
  public AllNumericsMessage nullify() {
    super.nullify();
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
    return this;
  }

  /**
   * Resets all instance properties to their default values
   */
  @Override
  public AllNumericsMessage reset() {
    super.reset();
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
    return this;
  }

  /**
   * Method copies state to a given instance
   */
  @Override
  public AllNumericsMessage clone() {
    AllNumericsMessage t = createInstance();
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
//    if (!(obj instanceof AllNumericsMessageInfo)) return false;
//    AllNumericsMessageInfo other =(AllNumericsMessageInfo)obj;
//    if (hasByteList() != other.hasByteList()) return false;
//    if (hasByteList()) {
//      if (getByteList().size() != other.getByteList().size()) return false;
//      else for (int j = 0; j < getByteList().size(); ++j) {
//        if (getByteList().getByte(j) != other.getByteList().getByte(j)) return false;
//      }
//    }
//    if (hasByteListOfNullable() != other.hasByteListOfNullable()) return false;
//    if (hasByteListOfNullable()) {
//      if (getByteListOfNullable().size() != other.getByteListOfNullable().size()) return false;
//      else for (int j = 0; j < getByteListOfNullable().size(); ++j) {
//        if (getByteListOfNullable().getByte(j) != other.getByteListOfNullable().getByte(j)) return false;
//      }
//    }
//    if (hasNullableByteList() != other.hasNullableByteList()) return false;
//    if (hasNullableByteList()) {
//      if (getNullableByteList().size() != other.getNullableByteList().size()) return false;
//      else for (int j = 0; j < getNullableByteList().size(); ++j) {
//        if (getNullableByteList().getByte(j) != other.getNullableByteList().getByte(j)) return false;
//      }
//    }
//    if (hasNullableByteListOfNullable() != other.hasNullableByteListOfNullable()) return false;
//    if (hasNullableByteListOfNullable()) {
//      if (getNullableByteListOfNullable().size() != other.getNullableByteListOfNullable().size()) return false;
//      else for (int j = 0; j < getNullableByteListOfNullable().size(); ++j) {
//        if (getNullableByteListOfNullable().getByte(j) != other.getNullableByteListOfNullable().getByte(j)) return false;
//      }
//    }
//    if (hasShortList() != other.hasShortList()) return false;
//    if (hasShortList()) {
//      if (getShortList().size() != other.getShortList().size()) return false;
//      else for (int j = 0; j < getShortList().size(); ++j) {
//        if (getShortList().getShort(j) != other.getShortList().getShort(j)) return false;
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
//    return true;
//  }

  /**
   * Returns a hash code value for the object. This method is * supported for the benefit of hash tables such as those provided by.
   */
  @Override
  public int hashCode() {
    int hash = super.hashCode();
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
    return hash;
  }

  /**
   * Method copies state to a given instance
   * @param template class instance that should be used as a copy source
   */
  @Override
  public AllNumericsMessage copyFrom(RecordInfo template) {
    super.copyFrom(template);
    if (template instanceof AllNumericsMessageInfo) {
      AllNumericsMessageInfo t = (AllNumericsMessageInfo)template;
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
    str.append("{ \"$type\":  \"AllNumericsMessage\"");
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
