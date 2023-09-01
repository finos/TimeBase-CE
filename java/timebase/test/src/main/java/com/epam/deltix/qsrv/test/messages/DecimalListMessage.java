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

package com.epam.deltix.qsrv.test.messages;

import com.epam.deltix.timebase.messages.*;
import com.epam.deltix.timebase.messages.InstrumentMessage;
import com.epam.deltix.util.collections.generated.LongArrayList;

/**
 */
public class DecimalListMessage extends InstrumentMessage implements DecimalListMessageInterface {
  public static final String CLASS_NAME = DecimalListMessage.class.getName();

  /**
   */
  protected LongArrayList decimalList = null;

  /**
   */
  protected LongArrayList decimalNullableList = null;

  /**
   * @return Decimal List
   */
  @SchemaElement
  @SchemaArrayType(
      isNullable = false,
      elementEncoding = "DECIMAL64",
      elementDataType = SchemaDataType.FLOAT
  )
  public LongArrayList getDecimalList() {
    return decimalList;
  }

  /**
   * @param value - Decimal List
   */
  public void setDecimalList(LongArrayList value) {
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
   * @return Decimal Nullable List
   */
  @SchemaElement
  @SchemaArrayType(
      elementEncoding = "DECIMAL64",
      elementDataType = SchemaDataType.FLOAT
  )
  public LongArrayList getDecimalNullableList() {
    return decimalNullableList;
  }

  /**
   * @param value - Decimal Nullable List
   */
  public void setDecimalNullableList(LongArrayList value) {
    this.decimalNullableList = value;
  }

  /**
   * @return true if Decimal Nullable List is not null
   */
  public boolean hasDecimalNullableList() {
    return decimalNullableList != null;
  }

  /**
   */
  public void nullifyDecimalNullableList() {
    this.decimalNullableList = null;
  }

  /**
   * Creates new instance of this class.
   * @return new instance of this class.
   */
  @Override
  protected DecimalListMessage createInstance() {
    return new DecimalListMessage();
  }

  /**
   * Method nullifies all instance properties
   */
  @Override
  public DecimalListMessage nullify() {
    super.nullify();
    nullifyDecimalList();
    nullifyDecimalNullableList();
    return this;
  }

  /**
   * Resets all instance properties to their default values
   */
  @Override
  public DecimalListMessage reset() {
    super.reset();
    decimalList = null;
    decimalNullableList = null;
    return this;
  }

  /**
   * Method copies state to a given instance
   */
  @Override
  public DecimalListMessage clone() {
    DecimalListMessage t = createInstance();
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
    if (!(obj instanceof DecimalListMessageInfo)) return false;
    DecimalListMessageInfo other =(DecimalListMessageInfo)obj;
    if (hasDecimalList() != other.hasDecimalList()) return false;
    if (hasDecimalList()) {
      if (getDecimalList().size() != other.getDecimalList().size()) return false;
      else for (int j = 0; j < getDecimalList().size(); ++j) {
        if (getDecimalList().getLong(j) != other.getDecimalList().getLong(j)) return false;
      }
    }
    if (hasDecimalNullableList() != other.hasDecimalNullableList()) return false;
    if (hasDecimalNullableList()) {
      if (getDecimalNullableList().size() != other.getDecimalNullableList().size()) return false;
      else for (int j = 0; j < getDecimalNullableList().size(); ++j) {
        if (getDecimalNullableList().getLong(j) != other.getDecimalNullableList().getLong(j)) return false;
      }
    }
    return true;
  }

  /**
   * Returns a hash code value for the object. This method is * supported for the benefit of hash tables such as those provided by.
   */
  @Override
  public int hashCode() {
    int hash = super.hashCode();
    if (hasDecimalList()) {
      hash = hash * 31 + getDecimalList().hashCode();
    }
    if (hasDecimalNullableList()) {
      hash = hash * 31 + getDecimalNullableList().hashCode();
    }
    return hash;
  }

  /**
   * Method copies state to a given instance
   * @param template class instance that should be used as a copy source
   */
  @Override
  public DecimalListMessage copyFrom(RecordInfo template) {
    super.copyFrom(template);
    if (template instanceof DecimalListMessageInfo) {
      DecimalListMessageInfo t = (DecimalListMessageInfo)template;
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
      if (t.hasDecimalNullableList()) {
        if (!hasDecimalNullableList()) {
          setDecimalNullableList(new LongArrayList(t.getDecimalNullableList().size()));
        } else {
          getDecimalNullableList().clear();
        }
        for (int i = 0; i < getDecimalNullableList().size(); ++i) ((LongArrayList)getDecimalNullableList()).add(t.getDecimalNullableList().get(i));
      } else {
        nullifyDecimalNullableList();
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
    str.append("{ \"$type\":  \"DecimalListMessage\"");
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
    if (hasDecimalNullableList()) {
      str.append(", \"decimalNullableList\": [");
      if (getDecimalNullableList().size() > 0) {
        str.append(getDecimalNullableList().get(0));
      }
      for (int i = 1; i < getDecimalNullableList().size(); ++i) {
        str.append(", ");
        str.append(getDecimalNullableList().get(i));
      }
      str.append("]");
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
