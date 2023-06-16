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
package com.epam.deltix.qsrv.hf.tickdb.lang.compiler.sem;

import com.epam.deltix.qsrv.hf.pub.md.RecordClassDescriptor;
import com.epam.deltix.util.parsers.Location;
import com.epam.deltix.qsrv.hf.tickdb.lang.pub.*;
import com.epam.deltix.qsrv.hf.tickdb.lang.errors.*;
import java.util.*;

/**
 *
 */
public class EnvironmentFrame implements Environment {    
    private static final Object             AMBIGUOUS = new Object ();

    private static final class Key {
        NamedObjectType     type;
        String              name;

        Key () {            
        }
        
        Key (NamedObjectType type, String name) {
            this.type = type;
            this.name = name;
        }
        
        @Override
        @SuppressWarnings ("EqualsWhichDoesntCheckParameterClass")
        public boolean      equals (Object obj) {
            if (obj == null)
                return false;

            final Key           other = (Key) obj;
            return (this.type == other.type && this.name.equals (other.name));
        }

        @Override
        public int          hashCode () {
            int                 hash = 5;
            hash = 97 * hash + this.type.hashCode ();
            hash = 97 * hash + this.name.hashCode ();
            return hash;
        }                
    }
    
    private final Environment               parent;
    private final Map <Key, Object>         local = new HashMap <Key, Object> ();
    private final Map <Key, Object>         cilocal = new HashMap <Key, Object> ();
    private Key                             buffer = new Key ();
    private RecordClassDescriptor[]         topTypes;
    
    public EnvironmentFrame () {
        this (null);
    }

    public EnvironmentFrame (Environment parent) {
        this.parent = parent;
    }

    public void                 setTopTypes(RecordClassDescriptor[] topTypes) {
        this.topTypes = topTypes;
    }

    public RecordClassDescriptor[] getTopTypes() {
        return topTypes;
    }

    public Object               lookUpExactLocal (NamedObjectType type, String id) {
        buffer.type = type;
        buffer.name = id;
        
        return (local.get (buffer));
    }
    
    public Object               lookUp (NamedObjectType type, String id, long location) {
        buffer.type = type;
        buffer.name = id;
        
        Object          obj = local.get (buffer);

        if (obj == AMBIGUOUS)
            throw new AmbiguousIdentifierException (id, location);

        if (obj != null)
            return (obj);

        buffer.name = id.toUpperCase ();
        obj = cilocal.get (buffer);

        if (obj == AMBIGUOUS)
            throw new AmbiguousIdentifierException (id, location);

        if (obj != null)
            return (obj);

        if (parent == null)
            throw new UnknownIdentifierException (id, location);
        
        return (parent.lookUp (type, id, location));
    }

    public void              bindNoDup (
        TypeIdentifier          id, 
        Object                  value
    )
    {        
        bind (NamedObjectType.TYPE, id.typeName, id.location, value, false);
    }
    
    public void              bindNoDup (
        Identifier              id, 
        Object                  value
    )
    {        
        bind (NamedObjectType.VARIABLE, id.id, id.location, value, false);
    }
    
    public void              bindNoDup (
        NamedObjectType         type, 
        String                  id, 
        long                    location,
        Object                  value
    )
    {        
        bind (type, id, location, value, false);
    }
    
    public void              bind (
        NamedObjectType         type,
        String                  id,
        Object                  value
    )
    {
        bind (type, id, Location.NONE, value, true);
    }
    
    private void            bind (
        NamedObjectType         type, 
        String                  id, 
        long                    location,
        Object                  value,
        boolean                 dupOk
    )
    {
        Key         key = new Key (type, id);
        
        if (!dupOk && local.containsKey (key))
            throw new DuplicateIdentifierException (id, location);
        
        Object      prev = local.put (key, value);

        if (prev != null)
            local.put (key, AMBIGUOUS);

        Key         ukey = new Key (type, id.toUpperCase ());

        prev = cilocal.put (ukey, value);

        if (prev != null)
            cilocal.put (ukey, AMBIGUOUS);
    }

    public boolean unbind(NamedObjectType type, String id) {
        Key key = new Key(type, id);

        boolean removed = false;
        if (local.containsKey(key)) {
            local.remove(key);
            removed = true;
        }

        Key ukey = new Key(type, id.toUpperCase());
        if (cilocal.containsKey(ukey)) {
            cilocal.remove(ukey);
            removed = true;
        }

        return removed;
    }
}