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

import com.epam.deltix.timebase.messages.MessageInterface;
import com.epam.deltix.timebase.messages.RecordInfo;
import com.epam.deltix.util.collections.generated.LongArrayList;
import com.epam.deltix.util.collections.generated.ObjectArrayList;

/**
 */
public interface VarcharListMessageInterface extends VarcharListMessageInfo, MessageInterface {
  /**
   * @param value - Alphanumeric List
   */
  void setAlphanumericList(LongArrayList value);

  /**
   */
  void nullifyAlphanumericList();

  /**
   * @param value - Alphanumeric Nullable List
   */
  void setAlphanumericNullableList(LongArrayList value);

  /**
   */
  void nullifyAlphanumericNullableList();

  /**
   * @param value - Char Sequence List
   */
  void setCharSequenceList(ObjectArrayList<CharSequence> value);

  /**
   */
  void nullifyCharSequenceList();

  /**
   * @param value - Char Sequence Nullable List
   */
  void setCharSequenceNullableList(ObjectArrayList<CharSequence> value);

  /**
   */
  void nullifyCharSequenceNullableList();

  /**
   * Method nullifies all instance properties
   */
  @Override
  VarcharListMessageInterface nullify();

  /**
   * Resets all instance properties to their default values
   */
  @Override
  VarcharListMessageInterface reset();

  @Override
  VarcharListMessageInterface copyFrom(RecordInfo template);
}
