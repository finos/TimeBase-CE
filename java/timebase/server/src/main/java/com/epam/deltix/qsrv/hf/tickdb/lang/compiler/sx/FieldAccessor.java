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
import com.epam.deltix.qsrv.hf.tickdb.lang.compiler.sem.DataFieldRef;

import java.util.Arrays;
import java.util.Objects;

/**
 *
 */
public class FieldAccessor extends CompiledComplexExpression {
    public final DataFieldRef[] fieldRefs;
    public final CompiledExpression<DataType> parent;
    public final DataType slicedType;
    public final boolean fetchNulls;

    public FieldAccessor(
        DataFieldRef[] fieldRefs, DataType outputType, CompiledExpression<DataType> parent, DataType slicedType,
        boolean fetchNulls
    ) {
        super(outputType.nullableInstance(true), parent);

        this.fieldRefs = fieldRefs;
        this.parent = parent;
        this.slicedType = slicedType != null ? slicedType.nullableInstance(true) : null;
        this.fetchNulls = fetchNulls;
        this.name = name();
    }

    private String name() {
        StringBuilder sb = new StringBuilder();
        print(sb);
        return sb.toString();
    }

    @Override
    public void print(StringBuilder out) {
        if (parent != null) {
            if (parent.name != null) {
                out.append(parent.name);
            } else {
                parent.print(out);
            }
            out.append(fetchNulls ? ".?" : ".");
        }

        out.append(getFieldName());
    }

    public String getFieldName() {
        return fieldRefs[0].field.getName();
    }

    @Override
    @SuppressWarnings("EqualsWhichDoesntCheckParameterClass")
    public boolean equals(Object obj) {
        return super.equals(obj) &&
            Arrays.equals(fieldRefs, ((FieldAccessor) obj).fieldRefs) &&
            Objects.equals(slicedType, ((FieldAccessor) obj).slicedType) &&
            fetchNulls == ((FieldAccessor) obj).fetchNulls;
    }

    @Override
    public int hashCode() {
        return super.hashCode() + Arrays.hashCode(fieldRefs) * 31 * 31 + Objects.hashCode(slicedType) * 31 + Objects.hash(fetchNulls);
    }
}