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
package com.epam.deltix.qsrv.hf.pub;

import com.epam.deltix.util.currency.CurrencyCodeList;

import java.io.IOException;

/**
 *  Converts three-letter codes into a 16-bit number and back.
 */
public class CurrencyCodec {
    private static final int          TEXT_MARKER = 0x8000;
    private static final int          MAX_CODE = 'Z' - 'A';
    
    public static final short   XXX = codeToShort ("XXX");
    public static final short   USD = codeToShort ("USD");
    
    private static char     n2c (int n) {
        n &= 0x1F;
        
        assert n >= 0 && n <= MAX_CODE : "Illegal lower 5 bits in code: " + n;
        
        return ((char) ('A' + n));
    }
    
    private static char     d2c (int n) {
        n %= 10;
        
        assert n >= 0 && n <= 9 : "Illegal digit: " + n;
        
        return ((char) ('0' + n));
    }
    
    public static short     codeToShort (String code) {
        return (codeToShort ((CharSequence) code));
    }
    
    public static short     codeToShort (CharSequence code) {
        return ((short) codeToInt (code));
    }
    
    public static int       codeToInt (CharSequence code) {
        if (code.length () == 3) {
            
            char    c1 = Character.toUpperCase (code.charAt (0));
            char    c2 = Character.toUpperCase (code.charAt (1));
            char    c3 = Character.toUpperCase (code.charAt (2));

            if (c1 >= '0' && c1 <= '9' && c2 >= '0' && c2 <= '9' && c3 >= '0' && c3 <= '9')
                return ((c1 - '0') * 100 + (c2 - '0') * 10 + (c3 - '0'));

            if (c1 >= 'A' && c1 <= 'Z' && c2 >= 'A' && c2 <= 'Z' && c3 >= 'A' && c3 <= 'Z')
                return (
                    TEXT_MARKER |
                    ((c1 - 'A') << 10) |
                    ((c2 - 'A') << 5) |
                    (c3 - 'A')
                );
        }

        throw new NumberFormatException (code.toString ());
    }
    
    public static int       codeToInt (String code) {
        return (codeToInt ((CharSequence) code));
    }

    public static boolean   isNumeric (int n) {
        return ((n & TEXT_MARKER) == 0);
    }

    public static void      intToCode (int n, char [] out) {
        if (isNumeric (n)) {
            assert n >= 0 && n <= 999 : "Illegal numeric code: " + n;
            
            out [0] = d2c (n / 100);
            out [1] = d2c (n / 10);
            out [2] = d2c (n);
        }
        else {            
            out [0] = n2c (n >> 10);
            out [1] = n2c (n >> 5);
            out [2] = n2c (n);            
        }
    }

    public static void      intToCode (int n, Appendable out) {
        if (isNumeric (n)) {
            assert n >= 0 && n <= 999 : "Illegal numeric code: " + n;

            try {
                out.append( d2c (n / 100) );
                out.append( d2c (n / 10) );
                out.append( d2c (n) );
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        else {
            try {
                out.append( n2c (n >> 10) );
                out.append( n2c (n >> 5) );
                out.append( n2c (n) );
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public static String    intToCode (int n) {
        char []     cc = new char [3];
        
        intToCode (n, cc);
        
        return (new String (cc));
    }

    public static CurrencyCodeList.CurrencyInfo getCurrencyCodeByObject (final Object value) {
        if (value != null) {
            final String s = String.valueOf (value);

            if (!s.isEmpty ()) {

                if (!(Character.isLetter (s.charAt (0)))) {
                    try {
                        final int n = Integer.parseInt (s);
                        if ((n & TEXT_MARKER) == 0)
                            return CurrencyCodeList.getInfoByNumeric (n);
                        else
                            return CurrencyCodeList.getInfoBySymbolic (intToCode(n));
                    } catch (final NumberFormatException e) {
                        // wrong decision - text is not a number, then parse as text
                    }
                }

                return CurrencyCodeList.getInfoBySymbolic (s);
            }
        }
        return null;
    }

    public static CurrencyCodeList.CurrencyInfo getCurrencyCode (final int value) {
        if ((value & TEXT_MARKER) == 0)
            return CurrencyCodeList.getInfoByNumeric (value);
        else
            return CurrencyCodeList.getInfoBySymbolic (intToCode(value));
    }

    public static boolean isValidValue (final Object value) {
        return getCurrencyCodeByObject(value) != null;
    }
}