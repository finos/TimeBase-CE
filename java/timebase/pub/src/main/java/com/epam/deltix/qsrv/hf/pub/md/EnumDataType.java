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

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import javax.xml.bind.annotation.*;

/**
 *
 */
@XmlType (name = "enum")
public final class EnumDataType extends DataType {
	private static final long serialVersionUID = 1L;

    // TODO: could I merger this constants?
    public static final int         NULL = -1;
    public static final long        NULL_CODE = -1;

    @XmlIDREF @XmlElement (name = "descriptor")
    public EnumClassDescriptor      descriptor;

    public static EnumDataType getDefaultInstance(EnumClassDescriptor md) {
        return new EnumDataType(true, md);
    }

    EnumDataType () {   // For JAXB
        super();
        descriptor = null; 
    }

    public EnumDataType(boolean nullable, EnumClassDescriptor md) {
        super(null, nullable);
        descriptor = md;
    }   

    public String           getBaseName () {
        return (descriptor.getName ());
    }

    @Override
    public int              getCode() {
        return T_ENUM_TYPE;
    }

    @Override
    protected void          assertValidImpl (Object obj) {
        if (!(obj instanceof Number))
            throw unsupportedType (obj);
        
        long                longval = ((Number) obj).longValue ();
        EnumValue []        values = descriptor.getValues ();        
        
        if (descriptor.isBitmask ()) {
            for (EnumValue v : values) {
                longval &= ~v.value;
                
                if (longval == 0)
                    return;
            }
            
            throw new IllegalArgumentException (
                "Unrecognized bits in " + this + " value: " + longval
            );
        }
        else {
            for (EnumValue v : values)
                if (v.value == longval)
                    return;
            
            throw new IllegalArgumentException (
                "Unrecognized " + this + " value: " + longval
            );
        }
    }
    
    @Override
    protected Object        toBoxedImpl (CharSequence text) {
        return (descriptor.stringToLong (text));
    }
    
    @Override
    protected String        toStringImpl (Object obj) {
        return (descriptor.longToString (((Number) obj).longValue ()));        
    }
    
    public ConversionType isConvertible(DataType to) {
        
        if (to instanceof VarcharDataType) {
            return ConversionType.Lossless;
        } else if (to instanceof EnumDataType) {
            EnumClassDescriptor d = ((EnumDataType) to).descriptor;
            if (descriptor.equals(d)) {
                return ConversionType.Lossless;
            } else {
                if (descriptor.computeStorageSize() != d.computeStorageSize())
                    return ConversionType.Lossy;

                EnumValue[] thisValues = descriptor.getValues();
                EnumValue[] toValues = d.getValues();                
                
                if (thisValues.length > toValues.length)
                    return ConversionType.Lossy;

                for (int i = 0; i < thisValues.length; i++) {
                    if (thisValues[i].value != toValues[i].value)
                        return ConversionType.Lossy;
                }
                return ConversionType.Lossless;
            }
        }

        return ConversionType.NotConvertible;
    }

    @Override
    public void                         writeTo (DataOutputStream out)
        throws IOException
    {
        out.writeByte (T_ENUM_TYPE);

        super.writeTo (out);

        descriptor.writeReference (out);
    }

    @Override
    protected void                      readFields (
        DataInputStream                     in,
        TypeResolver                        resolver
    )
        throws IOException
    {
        super.readFields (in, resolver);

        descriptor = (EnumClassDescriptor) ClassDescriptor.readReference (in, resolver);
    }

    // necessary for Velocity
    public EnumClassDescriptor getDescriptor() {
        return descriptor;
    }
}