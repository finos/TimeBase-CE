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
package com.epam.deltix.computations.api.generated;

import com.epam.deltix.computations.api.annotations.BuiltInTimestampMs;
import com.epam.deltix.computations.api.annotations.Compute;
import com.epam.deltix.computations.api.annotations.Reset;
import com.epam.deltix.computations.api.annotations.Result;

public abstract class DelegatingObjectToObjectStatefulFunction<T> implements ObjectToObjectStatefulFunction<T> {

    protected ObjectToObjectStatefulFunction<T> delegate;

    @Result
    @Override
    public final T get() {
        return delegate.get();
    }

    @Compute
    @Override
    public final void compute(@BuiltInTimestampMs long timestamp, T v) {
        delegate.compute(timestamp, v);
    }

    @Reset
    @Override
    public final void reset() {
        delegate.reset();
    }
}