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

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

/**
 *
 */
@XmlType(name = "array")
public class ArrayDataType extends DataType {

    @XmlElement(name = "type")
    private DataType dataType;

    ArrayDataType () {
        // For deserialization
    }

    public ArrayDataType(boolean nullable, DataType dataType) {
        super(null, nullable);
        this.dataType = dataType;
    }

    public DataType getElementDataType() {
        return dataType;
    }

    @Override
    public ConversionType isConvertible(DataType to) {

        if (to instanceof ArrayDataType)
            return dataType.isConvertible(((ArrayDataType)to).dataType);

        return (ConversionType.NotConvertible);
    }

    @Override
    protected Object toBoxedImpl(CharSequence text) {
        throw new UnsupportedOperationException ("Not supported yet.");
    }

    @Override
    protected String toStringImpl(Object obj) {
        throw new UnsupportedOperationException ("Not supported yet.");
    }

    @Override
    protected void assertValidImpl(Object obj) {
        throw new UnsupportedOperationException ("Not supported yet.");
    }

    @Override
    public String   getBaseName() {
        return ("ARRAY");
    }

    @Override
    public int      getCode() {
        return T_ARRAY_TYPE;
    }

    @Override
    public String toString() {
        return ("ArrayDataType: " + dataType);
    }

    @Override
    public void writeTo(DataOutputStream out) throws IOException {
        out.writeByte(T_ARRAY_TYPE);
        super.writeTo (out);

        dataType.writeTo(out);
    }

    @Override
    protected void readFields(DataInputStream in, ClassDescriptor.TypeResolver resolver) throws IOException {
        super.readFields (in, resolver);

        dataType = DataType.readFrom (in, resolver);
    }
}
