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
import com.epam.deltix.dfp.Decimal64Utils;
import com.epam.deltix.timebase.messages.InstrumentMessage;
import com.epam.deltix.timebase.messages.RecordInfo;
import com.epam.deltix.timebase.messages.*;
import com.epam.deltix.timebase.messages.TypeConstants;

/**
 */
public class AllSimpleNumericsMessage extends InstrumentMessage implements AllSimpleNumericsMessageInterface {
  public static final String CLASS_NAME = AllSimpleNumericsMessage.class.getName();

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
   * Creates new instance of this class.
   * @return new instance of this class.
   */
  @Override
  protected AllSimpleNumericsMessage createInstance() {
    return new AllSimpleNumericsMessage();
  }

  /**
   * Method nullifies all instance properties
   */
  @Override
  public AllSimpleNumericsMessage nullify() {
    super.nullify();
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
    return this;
  }

  /**
   * Resets all instance properties to their default values
   */
  @Override
  public AllSimpleNumericsMessage reset() {
    super.reset();
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
    return this;
  }

  /**
   * Method copies state to a given instance
   */
  @Override
  public AllSimpleNumericsMessage clone() {
    AllSimpleNumericsMessage t = createInstance();
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
    if (!(obj instanceof AllSimpleNumericsMessageInfo)) return false;
    AllSimpleNumericsMessageInfo other =(AllSimpleNumericsMessageInfo)obj;
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
    return true;
  }

  /**
   * Returns a hash code value for the object. This method is * supported for the benefit of hash tables such as those provided by.
   */
  @Override
  public int hashCode() {
    int hash = super.hashCode();
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
    return hash;
  }

  /**
   * Method copies state to a given instance
   * @param template class instance that should be used as a copy source
   */
  @Override
  public AllSimpleNumericsMessage copyFrom(RecordInfo template) {
    super.copyFrom(template);
    if (template instanceof AllSimpleNumericsMessageInfo) {
      AllSimpleNumericsMessageInfo t = (AllSimpleNumericsMessageInfo)template;
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
    str.append("{ \"$type\":  \"AllSimpleNumericsMessage\"");
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
