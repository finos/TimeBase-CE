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
        // Try to find direct match and use "native" implementation if possible.
        Class<? extends IdleStrategy> sClass = idleStrategy.getClass();
        if (sClass.equals(BusySpinIdleStrategy.class)) {
            return new org.agrona.concurrent.BusySpinIdleStrategy();
        } else if (sClass.equals(YieldingIdleStrategy.class)) {
            return new org.agrona.concurrent.YieldingIdleStrategy();
        } else if (sClass.equals(ArgonaToDeltixStrategyAdapter.class)) {
            return ((ArgonaToDeltixStrategyAdapter) idleStrategy).getWrapped();
        } else {
            // Fallback to proxy-based adapter
            return new DeltixToArgonaStrategyAdapter(idleStrategy);
        }
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