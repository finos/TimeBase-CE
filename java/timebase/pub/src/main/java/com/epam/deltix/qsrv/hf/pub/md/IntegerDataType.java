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
package com.epam.deltix.qsrv.hf.pub.md;

import com.epam.deltix.qsrv.hf.pub.md.ClassDescriptor.TypeResolver;

import com.epam.deltix.util.text.CharSequenceParser;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import javax.xml.bind.annotation.*;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.util.Arrays;

/**
 *
 */
@XmlType (name = "INTEGER")
public final class IntegerDataType extends DataType {
	private static final long serialVersionUID = 1L;
	
    public static final int PACKED_UNSIGNED_INT =   100;
    public static final int PACKED_UNSIGNED_LONG =  101;
    public static final int PACKED_INTERVAL =       102;

    public static final String ENCODING_INT8 = "INT8";
    public static final String ENCODING_INT16 = "INT16";
    public static final String ENCODING_INT32 = "INT32";
    public static final String ENCODING_INT48 = "INT48";
    public static final String ENCODING_INT64 = "INT64";
    public static final String ENCODING_PUINT30 = "PUINT30";
    public static final String ENCODING_PUINT61 = "PUINT61";
    public static final String ENCODING_PINTERVAL = "PINTERVAL";

    public static final String ENCODING_SIGNED = "SIGNED";
    public static final String ENCODING_UNSIGNED = "UNSIGNED";

    public static final byte INT8_NULL = Byte.MIN_VALUE;
    public static final short INT16_NULL = Short.MIN_VALUE;
    public static final int INT32_NULL = Integer.MIN_VALUE;
    public static final long INT48_NULL = -0x800000000000L;
    public static final long INT64_NULL = Long.MIN_VALUE;
    public static final int PUINT30_NULL = Integer.MAX_VALUE;
    public static final long PUINT61_NULL = Long.MAX_VALUE;
    public static final int PINTERVAL_NULL = 0;

    public static final String[] ENCODING = {ENCODING_INT8, ENCODING_INT16, ENCODING_INT32, ENCODING_INT48, ENCODING_INT64,
            ENCODING_PUINT30, /*PackedUnsignedInt*/
            ENCODING_PUINT61, /*PackedUnsignedLong*/
            ENCODING_PINTERVAL /* TimeIntervalCodec*/
    };

    public static final int[] SIZE = {1, 2, 4, 6, 8,
            PACKED_UNSIGNED_INT,
            PACKED_UNSIGNED_LONG,
            PACKED_INTERVAL
    };

    private static final DecimalFormat FORMAT_SIGNED = new DecimalFormat(ENCODING_SIGNED + "(0)");
    static int   extractSignedSize (String encoding) throws ParseException {
        synchronized (FORMAT_SIGNED) {
            return (FORMAT_SIGNED.parse (encoding).intValue ());
        }
    }

    private static final DecimalFormat FORMAT_UNSIGNED = new DecimalFormat(ENCODING_UNSIGNED + "(0)");
    static int   extractUnsignedSize (String encoding) throws ParseException {
        synchronized (FORMAT_UNSIGNED) {
            return (FORMAT_UNSIGNED.parse (encoding).intValue ());
        }
    }

    @XmlElement (name = "min")
    public Long     min; 

    @XmlElement (name = "max")
    public Long     max;

    private /*transient*/ int   size;

    // NULL values included
    public static final long[] MINS = {Byte.MIN_VALUE+1, Short.MIN_VALUE+1, Integer.MIN_VALUE+1, INT48_NULL+1, Long.MIN_VALUE+1, 0, 0, PINTERVAL_NULL+1};
    public static final long[] MAXS = {Byte.MAX_VALUE, Short.MAX_VALUE, Integer.MAX_VALUE, 0x7FFFFFFFFFFFL, Long.MAX_VALUE, 0x3FFFFFFE, 0x1FFFFFFFFFFFFFFEL, Integer.MAX_VALUE};
    public static final long[] NULLS = {INT8_NULL, INT16_NULL, INT32_NULL, INT48_NULL, INT64_NULL, PUINT30_NULL, PUINT61_NULL, PINTERVAL_NULL};

    public static IntegerDataType getDefaultInstance() {
        return new IntegerDataType(ENCODING_INT64, true, null, null);
    }

    IntegerDataType () { // For JAXB
        super();
        size = -1;
        min = null;
        max = null;
    }

    public IntegerDataType(String encoding, boolean nullable) {
        this(encoding, nullable, null, null);
    }

    public IntegerDataType (String encoding, boolean nullable, Number min, Number max) {
        super(encoding, nullable);

        // re-assign encoding to predefined
        this.encoding = getEncoding(size);

        // check bounds
        if (!check(min, max))
            throw new IllegalArgumentException(min + ", " + max);

        // and correct limits
        int idx = getIndex();
        if (min != null && min.longValue() == MINS[idx])
            min = null;
        if (max != null && max.longValue() == MAXS[idx])
            max = null;

        this.min = min != null ? min.longValue() : null;
        this.max = max != null ? max.longValue() : null;
    }

    public String           getBaseName () {
        return ("INTEGER");
    }

    @Override
    public int              getCode() {
        return T_INTEGER_TYPE;
    }

    @Override
    public void             parseEncoding(String encoding) {
        if (encoding == null)
            throw new IllegalArgumentException("null");

        encoding = encoding.toUpperCase();

        if (encoding.startsWith(ENCODING_SIGNED)) {
            try {
                int length = extractSignedSize(encoding);
                switch (length){
                    case 8: size = 1; break;
                    case 16: size = 2; break;
                    case 32: size = 4; break;
                    case 48: size = 6; break;
                    case 64: size = 8; break;

                    default:
                        throw new IllegalArgumentException(encoding);
                }

            } catch (ParseException e) {
                throw new IllegalArgumentException(encoding);
            }
        } else if (encoding.startsWith(ENCODING_UNSIGNED)) {
            try {
                int length = extractUnsignedSize (encoding);
                if (length == 30)
                    size = PACKED_UNSIGNED_INT;
                else if (length == 61)
                    size = PACKED_UNSIGNED_LONG;
                else
                    throw new IllegalArgumentException(encoding);

            } catch (ParseException e) {
                throw new IllegalArgumentException(encoding);
            }
        } else {
            for (int i = 0; i < ENCODING.length; i++)
                if (ENCODING[i].equals(encoding)) {
                    size = SIZE[i];
                    return;
                }

            throw new IllegalArgumentException(encoding);
        }
    }

    public Number           getMin() {
        return min;
    }

    public Number           getMax() {
        return max;
    }
    
    public Number           getMinNotNull () {
        return min == null ? MINS [getIndex ()] : min;
    }

    public Number           getMaxNotNull () {
        return max == null ? MAXS [getIndex ()] : max;
    }

    public Number[]         getRange() {
        return new Number[] { getMinNotNull (), getMaxNotNull () };
    }

    public boolean          check(Number min, Number max) {        
        Number[] range = getRange();
        
        return !((min != null && (min.longValue() < range[0].longValue() || min.longValue() > range[1].longValue())) ||
                (max != null && (max.longValue() < range[0].longValue() || max.longValue() > range[1].longValue())) ||
                (min != null && max != null && min.longValue() > max.longValue())
        );
    }

    public int getSize() {
        if(size == -1)
            parseEncoding(encoding);
        return size;
    }

    public int              getNativeTypeSize () {
        switch (getSize()) {
            case PACKED_UNSIGNED_INT:
            case PACKED_INTERVAL:
                return (4);

            case PACKED_UNSIGNED_LONG:
                return (8);

            default:
                return (size);
        }
    }

    public long             getNullValue () {
        return NULLS [getIndex ()];
    }
    
    @Override
    protected void          assertValidImpl (Object obj) {
        if (!(obj instanceof Number))
            throw unsupportedType (obj);
        
        long            value = ((Number) obj).longValue ();
        long            rmin = getMinNotNull ().longValue ();
        long            rmax = getMaxNotNull ().longValue ();
                
        if (value < rmin || value > rmax)
            throw outOfRange (value, rmin, rmax);
    }
    
    /**
     *  Convert non-null CharSequence to native
     */
    public static long      staticParse (CharSequence text) {
        return (CharSequenceParser.parseLong (text));
    }
    
    /**
     *  Standard formatter.
     */
    public static String    staticFormat (long value) {
        return (String.valueOf (value));
    }
    
    @Override
    protected Object        toBoxedImpl (CharSequence text) {
        return (staticParse (text));
    }
    
    @Override
    protected String        toStringImpl (Object obj) {
        return (staticFormat (((Number) obj).longValue ()));
    }

    public static String getEncoding(int size) {
        for (int i = 0; i < SIZE.length; i++)
            if (SIZE[i] == size)
                return ENCODING[i];

        throw new IllegalArgumentException(String.valueOf(size));
    }

    public int getIndex() {
        return getIndex(getSize());
    }

    public static int getIndex(int size) {
        int idx = Arrays.binarySearch(SIZE, size);
        if (idx < 0)
            throw new IllegalStateException(String.valueOf(size));
        return idx;
    }

     public ConversionType isConvertible(DataType to) {
        Number[] range = getRange();        

         if (to instanceof VarcharDataType) {
             return ConversionType.Lossless;
         } else if (to instanceof FloatDataType) {
             return ((FloatDataType) to).check(range[0], range[1]) ? ConversionType.Lossless : ConversionType.Lossy;
         } else if (to instanceof IntegerDataType) {
             return ((IntegerDataType) to).check(range[0], range[1]) ? ConversionType.Lossless : ConversionType.Lossy;
         } else if (to instanceof DateTimeDataType) {
             return check(0, Long.MAX_VALUE) ? ConversionType.Lossless : ConversionType.Lossy;
         } else if (to instanceof TimeOfDayDataType) {
             return check(0, Integer.MAX_VALUE) ? ConversionType.Lossless : ConversionType.Lossy;
         }

         return ConversionType.NotConvertible;
    }

    private static void                 writeLimit (Long n, DataOutputStream out)
        throws IOException
    {
        final boolean     b = n != null;

        out.writeBoolean (b);

        if (b)
            out.writeLong (n);
    }

    private static Long                 readLimit (DataInputStream in)
        throws IOException
    {
        if (in.readBoolean ())
            return (in.readLong ());
        else
            return (null);
    }

    @Override
    public void                         writeTo (DataOutputStream out)
        throws IOException
    {
        out.writeByte (T_INTEGER_TYPE);

        super.writeTo (out);

        writeLimit (min, out);
        writeLimit (max, out);
    }

    @Override
    protected void                      readFields (
        DataInputStream                     in,
        TypeResolver                        resolver
    )
        throws IOException
    {
        super.readFields (in, resolver);

        min = readLimit (in);
        max = readLimit (in);
    }
    }