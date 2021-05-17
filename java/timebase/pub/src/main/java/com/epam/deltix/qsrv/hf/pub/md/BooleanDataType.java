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

import java.io.DataOutputStream;
import java.io.IOException;
import javax.xml.bind.annotation.*;

/**
 *
 */
@XmlType (name = "boolean")
public final class BooleanDataType extends DataType {
	private static final long serialVersionUID = 1L;

    public static final byte NULL = (byte) 0xFF;
    public static final byte TRUE = (byte) 1;
    public static final byte FALSE = (byte) 0;

    public static BooleanDataType getDefaultInstance() {
        return new BooleanDataType(true);
    }

    BooleanDataType () { // For JAXB
        super();
    }

    public BooleanDataType(boolean nullable) {
        super(null, nullable);
    }

    public String           getBaseName () {
        return ("BOOLEAN");
    }

    @Override
    public int              getCode() {
        return T_BOOLEAN_TYPE;
    }

    @Override
    protected void          assertValidImpl (Object obj) {
        if (!(obj instanceof Boolean))
            throw unsupportedType (obj);               
    }

    /**
     *  Convert non-null CharSequence to boolean
     */
    public static boolean   staticParse (CharSequence text) {
        if (text.equals ("true"))
            return (true);
        
        if (text.equals ("false"))
            return (false);

        if (String.valueOf(TRUE).equals(text))
            return true;

        if (String.valueOf(FALSE).equals(text))
            return false;
        
        throw new IllegalArgumentException ("Illegal BOOLEAN value: '" + text + "'");
    }

    /**
     *  Convert non-null CharSequence to byte by parsing it as boolean
     */
    public static byte   parseAsByte (CharSequence text) {
        if (text.equals ("true"))
            return TRUE;

        if (text.equals ("false"))
            return FALSE;

        if (String.valueOf(TRUE).equals(text))
            return TRUE;

        if (String.valueOf(FALSE).equals(text))
            return FALSE;

        throw new IllegalArgumentException ("Illegal BOOLEAN value: '" + text + "'");
    }

    /**
     *  Standard formatter for BOOLEAN.
     */
    public static String    staticFormat (boolean obj) {
        return (obj ? "true" : "false");
    }
    
    @Override
    protected Object        toBoxedImpl (CharSequence text) {
        return (staticParse (text));
    }
    
    @Override
    protected String        toStringImpl (Object obj) {
        return (staticFormat ((Boolean) obj));
    }
    
    public ConversionType isConvertible(DataType to) {
        if (to instanceof VarcharDataType ||to instanceof BooleanDataType ||
                to instanceof IntegerDataType || to instanceof FloatDataType) {
            return ConversionType.Lossless;
        }

        return ConversionType.NotConvertible;
    }

    @Override
    public void             writeTo (DataOutputStream out) throws IOException {
        out.writeByte (T_BOOLEAN_TYPE);

        super.writeTo (out);
    }

/*
    // types, which can be bound with DataType
    private final static Class<?>[] SUPPORTED_TYPES = (IKVMUtil.IS_IKVM) ?
            new Class<?>[]{boolean.class, MdUtil.SByte, QCGStatic.getNullableType(boolean.class), byte.class} :
            new Class<?>[]{boolean.class, byte.class};

    @Override
    public void validateType(Class<?> type) {
        validateType(type, SUPPORTED_TYPES);


 TODO: temporary switch off validation
        if (isNullable() && type == boolean.class)
            throw new IllegalArgumentException("BOOLEAN (nullable) cannot be bound to " + type.getName() + " field");


    }


    @Override
    public void validateTypeStatic(Class<?> type, String staticValue) {
        validateType(type, SUPPORTED_TYPES);

        if (isNullable() && type == boolean.class && staticValue == null)
            throw new IllegalArgumentException(String.format("Cannot store static value null to boolean"));
    }
*/
}
