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
public interface BollingerMessageInfo extends MessageInterface {
  /**
   * @return Upper Band
   */
  double getUpperBand();

  /**
   * @return true if Upper Band is not null
   */
  boolean hasUpperBand();

  /**
   * @return Lower Band
   */
  double getLowerBand();

  /**
   * @return true if Lower Band is not null
   */
  boolean hasLowerBand();

  /**
   * @return Middle Band
   */
  double getMiddleBand();

  /**
   * @return true if Middle Band is not null
   */
  boolean hasMiddleBand();

  /**
   * @return Band Width
   */
  double getBandWidth();

  /**
   * @return true if Band Width is not null
   */
  boolean hasBandWidth();

  /**
   * @return Percent B
   */
  double getPercentB();

  /**
   * @return true if Percent B is not null
   */
  boolean hasPercentB();

  /**
   * Method copies state to a given instance
   */
  @Override
  BollingerMessageInfo clone();
}
