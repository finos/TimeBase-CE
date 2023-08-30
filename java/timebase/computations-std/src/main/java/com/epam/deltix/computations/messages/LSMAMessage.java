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

import com.epam.deltix.timebase.messages.InstrumentMessage;
import com.epam.deltix.timebase.messages.RecordInfo;
import com.epam.deltix.timebase.messages.SchemaElement;
import com.epam.deltix.timebase.messages.TypeConstants;

import java.lang.Double;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.lang.StringBuilder;

/**
 */
public class LSMAMessage extends InstrumentMessage implements LSMAMessageInterface {
  public static final String CLASS_NAME = LSMAMessage.class.getName();

  /**
   */
  protected double slope = TypeConstants.IEEE64_NULL;

  /**
   */
  protected double rSquared = TypeConstants.IEEE64_NULL;

  /**
   */
  protected double value = TypeConstants.IEEE64_NULL;

  /**
   * @return Slope
   */
  @SchemaElement
  public double getSlope() {
    return slope;
  }

  /**
   * @param value - Slope
   */
  public void setSlope(double value) {
    this.slope = value;
  }

  /**
   * @return true if Slope is not null
   */
  public boolean hasSlope() {
    return !Double.isNaN(slope);
  }

  /**
   */
  public void nullifySlope() {
    this.slope = TypeConstants.IEEE64_NULL;
  }

  /**
   * @return RSquared
   */
  @SchemaElement
  public double getRSquared() {
    return rSquared;
  }

  /**
   * @param value - RSquared
   */
  public void setRSquared(double value) {
    this.rSquared = value;
  }

  /**
   * @return true if RSquared is not null
   */
  public boolean hasRSquared() {
    return !Double.isNaN(rSquared);
  }

  /**
   */
  public void nullifyRSquared() {
    this.rSquared = TypeConstants.IEEE64_NULL;
  }

  /**
   * @return Value
   */
  @SchemaElement
  public double getValue() {
    return value;
  }

  /**
   * @param value - Value
   */
  public void setValue(double value) {
    this.value = value;
  }

  /**
   * @return true if Value is not null
   */
  public boolean hasValue() {
    return !Double.isNaN(value);
  }

  /**
   */
  public void nullifyValue() {
    this.value = TypeConstants.IEEE64_NULL;
  }

  /**
   * Creates new instance of this class.
   * @return new instance of this class.
   */
  @Override
  protected LSMAMessage createInstance() {
    return new LSMAMessage();
  }

  /**
   * Method nullifies all instance properties
   */
  @Override
  public LSMAMessage nullify() {
    super.nullify();
    nullifySlope();
    nullifyRSquared();
    nullifyValue();
    return this;
  }

  /**
   * Resets all instance properties to their default values
   */
  @Override
  public LSMAMessage reset() {
    super.reset();
    slope = TypeConstants.IEEE64_NULL;
    rSquared = TypeConstants.IEEE64_NULL;
    value = TypeConstants.IEEE64_NULL;
    return this;
  }

  /**
   * Method copies state to a given instance
   */
  @Override
  public LSMAMessage clone() {
    LSMAMessage t = createInstance();
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
    if (!(obj instanceof LSMAMessageInfo)) return false;
    LSMAMessageInfo other =(LSMAMessageInfo)obj;
    if (hasSlope() != other.hasSlope()) return false;
    if (hasSlope() && getSlope() != other.getSlope()) return false;
    if (hasRSquared() != other.hasRSquared()) return false;
    if (hasRSquared() && getRSquared() != other.getRSquared()) return false;
    if (hasValue() != other.hasValue()) return false;
    if (hasValue() && getValue() != other.getValue()) return false;
    return true;
  }

  /**
   * Returns a hash code value for the object. This method is * supported for the benefit of hash tables such as those provided by.
   */
  @Override
  public int hashCode() {
    int hash = super.hashCode();
    if (hasSlope()) {
      hash = hash * 31 + ((int)(Double.doubleToLongBits(getSlope()) ^ (Double.doubleToLongBits(getSlope()) >>> 32)));
    }
    if (hasRSquared()) {
      hash = hash * 31 + ((int)(Double.doubleToLongBits(getRSquared()) ^ (Double.doubleToLongBits(getRSquared()) >>> 32)));
    }
    if (hasValue()) {
      hash = hash * 31 + ((int)(Double.doubleToLongBits(getValue()) ^ (Double.doubleToLongBits(getValue()) >>> 32)));
    }
    return hash;
  }

  /**
   * Method copies state to a given instance
   * @param template class instance that should be used as a copy source
   */
  @Override
  public LSMAMessage copyFrom(RecordInfo template) {
    super.copyFrom(template);
    if (template instanceof LSMAMessageInfo) {
      LSMAMessageInfo t = (LSMAMessageInfo)template;
      if (t.hasSlope()) {
        setSlope(t.getSlope());
      } else {
        nullifySlope();
      }
      if (t.hasRSquared()) {
        setRSquared(t.getRSquared());
      } else {
        nullifyRSquared();
      }
      if (t.hasValue()) {
        setValue(t.getValue());
      } else {
        nullifyValue();
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
    str.append("{ \"$type\":  \"LSMAMessage\"");
    if (hasSlope()) {
      str.append(", \"slope\": ").append(getSlope());
    }
    if (hasRSquared()) {
      str.append(", \"rSquared\": ").append(getRSquared());
    }
    if (hasValue()) {
      str.append(", \"value\": ").append(getValue());
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
