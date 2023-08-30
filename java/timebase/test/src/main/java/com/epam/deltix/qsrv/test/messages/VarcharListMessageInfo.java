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

import com.epam.deltix.timebase.messages.MessageInfo;
import com.epam.deltix.util.collections.generated.LongList;
import com.epam.deltix.util.collections.generated.ObjectList;

/**
 */
public interface VarcharListMessageInfo extends MessageInfo {
  /**
   * @return Alphanumeric List
   */
  LongList getAlphanumericList();

  /**
   * @return true if Alphanumeric List is not null
   */
  boolean hasAlphanumericList();

  /**
   * @return Alphanumeric Nullable List
   */
  LongList getAlphanumericNullableList();

  /**
   * @return true if Alphanumeric Nullable List is not null
   */
  boolean hasAlphanumericNullableList();

  /**
   * @return Char Sequence List
   */
  ObjectList<CharSequence> getCharSequenceList();

  /**
   * @return true if Char Sequence List is not null
   */
  boolean hasCharSequenceList();

  /**
   * @return Char Sequence Nullable List
   */
  ObjectList<CharSequence> getCharSequenceNullableList();

  /**
   * @return true if Char Sequence Nullable List is not null
   */
  boolean hasCharSequenceNullableList();

  /**
   * Method copies state to a given instance
   */
  @Override
  VarcharListMessageInfo clone();
}
