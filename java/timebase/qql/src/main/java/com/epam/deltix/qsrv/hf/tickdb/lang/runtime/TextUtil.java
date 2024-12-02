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
package com.epam.deltix.qsrv.hf.tickdb.lang.runtime;

public class TextUtil {

    private static final long  LONG_SIGN_BIT           = 0x8000000000000000L;
    private static final long  DOUBLE_BIAS_EXP         = 1023;
    private static final long  DOUBLE_MANTISSA_WIDTH   = 52;
    private static final long  DOUBLE_ASSUMED_BIT      = 1L << DOUBLE_MANTISSA_WIDTH;
    private static final long  DOUBLE_MANTISSA_BITMASK = DOUBLE_ASSUMED_BIT - 1;
    private static final long  DOUBLE_NORM_EXP         = DOUBLE_BIAS_EXP + DOUBLE_MANTISSA_WIDTH;
    private static final long  DOUBLE_OVERFLOW_BITMASK = ~DOUBLE_MANTISSA_BITMASK - DOUBLE_ASSUMED_BIT;
    private static final int   INT_SIGN_BIT            = 0x80000000;
    private static final int   FLOAT_BIAS_EXP          = 127;
    private static final int   FLOAT_MANTISSA_WIDTH    = 23;
    private static final int   FLOAT_ASSUMED_BIT       = 1 << FLOAT_MANTISSA_WIDTH;
    private static final int   FLOAT_MANTISSA_BITMASK  = FLOAT_ASSUMED_BIT - 1;
    private static final int   FLOAT_NORM_EXP          = FLOAT_BIAS_EXP + FLOAT_MANTISSA_WIDTH;
    private static final int   FLOAT_OVERFLOW_BITMASK  = ~FLOAT_MANTISSA_BITMASK - FLOAT_ASSUMED_BIT;

    public static double parseDouble(CharSequence sc) {
        return parseDouble(sc, 0, sc.length());
    }

    public static double parseDouble(final CharSequence sc, final int startIncl, final int endExcl) {
        if (startIncl == endExcl) {
            throw new NumberFormatException("Empty string");
        }

        int     pos         = startIncl;
        long    numerator   = 0;
        double  denominator = 1;
        long    sign        = 0;
        boolean dotSeen     = false;
        boolean overflow    = false;
        char    ch          = sc.charAt(pos);

        if ((ch == '+') || (ch == '-')) {
            if (ch == '-') {
                sign = LONG_SIGN_BIT;
            }

            pos++;

            if (pos == endExcl) {
                throw new NumberFormatException(new StringBuilder().append(sc, startIncl, endExcl).toString());
            }

            ch = sc.charAt(pos);
        }

        for (;;) {
            if (ch != ',') {
                if (!dotSeen && (ch == '.')) {
                    dotSeen = true;
                } else {
                    final int digit = ch - '0';

                    if ((digit < 0) || (digit > 9)) {
                        if ((sc.length() == 3)
                            && (sc.charAt(0) == 'N')
                            && (sc.charAt(1) == 'a')
                            && (sc.charAt(2) == 'N')) {

                            return Double.NaN;
                        }

                        throw new NumberFormatException("Illegal digit at position " + (pos + 1) + " in: "
                            + new StringBuilder().append(sc,
                            startIncl,
                            endExcl).toString());
                    }

                    if (overflow) {
                        // Stop shifting the numerator
                        if (!dotSeen) {
                            denominator *= 0.1;
                        }
                    } else {
                        numerator = numerator * 10 + digit;

                        if (dotSeen) {
                            denominator *= 10;
                        }

                        if (numerator >= DOUBLE_ASSUMED_BIT) {
                            overflow = true;
                        }
                    }
                }
            }

            pos++;

            if (pos == endExcl) {
                break;
            }

            ch = sc.charAt(pos);
        }

        if (numerator == 0) {
            return (0.0);
        }

        // Build the double first, ignoring the denominator
        long exp = DOUBLE_NORM_EXP;

        if (overflow) {
            while ((numerator & DOUBLE_OVERFLOW_BITMASK) != 0) {
                exp++;
                numerator >>>= 1;
            }
        } else {
            while ((numerator & DOUBLE_ASSUMED_BIT) == 0) {
                exp--;
                numerator <<= 1;
            }
        }

        numerator &= DOUBLE_MANTISSA_BITMASK;

        final long bits   = sign | (exp << DOUBLE_MANTISSA_WIDTH) | numerator;
        double     result = Double.longBitsToDouble(bits);

        if (denominator != 1) {
            result /= denominator;
        }

        return result;
    }

    public static float parseFloat(CharSequence sc) {
        return parseFloat(sc, 0, sc.length());
    }

    public static float parseFloat(final CharSequence sc, final int startIncl, final int endExcl) {
        if (startIncl == endExcl) {
            throw new NumberFormatException("Empty string");
        }

        int     pos         = startIncl;
        int     numerator   = 0;
        float   denominator = 1;
        int     sign        = 0;
        boolean dotSeen     = false;
        boolean overflow    = false;
        char    ch          = sc.charAt(pos);

        if ((ch == '+') || (ch == '-')) {
            if (ch == '-') {
                sign = INT_SIGN_BIT;
            }

            pos++;

            if (pos == endExcl) {
                throw new NumberFormatException(new StringBuilder().append(sc, startIncl, endExcl).toString());
            }

            ch = sc.charAt(pos);
        }

        for (;;) {
            if (ch != ',') {
                if (!dotSeen && (ch == '.')) {
                    dotSeen = true;
                } else {
                    final int digit = ch - '0';

                    if ((digit < 0) || (digit > 9)) {
                        if ((sc.length() == 3)
                            && (sc.charAt(0) == 'N')
                            && (sc.charAt(1) == 'a')
                            && (sc.charAt(2) == 'N')) {

                            return Float.NaN;
                        }

                        throw new NumberFormatException("Illegal digit at position " + (pos + 1) + " in: "
                            + new StringBuilder().append(sc,
                            startIncl,
                            endExcl).toString());
                    }

                    if (overflow) {
                        // Stop shifting the numerator
                        if (!dotSeen) {
                            denominator *= 0.1;
                        }
                    } else {
                        numerator = numerator * 10 + digit;

                        if (dotSeen) {
                            denominator *= 10;
                        }

                        if (numerator >= FLOAT_ASSUMED_BIT) {
                            overflow = true;
                        }
                    }
                }
            }

            pos++;

            if (pos == endExcl) {
                break;
            }

            ch = sc.charAt(pos);
        }

        if (numerator == 0) {
            return 0.0f;
        }

        // Build the double first, ignoring the denominator
        int exp = FLOAT_NORM_EXP;

        if (overflow) {
            while ((numerator & FLOAT_OVERFLOW_BITMASK) != 0) {
                exp++;
                numerator >>>= 1;
            }
        } else {
            while ((numerator & FLOAT_ASSUMED_BIT) == 0) {
                exp--;
                numerator <<= 1;
            }
        }

        numerator &= FLOAT_MANTISSA_BITMASK;

        final int bits   = sign | (exp << FLOAT_MANTISSA_WIDTH) | numerator;
        float     result = Float.intBitsToFloat(bits);

        if (denominator != 1) {
            result /= denominator;
        }

        return result;
    }

}
