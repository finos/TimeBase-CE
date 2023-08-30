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
package com.epam.deltix.snmp.script;

import com.epam.deltix.snmp.mtree.*;
import org.mozilla.javascript.Scriptable;

/**
 * 
 */
public class SSContainer <T extends MOContainer> extends SSNode <T> {    
    SSContainer (
        SSRoot                  root,
        T                       mo
    )
    {
        super (root, mo);        
    }
    
    @Override
    public Object       get (String name, Scriptable start) {
        assert start == this;
        
        return (root.wrap (mo.getNodeByName (name)));
    }

    @Override
    public Object       get (int id, Scriptable start) {
        assert start == this;
        
        return (root.wrap (mo.getDirectChildById (id)));
    }

    @Override
    public boolean      has (String name, Scriptable start) {
        assert start == this;
        
        return (mo.getNodeByName (name) != null);
    }

    @Override
    public boolean      has (int id, Scriptable start) {
        assert start == this;
        
        return (mo.getDirectChildById (id) != null);
    }

    @Override
    public Object []    getIds () { 
        return (mo.getDirectChildIds ());
    }               
}