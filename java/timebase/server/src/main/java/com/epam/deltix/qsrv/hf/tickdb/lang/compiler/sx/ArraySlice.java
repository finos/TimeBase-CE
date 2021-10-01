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

/**
 *
 */
public class ArraySlice extends CompiledComplexExpression {
    public final CompiledExpression<DataType> selector;
    public final CompiledExpression<DataType> compiledFrom;
    public final CompiledExpression<DataType> compiledTo;
    public final CompiledExpression<DataType> compiledStep;

    public ArraySlice(CompiledExpression<DataType> selector,
                      CompiledExpression<DataType> compiledFrom,
                      CompiledExpression<DataType> compiledTo,
                      CompiledExpression<DataType> compiledStep)
    {
        super(selector.type.nullableInstance(true), selector, compiledFrom, compiledTo, compiledStep);

        this.selector = selector;
        this.compiledFrom = compiledFrom;
        this.compiledTo = compiledTo;
        this.compiledStep = compiledStep;
    }

    @Override
    public void print(StringBuilder out) {
        selector.print(out);
        out.append("[");
        if (compiledFrom != null) {
            compiledFrom.print(out);
        }
        out.append(":");
        if (compiledTo != null) {
            compiledTo.print(out);
        }
        out.append(":");
        if (compiledStep != null) {
            compiledStep.print(out);
        }
        out.append("]");
    }

}