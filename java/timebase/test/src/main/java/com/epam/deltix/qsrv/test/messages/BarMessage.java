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

import com.epam.deltix.timebase.messages.OldElementName;
import com.epam.deltix.timebase.messages.RecordInfo;
import com.epam.deltix.timebase.messages.RelativeTo;
import com.epam.deltix.timebase.messages.SchemaDataType;
import com.epam.deltix.timebase.messages.SchemaElement;
import com.epam.deltix.timebase.messages.SchemaType;
import com.epam.deltix.timebase.messages.TypeConstants;
import java.lang.Double;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.lang.StringBuilder;

/**
 * Basic information about a bar.
 */
@OldElementName("deltix.qsrv.hf.pub.BarMessage")
@SchemaElement(
        name = "com.epam.deltix.qsrv.test.messages.BarMessage",
        title = "Bar Message"
)
public class BarMessage extends MarketMessage {
    public static final String CLASS_NAME = BarMessage.class.getName();

    /**
     * Milliseconds per second.
     */
    public static final long BAR_SECOND = 1000L;

    /**
     * Milliseconds per minute.
     */
    public static final long BAR_MINUTE = 60000L;

    /**
     * Milliseconds per 5 minute.
     */
    public static final long BAR_5_MINUTE = 300000L;

    /**
     * Milliseconds per 10 minute.
     */
    public static final long BAR_10_MINUTE = 600000L;

    /**
     * Milliseconds per hour.
     */
    public static final long BAR_HOUR = 3600000L;

    /**
     * Milliseconds per day.
     */
    public static final long BAR_DAY = 86400000L;

    /**
     * Opening price for the bar interval.
     */
    protected double open = TypeConstants.IEEE64_NULL;

    /**
     * Highest price for the bar interval.
     */
    protected double high = TypeConstants.IEEE64_NULL;

    /**
     * Lowest price for the bar interval.
     */
    protected double low = TypeConstants.IEEE64_NULL;

    /**
     * Closing price for the bar interval.
     */
    protected double close = TypeConstants.IEEE64_NULL;

    /**
     * Trade volume.
     */
    protected double volume = TypeConstants.IEEE64_NULL;

    /**
     * Vendor-specific market code.
     */
    protected long exchangeId = TypeConstants.EXCHANGE_NULL;

    /**
     * Opening price for the bar interval.
     * @return Open
     */
    @SchemaType(
            encoding = "DECIMAL(8)",
            dataType = SchemaDataType.FLOAT
    )
    @SchemaElement(
            title = "Open"
    )
    @RelativeTo("close")
    public double getOpen() {
        return open;
    }

    /**
     * Opening price for the bar interval.
     * @param value - Open
     */
    public void setOpen(double value) {
        this.open = value;
    }

    /**
     * Opening price for the bar interval.
     * @return true if Open is not null
     */
    public boolean hasOpen() {
        return !Double.isNaN(open);
    }

    /**
     * Opening price for the bar interval.
     */
    public void nullifyOpen() {
        this.open = com.epam.deltix.timebase.messages.TypeConstants.IEEE64_NULL;
    }

    /**
     * Highest price for the bar interval.
     * @return High
     */
    @SchemaType(
            encoding = "DECIMAL(8)",
            dataType = SchemaDataType.FLOAT
    )
    @SchemaElement(
            title = "High"
    )
    @RelativeTo("close")
    public double getHigh() {
        return high;
    }

    /**
     * Highest price for the bar interval.
     * @param value - High
     */
    public void setHigh(double value) {
        this.high = value;
    }

    /**
     * Highest price for the bar interval.
     * @return true if High is not null
     */
    public boolean hasHigh() {
        return !Double.isNaN(high);
    }

    /**
     * Highest price for the bar interval.
     */
    public void nullifyHigh() {
        this.high = com.epam.deltix.timebase.messages.TypeConstants.IEEE64_NULL;
    }

    /**
     * Lowest price for the bar interval.
     * @return Low
     */
    @SchemaType(
            encoding = "DECIMAL(8)",
            dataType = SchemaDataType.FLOAT
    )
    @SchemaElement(
            title = "Low"
    )
    @RelativeTo("close")
    public double getLow() {
        return low;
    }

    /**
     * Lowest price for the bar interval.
     * @param value - Low
     */
    public void setLow(double value) {
        this.low = value;
    }

    /**
     * Lowest price for the bar interval.
     * @return true if Low is not null
     */
    public boolean hasLow() {
        return !Double.isNaN(low);
    }

    /**
     * Lowest price for the bar interval.
     */
    public void nullifyLow() {
        this.low = com.epam.deltix.timebase.messages.TypeConstants.IEEE64_NULL;
    }

    /**
     * Closing price for the bar interval.
     * @return Close
     */
    @SchemaType(
            encoding = "DECIMAL(8)",
            dataType = SchemaDataType.FLOAT
    )
    @SchemaElement(
            title = "Close"
    )
    public double getClose() {
        return close;
    }

    /**
     * Closing price for the bar interval.
     * @param value - Close
     */
    public void setClose(double value) {
        this.close = value;
    }

    /**
     * Closing price for the bar interval.
     * @return true if Close is not null
     */
    public boolean hasClose() {
        return !Double.isNaN(close);
    }

    /**
     * Closing price for the bar interval.
     */
    public void nullifyClose() {
        this.close = com.epam.deltix.timebase.messages.TypeConstants.IEEE64_NULL;
    }

    /**
     * Trade volume.
     * @return Volume
     */
    @SchemaType(
            encoding = "DECIMAL(8)",
            dataType = SchemaDataType.FLOAT
    )
    @SchemaElement(
            title = "Volume"
    )
    public double getVolume() {
        return volume;
    }

    /**
     * Trade volume.
     * @param value - Volume
     */
    public void setVolume(double value) {
        this.volume = value;
    }

    /**
     * Trade volume.
     * @return true if Volume is not null
     */
    public boolean hasVolume() {
        return !Double.isNaN(volume);
    }

    /**
     * Trade volume.
     */
    public void nullifyVolume() {
        this.volume = com.epam.deltix.timebase.messages.TypeConstants.IEEE64_NULL;
    }

    /**
     * Vendor-specific market code.
     * @return Exchange Code
     */
    @SchemaType(
            encoding = "ALPHANUMERIC(10)",
            dataType = SchemaDataType.VARCHAR
    )
    @SchemaElement(
            title = "Exchange Code"
    )
    @OldElementName("exchangeCode")
    public long getExchangeId() {
        return exchangeId;
    }

    /**
     * Vendor-specific market code.
     * @param value - Exchange Code
     */
    public void setExchangeId(long value) {
        this.exchangeId = value;
    }

    /**
     * Vendor-specific market code.
     * @return true if Exchange Code is not null
     */
    public boolean hasExchangeId() {
        return exchangeId != com.epam.deltix.timebase.messages.TypeConstants.EXCHANGE_NULL;
    }

    /**
     * Vendor-specific market code.
     */
    public void nullifyExchangeId() {
        this.exchangeId = com.epam.deltix.timebase.messages.TypeConstants.EXCHANGE_NULL;
    }

    /**
     * Creates new instance of this class.
     * @return new instance of this class.
     */
    @Override
    protected BarMessage createInstance() {
        return new BarMessage();
    }

    /**
     * Method nullifies all instance properties
     */
    @Override
    public BarMessage nullify() {
        super.nullify();
        nullifyOpen();
        nullifyHigh();
        nullifyLow();
        nullifyClose();
        nullifyVolume();
        nullifyExchangeId();
        return this;
    }

    /**
     * Resets all instance properties to their default values
     */
    @Override
    public BarMessage reset() {
        super.reset();
        open = com.epam.deltix.timebase.messages.TypeConstants.IEEE64_NULL;
        high = com.epam.deltix.timebase.messages.TypeConstants.IEEE64_NULL;
        low = com.epam.deltix.timebase.messages.TypeConstants.IEEE64_NULL;
        close = com.epam.deltix.timebase.messages.TypeConstants.IEEE64_NULL;
        volume = com.epam.deltix.timebase.messages.TypeConstants.IEEE64_NULL;
        exchangeId = com.epam.deltix.timebase.messages.TypeConstants.EXCHANGE_NULL;
        return this;
    }

    /**
     * Method copies state to a given instance
     */
    @Override
    public BarMessage clone() {
        BarMessage t = createInstance();
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
        if (!(obj instanceof BarMessage)) return false;
        BarMessage other = (BarMessage)obj;
        if (hasOpen() != other.hasOpen()) return false;
        if (hasOpen() && getOpen() != other.getOpen()) return false;
        if (hasHigh() != other.hasHigh()) return false;
        if (hasHigh() && getHigh() != other.getHigh()) return false;
        if (hasLow() != other.hasLow()) return false;
        if (hasLow() && getLow() != other.getLow()) return false;
        if (hasClose() != other.hasClose()) return false;
        if (hasClose() && getClose() != other.getClose()) return false;
        if (hasVolume() != other.hasVolume()) return false;
        if (hasVolume() && getVolume() != other.getVolume()) return false;
        if (hasExchangeId() != other.hasExchangeId()) return false;
        if (hasExchangeId() && getExchangeId() != other.getExchangeId()) return false;
        return true;
    }

    /**
     * Returns a hash code value for the object. This method is * supported for the benefit of hash tables such as those provided by.
     */
    @Override
    public int hashCode() {
        int hash = super.hashCode();
        if (hasOpen()) {
            hash = hash * 31 + ((int)(Double.doubleToLongBits(getOpen()) ^ (Double.doubleToLongBits(getOpen()) >>> 32)));
        }
        if (hasHigh()) {
            hash = hash * 31 + ((int)(Double.doubleToLongBits(getHigh()) ^ (Double.doubleToLongBits(getHigh()) >>> 32)));
        }
        if (hasLow()) {
            hash = hash * 31 + ((int)(Double.doubleToLongBits(getLow()) ^ (Double.doubleToLongBits(getLow()) >>> 32)));
        }
        if (hasClose()) {
            hash = hash * 31 + ((int)(Double.doubleToLongBits(getClose()) ^ (Double.doubleToLongBits(getClose()) >>> 32)));
        }
        if (hasVolume()) {
            hash = hash * 31 + ((int)(Double.doubleToLongBits(getVolume()) ^ (Double.doubleToLongBits(getVolume()) >>> 32)));
        }
        if (hasExchangeId()) {
            hash = hash * 31 + ((int)(getExchangeId() ^ (getExchangeId() >>> 32)));
        }
        return hash;
    }

    /**
     * Method copies state to a given instance
     * @param template class instance that should be used as a copy source
     */
    @Override
    public BarMessage copyFrom(RecordInfo template) {
        super.copyFrom(template);
        if (template instanceof BarMessage) {
            BarMessage t = (BarMessage)template;
            if (t.hasOpen()) {
                setOpen(t.getOpen());
            } else {
                nullifyOpen();
            }
            if (t.hasHigh()) {
                setHigh(t.getHigh());
            } else {
                nullifyHigh();
            }
            if (t.hasLow()) {
                setLow(t.getLow());
            } else {
                nullifyLow();
            }
            if (t.hasClose()) {
                setClose(t.getClose());
            } else {
                nullifyClose();
            }
            if (t.hasVolume()) {
                setVolume(t.getVolume());
            } else {
                nullifyVolume();
            }
            if (t.hasExchangeId()) {
                setExchangeId(t.getExchangeId());
            } else {
                nullifyExchangeId();
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
        str.append("{ \"$type\":  \"BarMessage\"");
        if (hasOpen()) {
            str.append(", \"open\": ").append(getOpen());
        }
        if (hasHigh()) {
            str.append(", \"high\": ").append(getHigh());
        }
        if (hasLow()) {
            str.append(", \"low\": ").append(getLow());
        }
        if (hasClose()) {
            str.append(", \"close\": ").append(getClose());
        }
        if (hasVolume()) {
            str.append(", \"volume\": ").append(getVolume());
        }
        if (hasExchangeId()) {
            str.append(", \"exchangeId\": ").append(getExchangeId());
        }
        if (hasOriginalTimestamp()) {
            str.append(", \"originalTimestamp\": \"").append(getOriginalTimestamp()).append("\"");
        }
        if (hasCurrencyCode()) {
            str.append(", \"currencyCode\": ").append(getCurrencyCode());
        }
        if (hasSequenceNumber()) {
            str.append(", \"sequenceNumber\": ").append(getSequenceNumber());
        }
        if (hasSourceId()) {
            str.append(", \"sourceId\": ").append(getSourceId());
        }

        if (hasTimeStampMs()) {
            str.append(", \"timeStampMs\": \"").append(getTimeStampMs()).append("\"");
        }
        if (hasSymbol()) {
            str.append(", \"symbol\": \"").append(getSymbol()).append("\"");
        }
        str.append("}");
        return str;
    }
}