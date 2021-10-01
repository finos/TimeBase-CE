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

import com.epam.deltix.qsrv.hf.pub.md.ClassDataType;
import com.epam.deltix.qsrv.hf.pub.md.DataType;
import com.epam.deltix.qsrv.hf.pub.md.RecordClassDescriptor;

import java.util.Arrays;
import java.util.Objects;

/**
 *
 */
public class CastClassType extends CompiledComplexExpression {
    public final CompiledExpression<DataType> parent;
    public final ClassDataType sourceType;
    public final RecordClassDescriptor[] descriptors;

    public CastClassType(CompiledExpression<DataType> parent, ClassDataType sourceType, DataType type) {
        super(type.nullableInstance(true), parent);
        this.parent = parent;
        this.sourceType = sourceType;
        this.descriptors = ((ClassDataType) type).getDescriptors();
    }

    @Override
    public void print(StringBuilder out) {
        out.append("(");
        parent.print(out);
        out.append(" CAST OBJECT(");
        RecordClassDescriptor[] descriptors = ((ClassDataType) type).getDescriptors();
        for (int i = 0; i < descriptors.length; ++i) {
            if (i > 0) {
                out.append(", ");
            }
            out.append(descriptors[i].getName());
        }
        out.append("))");
    }

    @Override
    @SuppressWarnings ("EqualsWhichDoesntCheckParameterClass")
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        CastClassType castClassType = (CastClassType) o;
        return Arrays.equals(descriptors, castClassType.descriptors);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode()) + 31 * Arrays.hashCode(descriptors);
    }
}