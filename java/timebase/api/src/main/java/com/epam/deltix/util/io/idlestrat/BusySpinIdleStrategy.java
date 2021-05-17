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
package com.epam.deltix.util.io.idlestrat;

import org.agrona.hints.ThreadHints;

/**
 * Aggressively burns CPU cycles. Use only if you need to get lowest possible latency and can afford dedicated
 * CPU core for each thread that uses this strategy.
 * <p>
 * Try using {@link YieldingIdleStrategy} before trying {@link BusySpinIdleStrategy}.
 *
 * @author Alexei Osipov
 */
public class BusySpinIdleStrategy implements IdleStrategy {
    @Override
    public void idle(int workCount) {
        if (workCount == 0) {
            idle();
        }
    }

    @Override
    public void idle() {
        ThreadHints.onSpinWait();
    }

    @Override
    public void reset() {
    }
}
