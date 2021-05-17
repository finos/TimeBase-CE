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

import com.epam.deltix.qsrv.hf.pub.md.DataType;
import java.util.Arrays;

/**
 *
 */
public abstract class CompiledComplexExpression 
    extends CompiledExpression <DataType>
{
    public final CompiledExpression []    args;

    public CompiledComplexExpression (DataType type, CompiledExpression ... args) {
        super (type);
        this.args = args;
    }

    @Override
    public boolean                  impliesAggregation () {
        if (args != null)
            for (CompiledExpression arg : args)
                if (arg != null && arg.impliesAggregation ())
                    return (true);
        
        return (super.impliesAggregation ());
    }
    
    @Override
    @SuppressWarnings ("EqualsWhichDoesntCheckParameterClass")
    public boolean                  equals (Object obj) {
        return
            super.equals (obj) &&
            Arrays.equals (args, ((CompiledComplexExpression) obj).args);
    }

    @Override
    public int                      hashCode () {
        return super.hashCode () + Arrays.hashCode (args);
    }

    protected final void                        printArgs (StringBuilder out) {
        printArgs (out, 0);
    }
    
    protected final void                        printArgs (StringBuilder out, int idx) {
        if (args.length > idx) {
            args [idx++].print (out);

            for (; idx < args.length; idx++) {
                out.append (", ");
                args [idx].print (out);
            }
        }
    }
}
