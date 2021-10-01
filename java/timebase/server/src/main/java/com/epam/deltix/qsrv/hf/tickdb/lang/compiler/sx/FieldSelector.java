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

/**
 *
 */
public class FieldSelector extends CompiledExpression<DataType> {
    public final DataFieldRef fieldRef;

    public FieldSelector(DataFieldRef fieldRef) {
        super(fieldRef.field.getType().nullableInstance(true));

        this.fieldRef = fieldRef;
        this.name = name();
    }

    private String name() {
        StringBuilder sb = new StringBuilder();
        print(sb);

        return sb.toString();
    }

    @Override
    public void print(StringBuilder out) {
        out.append(fieldRef.field.getName());
    }

    @Override
    @SuppressWarnings("EqualsWhichDoesntCheckParameterClass")
    public boolean equals(Object obj) {
        return super.equals(obj) &&
            fieldRef.equals(((FieldSelector) obj).fieldRef);
    }

    @Override
    public int hashCode() {
        return super.hashCode() + fieldRef.hashCode();
    }

}