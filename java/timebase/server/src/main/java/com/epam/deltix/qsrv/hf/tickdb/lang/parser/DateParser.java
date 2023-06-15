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
package com.epam.deltix.qsrv.hf.tickdb.lang.parser;

import com.epam.deltix.qsrv.hf.tickdb.lang.errors.IllegalDateLiteralException;
import com.epam.deltix.util.time.GMT;
import java.util.*;
import java.util.regex.*;
import com.epam.deltix.qsrv.hf.tickdb.lang.errors.IllegalDateLiteralException.Field;
import com.epam.deltix.util.time.TimeFormatter;

/**
 *  Compiles DATE ''. This class is not written for high performance 
 *  (allocates a couple of objects while parsing).
 */
public abstract class DateParser {
    private static final Pattern    DATE_PATTERN = 
        Pattern.compile (
            "([0-9][0-9][0-9][0-9])" 
            + "(\\-([01]?[0-9])"     
            + "(\\-([0-3]?[0-9])"
            + "(\\s+([0-2]?[0-9])"    
            + "(\\:([0-5]?[0-9])"
            + "(\\:([0-5]?[0-9])"
            + "(\\.([0-9]+)" 
            + ")?)?)?)?)?)?(\\s+(\\S+))?" 
        );
    // <year>[-<month>[-<day>[ <hour (24)>[:<minute>[:<second>[.<fraction>]]]]]][ <time zone>]
    
//    private static final Pattern    TOD_PATTERN =
//        Pattern.compile (
//            "([0-2]?[0-9])"
//            + "(\\:([0-5]?[0-9])"
//            + "(\\:([0-5]?[0-9])"
//            + "(\\.([0-9]+)"
//            + ")?)?)?"
//        );

    private static final String []  TZIDS = TimeZone.getAvailableIDs ().clone ();
    
    static {
        for (int ii = 0; ii < TZIDS.length; ii++)
            TZIDS [ii] = TZIDS [ii].toLowerCase ();
        
        Arrays.sort (TZIDS);
    }
    
    private static int      gn (long location, String text, Field f, Matcher m, int n, int min, int max) {
        String  s = m.group (n);
                
        if (s == null)
            return (min);
        
        int     ret = Integer.parseInt (s); // canont fail if compliant with the pattern
        
        if (ret > max || ret < min)
            throw new IllegalDateLiteralException (location, text, f);
        
        return (ret);
    }
    
    public static long      parseDateLiteral (long location, String text) {
        Matcher                 matcher = DATE_PATTERN.matcher (text);
        Calendar                cal = Calendar.getInstance ();
        
        if (!matcher.matches ())
            throw new IllegalDateLiteralException (location, text, Field.FORMAT);
        
        cal.set (Calendar.YEAR,         gn (location, text, Field.YEAR,   matcher,  1, 1680, 2262));
        cal.set (Calendar.MONTH,        gn (location, text, Field.MONTH,  matcher,  3,    1,   12) - 1);
        cal.set (Calendar.DAY_OF_MONTH, gn (location, text, Field.DAY,    matcher,  5,    1,   31));
        cal.set (Calendar.HOUR_OF_DAY,  gn (location, text, Field.HOUR,   matcher,  7,    0,   23));
        cal.set (Calendar.MINUTE,       gn (location, text, Field.MINUTE, matcher,  9,    0,   59));
        cal.set (Calendar.SECOND,       gn (location, text, Field.SECOND, matcher, 11,    0,   59));
        cal.set (Calendar.MILLISECOND, 0);
        
        String                  tztext = matcher.group (15);
        
        if (tztext == null)
            cal.setTimeZone (GMT.TZ);
        else {
            String tzone = tztext.toLowerCase();
            
            if (Arrays.binarySearch (TZIDS, tzone) < 0)
                throw new IllegalDateLiteralException (location, text, Field.TIMEZONE);
            
            cal.setTimeZone (TimeZone.getTimeZone (tztext));
        }
        
        long                    ns = cal.getTimeInMillis () * 1000000;
        
        return (parseFractionalPart (matcher, 13, ns));
        
//        return (
//            new CompiledConstant (
//                StandardTypes.CLEAN_TIMESTAMP, 
//                ns / 1000000
//            )
//        );    
    }

    private static long     parseFractionalPart (Matcher matcher, int g, long ns) {
        String                  nstext = matcher.group (g);
        
        if (nstext != null) {
            int                 len = nstext.length ();            
            long                v = 0;
            
            for (int ii = 0; ii < 9; ii++) {
                v = v * 10;
                
                if (ii < len)
                    v += nstext.charAt (ii) - '0';
            }
            
            ns += v; 
        }
        
        return ns;
    }
    
    public static long      parseTimeLiteral (long location, String text) {
        try {
            return TimeFormatter.parseTimeOfDay(text, 1_000_000_000); // nanoseconds
        } catch (NumberFormatException e) {
            throw new IllegalDateLiteralException (location, text, Field.FORMAT);
        }
//
//        Matcher                 matcher = TOD_PATTERN.matcher (text);
//
//        if (!matcher.matches ())
//            throw new IllegalDateLiteralException (location, text, Field.FORMAT);
//
//        long    ns =        gn (location, text, Field.HOUR,   matcher,  1,    0,   23);
//        ns = ns * 60 +      gn (location, text, Field.MINUTE, matcher,  3,    0,   59);
//        ns = ns * 60 +      gn (location, text, Field.SECOND, matcher,  5,    0,   59);
//
//        return (parseFractionalPart (matcher, 7, ns * 1000000000));
    } 
    

}