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
package com.epam.deltix.qsrv.hf.tickdb.lang.pub.constants;

import com.epam.deltix.dfp.Decimal;
import com.epam.deltix.dfp.Decimal64;
import com.epam.deltix.dfp.Decimal64Utils;
import com.epam.deltix.util.lang.Util;
import com.epam.deltix.util.parsers.Element;

/**
 *
 */
public final class FloatConstant extends Constant {

    public final long significand;
    public final int exponent;

    private final boolean isDecimal64;

    public FloatConstant(long location, long significand, int exponent, boolean isDecimal64) {
        super(location);
        this.significand = significand;
        this.exponent = exponent;
        this.isDecimal64 = isDecimal64;
    }

    public FloatConstant(long significand, int exponent, boolean isDecimal64) {
        this(Element.NO_LOCATION, significand, exponent, isDecimal64);
    }

    public static FloatConstant parseDecimal(CharSequence text) {
        return parseText(Element.NO_LOCATION, text, true);
    }

    public static FloatConstant parseDecimal(long location, CharSequence text) {
        return parseText(location, text, true);
    }

    public static FloatConstant parseDouble(CharSequence text) {
        return parseText(Element.NO_LOCATION, prepareDouble(text), false);
    }

    public static FloatConstant parseDouble(long location, CharSequence text) {
        return parseText(location, prepareDouble(text), false);
    }

    private static CharSequence prepareDouble(CharSequence text) {
        return text.charAt(text.length() - 1) == 'f' ? text.subSequence(0, text.length() - 1): text;
    }

    public static FloatConstant parseText(long location, CharSequence text, boolean isDecimal) {
        long s = 0;
        int eadd = 0;
        int e = 0;
        boolean dot = false;
        boolean exp = false;
        boolean expIsNegative = false;
        int len = text.length();

        for (int ii = 0; ii < len; ii++) {
            char c = text.charAt(ii);

            if (!exp && c == '.')
                dot = true;
            else if (!exp && (c == 'e' || c == 'E')) {
                exp = true;

                c = text.charAt(ii + 1);

                if (c == '-') {
                    ii++;
                    expIsNegative = true;
                } else if (c == '+')
                    ii++;
            } else if (c >= '0' && c <= '9') {
                int d = c - '0';

                if (exp)
                    e = e * 10 + d;
                else if (dot && s == 0 && d == 0)   // 0.000000xxx
                    eadd--;
                else {
                    s = s * 10 + d;

                    if (dot)
                        eadd--;
                }
            } else
                throw new IllegalArgumentException(text.toString());
        }

        if (expIsNegative)
            e = -e;

        e += eadd;

        if (s == 0) // small correction for things such as .0
            e = 0;
        else        //  strip trailing zeroes
            while (s % 10 == 0) {
                s /= 10;
                e++;
            }

        return (new FloatConstant(location, s, e, isDecimal));
    }

    public double toDouble() {
        return (Double.parseDouble(toFloatString()));
    }

    public Decimal64 toDecimal64() {
        return Decimal64.parse(toFloatString());
    }

    @Decimal
    public long toDecimalLong() {
        return Decimal64Utils.parse(toFloatString());
    }

    public String toFloatString() {
        if (exponent == 0)
            return (String.valueOf(significand));
        else
            return (significand + "E" + exponent);
    }

    @Override
    protected void print(int outerPriority, StringBuilder s) {
        s.append(toFloatString());
    }

    @Override
    @SuppressWarnings("EqualsWhichDoesntCheckParameterClass")
    public boolean equals(Object obj) {
        return (
                super.equals(obj) &&
                        significand == ((FloatConstant) obj).significand &&
                        exponent == ((FloatConstant) obj).exponent
        );
    }

    @Override
    public int hashCode() {
        return (
                (super.hashCode() * 41 + Util.hashCode(significand)) * 31 + exponent
        );
    }

    public boolean isDecimal64() {
        return isDecimal64;
    }
}