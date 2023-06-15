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
package com.epam.deltix.snmp.smi;

import com.epam.deltix.util.collections.generated.IntegerArrayList;

/**
 *
 */
public final class SMIOID implements Comparable <SMIOID> {
    private final int []        ids;
    private final int           offset;
    private final int           length;
    
    private static int []       cat (SMIOID a, int b) {
        int     n = a.length;
        int []  ret = new int [n + 1];
        
        if (n != 0)
            System.arraycopy (a.ids, a.offset, ret, 0, n);  
        
        ret [n] = b;
        
        return (ret);
    }
    
    public SMIOID (SMIOID prefix, int suffix) {
        this (cat (prefix, suffix));
    }
    
    public SMIOID (int [] ids, int offset, int length) {
        if (ids == null) {
            if (length != 0)
                throw new IllegalArgumentException ("null ids and non=0 length");
        }
        else if (length < 0 || offset < 0 || offset + length > ids.length)
            throw new IllegalArgumentException (
                "offset: " + offset + "; length: " + length + 
                "; ids.length: " + ids.length
            );
        
        for (int ii = 0; ii < length; ii++) {
            int     id = ids [ii + offset];
            
            if (ids [ii] < 0)
                throw new IllegalArgumentException ("Illegal oid component: " + id);
        }
        
        this.offset = offset;
        this.length = length;        
        this.ids = ids;
        
        
    }
        
    public SMIOID (int ... ids) {
        this (ids, 0, ids.length);
    }
    
    public SMIOID () {
        this (null, 0, 0);
    }
    
    public int                  getLength () {
        return (length);
    }
    
    public int                  getId (int idx) {
        return (ids [offset + idx]);
    }
    
    public SMIOID               getSuffix (int suffixLength) {
        return (new SMIOID (ids, offset + length - suffixLength, suffixLength));
    }
    
    public int                  getLast () {
        return (getId (getLength () - 1));
    }

    @Override
    public int                  compareTo (SMIOID that) {
        int         minLength = Math.min (this.length, that.length);
        int         dif = compareTo (that, minLength);
        
        if (dif != 0)
            return (dif);
        
        return (this.length - that.length);
    }

    public boolean              equals (SMIOID that, int length) {
        return (compareTo (that, length) == 0);
    }

    @Override
    public boolean              equals (Object obj) {
        if (obj == this)
            return (true);
        
        if (!(obj instanceof SMIOID))
            return (false);
            
        final SMIOID            that = (SMIOID) obj;
        
        if (that.length != this.length)
            return (false);
        
        return (equals (that, length));
    }

    @Override
    public int                  hashCode () {
        int hash = 7;
        
        for (int ii = 0; ii < length; ii++)
            hash = hash * 47 + ids [ii + offset];
        
        return hash;
    }
    
    public int                  getOffset () {
        return (offset);
    }
    
    public int []               getInternalBuffer () {
        return (ids);
    }
    
    public boolean              startsWith (SMIOID that) {
        return (equals (that, that.length));
    }
    
    public int                  compareTo (SMIOID that, int length) {
        for (int ii = 0; ii < length; ii++) {            
            int a = this.getId (ii);
            int b = that.getId (ii);

            if (a < b)
                return (-1);

            if (a > b)
                return (1);                                    
        }
        
        return (0);
    }
    
    @Override
    public String               toString () {
        if (length == 0) 
            return ("");        
        
        StringBuilder   sb = new StringBuilder ();
        
        sb.append (ids [offset]);
        
        for (int ii = 1; ii < length; ii++) {
            sb.append (".");
            sb.append (ids [offset + ii]);
        }
        
        return (sb.toString ());
    }      
    
    public static SMIOID        valueOf (String text) {
        int                 len = text.length ();
        
        if (len == 0)
            return (new SMIOID ());
        
        IntegerArrayList    ids = new IntegerArrayList ();
        int                 value = 0;
        int                 lastLength = 0;
        
        for (int ii = 0; ii < len; ii++) {
            char            ch = text.charAt (ii);
            
            switch (ch) {
                case '.':
                    if (lastLength == 0)
                        throw new NumberFormatException (
                            "Missing component at position " + (ii + 1) + 
                            " in '" + text + "'"
                        );
                    
                    ids.add (value);
                    value = 0;
                    lastLength = 0;
                    break;
                    
                case '0': case '1': case '2': case '3': case '4': 
                case '5': case '6': case '7': case '8': case '9': 
                    value = value * 10 + ch - '0';
                    lastLength++;
                    break;
                    
                default:
                    throw new NumberFormatException (
                        "Illegal character at position " + (ii + 1) + 
                        " in '" + text + "'"
                    );
            }
        }
        
        if (lastLength == 0)
            throw new NumberFormatException (
                "Missing component at end of '" + text + "'"
            );
        
        ids.add (value);
        
        return (new SMIOID (ids.getInternalBuffer (), 0, ids.size ()));
    }
    
    public static SMIOID        union (SMIOID a, SMIOID b) {
        int         alen = a.getLength ();
        int         blen = b.getLength ();
        int         clen = 0;
        
        for (; clen < alen && clen < blen; clen++)
            if (a.getId (clen) != b.getId (clen))
                break;
        
        if (clen == alen)
            return (a);
        
        if (clen == blen)
            return (b);
        
        return (new SMIOID (a.ids, a.offset, clen));        
    }
}