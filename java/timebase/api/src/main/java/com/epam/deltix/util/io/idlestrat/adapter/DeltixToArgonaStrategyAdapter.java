/*
 * Copyright 2024 EPAM Systems, Inc
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
package com.epam.deltix.util.io.idlestrat.adapter;

import com.epam.deltix.util.io.idlestrat.BusySpinIdleStrategy;
import com.epam.deltix.util.io.idlestrat.IdleStrategy;
import com.epam.deltix.util.io.idlestrat.YieldingIdleStrategy;

import javax.annotation.ParametersAreNonnullByDefault;

/**
 * Adapts {@link deltix.util.io.idlestrat.IdleStrategy} to {@link org.agrona.concurrent.IdleStrategy} interface.
 *
 * @author Alexei Osipov
 */
@ParametersAreNonnullByDefault
public class DeltixToArgonaStrategyAdapter implements org.agrona.concurrent.IdleStrategy {
    private final IdleStrategy wrapped;

    static org.agrona.concurrent.IdleStrategy adapt(IdleStrategy idleStrategy) {
        return DeltixToAgronaStrategyAdapter.adapt(idleStrategy);
    }

    IdleStrategy getWrapped() {
        return wrapped;
    }

    private DeltixToArgonaStrategyAdapter(IdleStrategy wrapped) {
        this.wrapped = wrapped;
    }

    @Override
    public void idle(int workCount) {
        wrapped.idle(workCount);
    }

    @Override
    public void idle() {
        wrapped.idle();
    }

    @Override
    public void reset() {
        wrapped.reset();
    }
}