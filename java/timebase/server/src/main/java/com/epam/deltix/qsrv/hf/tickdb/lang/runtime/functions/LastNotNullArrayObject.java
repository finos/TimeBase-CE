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

package com.epam.deltix.qsrv.hf.tickdb.lang.runtime.functions;

import com.epam.deltix.computations.api.annotations.*;
import com.epam.deltix.qsrv.hf.tickdb.lang.runtime.selectors.Instance;
import com.epam.deltix.qsrv.hf.tickdb.lang.runtime.selectors.InstancePool;
import com.epam.deltix.util.collections.generated.ObjectArrayList;

@Function("LASTNOTNULL")
@GenericParameter(name = "T")
public class LastNotNullArrayObject implements ObjectArrayStatefulFunction {


    protected ObjectArrayList<Instance> value = null;
    protected final ObjectArrayList<Instance> buffer = new ObjectArrayList<>();

    private final InstancePool instancePool = new InstancePool();

    @Compute
    public void compute(@Type("ARRAY(T?)?") ObjectArrayList<Instance> v) {
        if (v == null || v.size() == 0) {
            return;
        }

        buffer.clear();
        instancePool.reset();
        for (int i = 0; i < v.size(); ++i) {
            Instance instance = instancePool.borrow();
            instance.copyFrom(v.get(i));
            buffer.add(instance);
        }

        value = buffer;
    }

    @Result
    @Type("ARRAY(T?)?")
    @Override
    public ObjectArrayList<Instance> get() {
        return value;
    }

    @Override
    @Reset
    public void reset() {
    }
}
