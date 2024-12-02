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
import java.util.function.Supplier;

public abstract class WindowArrayList<T extends AbstractList<?>> {

    private final T values;

    private final WindowMoveStrategy windowMove;

    public WindowArrayList(int period, Supplier<T> supplier) {
        this.values = supplier.get();
        this.windowMove = new WindowCountMove(values, period);
    }

    public WindowArrayList(long timePeriod, Supplier<T> supplier) {
        this.values = supplier.get();
        this.windowMove = new WindowTimestampMove(timePeriod);
    }

    public T getValues() {
        return values;
    }

    public int size() {
        return values.size();
    }

    public void clear() {
        values.clear();
        windowMove.clear();
    }

    protected void addAndMove(long timestamp) {
        int index = windowMove.addAndMove(timestamp);
        if (index > 0) {
            move(index);
        }
    }

    protected abstract void move(int end);

}