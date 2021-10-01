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

import com.epam.deltix.qsrv.hf.pub.md.ArrayDataType;
import com.epam.deltix.qsrv.hf.pub.md.DataType;

/**
 *
 */
public class ArrayJoinElement extends CompiledComplexExpression {

    public final CompiledExpression<DataType> arrayJoinExpression;
    public final boolean left;

    public ArrayJoinElement(CompiledExpression<DataType> arrayJoinExpression, String name, boolean left) {
        super(((ArrayDataType) arrayJoinExpression.type).getElementDataType().nullableInstance(true), arrayJoinExpression);

        this.arrayJoinExpression = arrayJoinExpression;
        this.name = name;
        this.left = left;
    }

    @Override
    public void print(StringBuilder out) {
        arrayJoinExpression.print(out);
    }

    @Override
    @SuppressWarnings("EqualsWhichDoesntCheckParameterClass")
    public boolean equals(Object obj) {
        return super.equals(obj) &&
            left == ((ArrayJoinElement) obj).left;
    }

    @Override
    public int hashCode() {
        return super.hashCode() + Boolean.hashCode(left);
    }
}