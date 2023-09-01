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

import com.epam.deltix.containers.BinaryAsciiString;
import com.epam.deltix.containers.CharSequenceUtils;
import com.epam.deltix.containers.MutableString;

import com.epam.deltix.timebase.messages.*;
import com.epam.deltix.timebase.messages.InstrumentMessage;
import com.epam.deltix.util.collections.generated.LongArrayList;
import com.epam.deltix.util.collections.generated.ObjectArrayList;


/**
 */
public class VarcharListMessage extends InstrumentMessage implements VarcharListMessageInterface {
  public static final String CLASS_NAME = VarcharListMessage.class.getName();

  /**
   */
  protected LongArrayList alphanumericList = null;

  /**
   */
  protected LongArrayList alphanumericNullableList = null;

  /**
   */
  protected ObjectArrayList<CharSequence> charSequenceList = null;

  /**
   */
  protected ObjectArrayList<CharSequence> charSequenceNullableList = null;

  /**
   * @return Alphanumeric List
   */
  @SchemaElement
  @SchemaArrayType(
      isNullable = false,
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
   * @return Alphanumeric Nullable List
   */
  @SchemaElement
  @SchemaArrayType(
      elementEncoding = "ALPHANUMERIC(10)",
      elementDataType = SchemaDataType.VARCHAR
  )
  public LongArrayList getAlphanumericNullableList() {
    return alphanumericNullableList;
  }

  /**
   * @param value - Alphanumeric Nullable List
   */
  public void setAlphanumericNullableList(LongArrayList value) {
    this.alphanumericNullableList = value;
  }

  /**
   * @return true if Alphanumeric Nullable List is not null
   */
  public boolean hasAlphanumericNullableList() {
    return alphanumericNullableList != null;
  }

  /**
   */
  public void nullifyAlphanumericNullableList() {
    this.alphanumericNullableList = null;
  }

  /**
   * @return Char Sequence List
   */
  @SchemaElement
  @SchemaArrayType(
      isNullable = false,
      elementEncoding = "UTF8",
      elementDataType = SchemaDataType.VARCHAR
  )
  public ObjectArrayList<CharSequence> getCharSequenceList() {
    return charSequenceList;
  }

  /**
   * @param value - Char Sequence List
   */
  public void setCharSequenceList(ObjectArrayList<CharSequence> value) {
    this.charSequenceList = value;
  }

  /**
   * @return true if Char Sequence List is not null
   */
  public boolean hasCharSequenceList() {
    return charSequenceList != null;
  }

  /**
   */
  public void nullifyCharSequenceList() {
    this.charSequenceList = null;
  }

  /**
   * @return Char Sequence Nullable List
   */
  @SchemaElement
  @SchemaArrayType(
      elementEncoding = "UTF8",
      elementDataType = SchemaDataType.VARCHAR
  )
  public ObjectArrayList<CharSequence> getCharSequenceNullableList() {
    return charSequenceNullableList;
  }

  /**
   * @param value - Char Sequence Nullable List
   */
  public void setCharSequenceNullableList(ObjectArrayList<CharSequence> value) {
    this.charSequenceNullableList = value;
  }

  /**
   * @return true if Char Sequence Nullable List is not null
   */
  public boolean hasCharSequenceNullableList() {
    return charSequenceNullableList != null;
  }

  /**
   */
  public void nullifyCharSequenceNullableList() {
    this.charSequenceNullableList = null;
  }

  /**
   * Creates new instance of this class.
   * @return new instance of this class.
   */
  @Override
  protected VarcharListMessage createInstance() {
    return new VarcharListMessage();
  }

  /**
   * Method nullifies all instance properties
   */
  @Override
  public VarcharListMessage nullify() {
    super.nullify();
    nullifyAlphanumericList();
    nullifyAlphanumericNullableList();
    nullifyCharSequenceList();
    nullifyCharSequenceNullableList();
    return this;
  }

  /**
   * Resets all instance properties to their default values
   */
  @Override
  public VarcharListMessage reset() {
    super.reset();
    alphanumericList = null;
    alphanumericNullableList = null;
    charSequenceList = null;
    charSequenceNullableList = null;
    return this;
  }

  /**
   * Method copies state to a given instance
   */
  @Override
  public VarcharListMessage clone() {
    VarcharListMessage t = createInstance();
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
    if (!(obj instanceof VarcharListMessageInfo)) return false;
    VarcharListMessageInfo other =(VarcharListMessageInfo)obj;
    if (hasAlphanumericList() != other.hasAlphanumericList()) return false;
    if (hasAlphanumericList()) {
      if (getAlphanumericList().size() != other.getAlphanumericList().size()) return false;
      else for (int j = 0; j < getAlphanumericList().size(); ++j) {
        if (getAlphanumericList().getLong(j) != other.getAlphanumericList().getLong(j)) return false;
      }
    }
    if (hasAlphanumericNullableList() != other.hasAlphanumericNullableList()) return false;
    if (hasAlphanumericNullableList()) {
      if (getAlphanumericNullableList().size() != other.getAlphanumericNullableList().size()) return false;
      else for (int j = 0; j < getAlphanumericNullableList().size(); ++j) {
        if (getAlphanumericNullableList().getLong(j) != other.getAlphanumericNullableList().getLong(j)) return false;
      }
    }
    if (hasCharSequenceList() != other.hasCharSequenceList()) return false;
    if (hasCharSequenceList()) {
      if (getCharSequenceList().size() != other.getCharSequenceList().size()) return false;
      else for (int j = 0; j < getCharSequenceList().size(); ++j) {
        if ((getCharSequenceList().get(j) == null) == (other.getCharSequenceList().get(j) != null)) return false;
        if (getCharSequenceList().get(j) != null && getCharSequenceList().get(j).length() != other.getCharSequenceList().get(j).length()) return false; else {
          CharSequence s1 = getCharSequenceList().get(j);
          CharSequence s2 = other.getCharSequenceList().get(j);
          if ((s1 instanceof MutableString && s2 instanceof MutableString) || (s1 instanceof String && s2 instanceof String) || (s1 instanceof BinaryAsciiString && s2 instanceof BinaryAsciiString)) {
            if (!s1.equals(s2)) return false;
          } else {
            return CharSequenceUtils.equals(s1, s2);
          }
        }
      }
    }
    if (hasCharSequenceNullableList() != other.hasCharSequenceNullableList()) return false;
    if (hasCharSequenceNullableList()) {
      if (getCharSequenceNullableList().size() != other.getCharSequenceNullableList().size()) return false;
      else for (int j = 0; j < getCharSequenceNullableList().size(); ++j) {
        if ((getCharSequenceNullableList().get(j) == null) == (other.getCharSequenceNullableList().get(j) != null)) return false;
        if (getCharSequenceNullableList().get(j) != null && getCharSequenceNullableList().get(j).length() != other.getCharSequenceNullableList().get(j).length()) return false; else {
          CharSequence s1 = getCharSequenceNullableList().get(j);
          CharSequence s2 = other.getCharSequenceNullableList().get(j);
          if ((s1 instanceof MutableString && s2 instanceof MutableString) || (s1 instanceof String && s2 instanceof String) || (s1 instanceof BinaryAsciiString && s2 instanceof BinaryAsciiString)) {
            if (!s1.equals(s2)) return false;
          } else {
            return CharSequenceUtils.equals(s1, s2);
          }
        }
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
    if (hasAlphanumericList()) {
      hash = hash * 31 + getAlphanumericList().hashCode();
    }
    if (hasAlphanumericNullableList()) {
      hash = hash * 31 + getAlphanumericNullableList().hashCode();
    }
    if (hasCharSequenceList()) {
      for (int j = 0; j < getCharSequenceList().size(); ++j) {
        hash ^= getCharSequenceList().get(j).hashCode();
      }
    }
    if (hasCharSequenceNullableList()) {
      for (int j = 0; j < getCharSequenceNullableList().size(); ++j) {
        hash ^= getCharSequenceNullableList().get(j).hashCode();
      }
    }
    return hash;
  }

  /**
   * Method copies state to a given instance
   * @param template class instance that should be used as a copy source
   */
  @Override
  public VarcharListMessage copyFrom(RecordInfo template) {
    super.copyFrom(template);
    if (template instanceof VarcharListMessageInfo) {
      VarcharListMessageInfo t = (VarcharListMessageInfo)template;
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
      if (t.hasAlphanumericNullableList()) {
        if (!hasAlphanumericNullableList()) {
          setAlphanumericNullableList(new LongArrayList(t.getAlphanumericNullableList().size()));
        } else {
          getAlphanumericNullableList().clear();
        }
        for (int i = 0; i < getAlphanumericNullableList().size(); ++i) ((LongArrayList)getAlphanumericNullableList()).add(t.getAlphanumericNullableList().get(i));
      } else {
        nullifyAlphanumericNullableList();
      }
      if (t.hasCharSequenceList()) {
        if (!hasCharSequenceList()) {
          setCharSequenceList(new ObjectArrayList<CharSequence>(t.getCharSequenceList().size()));
        } else {
          getCharSequenceList().clear();
        }
        for (int i = 0; i < getCharSequenceList().size(); ++i) ((ObjectArrayList<CharSequence>)getCharSequenceList()).add(t.getCharSequenceList().get(i));
      } else {
        nullifyCharSequenceList();
      }
      if (t.hasCharSequenceNullableList()) {
        if (!hasCharSequenceNullableList()) {
          setCharSequenceNullableList(new ObjectArrayList<CharSequence>(t.getCharSequenceNullableList().size()));
        } else {
          getCharSequenceNullableList().clear();
        }
        for (int i = 0; i < getCharSequenceNullableList().size(); ++i) ((ObjectArrayList<CharSequence>)getCharSequenceNullableList()).add(t.getCharSequenceNullableList().get(i));
      } else {
        nullifyCharSequenceNullableList();
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
    str.append("{ \"$type\":  \"VarcharListMessage\"");
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
    if (hasAlphanumericNullableList()) {
      str.append(", \"alphanumericNullableList\": [");
      if (getAlphanumericNullableList().size() > 0) {
        str.append(getAlphanumericNullableList().get(0));
      }
      for (int i = 1; i < getAlphanumericNullableList().size(); ++i) {
        str.append(", ");
        str.append(getAlphanumericNullableList().get(i));
      }
      str.append("]");
    }
    if (hasCharSequenceList()) {
      str.append(", \"charSequenceList\": [");
      if (getCharSequenceList().size() > 0) {
        str.append(getCharSequenceList().get(0));
      }
      for (int i = 1; i < getCharSequenceList().size(); ++i) {
        str.append(", ");
        str.append(getCharSequenceList().get(i));
      }
      str.append("]");
    }
    if (hasCharSequenceNullableList()) {
      str.append(", \"charSequenceNullableList\": [");
      if (getCharSequenceNullableList().size() > 0) {
        str.append(getCharSequenceNullableList().get(0));
      }
      for (int i = 1; i < getCharSequenceNullableList().size(); ++i) {
        str.append(", ");
        str.append(getCharSequenceNullableList().get(i));
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
