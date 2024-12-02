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
package com.epam.deltix.qsrv.hf.tickdb.lang.pub;

import com.epam.deltix.util.parsers.Location;

/**
 *
 */
public final class DropStreamStatement extends Statement {
    public final Identifier        id;
    public final boolean           ifExists;
    
    public DropStreamStatement (long location, Identifier id, boolean ifExists) {
        super (location);
        
        this.id = id;
        this.ifExists = ifExists;
    }
    
    public DropStreamStatement (Identifier id, boolean ifExists) {
        this (Location.NONE, id, ifExists);
    }
    
    @Override
    public void                     print (StringBuilder s) {
        s.append ("DROP STREAM ");
        if (ifExists) {
            s.append(" IF EXISTS ");
        }
        id.print (s);        
    }        
}
