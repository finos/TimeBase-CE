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
package com.epam.deltix.qsrv.hf.tickdb.lang.pub;

import com.epam.deltix.util.parsers.Element;
import com.epam.deltix.util.parsers.Location;

/**
 *
 */
public final class EnumValueDef extends Element {
    public final Identifier     id;
    public final Expression     value;

    public EnumValueDef (long location, Identifier id, Expression value) {
        super (location);
        this.id = id;
        this.value = value;
    }

    public EnumValueDef (Identifier id, Expression value) {
        this (Location.NONE, id, value);
    }
    
    @Override
    public void             print (StringBuilder s) {
        id.print (s);
        
        if (value != null) {
            s.append (" = ");
            value.print (s);
        }
    }        
}