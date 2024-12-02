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
package com.epam.deltix.qsrv.hf.tickdb.pub.lock;

import java.time.Instant;
import java.util.Objects;

public class WriteLockOptionsImpl extends LockOptionsImpl implements WriteLockOptions {
    private final long startTime;
    private final long endTime;
    private final boolean isRanged;

    protected WriteLockOptionsImpl() {
        this(Long.MIN_VALUE, Long.MAX_VALUE);
    }

    protected WriteLockOptionsImpl(long startTime, long endTime) {
        super(LockType.WRITE);

        if (startTime > endTime) {
            throw new IllegalArgumentException("Invalid Data Lock timestamps (start > end)");
        }

        this.startTime = startTime;
        this.endTime = endTime;
        this.isRanged = (startTime != Long.MIN_VALUE || endTime != Long.MAX_VALUE);
    }

    @Override
    public long getStartTime() {
        return startTime;
    }

    @Override
    public long getEndTime() {
        return endTime;
    }

    @Override
    public boolean isRanged() {
        return isRanged;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        WriteLockOptionsImpl that = (WriteLockOptionsImpl) o;
        return startTime == that.startTime && endTime == that.endTime;
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), startTime, endTime);
    }

    @Override
    public String toString() {
        return super.toString() + " (" +
            (startTime == Long.MIN_VALUE ? "-Inf" : Instant.ofEpochMilli(startTime)) + " -> " +
            (endTime == Long.MAX_VALUE ? "+Inf" : Instant.ofEpochMilli(endTime)) + ")";
    }
}