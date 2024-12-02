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

import com.epam.deltix.util.collections.generated.LongArrayList;

public class WindowTimestampMove implements WindowMoveStrategy {

    private final long timePeriod;
    private final LongArrayList timestamps = new LongArrayList();

    public WindowTimestampMove(long timePeriod) {
        if (timePeriod <= 0) {
            throw new IllegalArgumentException("Invalid time period: " + timePeriod);
        }

        this.timePeriod = timePeriod;
    }

    @Override
    public int addAndMove(long timestamp) {
        int index = 0;
        timestamps.add(timestamp);
        long startInterval = timestamp - timePeriod;
        while ((timestamps.get(index)) < startInterval) {
            index++;
            if (index >= timestamps.size()) {
                index = timestamps.size();
                break;
            }
        }

        if (index > 0) {
            timestamps.removeRange(0, index);
        }

        return index;
    }

    @Override
    public void clear() {
        timestamps.clear();
    }

}