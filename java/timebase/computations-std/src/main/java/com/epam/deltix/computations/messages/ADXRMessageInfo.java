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
import java.lang.Override;

/**
 */
public interface ADXRMessageInfo extends MessageInterface {
  /**
   * @return Adxr
   */
  double getAdxr();

  /**
   * @return true if Adxr is not null
   */
  boolean hasAdxr();

  /**
   * @return Adx
   */
  double getAdx();

  /**
   * @return true if Adx is not null
   */
  boolean hasAdx();

  /**
   * @return Dx
   */
  double getDx();

  /**
   * @return true if Dx is not null
   */
  boolean hasDx();

  /**
   * @return Plus DI
   */
  double getPlusDI();

  /**
   * @return true if Plus DI is not null
   */
  boolean hasPlusDI();

  /**
   * @return Minus DI
   */
  double getMinusDI();

  /**
   * @return true if Minus DI is not null
   */
  boolean hasMinusDI();

  /**
   * Method copies state to a given instance
   */
  @Override
  ADXRMessageInfo clone();
}
