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
public class MACDMessage extends InstrumentMessage implements MACDMessageInterface {
  public static final String CLASS_NAME = MACDMessage.class.getName();

  /**
   */
  protected double histogram = TypeConstants.IEEE64_NULL;

  /**
   */
  protected double value = TypeConstants.IEEE64_NULL;

  /**
   */
  protected double signal = TypeConstants.IEEE64_NULL;

  /**
   * @return Histogram
   */
  @SchemaElement
  public double getHistogram() {
    return histogram;
  }

  /**
   * @param value - Histogram
   */
  public void setHistogram(double value) {
    this.histogram = value;
  }

  /**
   * @return true if Histogram is not null
   */
  public boolean hasHistogram() {
    return !Double.isNaN(histogram);
  }

  /**
   */
  public void nullifyHistogram() {
    this.histogram = TypeConstants.IEEE64_NULL;
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
   * @return Signal
   */
  @SchemaElement
  public double getSignal() {
    return signal;
  }

  /**
   * @param value - Signal
   */
  public void setSignal(double value) {
    this.signal = value;
  }

  /**
   * @return true if Signal is not null
   */
  public boolean hasSignal() {
    return !Double.isNaN(signal);
  }

  /**
   */
  public void nullifySignal() {
    this.signal = TypeConstants.IEEE64_NULL;
  }

  /**
   * Creates new instance of this class.
   * @return new instance of this class.
   */
  @Override
  protected MACDMessage createInstance() {
    return new MACDMessage();
  }

  /**
   * Method nullifies all instance properties
   */
  @Override
  public MACDMessage nullify() {
    super.nullify();
    nullifyHistogram();
    nullifyValue();
    nullifySignal();
    return this;
  }

  /**
   * Resets all instance properties to their default values
   */
  @Override
  public MACDMessage reset() {
    super.reset();
    histogram = TypeConstants.IEEE64_NULL;
    value = TypeConstants.IEEE64_NULL;
    signal = TypeConstants.IEEE64_NULL;
    return this;
  }

  /**
   * Method copies state to a given instance
   */
  @Override
  public MACDMessage clone() {
    MACDMessage t = createInstance();
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
    if (!(obj instanceof MACDMessageInfo)) return false;
    MACDMessageInfo other =(MACDMessageInfo)obj;
    if (hasHistogram() != other.hasHistogram()) return false;
    if (hasHistogram() && getHistogram() != other.getHistogram()) return false;
    if (hasValue() != other.hasValue()) return false;
    if (hasValue() && getValue() != other.getValue()) return false;
    if (hasSignal() != other.hasSignal()) return false;
    if (hasSignal() && getSignal() != other.getSignal()) return false;
    return true;
  }

  /**
   * Returns a hash code value for the object. This method is * supported for the benefit of hash tables such as those provided by.
   */
  @Override
  public int hashCode() {
    int hash = super.hashCode();
    if (hasHistogram()) {
      hash = hash * 31 + ((int)(Double.doubleToLongBits(getHistogram()) ^ (Double.doubleToLongBits(getHistogram()) >>> 32)));
    }
    if (hasValue()) {
      hash = hash * 31 + ((int)(Double.doubleToLongBits(getValue()) ^ (Double.doubleToLongBits(getValue()) >>> 32)));
    }
    if (hasSignal()) {
      hash = hash * 31 + ((int)(Double.doubleToLongBits(getSignal()) ^ (Double.doubleToLongBits(getSignal()) >>> 32)));
    }
    return hash;
  }

  /**
   * Method copies state to a given instance
   * @param template class instance that should be used as a copy source
   */
  @Override
  public MACDMessage copyFrom(RecordInfo template) {
    super.copyFrom(template);
    if (template instanceof MACDMessageInfo) {
      MACDMessageInfo t = (MACDMessageInfo)template;
      if (t.hasHistogram()) {
        setHistogram(t.getHistogram());
      } else {
        nullifyHistogram();
      }
      if (t.hasValue()) {
        setValue(t.getValue());
      } else {
        nullifyValue();
      }
      if (t.hasSignal()) {
        setSignal(t.getSignal());
      } else {
        nullifySignal();
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
    str.append("{ \"$type\":  \"MACDMessage\"");
    if (hasHistogram()) {
      str.append(", \"histogram\": ").append(getHistogram());
    }
    if (hasValue()) {
      str.append(", \"value\": ").append(getValue());
    }
    if (hasSignal()) {
      str.append(", \"signal\": ").append(getSignal());
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
