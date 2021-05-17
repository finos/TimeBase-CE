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
import com.epam.deltix.util.lang.Util;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlIDREF;
import javax.xml.bind.annotation.XmlType;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

/**
 *
 */
@XmlType(name = "object")
public final class ClassDataType extends DataType {
    @XmlIDREF
    @XmlElement(name = "types")
    private RecordClassDescriptor[] types;

    ClassDataType () {
        // For deserialization
    }

    public ClassDataType (boolean nullable, RecordClassDescriptor ... types) {
        super (null, nullable);
        this.types = types;
    }

    public RecordClassDescriptor []     getDescriptors () {
        return types;
    }

    public boolean                      isFixed () {
        return (types.length == 1);
    }
    
    public RecordClassDescriptor        getFixedDescriptor () {
        if (types.length != 1)
            throw new IllegalStateException ("#types = " + types.length);

        return types [0];
    }

    @Override
    public String                       getBaseName () {
        return ("OBJECT");
    }

    @Override
    public int                          getCode() {
        return T_OBJECT_TYPE;
    }

    @Override
    public ConversionType               isConvertible (DataType to) {
        return (ConversionType.NotConvertible);
    }

    @Override
    protected void                      assertValidImpl (Object obj) {
        throw new UnsupportedOperationException ("Not supported yet.");
    }

    @Override
    protected Object                    toBoxedImpl (CharSequence text) {
        throw new UnsupportedOperationException ("Not supported yet.");
    }

    @Override
    protected String                    toStringImpl (Object obj) {
        throw new UnsupportedOperationException ("Not supported yet.");
    }   

    @Override
    public void                         writeTo (DataOutputStream out)
        throws IOException
    {
        out.writeByte (T_OBJECT_TYPE);

        super.writeTo (out);

        int     n = types.length;

        out.writeShort (n);

        for (int ii = 0; ii < n; ii++)
            types [ii].writeReference (out);
    }

    @Override
    protected void                      readFields (
        DataInputStream                     in,
        TypeResolver                        resolver
    )
        throws IOException
    {
        super.readFields (in, resolver);

        int         n = in.readUnsignedShort ();

        types = new RecordClassDescriptor [n];

        for (int ii = 0; ii < n; ii++)
            types [ii] = (RecordClassDescriptor) ClassDescriptor.readReference (in, resolver);
    }

    @Override
    public String toString() {
        return "ClassDataType:" + Util.printArray(types);
    }
}
