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
package com.epam.deltix.qsrv.hf.tickdb.lang.pub;

/**
 *
 */
public class CallExpression extends ComplexExpression {
    public final String             name;

    public CallExpression (long location, String name, Expression ... args) {
        super (location, args);
        this.name = name;
    }

    public CallExpression (String name, Expression ... args) {
        this (NO_LOCATION, name, args);
    }

    @Override
    protected void              print (int outerPriority, StringBuilder s) {
        int     n = args.length;
        
        GrammarUtil.escapeIdentifier (NamedObjectType.FUNCTION, name, s);
        s.append (" (");
        printCommaSepArgs (0, args.length, s);
        s.append (")");
    }
    
    @Override
    @SuppressWarnings ("EqualsWhichDoesntCheckParameterClass")
    public boolean                  equals (Object obj) {
        return (
            super.equals (obj) &&
            name.equals (((CallExpression) obj).name)
        );
    }

    @Override
    public int                      hashCode () {
        return (super.hashCode () * 41 + name.hashCode ());
    }
}
