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
import com.epam.deltix.qsrv.hf.pub.md.IntegerDataType;

import java.util.Objects;

public class PredicateFunction extends CompiledExpression<DataType> {

    public enum FunctionName {
        POSITION,
        LAST
    }

    public final PredicateIterator context;
    public final FunctionName functionName;

    public PredicateFunction(PredicateIterator context, FunctionName functionName) {
        super(getFunctionType(functionName));
        this.context = context;
        this.functionName = functionName;
    }

    private static DataType getFunctionType(FunctionName functionName) {
        return new IntegerDataType(IntegerDataType.ENCODING_INT32, false);
    }

    @Override
    public void print(StringBuilder out) {
        out.append(functionName.toString()).append("()");
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        PredicateFunction that = (PredicateFunction) o;
        return Objects.equals(context, that.context) &&
            functionName == that.functionName;
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), context, functionName);
    }
}