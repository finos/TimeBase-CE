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
package com.epam.deltix.qsrv.hf.tickdb.lang.runtime.selectors;

import com.epam.deltix.dfp.Decimal;
import com.epam.deltix.dfp.Decimal64Utils;
import com.epam.deltix.qsrv.hf.pub.codec.AlphanumericCodec;
import com.epam.deltix.util.lang.Util;
import com.epam.deltix.util.memory.MemoryDataInput;

/**
 *  Nullable CharSequence.
 */
public final class Varchar {
    private CharSequence            value = null;        
    private RuntimeBuilder sb = null;

    private final static class      RuntimeBuilder implements CharSequence {

        StringBuilder           value;

        public RuntimeBuilder() {
            this.value = new StringBuilder();
        }

        public RuntimeBuilder(CharSequence arg) {
            this.value = new StringBuilder(arg);
        }

        @Override
        public int              length() {
            return value.length();
        }

        public void             setLength(int length) {
            value.setLength(length);
        }

        @Override
        public char             charAt(int index) {
            return value.charAt(index);
        }

        public void             append(CharSequence arg) {
            value.append(arg);
        }

        @Override
        public CharSequence     subSequence(int start, int end) {
            return value.subSequence(start, end);
        }

        @Override
        public boolean          equals(Object obj) {
            if (obj instanceof Varchar)
                return Util.equals(value, ((Varchar)obj).value);

            if (obj instanceof CharSequence)
                return Util.equals(value, (CharSequence)obj);

            return super.equals(obj);
        }

        @Override
        public int              hashCode() {
            return Util.hashCode(value);
        }
    }
            
    public CharSequence             get () {
        return (value);
    }

    public StringBuilder            getStringBuilder() {
        if (sb == null) {
            sb = new RuntimeBuilder();
        }
        value = sb;
        return sb.value;
    }
    
    public void                     set (CharSequence arg) {        
        if (arg instanceof String)
            set ((String) arg);
        else if (arg == null) 
            value = null;
        else {
            if (sb == null)
                sb = new RuntimeBuilder (arg);
            else {
                sb.setLength (0);
                sb.append (arg);
            }
            
            value = sb;
        }
    }
    
    public void                     set (String arg) {
        value = arg;
    }

    public void                     readAlphanumeric (
        MemoryDataInput                 in, 
        int                             numSizeBits, 
        int                             n
    )
    {
        if (sb == null)
            sb = new RuntimeBuilder();

        if (AlphanumericCodec.staticRead (in, numSizeBits, n, sb.value) != null)
            this.value = sb;
        else
            this.value = null;
    }

    @Override
    public boolean                  equals(Object obj) {
        if (obj instanceof Varchar)
            return Util.equals(value, ((Varchar)obj).value);

        if (obj instanceof CharSequence)
            return Util.equals(value, (CharSequence)obj);

        return super.equals(obj);
    }

    @Override
    public int                      hashCode() {
        return Util.hashCode(value);
    }
}