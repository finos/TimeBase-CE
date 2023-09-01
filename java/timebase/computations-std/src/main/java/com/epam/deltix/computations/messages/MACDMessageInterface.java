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

package com.epam.deltix.computations.messages;

import com.epam.deltix.timebase.messages.MessageInterface;
import com.epam.deltix.timebase.messages.RecordInfo;
import java.lang.Override;

/**
 */
public interface MACDMessageInterface extends MACDMessageInfo, MessageInterface {
  /**
   * @param value - Histogram
   */
  void setHistogram(double value);

  /**
   */
  void nullifyHistogram();

  /**
   * @param value - Value
   */
  void setValue(double value);

  /**
   */
  void nullifyValue();

  /**
   * @param value - Signal
   */
  void setSignal(double value);

  /**
   */
  void nullifySignal();

  /**
   * Method nullifies all instance properties
   */
  @Override
  MACDMessageInterface nullify();

  /**
   * Resets all instance properties to their default values
   */
  @Override
  MACDMessageInterface reset();

  @Override
  MACDMessageInterface copyFrom(RecordInfo template);
}
