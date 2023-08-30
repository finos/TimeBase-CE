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

/**
 *  DATE '...' literal.
 */
public final class CharConstant extends Constant {
    public final char             ch;

    public CharConstant (long location, char ch) {
        super (location);
        this.ch = ch;
    }

    public CharConstant(char ch) {
        this(NO_LOCATION, ch);
    }

    protected void      print (int outerPriority, StringBuilder s) {
        if (ch == '\'')
            s.append ("''''C");
        else {
            s.append ('\'');
            s.append (ch);
            s.append ("'C");
        }
    }
    
    @Override
    @SuppressWarnings ("EqualsWhichDoesntCheckParameterClass")
    public boolean                  equals (Object obj) {
        return (
            super.equals (obj) &&
            ch == (((CharConstant) obj).ch)
        );
    }

    @Override
    public int                      hashCode () {
        return (super.hashCode () * 41 + ch);
    }
}