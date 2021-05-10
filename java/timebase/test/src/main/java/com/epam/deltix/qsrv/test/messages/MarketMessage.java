package com.epam.deltix.qsrv.test.messages;

import com.epam.deltix.timebase.messages.*;

import java.lang.Deprecated;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.lang.StringBuilder;

/**
 * Most financial market-related messages subclass this abstract class.
 */
@SchemaElement(
        name = "deltix.timebase.api.messages.MarketMessage",
        title = "Market Message"
)
@OldElementName("deltix.qsrv.hf.pub.MarketMessage")
public class MarketMessage extends InstrumentMessage {
    public static final String CLASS_NAME = MarketMessage.class.getName();

    /**
     * Default currency. Deprecated and will be removed in future versions.
     */
    public static final short DEFAULT_CURRENCY = 999;

    /**
     * Exchange Time is measured in milliseconds that passed since January 1, 1970 UTC
     */
    protected long originalTimestamp = TypeConstants.TIMESTAMP_UNKNOWN;

    /**
     * Currency code represented as short. Use {currencyCodec} or
     * {link #setCurrencyCode} and {link #getCurrencyCode} to
     * convert this value to a three-character code.
     */
    protected short currencyCode = 999;

    /**
     * Market specific identifier of the given event in a sequence of market events.
     */
    protected long sequenceNumber = TypeConstants.INT64_NULL;

    /**
     * Identifies market data source. Different sessions of same connector
     * to a same data provider should have different id.
     */
    protected long sourceId = TypeConstants.INT64_NULL;

    /**
     * Exchange Time is measured in milliseconds that passed since January 1, 1970 UTC
     * @return Original Timestamp
     */
    @SchemaType(
            dataType = SchemaDataType.TIMESTAMP
    )
    @SchemaElement(
            description = "Original Timestamp"
    )
    public long getOriginalTimestamp() {
        return originalTimestamp;
    }

    /**
     * Exchange Time is measured in milliseconds that passed since January 1, 1970 UTC
     * @param value - Original Timestamp
     */
    public void setOriginalTimestamp(long value) {
        this.originalTimestamp = value;
    }

    /**
     * Exchange Time is measured in milliseconds that passed since January 1, 1970 UTC
     * @return true if Original Timestamp is not null
     */
    public boolean hasOriginalTimestamp() {
        return originalTimestamp != com.epam.deltix.timebase.messages.TypeConstants.TIMESTAMP_UNKNOWN;
    }

    /**
     * Exchange Time is measured in milliseconds that passed since January 1, 1970 UTC
     */
    public void nullifyOriginalTimestamp() {
        this.originalTimestamp = com.epam.deltix.timebase.messages.TypeConstants.TIMESTAMP_UNKNOWN;
    }

    /**
     * Currency code represented as short. Use {currencyCodec} or
     * {link #setCurrencyCode} and {link #getCurrencyCode} to
     * convert this value to a three-character code.
     * @return Currency Code
     */
    @SchemaElement
    @Deprecated
    public short getCurrencyCode() {
        return currencyCode;
    }

    /**
     * Currency code represented as short. Use {currencyCodec} or
     * {link #setCurrencyCode} and {link #getCurrencyCode} to
     * convert this value to a three-character code.
     * @param value - Currency Code
     */
    public void setCurrencyCode(short value) {
        this.currencyCode = value;
    }

    /**
     * Currency code represented as short. Use {currencyCodec} or
     * {link #setCurrencyCode} and {link #getCurrencyCode} to
     * convert this value to a three-character code.
     * @return true if Currency Code is not null
     */
    public boolean hasCurrencyCode() {
        return currencyCode != com.epam.deltix.timebase.messages.TypeConstants.INT16_NULL;
    }

    /**
     * Currency code represented as short. Use {currencyCodec} or
     * {link #setCurrencyCode} and {link #getCurrencyCode} to
     * convert this value to a three-character code.
     */
    public void nullifyCurrencyCode() {
        this.currencyCode = com.epam.deltix.timebase.messages.TypeConstants.INT16_NULL;
    }

    /**
     * Market specific identifier of the given event in a sequence of market events.
     * @return Sequence Number
     */
    @SchemaElement
    public long getSequenceNumber() {
        return sequenceNumber;
    }

    /**
     * Market specific identifier of the given event in a sequence of market events.
     * @param value - Sequence Number
     */
    public void setSequenceNumber(long value) {
        this.sequenceNumber = value;
    }

    /**
     * Market specific identifier of the given event in a sequence of market events.
     * @return true if Sequence Number is not null
     */
    public boolean hasSequenceNumber() {
        return sequenceNumber != com.epam.deltix.timebase.messages.TypeConstants.INT64_NULL;
    }

    /**
     * Market specific identifier of the given event in a sequence of market events.
     */
    public void nullifySequenceNumber() {
        this.sequenceNumber = com.epam.deltix.timebase.messages.TypeConstants.INT64_NULL;
    }

    /**
     * Identifies market data source. Different sessions of same connector
     * to a same data provider should have different id.
     * @return Source Id
     */
    @SchemaType(
            encoding = "ALPHANUMERIC(10)",
            dataType = SchemaDataType.VARCHAR
    )
    @SchemaElement
    public long getSourceId() {
        return sourceId;
    }

    /**
     * Identifies market data source. Different sessions of same connector
     * to a same data provider should have different id.
     * @param value - Source Id
     */
    public void setSourceId(long value) {
        this.sourceId = value;
    }

    /**
     * Identifies market data source. Different sessions of same connector
     * to a same data provider should have different id.
     * @return true if Source Id is not null
     */
    public boolean hasSourceId() {
        return sourceId != com.epam.deltix.timebase.messages.TypeConstants.INT64_NULL;
    }

    /**
     * Identifies market data source. Different sessions of same connector
     * to a same data provider should have different id.
     */
    public void nullifySourceId() {
        this.sourceId = com.epam.deltix.timebase.messages.TypeConstants.INT64_NULL;
    }

    /**
     * Creates new instance of this class.
     * @return new instance of this class.
     */
    @Override
    protected MarketMessage createInstance() {
        return new MarketMessage();
    }

    /**
     * Method nullifies all instance properties
     */
    @Override
    public MarketMessage nullify() {
        nullifyOriginalTimestamp();
        nullifyCurrencyCode();
        nullifySequenceNumber();
        nullifySourceId();
        return this;
    }

    /**
     * Resets all instance properties to their default values
     */
    @Override
    public MarketMessage reset() {
        originalTimestamp = com.epam.deltix.timebase.messages.TypeConstants.TIMESTAMP_UNKNOWN;
        currencyCode = 999;
        sequenceNumber = com.epam.deltix.timebase.messages.TypeConstants.INT64_NULL;
        sourceId = com.epam.deltix.timebase.messages.TypeConstants.INT64_NULL;
        return this;
    }

    /**
     * Method copies state to a given instance
     */
    @Override
    public MarketMessage clone() {
        MarketMessage t = createInstance();
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
        if (!(obj instanceof MarketMessage)) return false;
        MarketMessage other = (MarketMessage)obj;
        if (hasOriginalTimestamp() != other.hasOriginalTimestamp()) return false;
        if (hasOriginalTimestamp() && getOriginalTimestamp() != other.getOriginalTimestamp()) return false;
        if (hasCurrencyCode() != other.hasCurrencyCode()) return false;
        if (hasCurrencyCode() && getCurrencyCode() != other.getCurrencyCode()) return false;
        if (hasSequenceNumber() != other.hasSequenceNumber()) return false;
        if (hasSequenceNumber() && getSequenceNumber() != other.getSequenceNumber()) return false;
        if (hasSourceId() != other.hasSourceId()) return false;
        if (hasSourceId() && getSourceId() != other.getSourceId()) return false;
        return true;
    }

    /**
     * Returns a hash code value for the object. This method is * supported for the benefit of hash tables such as those provided by.
     */
    @Override
    public int hashCode() {
        int hash = super.hashCode();
        if (hasOriginalTimestamp()) {
            hash = hash * 31 + ((int)(getOriginalTimestamp() ^ (getOriginalTimestamp() >>> 32)));
        }
        if (hasCurrencyCode()) {
            hash = hash * 31 + ((int)getCurrencyCode());
        }
        if (hasSequenceNumber()) {
            hash = hash * 31 + ((int)(getSequenceNumber() ^ (getSequenceNumber() >>> 32)));
        }
        if (hasSourceId()) {
            hash = hash * 31 + ((int)(getSourceId() ^ (getSourceId() >>> 32)));
        }
        return hash;
    }

    /**
     * Method copies state to a given instance
     * @param template class instance that should be used as a copy source
     */
    public MarketMessage copyFrom(RecordInfo template) {

        if (template instanceof MarketMessage) {
            MarketMessage t = (MarketMessage)template;
            if (t.hasOriginalTimestamp()) {
                setOriginalTimestamp(t.getOriginalTimestamp());
            } else {
                nullifyOriginalTimestamp();
            }
            if (t.hasCurrencyCode()) {
                setCurrencyCode(t.getCurrencyCode());
            } else {
                nullifyCurrencyCode();
            }
            if (t.hasSequenceNumber()) {
                setSequenceNumber(t.getSequenceNumber());
            } else {
                nullifySequenceNumber();
            }
            if (t.hasSourceId()) {
                setSourceId(t.getSourceId());
            } else {
                nullifySourceId();
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
        str.append("{ \"$type\":  \"MarketMessage\"");
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
