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
package com.epam.deltix.util.io.waitstrat;

import com.epam.deltix.util.lang.Changeable;

import java.util.concurrent.locks.LockSupport;

/**
 *
 */
public class SleepingWaitForChangeStrategy implements WaitForChangeStrategy {
    private int                 count = 1000;

    public SleepingWaitForChangeStrategy() {
    }

    public void                 waitFor(Changeable value) {
        while (!value.changed()) {
            if (count > 500) {
                --count;
            } else if (count > 0) {
                --count;
                Thread.yield();
            } else {
                LockSupport.parkNanos(1);
            }
        }

        count = 1000;
    }

    public void                 close() {
    }
}