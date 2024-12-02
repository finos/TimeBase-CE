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
package com.epam.deltix.computations.stateful.window.containers;

import java.util.AbstractList;

public class WindowCountMove implements WindowMoveStrategy {

    private final AbstractList<?> values;
    private final int period;

    public WindowCountMove(AbstractList<?> values, int period) {
        if (period <= 0) {
            throw new IllegalArgumentException("Invalid period: " + period);
        }

        this.values = values;
        this.period = period;
    }

    @Override
    public int addAndMove(long timestamp) {
        return values.size() > period ? 1  : 0;
    }

    @Override
    public void clear() {
    }

}