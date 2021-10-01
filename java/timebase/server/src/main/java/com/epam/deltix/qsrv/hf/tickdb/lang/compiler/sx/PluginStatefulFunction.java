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

import com.epam.deltix.qsrv.hf.tickdb.lang.compiler.sem.functions.StatefulFunctionDescriptor;
import org.apache.commons.lang3.ArrayUtils;

import static com.epam.deltix.qsrv.hf.tickdb.lang.compiler.sem.functions.StatefulFunctionDescriptor.types;

public class PluginStatefulFunction extends CompiledComplexExpression {

    private final StatefulFunctionDescriptor descriptor;
    private final CompiledExpression<?>[] initArgs;
    private final CompiledExpression<?>[] otherArgs;

    public PluginStatefulFunction(StatefulFunctionDescriptor descriptor, CompiledExpression<?>[] initArgs, CompiledExpression<?>[] otherArgs) {
        super(descriptor.returnType(types(initArgs), types(otherArgs)), ArrayUtils.addAll(initArgs, otherArgs));
        this.descriptor = descriptor;
        this.initArgs = initArgs;
        this.otherArgs = otherArgs;
        this.name = toString();
    }

    @Override
    public void print(StringBuilder out) {
        descriptor.print(out, initArgs, otherArgs);
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof PluginStatefulFunction))
            return false;
        return super.equals(obj) && descriptor.equals(((PluginStatefulFunction) obj).descriptor);
    }

    @Override
    public int hashCode() {
        return super.hashCode() + descriptor.hashCode();
    }

    @Override
    public boolean impliesAggregation() {
        return true;
    }

    public StatefulFunctionDescriptor getDescriptor() {
        return descriptor;
    }

    public CompiledExpression<?>[] getInitArgs() {
        return initArgs;
    }

    public CompiledExpression<?>[] getOtherArgs() {
        return otherArgs;
    }
}