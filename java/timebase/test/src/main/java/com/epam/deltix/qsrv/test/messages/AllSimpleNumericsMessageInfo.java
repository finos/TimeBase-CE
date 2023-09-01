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

import com.epam.deltix.dfp.Decimal;
import com.epam.deltix.timebase.messages.MessageInfo;

/**
 */
public interface AllSimpleNumericsMessageInfo extends MessageInfo {
  /**
   * @return Byte Field
   */
  byte getByteField();

  /**
   * @return true if Byte Field is not null
   */
  boolean hasByteField();

  /**
   * @return Byte Nullable Field
   */
  byte getByteNullableField();

  /**
   * @return true if Byte Nullable Field is not null
   */
  boolean hasByteNullableField();

  /**
   * @return Short Field
   */
  short getShortField();

  /**
   * @return true if Short Field is not null
   */
  boolean hasShortField();

  /**
   * @return Short Nullable Field
   */
  short getShortNullableField();

  /**
   * @return true if Short Nullable Field is not null
   */
  boolean hasShortNullableField();

  /**
   * @return Int Field
   */
  int getIntField();

  /**
   * @return true if Int Field is not null
   */
  boolean hasIntField();

  /**
   * @return Int Nullable Field
   */
  int getIntNullableField();

  /**
   * @return true if Int Nullable Field is not null
   */
  boolean hasIntNullableField();

  /**
   * @return Long Field
   */
  long getLongField();

  /**
   * @return true if Long Field is not null
   */
  boolean hasLongField();

  /**
   * @return Long Nullable Field
   */
  long getLongNullableField();

  /**
   * @return true if Long Nullable Field is not null
   */
  boolean hasLongNullableField();

  /**
   * @return Float Field
   */
  float getFloatField();

  /**
   * @return true if Float Field is not null
   */
  boolean hasFloatField();

  /**
   * @return Float Nullable Field
   */
  float getFloatNullableField();

  /**
   * @return true if Float Nullable Field is not null
   */
  boolean hasFloatNullableField();

  /**
   * @return Double Field
   */
  double getDoubleField();

  /**
   * @return true if Double Field is not null
   */
  boolean hasDoubleField();

  /**
   * @return Double Nullable Field
   */
  double getDoubleNullableField();

  /**
   * @return true if Double Nullable Field is not null
   */
  boolean hasDoubleNullableField();

  /**
   * @return Decimal Field
   */
  @Decimal
  long getDecimalField();

  /**
   * @return true if Decimal Field is not null
   */
  boolean hasDecimalField();

  /**
   * @return Decimal Nullable Field
   */
  @Decimal
  long getDecimalNullableField();

  /**
   * @return true if Decimal Nullable Field is not null
   */
  boolean hasDecimalNullableField();

  /**
   * Method copies state to a given instance
   */
  @Override
  AllSimpleNumericsMessageInfo clone();
}
