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

import com.epam.deltix.qsrv.hf.pub.md.ClassDescriptor;
import com.epam.deltix.qsrv.hf.pub.md.QueryDataType;

import java.util.Arrays;
import java.util.Objects;
import java.util.Set;

public class CompiledUnion extends CompiledQuery {

    public final boolean isForward;
    public final CompiledQuery[] queries;
    public final SelectLimit limit;

    public CompiledUnion(QueryDataType type, boolean isForward, SelectLimit limit, CompiledQuery... queries) {
        super(type);

        this.isForward = isForward;
        this.limit = limit;
        this.queries = queries;
    }

    @Override
    public boolean isForward() {
        return isForward;
    }

    @Override
    public void getAllTypes(Set<ClassDescriptor> out) {

    }

    @Override
    public void print(StringBuilder out) {
        for (int i = 0; i < queries.length; ++i) {
            if (i > 0) {
                out.append(" UNION ");
            }

            queries[i].print(out);
        }
    }


    @Override
    @SuppressWarnings("EqualsWhichDoesntCheckParameterClass")
    public boolean equals(Object obj) {
        return super.equals(obj) &&
            Arrays.equals(queries, ((CompiledUnion) obj).queries)&&
            Objects.equals(limit, ((CompiledUnion) obj).limit);
    }

    @Override
    public int hashCode() {
        return super.hashCode() + Arrays.hashCode(queries) * 31 + Objects.hashCode(limit);
    }

}