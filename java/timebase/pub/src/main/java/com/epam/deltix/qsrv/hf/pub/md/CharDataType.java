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

import java.io.DataOutputStream;
import java.io.IOException;
import javax.xml.bind.annotation.*;

/**
 *
 */
@XmlType (name = "char")
public final class CharDataType extends DataType {
	private static final long serialVersionUID = 1L;
	
    public static final char NULL = 0;

    public static CharDataType getDefaultInstance() {
        return new CharDataType(true);
    }

    CharDataType () { // For JAXB
        super();
    }

    public CharDataType(boolean nullable) {
        super(null, nullable);
    }

    public String           getBaseName () {
        return ("CHAR");
    }

    @Override
    public int              getCode() {
        return T_CHAR_TYPE;
    }

    @Override
    protected void          assertValidImpl (Object obj) {
        if (!(obj instanceof Character))
            throw unsupportedType (obj);               
    }
    
    /**
     *  Convert non-null CharSequence to float
     */
    public static char      staticParse (CharSequence text) {
        if (text.length () != 1)
            throw new IllegalArgumentException ("Not a char: '" + text + "'");
        
        return (text.charAt (0));
    }        
    
    public static String    staticFormat (char c) {
        return (new String (new char [] { c }));
    }
    
    @Override
    protected Object        toBoxedImpl (CharSequence text) {
        return (staticParse (text));
    }
    
    @Override
    protected String        toStringImpl (Object obj) {
        return (staticFormat ((Character) obj));
    }
   
    public ConversionType isConvertible(DataType to) {

        if (to instanceof VarcharDataType || to instanceof CharDataType) {
            return ConversionType.Lossless;
        }
        
        return ConversionType.NotConvertible;
    }

    @Override
    public void             writeTo (DataOutputStream out) throws IOException {
        out.writeByte (T_CHAR_TYPE);

        super.writeTo (out);
    }
}