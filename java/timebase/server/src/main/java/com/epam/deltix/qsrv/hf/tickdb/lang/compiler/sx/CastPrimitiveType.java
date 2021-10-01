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
import com.epam.deltix.qsrv.hf.tickdb.lang.compiler.sem.NumericType;

import java.util.Objects;

/**
 *
 */
public class CastPrimitiveType extends CompiledComplexExpression {

    private static class TypeDefinition {
        private final String typeName;
        private final String encoding;
        private final boolean nullable;

        public TypeDefinition(String typeName, String encoding, boolean nullable) {
            this.typeName = typeName;
            this.encoding = encoding;
            this.nullable = nullable;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            TypeDefinition that = (TypeDefinition) o;
            return nullable == that.nullable &&
                Objects.equals(typeName, that.typeName) &&
                Objects.equals(encoding, that.encoding);
        }

        @Override
        public int hashCode() {
            return Objects.hash(typeName, encoding, nullable);
        }
    }

    public final CompiledExpression<DataType> parent;
    public final DataType sourceType;
    public final TypeDefinition typeDefinition;
    public final NumericType sourceNumeric;
    public final NumericType targetNumeric;
    public final boolean array;

    public CastPrimitiveType(CompiledExpression<DataType> parent,
                             DataType sourceType, DataType type,
                             NumericType sourceNumeric, NumericType targetNumeric,
                             boolean array)
    {
        super(type, parent);
        this.parent = parent;
        this.sourceType = sourceType;
        if (array) {
            DataType elementType = ((ArrayDataType) type).getElementDataType();
            this.typeDefinition = new TypeDefinition(elementType.getBaseName(), elementType.getEncoding(), elementType.isNullable());
        } else {
            this.typeDefinition = new TypeDefinition(type.getBaseName(), type.getEncoding(), type.isNullable());
        }
        this.sourceNumeric = sourceNumeric;
        this.targetNumeric = targetNumeric;
        this.array = array;
    }

    @Override
    public void print(StringBuilder out) {
        out.append("(");
        parent.print(out);
        out.append(" CAST ");
        if (array) {
            out.append("ARRAY(");
        }
        out.append(type.getBaseName());
        if (type.getEncoding() != null) {
            out.append(" ");
            out.append(type.getEncoding());
        }
        if (array) {
            out.append(")");
        }
        out.append(")");
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        CastPrimitiveType that = (CastPrimitiveType) o;
        return array == that.array &&
            Objects.equals(typeDefinition, that.typeDefinition);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), typeDefinition, array);
    }
}