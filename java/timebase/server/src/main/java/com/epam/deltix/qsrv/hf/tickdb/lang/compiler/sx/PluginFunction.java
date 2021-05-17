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

import com.epam.deltix.qsrv.hf.tickdb.lang.compiler.sem.FunctionDescriptor;

/**
 *
 */
public class PluginFunction extends CompiledComplexExpression {
    public final FunctionDescriptor fd;

    public PluginFunction (FunctionDescriptor fd, CompiledExpression ... args) {
        super (fd.returnType, args);
        this.fd = fd;
        this.name = toString ();
    }

    @Override
    public boolean                  impliesAggregation () {
        return (fd.aggregate || super.impliesAggregation ());
    }
    
    @Override
    @SuppressWarnings ("EqualsWhichDoesntCheckParameterClass")
    public boolean                  equals (Object obj) {
        return super.equals (obj) && fd.equals (((PluginFunction) obj).fd);
    }

    @Override
    public int                      hashCode () {
        return super.hashCode () + fd.hashCode ();
    }

    @Override
    protected void                  print (StringBuilder out) {
        out.append (fd.info.id ());
        out.append (" (");
        printArgs (out);
        out.append (")");
    }
}
