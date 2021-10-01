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

import com.epam.deltix.util.time.GMT;
import java.io.DataOutputStream;
import java.io.IOException;
import java.text.ParseException;
import javax.xml.bind.annotation.*;

/**
 *
 */
@XmlType (name = "dateTime")
public final class DateTimeDataType extends DataType {
	private static final long serialVersionUID = 1L;
	
    public static final long NULL = Long.MIN_VALUE;

    public static DateTimeDataType getDefaultInstance() {
        return new DateTimeDataType(true);
    }

    DateTimeDataType () { // For JAXB
        super();
    }

    public DateTimeDataType(boolean nullable) {
        super(null, nullable);
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
    
    /**
     *  Convert non-null CharSequence to long (milliseconds) by parsing it as
     *  canonical representation in GMT
     */
    public static long      staticParse (CharSequence text) {
        String  s = text.toString ();
        
        try {            
            return (GMT.parseDateTimeMillis (s).getTime ());
        } catch (ParseException x) {
            throw new NumberFormatException ("Illegal date: " + s);
        }
    }        
    
    public static String    staticFormat (long obj) {
        return (GMT.formatDateTimeMillis (obj));
    }
    
    @Override
    protected Object        toBoxedImpl (CharSequence text) {
        return (staticParse (text));
    }
    
    @Override
    protected String        toStringImpl (Object obj) {
        return (staticFormat ((Long) obj));
    }

    public ConversionType isConvertible(DataType to) {
        if (to instanceof VarcharDataType) {
            return ConversionType.Lossless;
        } else if (to instanceof FloatDataType) {
            return ((FloatDataType) to).check(0, Long.MAX_VALUE) ? ConversionType.Lossless : ConversionType.Lossy;
        } else if (to instanceof DateTimeDataType || to instanceof IntegerDataType) {
            return ConversionType.Lossy;
        }

        return ConversionType.NotConvertible;
    }

    @Override
    public void             writeTo (DataOutputStream out) throws IOException {
        out.writeByte (T_DATE_TIME_TYPE);

        super.writeTo (out);
    }
}