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

import com.epam.deltix.util.parsers.Element;
import com.epam.deltix.util.parsers.Location;

/**
 *
 */
public class TypeIdentifier extends Element {
    public final String                 typeName;

    public TypeIdentifier (String typeName) {
        super (NO_LOCATION);
        this.typeName = typeName;
    }

    public TypeIdentifier (long location, String typeName) {
        super (location);
        this.typeName = typeName;
    }

    public TypeIdentifier (TypeIdentifier pack, String typeName, long end) {
        super (Location.fromTo (pack.location, end));
        
        this.typeName = pack.typeName + "." + typeName;
    }

    @Override
    public void                         print (StringBuilder s) {
         GrammarUtil.escapeIdentifier (NamedObjectType.TYPE, typeName, s);
    }
    
    @Override
    @SuppressWarnings ("EqualsWhichDoesntCheckParameterClass")
    public boolean                  equals (Object obj) {
        return (
            super.equals (obj) &&
            typeName.equals (((TypeIdentifier) obj).typeName)
        );
    }

    @Override
    public int                      hashCode () {
        return (super.hashCode () * 41 + typeName.hashCode ());
    }
}
