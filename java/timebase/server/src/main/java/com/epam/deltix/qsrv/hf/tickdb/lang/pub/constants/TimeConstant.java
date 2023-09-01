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
package com.epam.deltix.qsrv.hf.tickdb.lang.pub.constants;

import com.epam.deltix.qsrv.hf.tickdb.lang.parser.DateParser;
import com.epam.deltix.qsrv.hf.tickdb.lang.pub.GrammarUtil;

/**
 *  '...'T literal.
 */
public final class TimeConstant extends Constant {
    public final long               nanoseconds;
    public final String             text;

    public TimeConstant (long location, String text) {
        super (location);
        
        this.text = text;
        this.nanoseconds = DateParser.parseTimeLiteral (location, text);
    }

    protected void      print (int outerPriority, StringBuilder s) {
        GrammarUtil.escapeStringLiteral (text, s);
        s.append ('T');
    }
    
    @Override
    @SuppressWarnings ("EqualsWhichDoesntCheckParameterClass")
    public boolean                  equals (Object obj) {
        return (
            super.equals (obj) &&
            text.equals (((TimeConstant) obj).text)
        );
    }

    @Override
    public int                      hashCode () {
        return (super.hashCode () * 41 + text.hashCode ());
    }
}