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

import com.epam.deltix.qsrv.hf.pub.codec.AlphanumericCodec;
import com.epam.deltix.qsrv.hf.pub.md.ClassDescriptor.TypeResolver;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import javax.xml.bind.annotation.*;
import java.text.DecimalFormat;
import java.text.ParseException;

/**
 *
 */
@XmlType (name = "VARCHAR")
public final class VarcharDataType extends DataType {
	private static final long serialVersionUID = 1L;

    public static final int             INLINE_VARSIZE =   -1000;
    public static final int             FORWARD_VARSIZE =  -1001;
    public static final int             ALPHANUMERIC =     -1002;

    public static final String          ENCODING_INLINE_VARSIZE = "UTF8";
    public static final String          ENCODING_ALPHANUMERIC = "ALPHANUMERIC";

    public static final long ALPHANUMERIC_NULL = IntegerDataType.INT64_NULL;

    private static final DecimalFormat  FORMAT = new DecimalFormat(ENCODING_ALPHANUMERIC + "(0)");

    public static int   extractSize (String encoding) throws ParseException {
        synchronized (FORMAT) {
            return (FORMAT.parse (encoding).intValue ());
        }
    }
    public static final String[] ENCODING = {
            ENCODING_INLINE_VARSIZE, /* Inline UTF8 */
            // 8 Feb 2012: completely disable FUTF8 support
            //ENCODING_FORWARD_VARSIZE /* Forward-referenced UTF8*/
    };

    public static final int[] ENCODING_TYPE = {
            INLINE_VARSIZE,
            // 8 Feb 2012: completely disable FUTF8 support
            //FORWARD_VARSIZE
    };

    @XmlElement (name = "multiLine")
    private boolean                     multiLine;

    // Do not access this field directly. Use getEncodingType
    private /*transient*/  int          encodingType;

    // number of chars for ALPHANUMERIC encoding
    private /*transient*/  int          length;

    public static VarcharDataType getDefaultInstance() {
        return new VarcharDataType(ENCODING_INLINE_VARSIZE, true, true);
    }

    VarcharDataType() { // For JAXB
        super();
        multiLine = false;
        encodingType = -1;
        length = -1;
    }

    public VarcharDataType(String encoding, boolean nullable, boolean inMultiLine) {
        super(encoding, nullable);
        this.multiLine = inMultiLine;
    }

    public String           getBaseName () {
        return ("VARCHAR");
    }

    @Override
    public int              getCode() {
        return T_STRING_TYPE;
    }

    @Override
    public void parseEncoding(String encoding) {
        if (encoding == null)
            throw new IllegalArgumentException("null");

        encoding = encoding.toUpperCase();
        if (encoding.startsWith(ENCODING_ALPHANUMERIC)) {
            try {
                length = extractSize (encoding);                
                encodingType = ALPHANUMERIC;
                return;
            } catch (ParseException e) {
                throw new IllegalArgumentException(encoding);
            }
        }

        for (int i = 0; i < ENCODING.length; i++)
            if (ENCODING[i].equals(encoding)) {
                encodingType = ENCODING_TYPE[i];
                return;
            }
        throw new IllegalArgumentException(encoding);
    }

    @Override
    protected void          assertValidImpl (Object obj) {
        if (!(obj instanceof String))
            throw unsupportedType (obj);
        
        if (getEncodingType () == ALPHANUMERIC) 
            AlphanumericCodec.validate (length, (String) obj);        
    }

    @Override
    protected Object        toBoxedImpl (CharSequence text) {
        return (text.toString ());
    }

    @Override
    protected String        toStringImpl (Object obj) {
        return ((String) obj);
    }
    
    public boolean          isMultiLine () {
        return multiLine;
    }

    public int              getEncodingType () {
        if (encodingType == -1)
            parseEncoding(encoding);
        return encodingType;
    }

    public int getLength() {
        if (length == -1)
            parseEncoding(encoding);
        return length;
    }

    public static String getEncodingAlphanumeric(int length) {
        return FORMAT.format(length);
    }

    public ConversionType isConvertible(DataType to) {
        if (to instanceof CharDataType)
            return ConversionType.Lossy;
        
        if (to instanceof VarcharDataType && ((VarcharDataType)to).getEncodingType() != getEncodingType()) {
            return ((VarcharDataType)to).getEncodingType() == ALPHANUMERIC ? ConversionType.Lossy : ConversionType.Lossless;
        }
        
        return (to instanceof VarcharDataType ? ConversionType.Lossless : ConversionType.NotConvertible);
    }

    @Override
    public void             writeTo (DataOutputStream out) throws IOException {
        out.writeByte (T_STRING_TYPE);

        super.writeTo (out);

        out.writeBoolean (multiLine);
    }

    @Override
    protected void          readFields (
        DataInputStream         in,
        TypeResolver            resolver
    )
        throws IOException
    {
        super.readFields (in, resolver);

        multiLine = in.readBoolean ();
    }
}
