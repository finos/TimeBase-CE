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
package com.epam.deltix.computations.stateful.collect;

import com.epam.deltix.computations.api.annotations.Compute;
import com.epam.deltix.computations.api.annotations.Function;
import com.epam.deltix.computations.api.annotations.Reset;
import com.epam.deltix.computations.api.annotations.Result;
import com.epam.deltix.computations.api.annotations.Type;
import com.epam.deltix.computations.api.generated.arrays.VarcharArrayStatefulFunction;
import com.epam.deltix.qsrv.hf.codec.cg.StringBuilderPool;
import com.epam.deltix.qsrv.hf.pub.md.TimebaseTypes;
import com.epam.deltix.util.collections.generated.ObjectArrayList;

@Function("COLLECT")
public class CollectVarchar implements VarcharArrayStatefulFunction {

    protected ObjectArrayList<CharSequence> value = null;
    protected final ObjectArrayList<CharSequence> buffer = new ObjectArrayList<>();

    protected final StringBuilderPool pool = new StringBuilderPool();

    @Compute
    public void compute(CharSequence v) {
        if (TimebaseTypes.isNull(v))
            return;
        if (value == null) {
            value = buffer;
        }
        StringBuilder sb = pool.borrow();
        sb.append(v);
        buffer.add(sb);
    }

    @Reset
    @Override
    public void reset() {
        pool.reset();
        value = null;
        buffer.clear();
    }

    @Result
    @Type("ARRAY(VARCHAR?)?")
    @Override
    public ObjectArrayList<CharSequence> get() {
        return value;
    }
}
