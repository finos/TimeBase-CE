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
import com.epam.deltix.timebase.messages.SchemaStaticType;

import javax.xml.bind.annotation.XmlElement;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Hashtable;

import static com.epam.deltix.qsrv.hf.pub.util.SerializationUtils.readNullableString;
import static com.epam.deltix.qsrv.hf.pub.util.SerializationUtils.writeNullableString;

/**
 *
 */
public final class StaticDataField extends DataField {
    public final static String STATIC_NULL_VALUE = "<STATIC_NULL>";
    private static final long serialVersionUID = 1L;

    @XmlElement (name = "staticValue")
    private String            staticValue;

    /**
     *  JAXB constructor
     */
    StaticDataField () {  // For JAXB
        super ();        
        staticValue = null;
    }

    /**
     *  Constructs a StaticDataField.
     */
    public StaticDataField (
        String          name,         
        String          title,
        DataType        type,
        String          value
    )
    {
        super (name, title, type);
                        
        type.parse (value);
        
        staticValue = value;
    }

    /**
     *  Constructs a StaticDataField.
     */
    public StaticDataField (
        String          name,         
        String          title,
        DataType        type,
        Object          value
    )
    {
        super (name, title, type);
        
        staticValue = type.toString (value);               
    }
    
    public StaticDataField (StaticDataField template, DataType newType) {
        super (template, newType);
        staticValue = template.staticValue;
    }

    StaticDataField (Field f, ClassAnnotator annotator, DataType inType) {
        super (f, annotator, inType);        
        staticValue = f.getAnnotation (SchemaStaticType.class).value ();
        // workaround for "attribute value must be constant" compilation error
        if (STATIC_NULL_VALUE.equals(staticValue))
            staticValue = null;
    }

    StaticDataField (String inName, Method m, ClassAnnotator annotator, DataType inType) {
        super (inName, m, annotator, inType);
        staticValue = m.getAnnotation (SchemaStaticType.class).value ();
        // workaround for "attribute value must be constant" compilation error
        if (STATIC_NULL_VALUE.equals(staticValue))
            staticValue = null;
    }

    @Override
    public String       toString () {
        return (super.toString () + " = " + staticValue);
    }

    public String       getStaticValue () {
        return staticValue;
    }

    public Object       getBoxedStaticValue () {
        return (getType ().parse (staticValue));
    }

    public boolean      invalidateValue() {
        try {
            getType().parse (staticValue);
        }
        catch (IllegalArgumentException x) {
            staticValue = null;
            return true;
        }
        
        return false;
    }

    @Override
    public void         writeTo (DataOutputStream out, int serial) throws IOException {
        out.writeByte (T_STATIC_FIELD);

        super.writeTo (out, serial);

        writeNullableString (staticValue, out);
    }

    @Override
    protected void      readFields (
        DataInputStream     in,
        TypeResolver        resolver,
        int                 serial
    )
        throws IOException
    {
        super.readFields (in, resolver, serial);

        staticValue = readNullableString (in);
    }

    @Override
    public Hashtable<String, String> getAttributes() {
        return null;
    }

    @Override
    public void setAttributes(Hashtable<String, String> attrs) {

    }
}