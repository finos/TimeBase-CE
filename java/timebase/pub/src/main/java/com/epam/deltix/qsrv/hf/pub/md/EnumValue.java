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

import com.epam.deltix.util.lang.StringUtils;
import com.epam.deltix.util.lang.Util;

import java.io.Serializable;

import javax.xml.bind.annotation.XmlElement;

/**
 *
 */
public final class EnumValue implements Serializable {
	private static final long serialVersionUID = 1L;
	
    @XmlElement (name = "symbol")
    public final String             symbol;
    
    @XmlElement (name = "value")
    public final long               value;

    public static EnumValue []      autoNumber (boolean bitmask, String ... symbols) {
        if (bitmask && symbols.length > 64)
            throw new IllegalArgumentException (
                "Too many bitmask values: " + symbols.length
            );

        int                             num = symbols.length;
        EnumValue []                    ret = new EnumValue [num];
        long                            value = bitmask ? 1 : 0;
        
        for (int ii = 0; ii < num; ii++) {
            ret [ii] = new EnumValue (symbols [ii], value);

            if (bitmask)
                value <<= 1;
            else
                value++;
        }

        return (ret);
    }

    /**
     *  For JAXB
     */
    private EnumValue () {      
        symbol = null;
        value = 0;
    }

    public EnumValue (String symbol, long value) {
        this.symbol = symbol;
        this.value = value;
    }        
    
    @Override
    public String                   toString () {
        return (symbol + " = " + value);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        EnumValue enumValue = (EnumValue) o;

        if (value != enumValue.value) return false;
        return Util.xequals(symbol, enumValue.symbol);

    }

    @Override
    public int hashCode() {
        int result;
        result = symbol.hashCode();
        result = 31 * result + (int) (value ^ (value >>> 32));
        return result;
    }

    public String getNormalizedSymbol() {
        return BeanGenerator.escapeIdentifierForJava (symbol);
    }
}