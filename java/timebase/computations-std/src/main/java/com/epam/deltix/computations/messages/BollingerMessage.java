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
public class BollingerMessage extends InstrumentMessage implements BollingerMessageInterface {
  public static final String CLASS_NAME = BollingerMessage.class.getName();

  /**
   */
  protected double upperBand = TypeConstants.IEEE64_NULL;

  /**
   */
  protected double lowerBand = TypeConstants.IEEE64_NULL;

  /**
   */
  protected double middleBand = TypeConstants.IEEE64_NULL;

  /**
   */
  protected double bandWidth = TypeConstants.IEEE64_NULL;

  /**
   */
  protected double percentB = TypeConstants.IEEE64_NULL;

  /**
   * @return Upper Band
   */
  @SchemaElement
  public double getUpperBand() {
    return upperBand;
  }

  /**
   * @param value - Upper Band
   */
  public void setUpperBand(double value) {
    this.upperBand = value;
  }

  /**
   * @return true if Upper Band is not null
   */
  public boolean hasUpperBand() {
    return !Double.isNaN(upperBand);
  }

  /**
   */
  public void nullifyUpperBand() {
    this.upperBand = TypeConstants.IEEE64_NULL;
  }

  /**
   * @return Lower Band
   */
  @SchemaElement
  public double getLowerBand() {
    return lowerBand;
  }

  /**
   * @param value - Lower Band
   */
  public void setLowerBand(double value) {
    this.lowerBand = value;
  }

  /**
   * @return true if Lower Band is not null
   */
  public boolean hasLowerBand() {
    return !Double.isNaN(lowerBand);
  }

  /**
   */
  public void nullifyLowerBand() {
    this.lowerBand = TypeConstants.IEEE64_NULL;
  }

  /**
   * @return Middle Band
   */
  @SchemaElement
  public double getMiddleBand() {
    return middleBand;
  }

  /**
   * @param value - Middle Band
   */
  public void setMiddleBand(double value) {
    this.middleBand = value;
  }

  /**
   * @return true if Middle Band is not null
   */
  public boolean hasMiddleBand() {
    return !Double.isNaN(middleBand);
  }

  /**
   */
  public void nullifyMiddleBand() {
    this.middleBand = TypeConstants.IEEE64_NULL;
  }

  /**
   * @return Band Width
   */
  @SchemaElement
  public double getBandWidth() {
    return bandWidth;
  }

  /**
   * @param value - Band Width
   */
  public void setBandWidth(double value) {
    this.bandWidth = value;
  }

  /**
   * @return true if Band Width is not null
   */
  public boolean hasBandWidth() {
    return !Double.isNaN(bandWidth);
  }

  /**
   */
  public void nullifyBandWidth() {
    this.bandWidth = TypeConstants.IEEE64_NULL;
  }

  /**
   * @return Percent B
   */
  @SchemaElement
  public double getPercentB() {
    return percentB;
  }

  /**
   * @param value - Percent B
   */
  public void setPercentB(double value) {
    this.percentB = value;
  }

  /**
   * @return true if Percent B is not null
   */
  public boolean hasPercentB() {
    return !Double.isNaN(percentB);
  }

  /**
   */
  public void nullifyPercentB() {
    this.percentB = TypeConstants.IEEE64_NULL;
  }

  /**
   * Creates new instance of this class.
   * @return new instance of this class.
   */
  @Override
  protected BollingerMessage createInstance() {
    return new BollingerMessage();
  }

  /**
   * Method nullifies all instance properties
   */
  @Override
  public BollingerMessage nullify() {
    super.nullify();
    nullifyUpperBand();
    nullifyLowerBand();
    nullifyMiddleBand();
    nullifyBandWidth();
    nullifyPercentB();
    return this;
  }

  /**
   * Resets all instance properties to their default values
   */
  @Override
  public BollingerMessage reset() {
    super.reset();
    upperBand = TypeConstants.IEEE64_NULL;
    lowerBand = TypeConstants.IEEE64_NULL;
    middleBand = TypeConstants.IEEE64_NULL;
    bandWidth = TypeConstants.IEEE64_NULL;
    percentB = TypeConstants.IEEE64_NULL;
    return this;
  }

  /**
   * Method copies state to a given instance
   */
  @Override
  public BollingerMessage clone() {
    BollingerMessage t = createInstance();
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
    if (!(obj instanceof BollingerMessageInfo)) return false;
    BollingerMessageInfo other =(BollingerMessageInfo)obj;
    if (hasUpperBand() != other.hasUpperBand()) return false;
    if (hasUpperBand() && getUpperBand() != other.getUpperBand()) return false;
    if (hasLowerBand() != other.hasLowerBand()) return false;
    if (hasLowerBand() && getLowerBand() != other.getLowerBand()) return false;
    if (hasMiddleBand() != other.hasMiddleBand()) return false;
    if (hasMiddleBand() && getMiddleBand() != other.getMiddleBand()) return false;
    if (hasBandWidth() != other.hasBandWidth()) return false;
    if (hasBandWidth() && getBandWidth() != other.getBandWidth()) return false;
    if (hasPercentB() != other.hasPercentB()) return false;
    if (hasPercentB() && getPercentB() != other.getPercentB()) return false;
    return true;
  }

  /**
   * Returns a hash code value for the object. This method is * supported for the benefit of hash tables such as those provided by.
   */
  @Override
  public int hashCode() {
    int hash = super.hashCode();
    if (hasUpperBand()) {
      hash = hash * 31 + ((int)(Double.doubleToLongBits(getUpperBand()) ^ (Double.doubleToLongBits(getUpperBand()) >>> 32)));
    }
    if (hasLowerBand()) {
      hash = hash * 31 + ((int)(Double.doubleToLongBits(getLowerBand()) ^ (Double.doubleToLongBits(getLowerBand()) >>> 32)));
    }
    if (hasMiddleBand()) {
      hash = hash * 31 + ((int)(Double.doubleToLongBits(getMiddleBand()) ^ (Double.doubleToLongBits(getMiddleBand()) >>> 32)));
    }
    if (hasBandWidth()) {
      hash = hash * 31 + ((int)(Double.doubleToLongBits(getBandWidth()) ^ (Double.doubleToLongBits(getBandWidth()) >>> 32)));
    }
    if (hasPercentB()) {
      hash = hash * 31 + ((int)(Double.doubleToLongBits(getPercentB()) ^ (Double.doubleToLongBits(getPercentB()) >>> 32)));
    }
    return hash;
  }

  /**
   * Method copies state to a given instance
   * @param template class instance that should be used as a copy source
   */
  @Override
  public BollingerMessage copyFrom(RecordInfo template) {
    super.copyFrom(template);
    if (template instanceof BollingerMessageInfo) {
      BollingerMessageInfo t = (BollingerMessageInfo)template;
      if (t.hasUpperBand()) {
        setUpperBand(t.getUpperBand());
      } else {
        nullifyUpperBand();
      }
      if (t.hasLowerBand()) {
        setLowerBand(t.getLowerBand());
      } else {
        nullifyLowerBand();
      }
      if (t.hasMiddleBand()) {
        setMiddleBand(t.getMiddleBand());
      } else {
        nullifyMiddleBand();
      }
      if (t.hasBandWidth()) {
        setBandWidth(t.getBandWidth());
      } else {
        nullifyBandWidth();
      }
      if (t.hasPercentB()) {
        setPercentB(t.getPercentB());
      } else {
        nullifyPercentB();
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
    str.append("{ \"$type\":  \"BollingerMessage\"");
    if (hasUpperBand()) {
      str.append(", \"upperBand\": ").append(getUpperBand());
    }
    if (hasLowerBand()) {
      str.append(", \"lowerBand\": ").append(getLowerBand());
    }
    if (hasMiddleBand()) {
      str.append(", \"middleBand\": ").append(getMiddleBand());
    }
    if (hasBandWidth()) {
      str.append(", \"bandWidth\": ").append(getBandWidth());
    }
    if (hasPercentB()) {
      str.append(", \"percentB\": ").append(getPercentB());
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
