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

/**
 */
public enum TestEnum {
  /**
   */
  ZERO(0),

  /**
   */
  ONE(1),

  /**
   */
  TWO(2),

  /**
   */
  THREE(3),

  /**
   */
  FOUR(4),

  /**
   */
  FIVE(5);

  private final int value;

  TestEnum(int value) {
    this.value = value;
  }

  public int getNumber() {
    return this.value;
  }

  public static TestEnum valueOf(int number) {
    switch (number) {
      case 0: return ZERO;
      case 1: return ONE;
      case 2: return TWO;
      case 3: return THREE;
      case 4: return FOUR;
      case 5: return FIVE;
      default: return null;
    }
  }

  public static TestEnum strictValueOf(int number) {
    final TestEnum value = valueOf(number);
    if (value == null) {
      throw new IllegalArgumentException("Enumeration 'TestEnum' does not have value corresponding to '" + number + "'.");
    }
    return value;
  }
}
