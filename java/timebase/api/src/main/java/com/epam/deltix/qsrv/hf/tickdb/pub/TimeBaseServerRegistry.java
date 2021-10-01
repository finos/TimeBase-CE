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
package com.epam.deltix.qsrv.hf.tickdb.pub;

import com.epam.deltix.util.collections.generated.IntegerToObjectHashMap;

/**
 *
 */
public abstract class TimeBaseServerRegistry {
    private static final IntegerToObjectHashMap <DXTickDB>   registry = 
        new IntegerToObjectHashMap <DXTickDB> ();
    
    public static synchronized void      registerServer (int port, DXTickDB instance) {
        if (registry.containsKey (port))
            throw new IllegalArgumentException (
                "TB server on port " + port + " is already registered"
            );
        
        registry.put (port, instance);
    }
    
    public static synchronized void      unregisterServer (int port) {
        if (registry.remove (port, null) == null)
            throw new IllegalArgumentException (
                "TB server on port " + port + " is not registered"
            );        
    }
    
    public static synchronized DXTickDB     getDirectConnection (int port) {
        DXTickDB      impl = registry.get (port, null);
        
        if (impl == null)
            return (null);
        
        return (new DirectTickDBClient (impl));
    }

    public static synchronized DXTickDB     getServer(int port) {
        return registry.get (port, null);
    }

    public static synchronized void clear() {
        registry.clear();
    }
}