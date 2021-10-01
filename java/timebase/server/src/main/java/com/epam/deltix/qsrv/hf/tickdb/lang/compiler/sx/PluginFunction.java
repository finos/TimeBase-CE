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

import com.epam.deltix.qsrv.hf.tickdb.lang.compiler.sem.functions.FunctionInfoDescriptor;

/**
 *
 */
public class PluginFunction extends CompiledComplexExpression {
    public final FunctionInfoDescriptor fd;

    public PluginFunction(FunctionInfoDescriptor fd, CompiledExpression<?> ... args) {
        super(fd.returnType(), args);
        this.fd = fd;
        this.name = toString();
    }

    @Override
    public boolean impliesAggregation() {
        return (fd.isAggregate() || super.impliesAggregation());
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof PluginFunction))
            return false;
        return super.equals(obj) && fd.equals(((PluginFunction) obj).fd);
    }

    @Override
    public int hashCode() {
        return super.hashCode() + fd.hashCode();
    }

    @Override
    public void print(StringBuilder out) {
        out.append(fd.id());
        out.append(" (");
        printArgs(out);
        out.append(")");
    }
}