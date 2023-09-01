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
import com.epam.deltix.util.lang.Util;

import javax.xml.bind.annotation.XmlElement;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Hashtable;
import java.util.Map;

/**
 *
 */
public abstract class DataField extends NamedDescriptor {

    public static final int  T_STATIC_FIELD =        1;
    public static final int  T_NON_STATIC_FIELD =    2;

	private static final long serialVersionUID = 1L;
	
	@XmlElement (name = "type")
    private DataType          type;

    DataField () {  // For JAXB
        type = null;
    }

    protected DataField (
        String          name,
        String          title,
        DataType        type
    )
    {        
        super (name, title);
        this.type = type;
    }
    
    protected DataField (DataField template, DataType newType) {
        super (template.getName(), template.getTitle());

        this.type = newType;
    }

    /**
     *  Used by Introspector.
     */
    protected DataField (
        Field               f,
        ClassAnnotator      annotator,
        DataType            inType
    )
    {
        super (f.getName (), annotator.getTitle (f));
        
        setDescription (annotator.getDescription (f));
        
        type = inType;        
    }

    /**
     *  Used by Introspector.
     */
    protected DataField (
            String              name,
            Method              method,
            ClassAnnotator      annotator,
            DataType            inType
    )
    {
        super (name, annotator.getTitle (method, name));

        setDescription (annotator.getDescription (method, name));

        type = inType;
    }

    @Override
    public String                   toString () {
        return (type + " " + getName () + " \"" + getTitle () + "\"");
    }

    public final DataType           getType () {
        return type;
    }

    @Override
    public void                     writeTo (DataOutputStream out, int serial)
        throws IOException
    {
        super.writeTo (out, serial);
        type.writeTo (out);
    }

    protected void                  readFields (
        DataInputStream                 in,
        TypeResolver                    resolver,
        int                             serial
    )
        throws IOException
    {
        super.readFields (in, serial);
        type = DataType.readFrom (in, resolver);
    }

    public static DataField         readFrom (DataInputStream in, TypeResolver resolver, int serial)
        throws IOException
    {
        int                 tag = in.readUnsignedByte ();
        DataField           df;

        switch (tag) {
            case T_STATIC_FIELD:        df = new StaticDataField (); break;
            case T_NON_STATIC_FIELD:    df = new NonStaticDataField (); break;
            default:                    throw new IOException ("Illegal tag: " + tag);
        }

        df.readFields (in, resolver, serial);
        return (df);
    }

    public boolean                  isEquals(DataField target) {

        if (target != null) {
            if (!Util.xequals(getName(), target.getName()))
                return false;

            if (getType().getClass() != target.getType().getClass())
                return false;
            else if (!Util.xequals(getType().getEncoding(), target.getType().getEncoding()))
                return false;
            else if (!Util.xequals(getType().isNullable(), target.getType().isNullable()))
                return false;

            return true;
        }

        return Util.xequals(this, target);
    }

    /*
        Returns custom attributes associated with this field.
     */
    public abstract Map<String, String> getAttributes();

    /*
        Set custom attributes for this instance.
     */
    public abstract void                setAttributes(Map<String, String> attrs);
}