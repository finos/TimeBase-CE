/*
 * Copyright 2024 EPAM Systems, Inc
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
package com.epam.deltix.qsrv.hf.pub.md;

import com.epam.deltix.timebase.messages.TimeStamp;
import com.epam.deltix.util.lang.StringUtils;
import com.epam.deltix.util.time.GMT;
import java.io.DataOutputStream;
import java.io.IOException;
import java.text.ParseException;
import javax.xml.bind.annotation.*;

import static com.epam.deltix.qsrv.hf.pub.util.SerializationUtils.writeNullableString;

/**
 *
 */
@XmlType (name = "dateTime")
public final class DateTimeDataType extends DataType {
	private static final long serialVersionUID = 1L;
	
    public static final long NULL = Long.MIN_VALUE;

    public static final String ENCODING_MILLISECONDS = "MILLISECOND";
    public static final String ENCODING_MILLISECONDS_1 = "MS";
    public static final String ENCODING_NANOSECONDS = "NANOSECOND";
    public static final String ENCODING_NANOSECONDS_1 = "NS";

    public static DateTimeDataType getDefaultInstance() {
        return new DateTimeDataType(true, ENCODING_MILLISECONDS);
    }

    DateTimeDataType () { // For JAXB
        super();
    }

    public DateTimeDataType(boolean nullable) {
        this(nullable, ENCODING_MILLISECONDS);
    }

    public DateTimeDataType(boolean nullable, String encoding) {
        parseEncoding(encoding);
        this.nullable = nullable;
    }

    @Override
    public void parseEncoding(String encoding) {
        if (ENCODING_NANOSECONDS.equalsIgnoreCase(encoding) || ENCODING_NANOSECONDS_1.equalsIgnoreCase(encoding))
            this.encoding = ENCODING_NANOSECONDS;
        else if (ENCODING_MILLISECONDS.equalsIgnoreCase(encoding) || ENCODING_MILLISECONDS_1.equalsIgnoreCase(encoding))
            this.encoding = ENCODING_MILLISECONDS;
        else if (encoding == null || StringUtils.isEmpty(encoding))
            this.encoding = ENCODING_MILLISECONDS;
        else
            throw new IllegalArgumentException("Unknown encoding for DateTime:" + encoding);
    }

    public static boolean isEquals(String encoding1, String encoding2) {
        return StringUtils.equals(staticParseEncoding(encoding1), staticParseEncoding(encoding2));
    }

    public static boolean isNotEquals(String encoding1, String encoding2) {
        return !StringUtils.equals(staticParseEncoding(encoding1), staticParseEncoding(encoding2));
    }

    public static String staticParseEncoding(String encoding) {
        if (ENCODING_NANOSECONDS.equalsIgnoreCase(encoding) || ENCODING_NANOSECONDS_1.equalsIgnoreCase(encoding))
            return ENCODING_NANOSECONDS;
        else if (ENCODING_MILLISECONDS.equalsIgnoreCase(encoding) || ENCODING_MILLISECONDS_1.equalsIgnoreCase(encoding))
            return ENCODING_MILLISECONDS;
        else if (encoding == null || StringUtils.isEmpty(encoding))
            return ENCODING_MILLISECONDS;
        else
            throw new IllegalArgumentException("Unknown encoding for DateTime:" + encoding);
    }

    public String           getBaseName () {
        return ("TIMESTAMP");
    }

    @Override
    public int              getCode() {
        return T_DATE_TIME_TYPE;
    }

    @Override
    protected void          assertValidImpl (Object obj) {
        if (!(obj instanceof Long))
            throw unsupportedType (obj);               
    }

    /*
     *   Returns true is type has nanoseconds encoding
     */
    public boolean          hasNanosecondPrecision() {
        return ENCODING_NANOSECONDS.equals(encoding) || ENCODING_NANOSECONDS_1.equals(encoding);
    }

    /**
     *  Convert non-null CharSequence to long (milliseconds) by parsing it as
     *  canonical representation in GMT
     */
    public static long      staticParse (CharSequence text, String encoding) {

        String  s = text.toString ();
        
        try {
            if (isEquals(ENCODING_MILLISECONDS, encoding)) {
                if (s.length() < 20)
                    return GMT.parseDateTime(s).getTime();
                else
                    return GMT.parseDateTimeMillis(s).getTime();
            }

            long time = GMT.parseDateTime(s).getTime();

            String nanos = s.length() >= 20 ? s.substring(20) : "0";

            if (nanos.length() > 9)
                throw new NumberFormatException ("Illegal date: " + s);

            int nsValue = Integer.parseInt(nanos);
            if (nanos.length() == 9) { // full nanoseconds
                return TimeStamp.getNanoTime(time, nsValue);
            } else if (nanos.length() == 3) { // milliseconds
                return TimeStamp.getNanoTime(time + nsValue);
            } else {
                double multiplier = Math.max(0, 9 - nanos.length());
                nsValue = nsValue * (int) Math.pow(10, multiplier);
                return TimeStamp.getNanoTime(time, nsValue);
            }

        } catch (ParseException x) {
            throw new NumberFormatException ("Illegal date: " + s);
        }
    }

    @Override
    protected Object        toBoxedImpl (CharSequence text) {
        return (staticParse (text, encoding));
    }
    
    @Override
    protected String        toStringImpl (Object obj) {
        if (hasNanosecondPrecision())
            return (GMT.formatNanos ((Long)obj));
        else
            return (GMT.formatDateTimeMillis ((Long)obj));
    }

    public ConversionType isConvertible(DataType to) {
        if (to instanceof VarcharDataType) {
            return ConversionType.Lossless;
        } else if (to instanceof FloatDataType) {
            return ((FloatDataType) to).check(0, Long.MAX_VALUE) ? ConversionType.Lossless : ConversionType.Lossy;
        } else if (to instanceof IntegerDataType) {
            return ConversionType.Lossy;
        } else if (to instanceof DateTimeDataType) {
            if (isEquals(to.getEncoding(), getEncoding()))
                return ConversionType.Lossless;
            else if (hasNanosecondPrecision())
                return ConversionType.Lossy;
            else
                return ConversionType.Lossless;
        }

        return ConversionType.NotConvertible;
    }

    /*
     * Creates DateTimeDataType without specified encoding compatible with previous versions (4.3 and 5.X)
     */
    public static DateTimeDataType createEmpty(boolean nullable) {
        DateTimeDataType type = new DateTimeDataType();
        type.nullable = nullable;
        return type;
    }

    @Override
    public void             writeTo (DataOutputStream out) throws IOException {
        out.writeByte (T_DATE_TIME_TYPE);
        out.writeBoolean (isNullable());
        // write only 'nanosecond' encoding to have backward compatibility
        writeNullableString (hasNanosecondPrecision() ? encoding : null, out);
    }
}