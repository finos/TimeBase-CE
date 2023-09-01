/*
 * Copyright 2023 EPAM Systems, Inc
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
import com.epam.deltix.containers.generated.CharSequenceToBoolHashMap;
import com.epam.deltix.qsrv.hf.codec.cg.StringBuilderPool;
import com.epam.deltix.qsrv.hf.pub.md.TimebaseTypes;
import com.epam.deltix.util.collections.generated.ObjectArrayList;

import javax.annotation.Nonnull;

@Function("COLLECT_UNIQUE")
public class CollectVarcharUnique implements VarcharArrayStatefulFunction {

    protected final ObjectArrayList<CharSequence> buffer = new ObjectArrayList<>();
    protected final CharSequenceToBoolHashMap set = new CharSequenceToBoolHashMap(false);

    protected final StringBuilderPool pool = new StringBuilderPool();
    protected StringBuilder sb;

    @Compute
    public void compute(CharSequence v) {
        if (TimebaseTypes.isNull(v))
            return;
        StringBuilder sb = borrow();
        sb.append(v);
        if (set.trySet(sb, true)) {
            buffer.add(sb);
        } else {
            sb.setLength(0);
            this.sb = sb;
        }
    }

    @Reset
    @Override
    public void reset() {
        pool.reset();
        buffer.clear();
        set.clear();
    }

    @Result
    @Type("ARRAY(VARCHAR?)?")
    @Override
    public ObjectArrayList<CharSequence> get() {
        if (set.isEmpty())
            return null;
        return buffer;
    }

    @Nonnull
    protected StringBuilder borrow() {
        if (sb == null) {
            return pool.borrow();
        } else {
            StringBuilder temp = sb;
            sb = null;
            return temp;
        }
    }
}