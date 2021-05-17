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
package com.epam.deltix.snmp.script;

import com.epam.deltix.snmp.mtree.*;

/**
 *
 */
public abstract class SSNode <T extends MONode> extends AbstractScriptable {
    protected final SSRoot      root;
    protected final T           mo;
    
    protected SSNode (SSRoot root, T mo) {
        super ("Node");
        
        this.root = root;
        this.mo = mo;        
    }

    public Object       getValue () {
        return (this);
    }        
        
    @Override
    public String       toString () {
        return ("SSNode (" + mo.toString () + ")");
    }        
}
