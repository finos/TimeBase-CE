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
package com.epam.deltix.qsrv.hf.tickdb.lang.compiler.sx;

import com.epam.deltix.qsrv.hf.pub.md.*;

/**
 *
 */
public final class CompiledConstant extends CompiledExpression <DataType> {
    public static final CompiledConstant    B_False =
        new CompiledConstant (
            StandardTypes.CLEAN_BOOLEAN,
            false
        );

    public static final CompiledConstant    B_True =
        new CompiledConstant (
            StandardTypes.CLEAN_BOOLEAN,
            true
        );

    public static CompiledConstant  trueOrFalse (boolean v) {
        return (v ? B_True : B_False);
    }

    public final Object         value;

    public CompiledConstant (DataType type, Object value) {
        super (type);
        this.value = value;
    }

    public CompiledConstant (DataType type, Object value, String name) {
        super (type);
        this.value = value;
        this.name = name;
    }

    @Override
    protected void                  print (StringBuilder out) {
        out.append (value);
    }

    public boolean                  isNull () {
        return (value == null);
    }

    public long                     getLong () {
        return ((Number) value).longValue ();
    }

    public double                   getDouble () {
        return ((Number) value).doubleValue ();
    }

    public float                    getFloat () {
        return ((Number) value).floatValue ();
    }

    public boolean                  getBoolean () {
        return (value != null && ((Boolean) value).booleanValue ());
    }

    public char                     getChar () {
        return ((Character) value);
    }

    public String                   getString () {
        return (value.toString ());
    }

    @Override
    @SuppressWarnings ("EqualsWhichDoesntCheckParameterClass")
    public boolean                  equals (Object obj) {
        if (!super.equals (obj))
            return (false);
        
        CompiledConstant    b = (CompiledConstant) obj;
        
        if (value == null)
            return (b.value == null);
        
        return (value.equals (b.value));
    }

    @Override
    public int                      hashCode () {
        return super.hashCode () + (value == null ? 772455 : value.hashCode ());
    }
}
