/*
 * Copyright 2024 EPAM Systems, Inc
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
package com.epam.deltix.qsrv.hf.tickdb.lang.pub.constants;

import com.epam.deltix.util.lang.Util;
import com.epam.deltix.util.parsers.CompilationException;

public class LongConstant extends Constant {
    public final long value;

    public LongConstant(long location, long value) {
        super(location);
        this.value = value;
    }

    public LongConstant(long value) {
        this(NO_LOCATION, value);
    }

    public static LongConstant parse(String value) {
        return parse(NO_LOCATION, value, 10);
    }

    public static LongConstant parse(long location, String value) {
        return parse(location, value, 10);
    }

    public static LongConstant parseBitVal(long location, String value) {
        if (!value.toLowerCase().startsWith("0b")) {
            throw new CompilationException("Invalid bit value '" + value + "'", location);
        }

        return parse(location, value.substring(2), 2);
    }

    public static LongConstant parseHexVal(long location, String value) {
        if (!value.toLowerCase().startsWith("0x")) {
            throw new CompilationException("Invalid hex value '" + value + "'", location);
        }

        return parse(location, value.substring(2), 16);
    }

    public static LongConstant parse(long location, String value, int radix) {
        try {
            if (value.toLowerCase().endsWith("l")) {
                value = value.substring(0, value.length() - 1);
            }

            return new LongConstant(location, Long.parseLong(value, radix));
        } catch (NumberFormatException e) {
            throw new CompilationException("Failed to parse numeric value '" + value + "'", location);
        }
    }

    protected void print(int outerPriority, StringBuilder s) {
        s.append(value);
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof LongConstant))
            return false;
        return super.equals(obj) && value == ((LongConstant) obj).value;
    }

    @Override
    public int hashCode() {
        return (super.hashCode() * 41 + Util.hashCode(value));
    }
}
