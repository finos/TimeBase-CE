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

import com.epam.deltix.timebase.messages.Bitmask;
import com.epam.deltix.util.lang.StringUtils;
import com.epam.deltix.util.lang.Util;

import javax.xml.bind.JAXBException;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 *
 */
@XmlRootElement (name = "enumClass")
@XmlType (name = "enumClass")
public final class EnumClassDescriptor extends ClassDescriptor
{
	private static final long serialVersionUID = 1L;
	
    @XmlElement (name = "value")
    private EnumValue []              values;

    @XmlElement (name = "isBitmask")
    private boolean                   bitmask;

    EnumClassDescriptor () { // For JAXB
        values = null;
        bitmask = false;
    }

    protected EnumClassDescriptor(EnumClassDescriptor from) {
        super(from);

        bitmask = from.bitmask;
        values = new EnumValue[from.values.length];
        System.arraycopy(from.values, 0, values, 0, from.values.length);
    }

    /**
     *  Creates a sequential nullable enum descriptor (Java-style).     
     */
    public EnumClassDescriptor (
        String                  name,
        String                  title,
        String ...              values
    )
    {
        this (name, title, false, values);
    }

    /**
     *  Creates an enum descriptor by auto-numbering specified symbolic constants.
     */
    public EnumClassDescriptor (
        String                  name,
        String                  title,
        boolean                 bitmask,
        String ...              values
    )
    {
        this (name, title, bitmask, EnumValue.autoNumber (bitmask, values));
    }

    public EnumClassDescriptor (
        String                  name, 
        String                  title,
        boolean                 bitmask,
        EnumValue ...           values
    )
    { 
        super (name, title);
        
        if (values.length == 0)
            throw new IllegalArgumentException ("Enum must contain at least one constant");
        
        this.bitmask = bitmask;
        this.values = values;
    }
    

    public EnumClassDescriptor (Class <?> cls) {
        super (cls);
        
        int                     num;
        Object []               consts = cls.getEnumConstants ();
        num = consts.length;

        values = new EnumValue [num];

        bitmask = cls.isAnnotationPresent (Bitmask.class);

        for (int ii = 0; ii < num; ii++) {
            Enum        eval = (Enum) consts [ii];

            values [ii] =
                    new EnumValue (
                            eval.name (),
                            bitmask ? (1 << ii) : ii
                    );
        }
    }

    public long             stringToLong (CharSequence symbol) {
        for (EnumValue v : values)
            if (Util.equals (v.symbol, symbol))
                return (v.value);
        
        throw new NumberFormatException ("Unknown constant: " + symbol);
    }    
    
    public boolean          isBitmask () {
        return bitmask;
    }

    public EnumValue []     getValues () {
        return values;
    }

    public String []        getSymbols () {
        int         num = values.length;
        String []   ret = new String [num];

        for (int ii = 0; ii < num; ii++)
            ret [ii] = values [ii].symbol;

        return (ret);
    }

    public String           longToString (long longval) {
        if (bitmask) {
            StringBuilder   sb = new StringBuilder ();

            for (EnumValue v : values)
                if ((v.value & longval) != 0) {
                    if (sb.length () != 0)
                        sb.append ("|");

                    sb.append (v.symbol);
                }

            return (sb.toString ());
        }
        else {
            for (EnumValue v : values)
                if (v.value == longval)
                    return (v.symbol);

            return (String.valueOf (longval));
        }
    }

    public int      computeStorageSize () {
        int         size = 1;

        for (EnumValue v : values) {
            long    n = v.value;

            if (n > Integer.MAX_VALUE || n < Integer.MIN_VALUE) {
                size = 8;
                break;
            }
            else if (size < 4 && (n > Short.MAX_VALUE || n < Short.MIN_VALUE))
                size = 4;
            else if (size < 2 && (n > Byte.MAX_VALUE || n < Byte.MIN_VALUE))
                size = 2;
        }

        return (size);
    }

    @Override
    public void                     writeTo (DataOutputStream out, int serial)
        throws IOException
    {
        out.writeByte (T_ENUM);

        super.writeTo (out, serial);

        out.writeBoolean (bitmask);

        int             n = values.length;

        out.writeShort (n);

        for (int ii = 0; ii < n; ii++) {
            EnumValue   ev = values [ii];

            out.writeUTF (ev.symbol);
            out.writeLong (ev.value);
        }
    }

    public void                 dump (OutputStream os) {
        try {
            UHFJAXBContext.createMarshaller ().marshal (this, os);
        } catch (JAXBException ex) {
            throw new com.epam.deltix.util.io.UncheckedIOException(ex);
        }
    }

    @Override
    protected void                  readFields (
        DataInputStream                 in,
        TypeResolver                    resolver,
        int                             serial
    )
        throws IOException
    {
        super.readFields (in, resolver, serial);

        bitmask = in.readBoolean ();

        int             n = in.readUnsignedShort ();

        values = new EnumValue [n];

        for (int ii = 0; ii < n; ii++) {
            String      symbol = in.readUTF ();
            long        nval = in.readLong ();

            values [ii] = new EnumValue (symbol, nval);
        }
    }

    @Override
    protected void readFieldsWithoutGuid(DataInputStream in, TypeResolver resolver, int serial) throws IOException {
        super.readFieldsWithoutGuid(in, resolver, serial);

        bitmask = in.readBoolean ();

        int             n = in.readUnsignedShort ();

        values = new EnumValue [n];

        for (int ii = 0; ii < n; ii++) {
            String      symbol = in.readUTF ();
            long        nval = in.readLong ();

            values [ii] = new EnumValue (symbol, nval);
        }
    }

    @Override
    public boolean              isEquals(ClassDescriptor target) {
        if (target instanceof EnumClassDescriptor && super.isEquals(target)) {
            EnumClassDescriptor e = (EnumClassDescriptor)target;

            if (bitmask != e.bitmask)
                return false;
            
            if (values.length != e.values.length)
                return false;

            for (int i = 0; i < values.length; i++) {
                if (!Util.xequals(values[i], e.values[i]))
                    return false;
            }

            return true;
        }

        return false;
    }

    @Override
    public String toString() {
        return getName();
    }

    public boolean needsNormalization() {
        if (values == null)
            return false;
        else
            for (EnumValue value : values) {
                if (!StringUtils.isValidJavaIdOrKeyword (value.symbol) ||
                    StringUtils.isJavaReservedWord (value.symbol))
                    return true;
            }

        return false;
    }
}