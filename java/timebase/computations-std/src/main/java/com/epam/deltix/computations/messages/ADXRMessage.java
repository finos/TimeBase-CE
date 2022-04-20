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
public class ADXRMessage extends InstrumentMessage implements ADXRMessageInterface {
  public static final String CLASS_NAME = ADXRMessage.class.getName();

  /**
   */
  protected double adxr = TypeConstants.IEEE64_NULL;

  /**
   */
  protected double adx = TypeConstants.IEEE64_NULL;

  /**
   */
  protected double dx = TypeConstants.IEEE64_NULL;

  /**
   */
  protected double plusDI = TypeConstants.IEEE64_NULL;

  /**
   */
  protected double minusDI = TypeConstants.IEEE64_NULL;

  /**
   * @return Adxr
   */
  @SchemaElement
  public double getAdxr() {
    return adxr;
  }

  /**
   * @param value - Adxr
   */
  public void setAdxr(double value) {
    this.adxr = value;
  }

  /**
   * @return true if Adxr is not null
   */
  public boolean hasAdxr() {
    return !Double.isNaN(adxr);
  }

  /**
   */
  public void nullifyAdxr() {
    this.adxr = TypeConstants.IEEE64_NULL;
  }

  /**
   * @return Adx
   */
  @SchemaElement
  public double getAdx() {
    return adx;
  }

  /**
   * @param value - Adx
   */
  public void setAdx(double value) {
    this.adx = value;
  }

  /**
   * @return true if Adx is not null
   */
  public boolean hasAdx() {
    return !Double.isNaN(adx);
  }

  /**
   */
  public void nullifyAdx() {
    this.adx = TypeConstants.IEEE64_NULL;
  }

  /**
   * @return Dx
   */
  @SchemaElement
  public double getDx() {
    return dx;
  }

  /**
   * @param value - Dx
   */
  public void setDx(double value) {
    this.dx = value;
  }

  /**
   * @return true if Dx is not null
   */
  public boolean hasDx() {
    return !Double.isNaN(dx);
  }

  /**
   */
  public void nullifyDx() {
    this.dx = TypeConstants.IEEE64_NULL;
  }

  /**
   * @return Plus DI
   */
  @SchemaElement
  public double getPlusDI() {
    return plusDI;
  }

  /**
   * @param value - Plus DI
   */
  public void setPlusDI(double value) {
    this.plusDI = value;
  }

  /**
   * @return true if Plus DI is not null
   */
  public boolean hasPlusDI() {
    return !Double.isNaN(plusDI);
  }

  /**
   */
  public void nullifyPlusDI() {
    this.plusDI = TypeConstants.IEEE64_NULL;
  }

  /**
   * @return Minus DI
   */
  @SchemaElement
  public double getMinusDI() {
    return minusDI;
  }

  /**
   * @param value - Minus DI
   */
  public void setMinusDI(double value) {
    this.minusDI = value;
  }

  /**
   * @return true if Minus DI is not null
   */
  public boolean hasMinusDI() {
    return !Double.isNaN(minusDI);
  }

  /**
   */
  public void nullifyMinusDI() {
    this.minusDI = TypeConstants.IEEE64_NULL;
  }

  /**
   * Creates new instance of this class.
   * @return new instance of this class.
   */
  @Override
  protected ADXRMessage createInstance() {
    return new ADXRMessage();
  }

  /**
   * Method nullifies all instance properties
   */
  @Override
  public ADXRMessage nullify() {
    super.nullify();
    nullifyAdxr();
    nullifyAdx();
    nullifyDx();
    nullifyPlusDI();
    nullifyMinusDI();
    return this;
  }

  /**
   * Resets all instance properties to their default values
   */
  @Override
  public ADXRMessage reset() {
    super.reset();
    adxr = TypeConstants.IEEE64_NULL;
    adx = TypeConstants.IEEE64_NULL;
    dx = TypeConstants.IEEE64_NULL;
    plusDI = TypeConstants.IEEE64_NULL;
    minusDI = TypeConstants.IEEE64_NULL;
    return this;
  }

  /**
   * Method copies state to a given instance
   */
  @Override
  public ADXRMessage clone() {
    ADXRMessage t = createInstance();
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
    if (!(obj instanceof ADXRMessageInfo)) return false;
    ADXRMessageInfo other =(ADXRMessageInfo)obj;
    if (hasAdxr() != other.hasAdxr()) return false;
    if (hasAdxr() && getAdxr() != other.getAdxr()) return false;
    if (hasAdx() != other.hasAdx()) return false;
    if (hasAdx() && getAdx() != other.getAdx()) return false;
    if (hasDx() != other.hasDx()) return false;
    if (hasDx() && getDx() != other.getDx()) return false;
    if (hasPlusDI() != other.hasPlusDI()) return false;
    if (hasPlusDI() && getPlusDI() != other.getPlusDI()) return false;
    if (hasMinusDI() != other.hasMinusDI()) return false;
    if (hasMinusDI() && getMinusDI() != other.getMinusDI()) return false;
    return true;
  }

  /**
   * Returns a hash code value for the object. This method is * supported for the benefit of hash tables such as those provided by.
   */
  @Override
  public int hashCode() {
    int hash = super.hashCode();
    if (hasAdxr()) {
      hash = hash * 31 + ((int)(Double.doubleToLongBits(getAdxr()) ^ (Double.doubleToLongBits(getAdxr()) >>> 32)));
    }
    if (hasAdx()) {
      hash = hash * 31 + ((int)(Double.doubleToLongBits(getAdx()) ^ (Double.doubleToLongBits(getAdx()) >>> 32)));
    }
    if (hasDx()) {
      hash = hash * 31 + ((int)(Double.doubleToLongBits(getDx()) ^ (Double.doubleToLongBits(getDx()) >>> 32)));
    }
    if (hasPlusDI()) {
      hash = hash * 31 + ((int)(Double.doubleToLongBits(getPlusDI()) ^ (Double.doubleToLongBits(getPlusDI()) >>> 32)));
    }
    if (hasMinusDI()) {
      hash = hash * 31 + ((int)(Double.doubleToLongBits(getMinusDI()) ^ (Double.doubleToLongBits(getMinusDI()) >>> 32)));
    }
    return hash;
  }

  /**
   * Method copies state to a given instance
   * @param template class instance that should be used as a copy source
   */
  @Override
  public ADXRMessage copyFrom(RecordInfo template) {
    super.copyFrom(template);
    if (template instanceof ADXRMessageInfo) {
      ADXRMessageInfo t = (ADXRMessageInfo)template;
      if (t.hasAdxr()) {
        setAdxr(t.getAdxr());
      } else {
        nullifyAdxr();
      }
      if (t.hasAdx()) {
        setAdx(t.getAdx());
      } else {
        nullifyAdx();
      }
      if (t.hasDx()) {
        setDx(t.getDx());
      } else {
        nullifyDx();
      }
      if (t.hasPlusDI()) {
        setPlusDI(t.getPlusDI());
      } else {
        nullifyPlusDI();
      }
      if (t.hasMinusDI()) {
        setMinusDI(t.getMinusDI());
      } else {
        nullifyMinusDI();
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
    str.append("{ \"$type\":  \"ADXRMessage\"");
    if (hasAdxr()) {
      str.append(", \"adxr\": ").append(getAdxr());
    }
    if (hasAdx()) {
      str.append(", \"adx\": ").append(getAdx());
    }
    if (hasDx()) {
      str.append(", \"dx\": ").append(getDx());
    }
    if (hasPlusDI()) {
      str.append(", \"plusDI\": ").append(getPlusDI());
    }
    if (hasMinusDI()) {
      str.append(", \"minusDI\": ").append(getMinusDI());
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
