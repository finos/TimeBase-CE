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
import com.epam.deltix.timebase.messages.MessageInfo;
import com.epam.deltix.util.collections.generated.*;

/**
 */
public interface AllListsMessageInfo extends MessageInfo {
  /**
   * @return Nested Boolean List
   */
  ByteList getNestedBooleanList();

  /**
   * @return true if Nested Boolean List is not null
   */
  boolean hasNestedBooleanList();

  /**
   * @return Nested Byte List
   */
  ByteList getNestedByteList();

  /**
   * @return true if Nested Byte List is not null
   */
  boolean hasNestedByteList();

  /**
   * @return Nested Short List
   */
  ShortList getNestedShortList();

  /**
   * @return true if Nested Short List is not null
   */
  boolean hasNestedShortList();

  /**
   * @return Nested Int List
   */
  IntegerList getNestedIntList();

  /**
   * @return true if Nested Int List is not null
   */
  boolean hasNestedIntList();

  /**
   * @return Nested Long List
   */
  LongList getNestedLongList();

  /**
   * @return true if Nested Long List is not null
   */
  boolean hasNestedLongList();

  /**
   * @return Nested Decimal List
   */
  @Decimal
  LongList getNestedDecimalList();

  /**
   * @return true if Nested Decimal List is not null
   */
  boolean hasNestedDecimalList();

  /**
   * @return Nested Double List
   */
  DoubleList getNestedDoubleList();

  /**
   * @return true if Nested Double List is not null
   */
  boolean hasNestedDoubleList();

  /**
   * @return Nested Float List
   */
  FloatList getNestedFloatList();

  /**
   * @return true if Nested Float List is not null
   */
  boolean hasNestedFloatList();

  /**
   * @return Nested Text List
   */
  ObjectList<CharSequence> getNestedTextList();

  /**
   * @return true if Nested Text List is not null
   */
  boolean hasNestedTextList();

  /**
   * @return Nested Ascii Text List
   */
  ObjectList<CharSequence> getNestedAsciiTextList();

  /**
   * @return true if Nested Ascii Text List is not null
   */
  boolean hasNestedAsciiTextList();

  /**
   * @return Nested Alphanumeric List
   */
  LongList getNestedAlphanumericList();

  /**
   * @return true if Nested Alphanumeric List is not null
   */
  boolean hasNestedAlphanumericList();

  /**
   * @return Nested Objects List
   */
  ObjectList<AllSimpleTypesMessageInfo> getNestedObjectsList();

  /**
   * @return true if Nested Objects List is not null
   */
  boolean hasNestedObjectsList();

  /**
   * Method copies state to a given instance
   */
  @Override
  AllListsMessageInfo clone();
}
