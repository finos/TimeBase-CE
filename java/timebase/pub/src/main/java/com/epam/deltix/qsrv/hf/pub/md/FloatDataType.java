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
package com.epam.deltix.qsrv.hf.pub.md;

import com.epam.deltix.qsrv.hf.pub.md.ClassDescriptor.TypeResolver;
import com.epam.deltix.util.memory.*;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import javax.xml.bind.annotation.*;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import javax.xml.bind.annotation.adapters.XmlAdapter;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.util.regex.Pattern;
import java.util.regex.Matcher;


/**
 *
 */
@XmlType (name = "FLOAT")
public final class FloatDataType extends DataType {
	private static final long serialVersionUID = 1L;

    private static final int    T_NULL = 0;
    private static final int    T_FLOAT = 1;
    private static final int    T_DOUBLE = 2;

    public static final float  IEEE32_NULL = Float.NaN;
    public static final double IEEE64_NULL = Double.NaN;
    public static final double DECIMAL_NULL = IEEE64_NULL;

    /**
     *  Encode the number as a 32-bit float.
     */
    public static final int FIXED_FLOAT = 100;
    public static final String ENCODING_FIXED_FLOAT = "IEEE32";
    
    /**
     *  Encode the number as a 64-bit double.
     */
    public static final int FIXED_DOUBLE = 101;
    public static final String ENCODING_FIXED_DOUBLE = "IEEE64";
    
    /**
     *  Encode the number as a scaled double, with automatically determined 
     *  scale.
     */
    public static final int SCALE_AUTO = 102;
    public static final String ENCODING_SCALE_AUTO = "DECIMAL";

    public static final int SCALE_DECIMAL64 = 103;
    public static final String ENCODING_DECIMAL64 = "DECIMAL64";

    public static final String ENCODING_BINARY = "BINARY";

    public static final String[] ENCODING = {ENCODING_FIXED_FLOAT, ENCODING_FIXED_DOUBLE,
            ENCODING_SCALE_AUTO, /* ScaledDouble with SCALE_AUTO*/
    };

    private static Pattern PATTERN = Pattern.compile(
            "DECIMAL\\s*\\(\\s*([0,1]{0,1}[0-9])\\s*\\)"
    );

    private static final double[] MINS = {-Float.MAX_VALUE, -Double.MAX_VALUE};
    private static final double[] MAXS = {Float.MAX_VALUE, Double.MAX_VALUE};

    public static final int[] SCALE = {FIXED_FLOAT, FIXED_DOUBLE, SCALE_AUTO };

    private static final DecimalFormat FORMAT = new DecimalFormat(ENCODING_BINARY + "(0)");
    public static int   extractSize (String encoding) throws ParseException {
        synchronized (FORMAT) {
            return (FORMAT.parse (encoding).intValue ());
        }
    }

    @XmlElement (name = "min")
    @XmlJavaTypeAdapter (NumberAdapter.class)
    public Number     min;

    @XmlElement (name = "max")
    @XmlJavaTypeAdapter (NumberAdapter.class)
    public Number     max;

    private /*transient*/  int   scale;

    public static FloatDataType getDefaultInstance() {
        return new FloatDataType(ENCODING_FIXED_DOUBLE, true, null, null);
    }

    FloatDataType () { // For JAXB
        super();
        scale = -1;
        min = null;
        max = null;
    }

    public FloatDataType (String encoding, boolean nullable) {
        this(encoding, nullable, null, null);
    }

    public FloatDataType (String encoding, boolean nullable, Number min, Number max) {
        super(encoding, nullable);
        // check ...
        if (!check(min, max))
            throw new IllegalArgumentException(min + ", " + max);

        // and correct limits
        final int idx = getIndex();
        if (min != null && min.doubleValue() == MINS[idx])
            min = null;
        if (max != null && max.doubleValue() == MAXS[idx])
            max = null;

        this.min = min;
        this.max = max;
    }

    public String           getBaseName () {
        return ("FLOAT");
    }

    @Override
    public int              getCode() {
        return ENCODING_FIXED_FLOAT.equals(encoding) ? T_FLOAT_TYPE : T_DOUBLE_TYPE;
    }

    @Override
    public void parseEncoding(String encoding) {
        if (encoding == null)
            throw new IllegalArgumentException("null");

        encoding = encoding.toUpperCase();
        if (encoding.startsWith(ENCODING_DECIMAL64)) {
            scale = SCALE_DECIMAL64;
            return;
        } else if (encoding.startsWith(ENCODING_BINARY)) {
            try {
                int length = extractSize (encoding);
                if (length == 32){
                    scale = FIXED_FLOAT;
                    return;
                } else if(length == 64){
                    scale = FIXED_DOUBLE;
                    return;
                } else
                    throw new IllegalArgumentException(encoding);
            } catch (ParseException e) {
                throw new IllegalArgumentException(encoding);
            }
        }

        for (int i = 0; i < ENCODING.length; i++)
            if (ENCODING[i].equals(encoding)) {
                scale = SCALE[i];
                return;
            }

        int p = parseEncodingScaled(encoding);
        if (p != -1) {
            scale = p;
            return;
        }

        throw new IllegalArgumentException(encoding);
    }

    public Number getMin() {
        return min;
    }

    public Number getMax() {
        return max;
    }

    public Number getMinNotNull () {
        return min == null ? MINS [getIndex ()] : min;
    }

    public Number getMaxNotNull () {
        return max == null ? MAXS [getIndex ()] : max;
    }

    public boolean check(Number min, Number max) {        
        Number[] range = getRange();
        return !((min != null && (min.doubleValue() < range[0].doubleValue() || min.doubleValue() > range[1].doubleValue())) ||
                (max != null && (max.doubleValue() < range[0].doubleValue() || max.doubleValue() > range[1].doubleValue())) ||
                (min != null && max != null && min.doubleValue() > max.doubleValue())
        );
    }

    public boolean          isFloat () {
        return (getScale() == FIXED_FLOAT);        
    }

    public boolean          isDecimal64 () {
        return (getScale() == SCALE_DECIMAL64);
    }

    public int getScale() {
        if (scale == -1)
            parseEncoding(encoding);
        return scale;
    }

    public static String getEncodingScaled(int size) {
        return String.format("DECIMAL(%d)", size);
    }

    public static int parseEncodingScaled(String encoding) {
        Matcher m = PATTERN.matcher(encoding);
        if (m.matches()) {
            final String precision = m.group(1);
            final int p = Integer.parseInt(precision);
            if (p <= MemoryDataOutput.MAX_SCALE_EXP)
                return p;
        }
        return -1;
    }

    @Override
    protected void          assertValidImpl (Object obj) {
        if (!(obj instanceof Number))
            throw unsupportedType (obj);
        
        if (obj instanceof Double) {
            double          value = ((Double) obj).doubleValue ();        
            double          rmin = getMinNotNull ().doubleValue ();
            double          rmax = getMaxNotNull ().doubleValue ();

            if (value < rmin || value > rmax)
                throw outOfRange (value, rmin, rmax);
        }
        else {
            float           value = ((Float) obj).floatValue ();        
            float           rmin = getMinNotNull ().floatValue ();
            float           rmax = getMaxNotNull ().floatValue ();

            if (value < rmin || value > rmax)
                throw outOfRange (value, rmin, rmax);
        }
    }
    
    /**
     *  Convert non-null CharSequence to float
     */
    public static float     staticParseFloat (CharSequence text) {
        return (Float.parseFloat (text.toString ()));
    }
    
    /**
     *  Convert non-null CharSequence to double
     */
    public static double    staticParseDouble (CharSequence text) {
        return (Double.parseDouble (text.toString ()));
    }
    
    public static String    staticFormat (float f) {
        return (String.valueOf (f));
    }
    
    public static String    staticFormat (double d) {
        return (String.valueOf (d));
    }
    
    @Override
    protected Object        toBoxedImpl (CharSequence text) {
        if (isFloat ())
            return (staticParseFloat (text));
        
        return (staticParseDouble (text));
    }
    
    @Override
    protected String        toStringImpl (Object obj) {
        return (obj instanceof Float ? staticFormat ((Float) obj) : staticFormat ((Double) obj));
    }

    public Number[]         getRange() {
        return new Number[] { getMinNotNull(), getMaxNotNull()};
    }

    public ConversionType   isConvertible(DataType to) {

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

    private int getIndex() {
        return isFloat() ? 0 : 1;
    }

    public static class NumberAdapter extends XmlAdapter<String, Number> {
        
        public NumberAdapter() {
        }

        public Number unmarshal(String v) throws Exception {
            if (v != null) {
              if (v.endsWith("f"))
                return Float.parseFloat(v);
                
              return Double.parseDouble(v);
            }
            return null;
        }

        public String marshal(Number v) throws Exception {
            if (v instanceof Float)
                return v.toString() + "f";
            return v != null ? v.toString() : null;            
        }
    }

    private static void                 writeNumber (Number n, DataOutputStream out)
        throws IOException
    {
        if (n == null)
            out.writeByte (T_NULL);
        else if (n.getClass () == Float.class) {
            out.writeByte (T_FLOAT);
            out.writeFloat (n.floatValue ());
        }
        else {
            out.writeByte (T_DOUBLE);
            out.writeDouble (n.doubleValue ());
        }
    }

    private static Number               readNumber (DataInputStream in)
        throws IOException
    {
        int     tag = in.readByte ();

        switch (tag) {
            case T_NULL:        return (null);
            case T_FLOAT:       return (in.readFloat ());
            case T_DOUBLE:      return (in.readDouble ());
            default:            throw new IOException ("Illegal tag: " + tag);
        }
    }

    @Override
    public void                         writeTo (DataOutputStream out)
        throws IOException
    {
        out.writeByte (T_FLOAT_TYPE);

        super.writeTo (out);

        writeNumber (min, out);
        writeNumber (max, out);
    }

    @Override
    protected void                      readFields (
        DataInputStream                     in,
        TypeResolver                        resolver
    )
        throws IOException
    {
        super.readFields (in, resolver);

        min = readNumber (in);
        max = readNumber (in);
    }
    }
