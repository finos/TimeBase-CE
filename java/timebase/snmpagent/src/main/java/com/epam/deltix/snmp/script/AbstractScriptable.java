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

import org.mozilla.javascript.Scriptable;

/**
 *
 */
public class AbstractScriptable implements Scriptable {
    private final String    className;
    private Scriptable      prototype, parent;

    public AbstractScriptable (String className) {
        this.className = className;
    }

    public void             delete (String name) {
        throw new UnsupportedOperationException ();
    }

    public void             delete (int i) {
        throw new UnsupportedOperationException ();
    }

    public Object           get (String name, Scriptable start) {
        return (NOT_FOUND);
    }

    public Object           get (int i, Scriptable start) {
        return (NOT_FOUND);
    }

    public String           getClassName () {
        return (className);
    }

    public Object           getDefaultValue (Class type) {
        if (type == String.class)
            return (toString ());

        if (type == Scriptable.class)
            return (this);

        return ("[object " + className);
    }

    public boolean          has (String name, Scriptable start) {
        return (false);
    }

    public boolean          has (int i, Scriptable start) {
        return (false);
    }

    /**
     * instanceof operator.
     *
     * We mimic the normal JavaScript instanceof semantics, returning
     * true if <code>this</code> appears in <code>value</code>'start prototype
     * chain.
     */
    public boolean          hasInstance(Scriptable value) {
        Scriptable proto = value.getPrototype();
        while (proto != null) {
            if (proto.equals(this))
                return true;
            proto = proto.getPrototype();
        }

        return false;
    }

    protected boolean       isSpecialProperty (String name) {
        return (name.equals ("constructor"));
    }
    
    public void             put (String name, Scriptable start, Object o) {
        if (!isSpecialProperty (name))
            throw new UnsupportedOperationException ();
    }

    public void             put (int i, Scriptable start, Object o) {
        throw new UnsupportedOperationException ();
    }

    /**
     * Get prototype.
     */
    public Scriptable       getPrototype() {
        return prototype;
    }

    /**
     * Set prototype.
     */
    public void             setPrototype(Scriptable prototype) {
        this.prototype = prototype;
    }

    /**
     * Get parent.
     */
    public Scriptable       getParentScope() {
        return parent;
    }

    /**
     * Set parent.
     */
    public void             setParentScope(Scriptable parent) {
        this.parent = parent;
    }

    /**
     * Get properties.
     *
     * We return an empty array since we define all properties to be DONTENUM.
     */
    public Object []        getIds() {
        return new Object [0];
    }        
}
