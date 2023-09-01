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
import com.epam.deltix.dfp.Decimal;
import com.epam.deltix.dfp.Decimal64Utils;
import com.epam.deltix.timebase.messages.*;
import com.epam.deltix.util.collections.generated.*;

/**
 */
public class AllListsMessage extends InstrumentMessage implements AllListsMessageInterface {
  public static final String CLASS_NAME = AllListsMessage.class.getName();

  /**
   */
  protected ByteArrayList nestedBooleanList = null;

  /**
   */
  protected ByteArrayList nestedByteList = null;

  /**
   */
  protected ShortArrayList nestedShortList = null;

  /**
   */
  protected IntegerArrayList nestedIntList = null;

  /**
   */
  protected LongArrayList nestedLongList = null;

  /**
   */
  @Decimal
  protected LongArrayList nestedDecimalList = null;

  /**
   */
  protected DoubleArrayList nestedDoubleList = null;

  /**
   */
  protected FloatArrayList nestedFloatList = null;

  /**
   */
  protected ObjectArrayList<CharSequence> nestedTextList = null;

  /**
   */
  protected ObjectArrayList<CharSequence> nestedAsciiTextList = null;

  /**
   */
  protected LongArrayList nestedAlphanumericList = null;

  /**
   */
  protected ObjectArrayList<AllSimpleTypesMessageInfo> nestedObjectsList = null;

  /**
   * @return Nested Boolean List
   */
  @SchemaElement
  @SchemaArrayType(
      elementDataType = SchemaDataType.BOOLEAN
  )
  public ByteArrayList getNestedBooleanList() {
    return nestedBooleanList;
  }

  /**
   * @param value - Nested Boolean List
   */
  public void setNestedBooleanList(ByteArrayList value) {
    this.nestedBooleanList = value;
  }

  /**
   * @return true if Nested Boolean List is not null
   */
  public boolean hasNestedBooleanList() {
    return nestedBooleanList != null;
  }

  /**
   */
  public void nullifyNestedBooleanList() {
    this.nestedBooleanList = null;
  }

  /**
   * @return Nested Byte List
   */
  @SchemaElement
  @SchemaArrayType(
      elementEncoding = "INT8",
      elementDataType = SchemaDataType.INTEGER
  )
  public ByteArrayList getNestedByteList() {
    return nestedByteList;
  }

  /**
   * @param value - Nested Byte List
   */
  public void setNestedByteList(ByteArrayList value) {
    this.nestedByteList = value;
  }

  /**
   * @return true if Nested Byte List is not null
   */
  public boolean hasNestedByteList() {
    return nestedByteList != null;
  }

  /**
   */
  public void nullifyNestedByteList() {
    this.nestedByteList = null;
  }

  /**
   * @return Nested Short List
   */
  @SchemaElement
  public ShortArrayList getNestedShortList() {
    return nestedShortList;
  }

  /**
   * @param value - Nested Short List
   */
  public void setNestedShortList(ShortArrayList value) {
    this.nestedShortList = value;
  }

  /**
   * @return true if Nested Short List is not null
   */
  public boolean hasNestedShortList() {
    return nestedShortList != null;
  }

  /**
   */
  public void nullifyNestedShortList() {
    this.nestedShortList = null;
  }

  /**
   * @return Nested Int List
   */
  @SchemaElement
  public IntegerArrayList getNestedIntList() {
    return nestedIntList;
  }

  /**
   * @param value - Nested Int List
   */
  public void setNestedIntList(IntegerArrayList value) {
    this.nestedIntList = value;
  }

  /**
   * @return true if Nested Int List is not null
   */
  public boolean hasNestedIntList() {
    return nestedIntList != null;
  }

  /**
   */
  public void nullifyNestedIntList() {
    this.nestedIntList = null;
  }

  /**
   * @return Nested Long List
   */
  @SchemaElement
  public LongArrayList getNestedLongList() {
    return nestedLongList;
  }

  /**
   * @param value - Nested Long List
   */
  public void setNestedLongList(LongArrayList value) {
    this.nestedLongList = value;
  }

  /**
   * @return true if Nested Long List is not null
   */
  public boolean hasNestedLongList() {
    return nestedLongList != null;
  }

  /**
   */
  public void nullifyNestedLongList() {
    this.nestedLongList = null;
  }

  /**
   * @return Nested Decimal List
   */
  @Decimal
  @SchemaElement
  @SchemaArrayType(
      elementEncoding = "DECIMAL64",
      elementDataType = SchemaDataType.FLOAT
  )
  public LongArrayList getNestedDecimalList() {
    return nestedDecimalList;
  }

  /**
   * @param value - Nested Decimal List
   */
  public void setNestedDecimalList(@Decimal LongArrayList value) {
    this.nestedDecimalList = value;
  }

  /**
   * @return true if Nested Decimal List is not null
   */
  public boolean hasNestedDecimalList() {
    return nestedDecimalList != null;
  }

  /**
   */
  public void nullifyNestedDecimalList() {
    this.nestedDecimalList = null;
  }

  /**
   * @return Nested Double List
   */
  @SchemaElement
  public DoubleArrayList getNestedDoubleList() {
    return nestedDoubleList;
  }

  /**
   * @param value - Nested Double List
   */
  public void setNestedDoubleList(DoubleArrayList value) {
    this.nestedDoubleList = value;
  }

  /**
   * @return true if Nested Double List is not null
   */
  public boolean hasNestedDoubleList() {
    return nestedDoubleList != null;
  }

  /**
   */
  public void nullifyNestedDoubleList() {
    this.nestedDoubleList = null;
  }

  /**
   * @return Nested Float List
   */
  @SchemaElement
  public FloatArrayList getNestedFloatList() {
    return nestedFloatList;
  }

  /**
   * @param value - Nested Float List
   */
  public void setNestedFloatList(FloatArrayList value) {
    this.nestedFloatList = value;
  }

  /**
   * @return true if Nested Float List is not null
   */
  public boolean hasNestedFloatList() {
    return nestedFloatList != null;
  }

  /**
   */
  public void nullifyNestedFloatList() {
    this.nestedFloatList = null;
  }

  /**
   * @return Nested Text List
   */
  @SchemaElement
  public ObjectArrayList<CharSequence> getNestedTextList() {
    return nestedTextList;
  }

  /**
   * @param value - Nested Text List
   */
  public void setNestedTextList(ObjectArrayList<CharSequence> value) {
    this.nestedTextList = value;
  }

  /**
   * @return true if Nested Text List is not null
   */
  public boolean hasNestedTextList() {
    return nestedTextList != null;
  }

  /**
   */
  public void nullifyNestedTextList() {
    this.nestedTextList = null;
  }

  /**
   * @return Nested Ascii Text List
   */
  @SchemaElement
  public ObjectArrayList<CharSequence> getNestedAsciiTextList() {
    return nestedAsciiTextList;
  }

  /**
   * @param value - Nested Ascii Text List
   */
  public void setNestedAsciiTextList(ObjectArrayList<CharSequence> value) {
    this.nestedAsciiTextList = value;
  }

  /**
   * @return true if Nested Ascii Text List is not null
   */
  public boolean hasNestedAsciiTextList() {
    return nestedAsciiTextList != null;
  }

  /**
   */
  public void nullifyNestedAsciiTextList() {
    this.nestedAsciiTextList = null;
  }

  /**
   * @return Nested Alphanumeric List
   */
  @SchemaElement
  @SchemaArrayType(
      elementEncoding = "ALPHANUMERIC(10)",
      elementDataType = SchemaDataType.VARCHAR
  )
  public LongArrayList getNestedAlphanumericList() {
    return nestedAlphanumericList;
  }

  /**
   * @param value - Nested Alphanumeric List
   */
  public void setNestedAlphanumericList(LongArrayList value) {
    this.nestedAlphanumericList = value;
  }

  /**
   * @return true if Nested Alphanumeric List is not null
   */
  public boolean hasNestedAlphanumericList() {
    return nestedAlphanumericList != null;
  }

  /**
   */
  public void nullifyNestedAlphanumericList() {
    this.nestedAlphanumericList = null;
  }

  /**
   * @return Nested Objects List
   */
  @SchemaElement
  @SchemaArrayType(
      elementTypes =  {
            AllSimpleTypesMessage.class}

  )
  public ObjectArrayList<AllSimpleTypesMessageInfo> getNestedObjectsList() {
    return nestedObjectsList;
  }

  /**
   * @param value - Nested Objects List
   */
  public void setNestedObjectsList(ObjectArrayList<AllSimpleTypesMessageInfo> value) {
    this.nestedObjectsList = value;
  }

  /**
   * @return true if Nested Objects List is not null
   */
  public boolean hasNestedObjectsList() {
    return nestedObjectsList != null;
  }

  /**
   */
  public void nullifyNestedObjectsList() {
    this.nestedObjectsList = null;
  }

  /**
   * Creates new instance of this class.
   * @return new instance of this class.
   */
  @Override
  protected AllListsMessage createInstance() {
    return new AllListsMessage();
  }

  /**
   * Method nullifies all instance properties
   */
  @Override
  public AllListsMessage nullify() {
    super.nullify();
    nullifyNestedBooleanList();
    nullifyNestedByteList();
    nullifyNestedShortList();
    nullifyNestedIntList();
    nullifyNestedLongList();
    nullifyNestedDecimalList();
    nullifyNestedDoubleList();
    nullifyNestedFloatList();
    nullifyNestedTextList();
    nullifyNestedAsciiTextList();
    nullifyNestedAlphanumericList();
    nullifyNestedObjectsList();
    return this;
  }

  /**
   * Resets all instance properties to their default values
   */
  @Override
  public AllListsMessage reset() {
    super.reset();
    nestedBooleanList = null;
    nestedByteList = null;
    nestedShortList = null;
    nestedIntList = null;
    nestedLongList = null;
    nestedDecimalList = null;
    nestedDoubleList = null;
    nestedFloatList = null;
    nestedTextList = null;
    nestedAsciiTextList = null;
    nestedAlphanumericList = null;
    nestedObjectsList = null;
    return this;
  }

  /**
   * Method copies state to a given instance
   */
  @Override
  public AllListsMessage clone() {
    AllListsMessage t = createInstance();
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
//    if (!(obj instanceof AllListsMessageInfo)) return false;
//    AllListsMessageInfo other =(AllListsMessageInfo)obj;
//    if (hasNestedBooleanList() != other.hasNestedBooleanList()) return false;
//    if (hasNestedBooleanList()) {
//      if (getNestedBooleanList().size() != other.getNestedBooleanList().size()) return false;
//      else for (int j = 0; j < getNestedBooleanList().size(); ++j) {
//        if (getNestedBooleanList().get(j) != other.getNestedBooleanList().get(j)) return false;
//      }
//    }
//    if (hasNestedByteList() != other.hasNestedByteList()) return false;
//    if (hasNestedByteList()) {
//      if (getNestedByteList().size() != other.getNestedByteList().size()) return false;
//      else for (int j = 0; j < getNestedByteList().size(); ++j) {
//        if (getNestedByteList().get(j) != other.getNestedByteList().get(j)) return false;
//      }
//    }
//    if (hasNestedShortList() != other.hasNestedShortList()) return false;
//    if (hasNestedShortList()) {
//      if (getNestedShortList().size() != other.getNestedShortList().size()) return false;
//      else for (int j = 0; j < getNestedShortList().size(); ++j) {
//        if (getNestedShortList().get(j) != other.getNestedShortList().get(j)) return false;
//      }
//    }
//    if (hasNestedIntList() != other.hasNestedIntList()) return false;
//    if (hasNestedIntList()) {
//      if (getNestedIntList().size() != other.getNestedIntList().size()) return false;
//      else for (int j = 0; j < getNestedIntList().size(); ++j) {
//        if (getNestedIntList().get(j) != other.getNestedIntList().get(j)) return false;
//      }
//    }
//    if (hasNestedLongList() != other.hasNestedLongList()) return false;
//    if (hasNestedLongList()) {
//      if (getNestedLongList().size() != other.getNestedLongList().size()) return false;
//      else for (int j = 0; j < getNestedLongList().size(); ++j) {
//        if (getNestedLongList().get(j) != other.getNestedLongList().get(j)) return false;
//      }
//    }
//    if (hasNestedDecimalList() != other.hasNestedDecimalList()) return false;
//    if (hasNestedDecimalList()) {
//      if (getNestedDecimalList().size() != other.getNestedDecimalList().size()) return false;
//      else for (int j = 0; j < getNestedDecimalList().size(); ++j) {
//        if ( !Decimal64Utils.equals((long)getNestedDecimalList().get(j), (long)other.getNestedDecimalList().get(j))) return false;
//      }
//    }
//    if (hasNestedDoubleList() != other.hasNestedDoubleList()) return false;
//    if (hasNestedDoubleList()) {
//      if (getNestedDoubleList().size() != other.getNestedDoubleList().size()) return false;
//      else for (int j = 0; j < getNestedDoubleList().size(); ++j) {
//        if (getNestedDoubleList().get(j) != other.getNestedDoubleList().get(j)) return false;
//      }
//    }
//    if (hasNestedFloatList() != other.hasNestedFloatList()) return false;
//    if (hasNestedFloatList()) {
//      if (getNestedFloatList().size() != other.getNestedFloatList().size()) return false;
//      else for (int j = 0; j < getNestedFloatList().size(); ++j) {
//      }
//    }
//    if (hasNestedTextList() != other.hasNestedTextList()) return false;
//    if (hasNestedTextList()) {
//      if (getNestedTextList().size() != other.getNestedTextList().size()) return false;
//      else for (int j = 0; j < getNestedTextList().size(); ++j) {
//        if ((getNestedTextList().get(j) != null) != (other.getNestedTextList().get(j) != null)) return false;
//        if (getNestedTextList().get(j) != null && getNestedTextList().get(j).length() != other.getNestedTextList().get(j).length()) return false; else {
//          CharSequence s1 = getNestedTextList().get(j);
//          CharSequence s2 = other.getNestedTextList().get(j);
//          if ((s1 instanceof MutableString && s2 instanceof MutableString) || (s1 instanceof String && s2 instanceof String) || (s1 instanceof BinaryAsciiString && s2 instanceof BinaryAsciiString)) {
//            if (!s1.equals(s2)) return false;
//          } else {
//            return CharSequenceUtils.equals(s1, s2);
//          }
//        }
//      }
//    }
//    if (hasNestedAsciiTextList() != other.hasNestedAsciiTextList()) return false;
//    if (hasNestedAsciiTextList()) {
//      if (getNestedAsciiTextList().size() != other.getNestedAsciiTextList().size()) return false;
//      else for (int j = 0; j < getNestedAsciiTextList().size(); ++j) {
//        if ((getNestedAsciiTextList().get(j) != null) != (other.getNestedAsciiTextList().get(j) != null)) return false;
//        if (getNestedAsciiTextList().get(j) != null && getNestedAsciiTextList().get(j).length() != other.getNestedAsciiTextList().get(j).length()) return false; else {
//          CharSequence s1 = getNestedAsciiTextList().get(j);
//          CharSequence s2 = other.getNestedAsciiTextList().get(j);
//          if ((s1 instanceof MutableString && s2 instanceof MutableString) || (s1 instanceof String && s2 instanceof String) || (s1 instanceof BinaryAsciiString && s2 instanceof BinaryAsciiString)) {
//            if (!s1.equals(s2)) return false;
//          } else {
//            return CharSequenceUtils.equals(s1, s2);
//          }
//        }
//      }
//    }
//    if (hasNestedAlphanumericList() != other.hasNestedAlphanumericList()) return false;
//    if (hasNestedAlphanumericList()) {
//      if (getNestedAlphanumericList().size() != other.getNestedAlphanumericList().size()) return false;
//      else for (int j = 0; j < getNestedAlphanumericList().size(); ++j) {
//        if (getNestedAlphanumericList().get(j) != other.getNestedAlphanumericList().get(j)) return false;
//      }
//    }
//    if (hasNestedObjectsList() != other.hasNestedObjectsList()) return false;
//    if (hasNestedObjectsList()) {
//      if (getNestedObjectsList().size() != other.getNestedObjectsList().size()) return false;
//      else for (int j = 0; j < getNestedObjectsList().size(); ++j) {
//        if ((getNestedObjectsList().get(j) != null) != (other.getNestedObjectsList().get(j) != null)) return false;
//        if (getNestedObjectsList().get(j) != null && !getNestedObjectsList().get(j).equals(other.getNestedObjectsList().get(j))) return false;
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
    if (hasNestedBooleanList()) {
      hash = hash * 31 + getNestedBooleanList().hashCode();
    }
    if (hasNestedByteList()) {
      hash = hash * 31 + getNestedByteList().hashCode();
    }
    if (hasNestedShortList()) {
      hash = hash * 31 + getNestedShortList().hashCode();
    }
    if (hasNestedIntList()) {
      hash = hash * 31 + getNestedIntList().hashCode();
    }
    if (hasNestedLongList()) {
      hash = hash * 31 + getNestedLongList().hashCode();
    }
    if (hasNestedDecimalList()) {
      hash = hash * 31 + getNestedDecimalList().hashCode();
    }
    if (hasNestedDoubleList()) {
      hash = hash * 31 + getNestedDoubleList().hashCode();
    }
    if (hasNestedTextList()) {
      for (int j = 0; j < getNestedTextList().size(); ++j) {
        hash ^= getNestedTextList().get(j).hashCode();
      }
    }
    if (hasNestedAsciiTextList()) {
      for (int j = 0; j < getNestedAsciiTextList().size(); ++j) {
        hash ^= getNestedAsciiTextList().get(j).hashCode();
      }
    }
    if (hasNestedAlphanumericList()) {
      hash = hash * 31 + getNestedAlphanumericList().hashCode();
    }
    if (hasNestedObjectsList()) {
      for (int j = 0; j < getNestedObjectsList().size(); ++j) {
        hash ^= getNestedObjectsList().get(j).hashCode();
      }
    }
    return hash;
  }

  /**
   * Method copies state to a given instance
   * @param template class instance that should be used as a copy source
   */
  @Override
  public AllListsMessage copyFrom(RecordInfo template) {
    super.copyFrom(template);
    if (template instanceof AllListsMessageInfo) {
      AllListsMessageInfo t = (AllListsMessageInfo)template;
      if (t.hasNestedBooleanList()) {
        if (!hasNestedBooleanList()) {
          setNestedBooleanList(new ByteArrayList(t.getNestedBooleanList().size()));
        } else {
          getNestedBooleanList().clear();
        }
        for (int i = 0; i < getNestedBooleanList().size(); ++i) ((ByteArrayList)getNestedBooleanList()).add(t.getNestedBooleanList().get(i));
      } else {
        nullifyNestedBooleanList();
      }
      if (t.hasNestedByteList()) {
        if (!hasNestedByteList()) {
          setNestedByteList(new ByteArrayList(t.getNestedByteList().size()));
        } else {
          getNestedByteList().clear();
        }
        for (int i = 0; i < getNestedByteList().size(); ++i) ((ByteArrayList)getNestedByteList()).add(t.getNestedByteList().get(i));
      } else {
        nullifyNestedByteList();
      }
      if (t.hasNestedShortList()) {
        if (!hasNestedShortList()) {
          setNestedShortList(new ShortArrayList(t.getNestedShortList().size()));
        } else {
          getNestedShortList().clear();
        }
        for (int i = 0; i < getNestedShortList().size(); ++i) ((ShortArrayList)getNestedShortList()).add(t.getNestedShortList().get(i));
      } else {
        nullifyNestedShortList();
      }
      if (t.hasNestedIntList()) {
        if (!hasNestedIntList()) {
          setNestedIntList(new IntegerArrayList(t.getNestedIntList().size()));
        } else {
          getNestedIntList().clear();
        }
        for (int i = 0; i < getNestedIntList().size(); ++i) ((IntegerArrayList)getNestedIntList()).add(t.getNestedIntList().get(i));
      } else {
        nullifyNestedIntList();
      }
      if (t.hasNestedLongList()) {
        if (!hasNestedLongList()) {
          setNestedLongList(new LongArrayList(t.getNestedLongList().size()));
        } else {
          getNestedLongList().clear();
        }
        for (int i = 0; i < getNestedLongList().size(); ++i) ((LongArrayList)getNestedLongList()).add(t.getNestedLongList().get(i));
      } else {
        nullifyNestedLongList();
      }
      if (t.hasNestedDecimalList()) {
        if (!hasNestedDecimalList()) {
          setNestedDecimalList(new LongArrayList(t.getNestedDecimalList().size()));
        } else {
          getNestedDecimalList().clear();
        }
        for (int i = 0; i < getNestedDecimalList().size(); ++i) ((LongArrayList)getNestedDecimalList()).add(t.getNestedDecimalList().get(i));
      } else {
        nullifyNestedDecimalList();
      }
      if (t.hasNestedDoubleList()) {
        if (!hasNestedDoubleList()) {
          setNestedDoubleList(new DoubleArrayList(t.getNestedDoubleList().size()));
        } else {
          getNestedDoubleList().clear();
        }
        for (int i = 0; i < getNestedDoubleList().size(); ++i) ((DoubleArrayList)getNestedDoubleList()).add(t.getNestedDoubleList().get(i));
      } else {
        nullifyNestedDoubleList();
      }
      if (t.hasNestedFloatList()) {
        if (!hasNestedFloatList()) {
          setNestedFloatList(new FloatArrayList(t.getNestedFloatList().size()));
        } else {
          getNestedFloatList().clear();
        }
        for (int i = 0; i < getNestedFloatList().size(); ++i) ((FloatArrayList)getNestedFloatList()).add(t.getNestedFloatList().get(i));
      } else {
        nullifyNestedFloatList();
      }
      if (t.hasNestedTextList()) {
        if (!hasNestedTextList()) {
          setNestedTextList(new ObjectArrayList<CharSequence>(t.getNestedTextList().size()));
        } else {
          getNestedTextList().clear();
        }
        for (int i = 0; i < getNestedTextList().size(); ++i) ((ObjectArrayList<CharSequence>)getNestedTextList()).add(t.getNestedTextList().get(i));
      } else {
        nullifyNestedTextList();
      }
      if (t.hasNestedAsciiTextList()) {
        if (!hasNestedAsciiTextList()) {
          setNestedAsciiTextList(new ObjectArrayList<CharSequence>(t.getNestedAsciiTextList().size()));
        } else {
          getNestedAsciiTextList().clear();
        }
        for (int i = 0; i < getNestedAsciiTextList().size(); ++i) ((ObjectArrayList<CharSequence>)getNestedAsciiTextList()).add(t.getNestedAsciiTextList().get(i));
      } else {
        nullifyNestedAsciiTextList();
      }
      if (t.hasNestedAlphanumericList()) {
        if (!hasNestedAlphanumericList()) {
          setNestedAlphanumericList(new LongArrayList(t.getNestedAlphanumericList().size()));
        } else {
          getNestedAlphanumericList().clear();
        }
        for (int i = 0; i < getNestedAlphanumericList().size(); ++i) ((LongArrayList)getNestedAlphanumericList()).add(t.getNestedAlphanumericList().get(i));
      } else {
        nullifyNestedAlphanumericList();
      }
      if (t.hasNestedObjectsList()) {
        if (!hasNestedObjectsList()) {
          setNestedObjectsList(new ObjectArrayList<AllSimpleTypesMessageInfo>(t.getNestedObjectsList().size()));
        } else {
          getNestedObjectsList().clear();
        }
        for (int i = 0; i < t.getNestedObjectsList().size(); ++i) ((ObjectArrayList<AllSimpleTypesMessageInfo>)getNestedObjectsList()).add((AllSimpleTypesMessageInfo)t.getNestedObjectsList().get(i).clone());
      } else {
        nullifyNestedObjectsList();
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
    str.append("{ \"$type\":  \"AllListsMessage\"");
    if (hasNestedBooleanList()) {
      str.append(", \"nestedBooleanList\": [");
      if (getNestedBooleanList().size() > 0) {
        str.append(getNestedBooleanList().get(0));
      }
      for (int i = 1; i < getNestedBooleanList().size(); ++i) {
        str.append(", ");
        str.append(getNestedBooleanList().get(i));
      }
      str.append("]");
    }
    if (hasNestedByteList()) {
      str.append(", \"nestedByteList\": [");
      if (getNestedByteList().size() > 0) {
        str.append(getNestedByteList().get(0));
      }
      for (int i = 1; i < getNestedByteList().size(); ++i) {
        str.append(", ");
        str.append(getNestedByteList().get(i));
      }
      str.append("]");
    }
    if (hasNestedShortList()) {
      str.append(", \"nestedShortList\": [");
      if (getNestedShortList().size() > 0) {
        str.append(getNestedShortList().get(0));
      }
      for (int i = 1; i < getNestedShortList().size(); ++i) {
        str.append(", ");
        str.append(getNestedShortList().get(i));
      }
      str.append("]");
    }
    if (hasNestedIntList()) {
      str.append(", \"nestedIntList\": [");
      if (getNestedIntList().size() > 0) {
        str.append(getNestedIntList().get(0));
      }
      for (int i = 1; i < getNestedIntList().size(); ++i) {
        str.append(", ");
        str.append(getNestedIntList().get(i));
      }
      str.append("]");
    }
    if (hasNestedLongList()) {
      str.append(", \"nestedLongList\": [");
      if (getNestedLongList().size() > 0) {
        str.append(getNestedLongList().get(0));
      }
      for (int i = 1; i < getNestedLongList().size(); ++i) {
        str.append(", ");
        str.append(getNestedLongList().get(i));
      }
      str.append("]");
    }
    if (hasNestedDecimalList()) {
      str.append(", \"nestedDecimalList\": [");
      if (getNestedDecimalList().size() > 0) {
        str.append(getNestedDecimalList().get(0));
      }
      for (int i = 1; i < getNestedDecimalList().size(); ++i) {
        str.append(", ");
        str.append(getNestedDecimalList().get(i));
      }
      str.append("]");
    }
    if (hasNestedDoubleList()) {
      str.append(", \"nestedDoubleList\": [");
      if (getNestedDoubleList().size() > 0) {
        str.append(getNestedDoubleList().get(0));
      }
      for (int i = 1; i < getNestedDoubleList().size(); ++i) {
        str.append(", ");
        str.append(getNestedDoubleList().get(i));
      }
      str.append("]");
    }
    if (hasNestedFloatList()) {
      str.append(", \"nestedFloatList\": [");
      if (getNestedFloatList().size() > 0) {
        str.append(getNestedFloatList().get(0));
      }
      for (int i = 1; i < getNestedFloatList().size(); ++i) {
        str.append(", ");
        str.append(getNestedFloatList().get(i));
      }
      str.append("]");
    }
    if (hasNestedTextList()) {
      str.append(", \"nestedTextList\": [");
      if (getNestedTextList().size() > 0) {
        str.append(getNestedTextList().get(0));
      }
      for (int i = 1; i < getNestedTextList().size(); ++i) {
        str.append(", ");
        str.append(getNestedTextList().get(i));
      }
      str.append("]");
    }
    if (hasNestedAsciiTextList()) {
      str.append(", \"nestedAsciiTextList\": [");
      if (getNestedAsciiTextList().size() > 0) {
        str.append(getNestedAsciiTextList().get(0));
      }
      for (int i = 1; i < getNestedAsciiTextList().size(); ++i) {
        str.append(", ");
        str.append(getNestedAsciiTextList().get(i));
      }
      str.append("]");
    }
    if (hasNestedAlphanumericList()) {
      str.append(", \"nestedAlphanumericList\": [");
      if (getNestedAlphanumericList().size() > 0) {
        str.append(getNestedAlphanumericList().get(0));
      }
      for (int i = 1; i < getNestedAlphanumericList().size(); ++i) {
        str.append(", ");
        str.append(getNestedAlphanumericList().get(i));
      }
      str.append("]");
    }
    if (hasNestedObjectsList()) {
      str.append(", \"nestedObjectsList\": [");
      if (getNestedObjectsList().size() > 0) {
        if (getNestedObjectsList().get(0) == null) {
          str.append("null");
        } else {
          getNestedObjectsList().get(0).toString(str);
        }
      }
      for (int i = 1; i < getNestedObjectsList().size(); ++i) {
        str.append(", ");
        if (getNestedObjectsList().get(i) == null) {
          str.append("null");
        } else {
          getNestedObjectsList().get(i).toString(str);
        }
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
